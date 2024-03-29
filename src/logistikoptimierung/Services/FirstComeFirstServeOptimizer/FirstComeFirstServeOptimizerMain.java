package logistikoptimierung.Services.FirstComeFirstServeOptimizer;

import logistikoptimierung.Contracts.IOptimizationService;
import logistikoptimierung.Entities.FactoryObjects.*;
import logistikoptimierung.Entities.Instance;
import logistikoptimierung.Entities.WarehouseItems.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates an object of the optimizer with the first come first serve principal.
 * For this the algorithm gets every needed step for transportation, production and delivery and orders them via
 * the priority of the order. So the first order is handled first, then the next and so on.
 */
public class FirstComeFirstServeOptimizerMain implements IOptimizationService {

    private final FactoryConglomerate factoryConglomerate;
    private final List<Order> orderList;
    private List<TransporterPlanningItem> transporterPlanningItems;
    private List<ProductionPlanningItem> productionPlanningItems;

    /**
     * Creates an object of the optimizer with the first come first serve principal.
     * For this the algorithm gets every needed step for transportation, production and delivery and orders them via
     * the priority of the order. So the first order is handled first, then the next and so on.
     * @param instance the instance with the factory and the orderlist where the optimization should happen
     */
    public FirstComeFirstServeOptimizerMain(Instance instance)
    {
        this.factoryConglomerate = instance.getFactoryConglomerate();
        this.orderList = instance.getOrderList();
        this.transporterPlanningItems = new ArrayList<>();

        for (var transporter: this.factoryConglomerate.getTransporters())
        {
            transporterPlanningItems.add(new TransporterPlanningItem(transporter));
        }

        this.productionPlanningItems = new ArrayList<>();

        for(var production : this.factoryConglomerate.getFactories())
        {
            productionPlanningItems.add(new ProductionPlanningItem(production));
        }
    }

    /**
     * Optimize a given nr of order from the order list with the first come, first serve algorithm. Returns an empty list
     * if the order nr is higher than the available  orders.
     * @param nrOfOrdersToOptimize nr of orders to optimize
     * @return returns a list of factory steps for the simulation of the factory
     */
    @Override
    public List<FactoryStep> optimize(int nrOfOrdersToOptimize)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        var orderCount = 0;

        for (var order : this.orderList)
        {
            if(orderCount == nrOfOrdersToOptimize)
                break;
            factorySteps.addAll(handleOrder(order));
            orderCount++;
        }

        return factorySteps;
    }

    /**
     * Handles the order and creates factory steps for the given order
     * @param order order for the factory steps
     * @return list of factory steps to handle an order from the transportation of the material to the delivery of the
     * product
     */
    private List<FactoryStep> handleOrder(Order order)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        var productToProduce = order.getWarehousePosition().item();

        //Bring Material From Supplier To Warehouse
        //Bring Material From Warehouse to Customer
        if(productToProduce.getItemType().equals(WarehouseItemType.Material))
        {
            factorySteps.addAll(splitBomOnTransporters(order));
            factorySteps.addAll(sendOrderToCustomerSteps(order, factorySteps));
            return factorySteps;
        }

        factorySteps.addAll(splitBomOnTransporters(order));
        factorySteps.addAll(splitBomOnMachines(order, factorySteps));
        factorySteps.addAll(sendOrderToCustomerSteps(order, factorySteps));

        return factorySteps;
    }

    /**
     * Gets the factory steps for sending the product to the customer
     * @param order order to handle
     * @param factoryStepsBefore factory steps which are done before
     * @return list of factory step for handling the order
     */
    private List<FactoryStep> sendOrderToCustomerSteps(Order order, List<FactoryStep> factoryStepsBefore)
    {
        var factorySteps = new ArrayList<>(getTransportationFactoryStepsForOneTask(
                order,
                order.getWarehousePosition().amount(),
                getFittingTransporters(order),
                factoryStepsBefore));

        return factorySteps;
    }

    /**
     * Returns a list of transporter for the warehouse item (product, material)
     * @param item material or product to check
     * @return list of transporters
     */
    private List<TransporterPlanningItem> getFittingTransporters(WarehouseItem item)
    {
        var fittingTransporters = new ArrayList<TransporterPlanningItem>();

        for (var transporterPlanningItem : this.transporterPlanningItems)
        {
            if(item.getItemType().equals(WarehouseItemType.Order))
            {
                if(transporterPlanningItem.getTransporter().areTransportationConstraintsFulfilledForOrder((Order) item))
                    fittingTransporters.add(transporterPlanningItem);
            }

            if(item.getItemType().equals(WarehouseItemType.Material))
            {
                if(transporterPlanningItem.getTransporter().areTransportationConstraintsFulfilledForMaterial((Material) item))
                    fittingTransporters.add(transporterPlanningItem);
            }
        }

        return fittingTransporters;
    }

    /**
     * Returns the factory steps which are needed for transporting the specific warehouse item in the amount from the
     * warehouse to the customer
     * @param item item to deliver to the customer
     * @param amountOfItems the amount which should be delivered
     * @param fittingTransporters transporters who fit the transportation constrains
     * @param factoryStepsToDoBefore factory steps which are done before
     * @return a list of factory steps
     */
    private List<FactoryStep> getTransportationFactoryStepsForOneTask(WarehouseItem item,
                                                                      int amountOfItems,
                                                                      List<TransporterPlanningItem> fittingTransporters,
                                                                      List<FactoryStep> factoryStepsToDoBefore)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        var remainingAmount = amountOfItems;

        if(fittingTransporters.isEmpty())
            return factorySteps;

        long startTimeStep = 0;
        if(factoryStepsToDoBefore != null)
        {
            if(item instanceof Order)
            {
                var order = (Order)item;

                for(var step : factoryStepsToDoBefore)
                {
                    if(step.getStepType() != FactoryStepTypes.Produce &&
                            step.getStepType() != FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse)
                        continue;

                    if(step.getItemToManipulate().equals(order.getWarehousePosition().item()))
                    {
                        var startTimeFromStep = step.getDoTimeStep();
                        var additionalTime = 0;
                        if(order.getWarehousePosition().item() instanceof Product)
                            additionalTime = this.factoryConglomerate.getProductionProcessForProduct((Product) order.getWarehousePosition().item()).getProductionTime();
                        if(order.getWarehousePosition().item()instanceof Material)
                            additionalTime = ((Material) order.getWarehousePosition().item()).getTravelTime();

                        startTimeStep = startTimeFromStep + additionalTime;
                        break;
                    }
                }
            }

        }

        while(remainingAmount != 0)
        {
            var transporterPlanningItem = findTransporterWhichEarliestFree(fittingTransporters);
            var transporterAmount = 0;
            if(transporterPlanningItem.getTransporter().getCapacity() >= remainingAmount)
                transporterAmount = remainingAmount;
            else
            {
                transporterAmount = transporterPlanningItem.getTransporter().getCapacity();
            }

            remainingAmount -= transporterAmount;
            var travelTime = 0;

            if(item instanceof Material)
            {
                travelTime = ((Material) item).getTravelTime();

                factorySteps.add(new FactoryStep(
                        factoryConglomerate,
                        transporterPlanningItem.getBlockedTime(),
                        item,
                        transporterAmount,
                        transporterPlanningItem.getTransporter(),
                        FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse));

                factorySteps.add(new FactoryStep(
                        factoryConglomerate,
                        transporterPlanningItem.getBlockedTime() + travelTime,
                        item,
                        transporterAmount,
                        transporterPlanningItem.getTransporter(),
                        FactoryStepTypes.MoveMaterialFromTransporterToWarehouse));
            }

            if(item instanceof Order)
            {
                travelTime = ((Order) item).getTravelTime();
                factorySteps.add(new FactoryStep(
                        factoryConglomerate,
                        startTimeStep,
                        item,
                        transporterAmount,
                        transporterPlanningItem.getTransporter(),
                        FactoryStepTypes.ConcludeOrderTransportToCustomer));

                if(remainingAmount == 0)
                {
                    factorySteps.add(new FactoryStep(
                            factoryConglomerate,
                            startTimeStep,
                            item,
                            transporterAmount,
                            transporterPlanningItem.getTransporter(),
                            FactoryStepTypes.ClosesOrderFromCustomer));
                }
            }

            transporterPlanningItem.increaseBlockedTime(travelTime);
        }
        return factorySteps;
    }

    /**
     * returns the transporter which is free at the earliest time
     * @param transporters list of transporters to check
     * @return the transporter which is free at the earliest time
     */
    private TransporterPlanningItem findTransporterWhichEarliestFree(List<TransporterPlanningItem> transporters)
    {
        TransporterPlanningItem freeTransporter = transporters.get(0);
        for (var transporter : transporters)
        {
            if(transporter.getBlockedTime() < freeTransporter.getBlockedTime())
                freeTransporter = transporter;

        }
        return freeTransporter;
    }

    /**
     * splits the bill of material for one order on the transporters. Every item gets produced (Stahl) and the needed
     * material of every step aggregated (condensed) and split on the available transporters.
     * @param order
     * @return
     */
    private List<FactoryStep> splitBomOnTransporters(Order order)
    {
        var factorySteps = new ArrayList<FactoryStep>();

        //Only get Material from the Supplier
        if(order.getWarehousePosition().item().getItemType().equals(WarehouseItemType.Material))
        {
            factorySteps.addAll(getTransportationFactoryStepsForOneTask(
                    order.getWarehousePosition().item(),
                    order.getWarehousePosition().amount(),
                    getFittingTransporters(order.getWarehousePosition().item()),
                    null));

            return factorySteps;
        }

        var productToProduce = (Product)order.getWarehousePosition().item();
        var materialList = this.factoryConglomerate
                .getWarehousePositionsForProductWithRespectOfBatchSize(
                        productToProduce,
                        order.getWarehousePosition().amount(), false);

        materialList = condenseMaterialList(materialList);

        for(var materialPosition : materialList)
        {
            //get everything. Also, Stahl and Bauholz
            if(!factoryConglomerate.checkIfItemHasASupplier(materialPosition.item()))
                continue;

            factorySteps.addAll(getTransportationFactoryStepsForOneTask(
                    materialPosition.item(),
                    materialPosition.amount(),
                    getFittingTransporters(materialPosition.item()),
                    null));
        }

        return factorySteps;
    }

    /**
     * Returns a condensed material position list
     * @param materialList not condensed material position list
     * @return condensed material position list
     */
    private List<WarehousePosition> condenseMaterialList(List<WarehousePosition> materialList)
    {
        var newMaterialList = new ArrayList<WarehousePosition>();

        for (var item: materialList)
        {
            var position = findMaterialPositionByName(item.item().getName(), newMaterialList);
            if(position == null)
            {
                var newPosition = new WarehousePosition(item.item(), item.amount());
                newMaterialList.add(newPosition);
                continue;
            }

            var newPosition = new WarehousePosition(item.item(), position.amount() + item.amount());
            newMaterialList.remove(position);
            newMaterialList.add(newPosition);
        }

        return newMaterialList;
    }

    /**
     * Finds the material from the material list by name
     * @param name name to search
     * @param materialList material list to search
     * @return material position
     */
    private WarehousePosition findMaterialPositionByName(String name, List<WarehousePosition> materialList)
    {
        for (var position : materialList)
        {
            if(position.item().getName().equals(name))
                return position;
        }

        return null;
    }

    /**
     * Splits the production processes for an order on the needed production sites and returns the needed factory steps.
     * @param order order for the factory steps
     * @param factoryStepsBefore factory steps which are done before the new factory steps
     * @return a list of factory steps
     */
    private List<FactoryStep> splitBomOnMachines(Order order, List<FactoryStep> factoryStepsBefore)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        factoryStepsBefore = new ArrayList<>(factoryStepsBefore);
        var processesToProduce = this.factoryConglomerate
                .getProductionProcessesForProduct((Product) order.getWarehousePosition().item());

        var fittingProcessPlaningItems = new ProductionPlanningItem[processesToProduce.size()];

        for(int i = 0; i < processesToProduce.size(); i++)
        {
            var process = processesToProduce.get(i);
            for(var processPlaningItem : this.productionPlanningItems)
            {
                if(process.getFactory().equals(processPlaningItem.getProduction()))
                    fittingProcessPlaningItems[i] = processPlaningItem;
            }
        }

        var materialToProduces = this.factoryConglomerate
                .getWarehousePositionsForProductWithRespectOfBatchSize((Product) order.getWarehousePosition().item(),
                        order.getWarehousePosition().amount(), false);

        for(int i = fittingProcessPlaningItems.length-1; i >= 0; i--)
        {
            var processPlaningItem = fittingProcessPlaningItems[i];
            var process = processesToProduce.get(i);

            if(this.factoryConglomerate.checkIfItemHasASupplier(process.getProductToProduce()))
                continue;

            var amountNeeded = 0;
            var batchSizeOfProduct = 0;
            var nrOfBatchesNeeded = 0;
            for(var productToProduce : materialToProduces)
            {
                if(this.factoryConglomerate.checkIfItemHasASupplier(productToProduce.item()))
                    continue;
                if(process.getProductToProduce().equals(productToProduce.item()))
                {
                    amountNeeded = productToProduce.amount();
                    batchSizeOfProduct = process.getProductionBatchSize();
                    nrOfBatchesNeeded = (int)Math.ceil((double) amountNeeded / (double)batchSizeOfProduct);
                    break;
                }
            }

            long startTime = 0;
            for(var material : process.getMaterialPositions())
            {
                var amountMaterialNeeded = material.amount();

                if(this.factoryConglomerate.checkIfItemHasASupplier(material.item()))
                {
                    //For Transportation of Material
                    for(var step : factoryStepsBefore)
                    {
                        if(step.getStepType() != FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse)
                            continue;

                        if(step.getItemToManipulate().equals(material.item()))
                            amountMaterialNeeded -= step.getAmountOfItems();

                        if(amountMaterialNeeded <= 0)
                        {
                            var travelTime =((Material) material.item()).getTravelTime();
                            var startTimeOfStep = step.getDoTimeStep();
                            var returnTimeOfStep = startTimeOfStep + travelTime;
                            if(startTime < returnTimeOfStep)
                                startTime = returnTimeOfStep;
                            break;
                        }
                    }
                }
                else
                {
                    //For Production of Material
                    for(var step : factorySteps)
                    {
                        if(step.getStepType() != FactoryStepTypes.Produce)
                            continue;

                        if(step.getItemToManipulate().equals(material.item()))
                            amountMaterialNeeded -= material.amount();

                        if(amountMaterialNeeded <= 0)
                        {
                            var processOfMaterial = this.factoryConglomerate.getProductionProcessForProduct((Product) material.item());
                            var productionTime = processOfMaterial.getProductionTime();
                            var startTimeOfStep = step.getDoTimeStep();
                            var finishedTimeStep = startTimeOfStep + productionTime;
                            if(startTime < finishedTimeStep)
                                startTime = finishedTimeStep;
                            break;
                        }
                    }
                }
            }

            processPlaningItem.increaseBlockedTime(startTime);

            for(int j = 0; j < nrOfBatchesNeeded; j++)
            {
                var productionStart = processPlaningItem.getBlockedTime();
                var productionTime = process.getProductionTime();

                var newStep = new FactoryStep(factoryConglomerate,
                        productionStart,
                        process.getProductToProduce(),
                        1,
                        process.getFactory(),
                        FactoryStepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer);
                factorySteps.add(newStep);


                newStep = new FactoryStep(factoryConglomerate,
                        productionStart,
                        process.getProductToProduce(),
                        1,
                        process.getFactory(),
                        FactoryStepTypes.Produce);
                factorySteps.add(newStep);


                newStep = new FactoryStep(factoryConglomerate,
                        productionStart + productionTime,
                        process.getProductToProduce(),
                        1,
                        process.getFactory(),
                        FactoryStepTypes.MoveProductToOutputBuffer);
                factorySteps.add(newStep);

                newStep = new FactoryStep(factoryConglomerate,
                        productionStart + productionTime,
                        process.getProductToProduce(),
                        1,
                        process.getFactory(),
                        FactoryStepTypes.MoveProductFromOutputBufferToWarehouse);
                factorySteps.add(newStep);
            }
        }

        return factorySteps;
    }
}

