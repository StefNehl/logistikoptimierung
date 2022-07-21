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
    private FactoryMessageSettings factoryMessageSettings;

    private List<Transporter> sortedAvailableTransportList;
    private List<Driver> availableDrivers;
    private List<DriverPoolItem> driverPoolItems;

    private long bestTimeSolution;
    private List<FactoryStep> bestSolution = new ArrayList<>();
    private long nrOfSimulations = 0;

    /**
     * Creates an object of the optimizer with an enumeration of the possibilities and combinations for handling the order.
     * For this the allgorithm gets every needed step for transportation, production and delivery and orders them in
     * every combination and simulates the factory to find the best result.
     * @param factory the factory where the optimization should happen
     * @param maxRuntime maximum run time for the optimization
     * @param factoryMessageSettings factory messages settings for the simulating factory
     */
    public EnumeratedCalculationMain(Factory factory, long maxRuntime, FactoryMessageSettings factoryMessageSettings)
    {
        this.factory = factory;
        this.factoryMessageSettings = factoryMessageSettings;

        this.availableDrivers = new ArrayList<>(this.factory.getDrivers());
        this.sortedAvailableTransportList = new ArrayList<>(this.factory.getTransporters());
        this.sortedAvailableTransportList.sort(Comparator.comparingInt(Transporter::getCapacity));
        this.driverPoolItems = new ArrayList<>(this.factory.getNrOfDrivers());
        this.bestTimeSolution = maxRuntime;
    }

    @Override
    public List<FactoryStep> optimize(List<Order> orderList, int nrOfOrdersToOptimize)
    {
        var subOrderList = new ArrayList<Order>();
        for(int i = 0; i < nrOfOrdersToOptimize; i++)
        {
            subOrderList.add(orderList.get(i));
        }

        var planningItems = getAllNeededFactoryPlanningItemsForOrder(subOrderList);

        nrOfSimulations = 0;
        var stepToDo = new ArrayList<FactoryStep>();
        getPlanningSolutionRecursive(stepToDo, planningItems);

        return bestSolution;
    }

    /**
     * Checks every combination of the planning items and simulate the factory to find the best result.
     * @param stepsToDo Steps to perform before the current step
     * @param planningItems planning item to add the factory steps
     */
    private void getPlanningSolutionRecursive(List<FactoryStep> stepsToDo, List<PlanningItem> planningItems)
    {
        if(planningItems.isEmpty())
        {
            var result = this.factory.startFactory(stepsToDo, bestTimeSolution, factoryMessageSettings);
            this.factory.resetFactory();
            nrOfSimulations++;

            if(nrOfSimulations % 100 == 0)
                System.out.println("Nr of simulations: " + nrOfSimulations + " Result: " + result);

            if(result < bestTimeSolution)
            {
                bestTimeSolution = result;
                bestSolution = new ArrayList<>(stepsToDo);
                System.out.println("Nr of simulations: " + nrOfSimulations + " Result: " + result);
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
                case Produce -> {
                    stepsToAdd = getProductionSteps(stepsToDo, planningItem);
                }
                case Deliver -> {
                    stepsToAdd = getDeliverTransporters(stepsToDo, planningItem);
                }
            }

            //abort complete solution because no material for the production or delivery in the warehouse
            if(stepsToAdd.isEmpty())
                continue;

            stepsToDo.addAll(stepsToAdd);
            var copyOfSteps = new ArrayList<>(planningItems);
            copyOfSteps.remove(planningItem);
            getPlanningSolutionRecursive(stepsToDo, copyOfSteps);
            stepsToDo.removeAll(stepsToAdd);

            //Release driver
            if(planningItem.planningType() == PlanningType.Acquire ||
                    planningItem.planningType() == PlanningType.Deliver)
            {
                if(this.driverPoolItems.isEmpty())
                    break;

                var poolItem = this.driverPoolItems.remove(this.driverPoolItems.size() - 1);
                this.availableDrivers.add(poolItem.driver());
                addTransporterToSortedTransportList(poolItem.transporter());
            }
        }
    }

    /**
     * Add the transporter to the sorted transportation list and keeps the order
     * @param transporterToAdd transporter to add
     */
    private void addTransporterToSortedTransportList(Transporter transporterToAdd)
    {
        var indexToAdd = 0;
        for(var transporter : this.sortedAvailableTransportList)
        {
            if(transporter.getCapacity() <= transporterToAdd.getCapacity())
                continue;

            indexToAdd = this.sortedAvailableTransportList.indexOf(transporter);
            break;
        }

        this.sortedAvailableTransportList.add(indexToAdd, transporterToAdd);
    }

    /**
     * Returns a list of factory steps for a planning item with the deliver property. The method checks if the material
     * was produced in the needed amount before. If not an empty list is returned.
     * @param stepsToDo steps which are getting performed before this planning item
     * @param planningItem planning item for the factory steps
     * @return factory steps for the planning item, returns an empty list if no product to deliver is available in the warehouse
     */
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
                if(step.getItemToManipulate().getName()
                    .equals(order.getProduct().item().getName()))
                {
                    amountOfProductInWarehouse += step.getAmountOfItems();
                }
            }
        }

        if(amountOfProductInWarehouse < order.getProduct().amount())
        {
            //System.out.println("Abort solution: Not enough products in the Warehouse to deliver");
            return steps;
        }

        //The last transporter needs to close the order
        Transporter lastTransport = null;
        var lastAmountToTransport = 0;
        while (amount > 0)
        {
            //Driver list is empty take release first driver from pool
            if(this.availableDrivers.isEmpty())
            {
                var poolItem = this.driverPoolItems.remove(0);
                this.availableDrivers.add(poolItem.driver());
                addTransporterToSortedTransportList(poolItem.transporter());
            }

            var bestTransporter = findBestTransporterForTheAmount(planningItem.item(), amount);

            if(bestTransporter == null)
                throw new RuntimeException("Should not happen ");

            var amountToTransport = 0;
            if(amount < bestTransporter.getCapacity())
                amountToTransport = amount;
            else
                amountToTransport = bestTransporter.getCapacity();

            var newStep = new FactoryStep(this.factory, 0,
                    planningItem.item(),
                    amountToTransport,
                    bestTransporter,
                    FactoryStepTypes.ConcludeOrderTransportToCustomer);

            steps.add(newStep);

            var newDriver = this.availableDrivers.remove(0);
            this.sortedAvailableTransportList.remove(bestTransporter);
            var newPoolItem = new DriverPoolItem(newDriver, bestTransporter);
            driverPoolItems.add(newPoolItem);
            amount -= bestTransporter.getCapacity();

            lastTransport  = bestTransporter;
            lastAmountToTransport = amountToTransport;
        }

        var newStep = new FactoryStep(this.factory, 0,
                planningItem.item(),
                lastAmountToTransport,
                lastTransport,
                FactoryStepTypes.ClosesOrderFromCustomer);

        steps.add(newStep);

        return steps;
    }

    /**
     * Returns a list of factory steps for a planning item with the acquire property.
     * @param planningItem planning item for the factory steps
     * @return factory steps for the planning item
     */
    private ArrayList<FactoryStep> getAcquireTransportSteps(PlanningItem planningItem)
    {
        var steps = new ArrayList<FactoryStep>();
        var amount = planningItem.amount();

        //ToDo Check if warehouse is full (SumUp every acquiring minus every production and delivery)

        while (amount > 0)
        {
            //Driver list is empty take release first driver from pool
            if(this.availableDrivers.isEmpty())
            {
                var poolItem = this.driverPoolItems.remove(0);
                this.availableDrivers.add(poolItem.driver());
                addTransporterToSortedTransportList(poolItem.transporter());
            }

            var bestTransporter = findBestTransporterForTheAmount(planningItem.item(), amount);

            if(bestTransporter == null)
                throw new RuntimeException("Should not happen ");

            var amountToTransport = 0;
            if(amount < bestTransporter.getCapacity())
                amountToTransport = amount;
            else
                amountToTransport = bestTransporter.getCapacity();

            var newStep = new FactoryStep(this.factory, 0,
                    planningItem.item(),
                    amountToTransport,
                    bestTransporter,
                    FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse);

            steps.add(newStep);

            newStep = new FactoryStep(this.factory, 0,
                    planningItem.item(),
                    amountToTransport,
                    bestTransporter,
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

    /**
     * Finds the best transporter from the available transporters. If none is available it returns one
     * of the driver pool.
     * @param item item for the transportation constraints
     * @param amount amount of the item
     * @return transporter, null if none was found
     */
    private Transporter findBestTransporterForTheAmount(WarehouseItem item, int amount)
    {
        var bestTransporter = this.getTransporterWithSmallestDifferenceFromAmountAndHigherCapacity(
                this.sortedAvailableTransportList,
                item,
                amount);

        //No Transporter found, need to reuse already used transporter
        if(bestTransporter == null)
        {
            var bestDriverPoolItem = getDriverPoolItemWithSmallestDifferenceFromAmountAndHigherCapacity(item, amount);
            bestTransporter = bestDriverPoolItem.transporter();

            //Release driver from this transporter
            this.driverPoolItems.remove(bestDriverPoolItem);
            this.availableDrivers.add(bestDriverPoolItem.driver());
            addTransporterToSortedTransportList(bestDriverPoolItem.transporter());
        }

        return bestTransporter;
    }

    /**
     * Returns the transporter with the smallest difference of the amount needed. The transporter is equal or higher than the amount needed.
     * If no transporter fits the needed amount. The transporter with the highest capacity gets returned
     * @param availableTransportersSortedByCapacity sorted list of available transporters
     * @param item item to transport
     * @param amount amount of the item
     * @return a fitting transporter. If none is found the method returns null.
     */
    private Transporter getTransporterWithSmallestDifferenceFromAmountAndHigherCapacity(List<Transporter> availableTransportersSortedByCapacity,
                                                                                        WarehouseItem item, int amount)
    {
        Transporter fittingTransporterWithHighestCapacity = null;
        for(var transporter : availableTransportersSortedByCapacity)
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

            if(transporter.getCapacity() >= amount)
                return transporter;

            if(fittingTransporterWithHighestCapacity == null ||
                    fittingTransporterWithHighestCapacity.getCapacity() < transporter.getCapacity())
                fittingTransporterWithHighestCapacity = transporter;
        }

        if(fittingTransporterWithHighestCapacity != null)
            return fittingTransporterWithHighestCapacity;

        return null;
    }

    private DriverPoolItem getDriverPoolItemWithSmallestDifferenceFromAmountAndHigherCapacity(WarehouseItem item, int amount)
    {
        DriverPoolItem bestDriverPoolItem = null;
        DriverPoolItem driverPoolItemWithHighestCapacity = null;
        var minDiff = Integer.MAX_VALUE;
        for(var driverPoolItem : this.driverPoolItems)
        {
            if(item instanceof Material)
            {
                if(!driverPoolItem.transporter().areTransportationConstraintsFulfilledForMaterial((Material) item))
                    continue;
            }
            else if(item instanceof  Order)
            {
                if(!driverPoolItem.transporter().areTransportationConstraintsFulfilledForOrder((Order) item))
                    continue;
            }

            var diff = driverPoolItem.transporter().getCapacity() - amount;

            if(bestDriverPoolItem == null)
            {
                bestDriverPoolItem = driverPoolItem;
                driverPoolItemWithHighestCapacity = driverPoolItem;
                minDiff = diff;
                continue;
            }

            if(driverPoolItem.transporter().getCapacity() >
                    driverPoolItemWithHighestCapacity.transporter().getCapacity())
                driverPoolItemWithHighestCapacity = driverPoolItem;

            if(diff < 0)
                continue;

            if(diff < minDiff)
            {
                bestDriverPoolItem = driverPoolItem;
                minDiff = diff;
            }

        }

        if(bestDriverPoolItem.transporter().getCapacity() >= amount)
            return bestDriverPoolItem;

        return driverPoolItemWithHighestCapacity;
    }

    /**
     * Returns a list of factory steps for a planning item with the produce property. The method checks if the material
     * was transported in the needed amount before. If not an empty list is returned.
     * @param stepsToDo steps which are getting performed before this planning item
     * @param planningItem planning item for the factory steps
     * @return factory steps for the planning item, returns an empty list if no material is available in the warehouse
     */
    private ArrayList<FactoryStep> getProductionSteps(List<FactoryStep> stepsToDo, PlanningItem planningItem)
    {
        var steps = new ArrayList<FactoryStep>();
        var process = this
                .factory
                .getProductionProcessForWarehouseItem(planningItem.item());

        //Check if material is actually in the warehouse
        for (var materialPosition : process.getMaterialPositions())
        {
            var materialInWarehouse = 0;
            for(var step : stepsToDo)
            {
                if(step.getItemToManipulate().getName().equals(materialPosition.item().getName()))
                    materialInWarehouse += step.getAmountOfItems();
            }

            if(materialInWarehouse < planningItem.amount())
            {
                //System.out.println("Abort solution: Not enough material in the Warehouse to produce");
                return steps;
            }
        }

        var newStep = new FactoryStep(this.factory, 0,
                planningItem.item(),
                1,
                process.getProduction(),
                FactoryStepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer);

        steps.add(newStep);

        newStep = new FactoryStep(this.factory, 0,
                planningItem.item(),
                1,
                process.getProduction(),
                FactoryStepTypes.Produce);

        steps.add(newStep);

        newStep = new FactoryStep(this.factory, 0,
                planningItem.item(),
                1,
                process.getProduction(),
                FactoryStepTypes.MoveProductToOutputBuffer);

        steps.add(newStep);

        newStep = new FactoryStep(this.factory, 0,
                planningItem.item(),
                planningItem.amount(),
                process.getProduction(),
                FactoryStepTypes.MoveProductFromOutputBufferToWarehouse);

        steps.add(newStep);

        return steps;
    }

    /**
     * Returns a list of planning items which are needed to fulfill the orders in the subOrder list
     * @param subOrderList order list for the planning items
     * @return list of planning items needed
     */
    private List<PlanningItem> getAllNeededFactoryPlanningItemsForOrder(List<Order> subOrderList)
    {
        var productionPlanningItems = createProcessList(subOrderList);
        setProcessDepthForPlanningItems(productionPlanningItems);
        removeDoubleEntriesFromPlaningItemList(productionPlanningItems);
        getProcessesForEveryBatchAndOrderAfterDepth(subOrderList, productionPlanningItems);
        var acquiringMaterialPositions = addAcquiringPlaningItemsForEveryBatch(productionPlanningItems);
        acquiringMaterialPositions.addAll(addAcquiringPlaningItemsForDirectDelivery(subOrderList));
        var deliverMaterialPositions = addDeliveryPlanningItems(subOrderList);

        var planningItems = new ArrayList<PlanningItem>();

        for(var item : acquiringMaterialPositions)
            planningItems.add(new PlanningItem(item.item(), item.amount(), PlanningType.Acquire));

        for (var item : this.getFlatProductionProcessList(productionPlanningItems))
            planningItems.add(new PlanningItem(item.getProcess().getProductToProduce(),
                    1,
                    PlanningType.Produce));

        for(var item : deliverMaterialPositions)
            planningItems.add(new PlanningItem(item.item(), item.amount(), PlanningType.Deliver));

        return planningItems;
    }

    /**
     * Returns production planning items for the orders without products which can be supplied
     * @param orderList
     * @return list of production planning items
     */
    private ArrayList<ProductionPlanningItem> createProcessList(List<Order> orderList)
    {
        var productionItems = new ArrayList<ProductionPlanningItem>();
        for (var production : this.factory.getProductions())
        {
            productionItems.add(new ProductionPlanningItem(production));
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
                var planningItem = getProductionPlanningItemForProcess(process, productionItems);
                planningItem.getProcessPlanningItems()
                        .add(new ProcessPlaningItem(process, orderCount));
            }
            orderCount++;
        }

        return productionItems;
    }

    private ProductionPlanningItem getProductionPlanningItemForProcess(ProductionProcess process, List<ProductionPlanningItem> planningItems)
    {
        for (var item : planningItems)
        {
            var prodItem = item.getProduction().getProductionProcessForProduct(
                    process.getProductToProduce());
            if(prodItem != null)
                return item;
        }

        return null;
    }

    private void setProcessDepthForPlanningItems(List<ProductionPlanningItem> planningItems)
    {
        for(var planingItem : this.getFlatProductionProcessList(planningItems))
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

    private void getProcessesForEveryBatchAndOrderAfterDepth(List<Order> orderList, List<ProductionPlanningItem> planningItems)
    {
        var flatProcessList = getFlatProductionProcessList(planningItems);
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
        for(var production : planningItems)
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

    /**
     * Returns a list flat list from all production processes.
     * @return list with process planning items
     */
    private List<ProcessPlaningItem> getFlatProductionProcessList(List<ProductionPlanningItem> planningItems)
    {
        var result = new ArrayList<ProcessPlaningItem>();

        for (var production : planningItems)
        {
            for (var process : production.getProcessPlanningItems())
            {
                result.add(process);
            }
        }
        return result;
    }

    private void removeDoubleEntriesFromPlaningItemList(List<ProductionPlanningItem> productionPlanningItems)
    {
        for(var production : productionPlanningItems)
        {
            var hashSet = new HashSet<>(production.getProcessPlanningItems());
            production.getProcessPlanningItems().clear();
            production.getProcessPlanningItems().addAll(hashSet);
            production.getProcessPlanningItems().sort((i1, i2) -> Integer.compare(i1.getOrderNr(), i2.getOrderNr()));
        }
    }

    /**
     * Returns a list of material positions which are needed for the production
     * @param productionPlanningItems production processes for the orders to optimize
     * @return list of material positions
     */
    private List<MaterialPosition> addAcquiringPlaningItemsForEveryBatch(List<ProductionPlanningItem> productionPlanningItems)
    {
        var materialPositionsToAcquire = new ArrayList<MaterialPosition>();
        for(var processPlaningItem : getFlatProductionProcessList(productionPlanningItems))
        {
            var materialPositions = processPlaningItem.getProcess().getMaterialPositions();

            for(var materialPosition : materialPositions)
            {
                if(!this.factory.checkIfItemHasASupplier(materialPosition.item()))
                    continue;

                materialPositionsToAcquire.add(new MaterialPosition(materialPosition.item(), materialPosition.amount()));
            }
        }

        return materialPositionsToAcquire;
    }

    /**
     * Returns a list of material position which are delivered directly to the customer
     * @param orderList orders to check for direct delivery
     * @return a list of material positions
     */
    private List<MaterialPosition> addAcquiringPlaningItemsForDirectDelivery(List<Order> orderList)
    {
        var materialPositionsToAcquire = new ArrayList<MaterialPosition>();
        for(var order : orderList)
        {
            if(this.factory.checkIfItemHasASupplier(order.getProduct().item()))
                materialPositionsToAcquire.add(new MaterialPosition(order.getProduct().item(), order.getProduct().amount()));
        }
        return materialPositionsToAcquire;
    }

    /**
     * Returns a list of material positions to deliver to the customer
     * @param subOrderList orders to check
     * @return list of material positions
     */
    private List<MaterialPosition> addDeliveryPlanningItems(List<Order> subOrderList)
    {
        var deliveryItems = new ArrayList<MaterialPosition>();
        for (var order : subOrderList)
        {
            deliveryItems.add(new MaterialPosition(order,
                    order.getProduct().amount()));
        }
        return deliveryItems;
    }
}
