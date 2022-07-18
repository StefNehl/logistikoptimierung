package logistikoptimierung.Services.EnumeratedCalculation;

import logistikoptimierung.Contracts.IOptimizationService;
import logistikoptimierung.Entities.FactoryObjects.*;
import logistikoptimierung.Entities.WarehouseItems.Material;
import logistikoptimierung.Entities.WarehouseItems.MaterialPosition;
import logistikoptimierung.Entities.WarehouseItems.Order;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.*;

public class EnumeratedCalculationMain implements IOptimizationService
{
    private final Factory factory;
    private List<ProductionPlanningItem> productionItem;
    private List<MaterialPosition> acquiringPlanningItems;
    private List<MaterialPosition> deliverPlanningItems;
    private List<PlanningItem> allPlanningItems;
    private FactoryMessageSettings factoryMessageSettings;

    private List<Transporter> sortedAvailableTransportList;
    private List<Driver> availableDrivers;
    private List<DriverPoolItem> driverPoolItems;

    private long bestTimeSolution = Long.MAX_VALUE;
    private List<FactoryStep> bestSolution = new ArrayList<>();



    public EnumeratedCalculationMain(Factory factory, FactoryMessageSettings factoryMessageSettings)
    {
        this.factory = factory;
        this.productionItem = new ArrayList<>();
        this.acquiringPlanningItems = new ArrayList<>();
        this.deliverPlanningItems = new ArrayList<>();
        this.allPlanningItems = new ArrayList<>();
        this.factoryMessageSettings = factoryMessageSettings;

        this.availableDrivers = new ArrayList<>(this.factory.getDrivers());
        this.sortedAvailableTransportList = new ArrayList<>(this.factory.getTransporters());
        this.sortedAvailableTransportList.sort(Comparator.comparingInt(Transporter::getCapacity));
        this.driverPoolItems = new ArrayList<>(this.factory.getNrOfDrivers());
    }

    @Override
    public List<FactoryStep> optimize(List<Order> orderList, int nrOfOrdersToOptimize)
    {
        var subOrderList = new ArrayList<Order>();

        for(int i = 0; i < nrOfOrdersToOptimize; i++)
        {
            subOrderList.add(orderList.get(i));
        }

        getAllNeededFactoryPlanningItemsForOrder(subOrderList);

        var stepToDo = new ArrayList<FactoryStep>();
        getPlanningSolutionRecursive(stepToDo, this.allPlanningItems);

        return bestSolution;
    }

    private void getPlanningSolutionRecursive(List<FactoryStep> stepsToDo, List<PlanningItem> planningItems)
    {
        if(planningItems.isEmpty())
        {
            var result = this.factory.startFactory(stepsToDo, 10000, factoryMessageSettings);
            this.factory.resetFactory();
            if(result < bestTimeSolution) {
                bestTimeSolution = result;
                bestSolution = new ArrayList<>(stepsToDo);
            }
        }

        for (var planningItem : planningItems)
        {
            var stepsToAdd = new ArrayList<FactoryStep>();
            switch (planningItem.planningType())
            {
                case Acquire -> {
                    stepsToAdd = getAcquireTransportSteps(planningItem);
                }
                case Deliver -> {
                    stepsToAdd = getDeliverTransporters(stepsToDo, planningItem);
                }
                case Produce -> {
                    stepsToAdd = getProductionSteps(stepsToDo, planningItem);
                }
            }

            //abort complete solution because no material for the production in the warehouse
            if(stepsToAdd.isEmpty())
                return;

            stepsToDo.addAll(stepsToAdd);
            var copyOfSteps = new ArrayList<>(planningItems);
            copyOfSteps.remove(planningItem);
            getPlanningSolutionRecursive(stepsToDo, copyOfSteps);
            stepsToDo.removeAll(stepsToAdd);

            //Release driver
            if(planningItem.planningType() == PlanningType.Acquire ||
                    planningItem.planningType() == PlanningType.Deliver)
            {
                var poolItem = this.driverPoolItems.remove(this.driverPoolItems.size() - 1);
                this.availableDrivers.add(poolItem.driver());
                this.sortedAvailableTransportList.add(poolItem.transporter());
                this.sortedAvailableTransportList.sort(Comparator.comparingInt(Transporter::getCapacity));
            }
        }
    }

    private ArrayList<FactoryStep> getDeliverTransporters(List<FactoryStep> stepsToDo, PlanningItem planningItem)
    {
        var steps = new ArrayList<FactoryStep>();
        var amount = planningItem.amount();

        //Check if product is actually in the warehouse
        var amountOfProductInWarehouse = 0;
        var order = (Order)planningItem.item();

        for(var step : stepsToDo)
        {
            if(step.getStepType().equals(FactoryStepTypes.Produce))
            {
                var productPosition = order.getProduct();

                if(step.getItemToManipulate().getName()
                        .equals(productPosition.item().getName()))
                {
                    var production = (Production)step.getFactoryObject();
                    var process = production.getProductionProcessForProduct(productPosition.item());
                    if(process == null)
                        continue;

                    amountOfProductInWarehouse += process.getProductionBatchSize();
                }
            }

            if(step.getStepType().equals(FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse))
            {
                amountOfProductInWarehouse += step.getAmountOfItems();
            }

        }

        if(amountOfProductInWarehouse < order.getProduct().amount())
            return steps;

        if(this.availableDrivers.isEmpty())
        {
            var poolItem = this.driverPoolItems.remove(0);
            this.availableDrivers.add(poolItem.driver());
            this.sortedAvailableTransportList.add(poolItem.transporter());
            this.sortedAvailableTransportList.sort(Comparator.comparingInt(Transporter::getCapacity));
        }

        Transporter lastTransport = null;
        var lastAmountToTransport = 0;
        while (amount >= 0)
        {
            var bestTransporter = this.getTransporterWithSmallestDifferenceFromAmountAndHigherCapacity(
                    this.sortedAvailableTransportList,
                    planningItem.item(), amount);

            if(bestTransporter == null)
                throw new RuntimeException("Should not happen ");

            var amountToTransport = 0;
            if(amount < bestTransporter.getCapacity())
                amountToTransport = amount;
            else
                amountToTransport = bestTransporter.getCapacity();

            var newStep = new FactoryStep(this.factory, 0,
                    planningItem.item().getName(),
                    amountToTransport,
                    bestTransporter.getName(),
                    FactoryStepTypes.ConcludeOrderTransportToCustomer);

            steps.add(newStep);

            var newDriver = this.availableDrivers.get(0);
            var newPoolItem = new DriverPoolItem(newDriver, bestTransporter);
            driverPoolItems.add(newPoolItem);
            amount -= bestTransporter.getCapacity();

            lastTransport  = bestTransporter;
            lastAmountToTransport = amountToTransport;

        }

        var newStep = new FactoryStep(this.factory, 0,
                planningItem.item().getName(),
                lastAmountToTransport,
                lastTransport.getName(),
                FactoryStepTypes.TransporterClosesOrderFromCustomer);

        steps.add(newStep);

        return steps;
    }

    private ArrayList<FactoryStep> getAcquireTransportSteps(PlanningItem planningItem)
    {
        var steps = new ArrayList<FactoryStep>();
        var amount = planningItem.amount();

        //ToDo Check if warehouse is full (SumUp every acquiring minus every production and delivery)

        if(this.availableDrivers.isEmpty())
        {
            var poolItem = this.driverPoolItems.remove(0);
            this.availableDrivers.add(poolItem.driver());
            this.sortedAvailableTransportList.add(poolItem.transporter());
        }

        while (amount >= 0)
        {
            var bestTransporter = this.getTransporterWithSmallestDifferenceFromAmountAndHigherCapacity(
                    this.sortedAvailableTransportList,
                    planningItem.item(),
                    amount);

            //No Transporter found, need to reuse already used transporter
            if(bestTransporter == null)
            {
                var usedTransporters = new ArrayList<Transporter>();
                for(var driverPoolItem : driverPoolItems)
                    usedTransporters.add(driverPoolItem.transporter());

                bestTransporter = this.getTransporterWithSmallestDifferenceFromAmountAndHigherCapacity(
                        usedTransporters,
                        planningItem.item(),
                        amount
                );
            }

            var amountToTransport = 0;
            if(amount < bestTransporter.getCapacity())
                amountToTransport = amount;
            else
                amountToTransport = bestTransporter.getCapacity();

            var newStep = new FactoryStep(this.factory, 0,
                    planningItem.item().getName(),
                    amountToTransport,
                    bestTransporter.getName(),
                    FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse);

            steps.add(newStep);

            newStep = new FactoryStep(this.factory, 0,
                    planningItem.item().getName(),
                    amountToTransport,
                    bestTransporter.getName(),
                    FactoryStepTypes.MoveMaterialFromTransporterToWarehouse);

            steps.add(newStep);

            var newDriver = this.availableDrivers.remove(0);
            this.sortedAvailableTransportList.remove(bestTransporter);
            var newPoolItem = new DriverPoolItem(newDriver, bestTransporter);
            driverPoolItems.add(newPoolItem);
            amount -= bestTransporter.getCapacity();

        }

        return steps;
    }

    private Transporter getTransporterWithSmallestDifferenceFromAmountAndHigherCapacity(List<Transporter> availableTransporters,
                                                                                        WarehouseItem item, int amount)
    {
        Transporter fittingTransporterWithHighestCapacity = null;

        for(var transporter : availableTransporters)
        {
            if(item instanceof Material)
            {
                if(!transporter.areTransportationConstraintsFulfilledForMaterial((Material) item))
                    continue;
            }
            else if(item instanceof  Order)
            {
                if(!transporter.areTransportationConstraintsFulfilledForOrder((Order) item))
                    continue;
            }

            if(transporter.getCapacity() > amount)
                return transporter;

            if(fittingTransporterWithHighestCapacity == null ||
                    fittingTransporterWithHighestCapacity.getCapacity() < transporter.getCapacity())
                fittingTransporterWithHighestCapacity = transporter;
        }

        if(fittingTransporterWithHighestCapacity != null)
            return fittingTransporterWithHighestCapacity;

        return null;
    }

    private ArrayList<FactoryStep> getProductionSteps(List<FactoryStep> stepsToDo, PlanningItem planningItem)
    {
        var steps = new ArrayList<FactoryStep>();
        var process = this
                .factory
                .getProductionProcessForWarehouseItem(planningItem.item());

        //Check if material is actually in the warehouse
        for (var materialPosition : process.getMaterialPositions())
        {
            var isMaterialAcquired = false;
            for(var step : stepsToDo)
            {
                if(step.getItemToManipulate().getName().equals(materialPosition.item().getName()) &&
                    step.getAmountOfItems() >= materialPosition.amount())
                {
                    isMaterialAcquired = true;
                    break;
                }
            }

            if(!isMaterialAcquired)
                return steps;
        }

        var newStep = new FactoryStep(this.factory, 0,
                planningItem.item().getName(),
                1,
                process.getProduction().getName(),
                FactoryStepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer);

        steps.add(newStep);

        newStep = new FactoryStep(this.factory, 0,
                planningItem.item().getName(),
                1,
                process.getProduction().getName(),
                FactoryStepTypes.Produce);

        steps.add(newStep);

        newStep = new FactoryStep(this.factory, 0,
                planningItem.item().getName(),
                1,
                process.getProduction().getName(),
                FactoryStepTypes.MoveProductToOutputBuffer);

        steps.add(newStep);

        newStep = new FactoryStep(this.factory, 0,
                planningItem.item().getName(),
                planningItem.amount(),
                process.getProduction().getName(),
                FactoryStepTypes.MoveProductFromOutputBufferToWarehouse);

        steps.add(newStep);

        return steps;
    }

    private void getAllNeededFactoryPlanningItemsForOrder(List<Order> subOrderList)
    {
        createProcessList(subOrderList);
        setProcessDepthForPlanningItems();
        removeDoubleEntriesFromPlaningItemList();
        getProcessesForEveryBatchAndOrderAfterDepth(subOrderList);
        addAcquiringPlaningItemsForEveryBatch();
        addDeliveryPlanningItems(subOrderList);

        for(var item : this.acquiringPlanningItems)
            this.allPlanningItems.add(new PlanningItem(item.item(), item.amount(), PlanningType.Acquire));

        for (var item : this.getFlatProcessList())
            this.allPlanningItems.add(new PlanningItem(item.getProcess().getProductToProduce(),
                    1,
                    PlanningType.Produce));

        for(var item : this.deliverPlanningItems)
            this.allPlanningItems.add(new PlanningItem(item.item(), item.amount(), PlanningType.Deliver));
    }

    private void createProcessList(List<Order> orderList)
    {
        for (var production : this.factory.getProductions())
        {
            productionItem.add(new ProductionPlanningItem(production));
        }

        var orderCount = 1;
        for (var order : orderList)
        {
            var processes = this.factory.getProductionProcessesForProduct(
                    order.getProduct().item());

            var filteredProcesses = new ArrayList<ProductionProcess>();
            for(var process : processes)
            {
                if(this.factory.checkIfItemHasASupplier(process.getProductToProduce()))
                    continue;
                filteredProcesses.add(process);
            }

            for (var process : filteredProcesses)
            {
                var planningItem = getProductionPlanningItemForProcess(process);
                planningItem.getProcessPlanningItems()
                        .add(new ProcessPlaningItem(process, orderCount));
            }
            orderCount++;
        }
    }

    private ProductionPlanningItem getProductionPlanningItemForProcess(ProductionProcess process)
    {
        for (var item : this.productionItem)
        {
            var prodItem = item.getProduction().getProductionProcessForProduct(
                    process.getProductToProduce());
            if(prodItem != null)
                return item;
        }

        return null;
    }

    private void setProcessDepthForPlanningItems()
    {
        for(var planingItem : this.getFlatProcessList())
        {
            var depth = this.getProcessDepthRecursive(planingItem.getProcess().getProductToProduce());
            planingItem.setProcessDepth(depth);
        }

    }

    private int getProcessDepthRecursive(WarehouseItem item)
    {
        if(this.factory.checkIfItemHasASupplier(item))
            return 0;

        var process = this.factory.getProductionProcessForWarehouseItem(item);
        for (var position : process.getMaterialPositions())
        {
            return getProcessDepthRecursive(position.item()) + 1;
        }
        return 0;
    }

    private void getProcessesForEveryBatchAndOrderAfterDepth(List<Order> orderList)
    {
        var flatProcessList = getFlatProcessList();
        flatProcessList.sort(Comparator.comparingInt(ProcessPlaningItem::getProcessDepth));

        var newProcessPlaningItemList = new ArrayList<ProcessPlaningItem>();

        for(int i = flatProcessList.size() - 1; i >= 0; i--)
        {
            var planningItem = flatProcessList.get(i);
            var amountToProduce = 0;

            var parentPlanningItems = getParentProcessesPlanningItemFromProduct(planningItem
                            .getProcess()
                            .getProductToProduce(),
                    newProcessPlaningItemList);

            var orderMap = new HashMap<Integer, Integer>();

            for (var parentItem : parentPlanningItems)
            {
                var amountForParentItem = parentItem.getProcess().getAmountFromMaterialPositions(
                        planningItem.getProcess().getProductToProduce());
                amountToProduce += amountForParentItem;
                orderMap.put(parentItem.getOrderNr(), amountForParentItem);
            }

            for(var order : orderList)
            {
                if(planningItem.getProcess().getProductToProduce().equals(order.getProduct().item()))
                {
                    amountToProduce += order.getProduct().amount();
                    orderMap.put(order.getOrderNr(), order.getProduct().amount());
                }
            }

            var nrOfBatches = (int)Math.ceil((double) amountToProduce / (double) planningItem.getProcess().getProductionBatchSize());

            var batchCount = 0;
            for(var orderKey : orderMap.keySet())
            {
                var orderAmount = orderMap.get(orderKey);
                var amountFromBatch = 0;
                while (orderAmount > amountFromBatch )
                {
                    if(batchCount == nrOfBatches)
                        break;

                    var batchSize = planningItem.getProcess().getProductionBatchSize();
                    var newPlaningItem = new ProcessPlaningItem(planningItem.getProcess(), orderKey);
                    newPlaningItem.setProcessDepth(planningItem.getProcessDepth());
                    newProcessPlaningItemList.add(newPlaningItem);
                    amountFromBatch += batchSize;
                    batchCount++;
                }
            }
        }

        //merge new planing list to old one
        for(var production : productionItem)
        {
            production.getProcessPlanningItems().clear();
            for(var newProcessItem : newProcessPlaningItemList)
            {
                if(newProcessItem.getProcess().getProduction().equals(production.getProduction()))
                    production.getProcessPlanningItems().add(newProcessItem);
            }
        }
    }

    private List<ProcessPlaningItem> getParentProcessesPlanningItemFromProduct(WarehouseItem warehouseItem, List<ProcessPlaningItem> flatList)
    {
        var parentProcesses = new ArrayList<ProcessPlaningItem>();
        for (var planningItem : flatList)
        {
            var bom = planningItem.getProcess().getMaterialPositions();
            for(var item : bom)
            {
                if(item.item().equals(warehouseItem))
                    parentProcesses.add(planningItem);
            }
        }

        return parentProcesses;
    }

    private List<ProcessPlaningItem> getFlatProcessList()
    {
        var result = new ArrayList<ProcessPlaningItem>();

        for (var production : this.productionItem)
        {
            for (var process : production.getProcessPlanningItems())
            {
                result.add(process);
            }
        }
        return result;
    }

    private void removeDoubleEntriesFromPlaningItemList()
    {
        for(var production : this.productionItem)
        {
            var hashSet = new HashSet<ProcessPlaningItem>(production.getProcessPlanningItems());
            production.getProcessPlanningItems().clear();
            production.getProcessPlanningItems().addAll(hashSet);
            production.getProcessPlanningItems().sort((i1, i2) -> Integer.compare(i1.getOrderNr(), i2.getOrderNr()));
        }
    }

    private void addAcquiringPlaningItemsForEveryBatch()
    {
        for(var processPlaningItem : getFlatProcessList())
        {
            var materialPositions = processPlaningItem.getProcess().getMaterialPositions();

            for(var materialPosition : materialPositions)
            {
                if(!this.factory.checkIfItemHasASupplier(materialPosition.item()))
                    continue;

                acquiringPlanningItems.add(new MaterialPosition(materialPosition.item(), materialPosition.amount()));
            }
        }
    }

    private void addDeliveryPlanningItems(List<Order> subOrderList)
    {
        for (var order : subOrderList)
        {
            deliverPlanningItems.add(new MaterialPosition(order,
                    order.getProduct().amount()));
        }
    }
}
