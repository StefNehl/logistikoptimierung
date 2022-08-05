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

    private final Factory factory;
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
        this.factory = instance.factory();
        this.orderList = instance.orderList();
        this.transporterPlanningItems = new ArrayList<>();

        for (var transporter: this.factory.getTransporters())
        {
            transporterPlanningItems.add(new TransporterPlanningItem(transporter));
        }

        this.productionPlanningItems = new ArrayList<>();

        for(var production : this.factory.getProductions())
        {
            productionPlanningItems.add(new ProductionPlanningItem(production));
        }
    }

    /**
     * Optimize a given nr of order from the order list with the first come, first serve algorithm.
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
        var productToProduce = order.getProduct().item();

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
     * Finds the latest time step of a list with factory steps
     * @param factorySteps list to look
     * @return latest time step
     */
    private long findLatestTimeStamp(List<FactoryStep> factorySteps)
    {
        long timeStamp = 0;
        for(var step : factorySteps)
        {
            if(step.getDoTimeStep() > timeStamp)
                timeStamp = step.getDoTimeStep();
        }
        return timeStamp;
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
                order.getProduct().amount(),
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
                    if(step.getItemToManipulate().equals(order.getProduct().item()))
                    {
                        var startTimeFromStep = step.getDoTimeStep();
                        var productionTime = 0;
                        if(order.getProduct().item() instanceof Product)
                            productionTime = this.factory.getProductionProcessForProduct((Product) order.getProduct().item()).getProductionTime();
                        startTimeStep = startTimeFromStep + productionTime;
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
                        factory,
                        transporterPlanningItem.getBlockedTime(),
                        item,
                        transporterAmount,
                        transporterPlanningItem.getTransporter(),
                        FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse));

                factorySteps.add(new FactoryStep(
                        factory,
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
                        factory,
                        startTimeStep,
                        item,
                        transporterAmount,
                        transporterPlanningItem.getTransporter(),
                        FactoryStepTypes.ConcludeOrderTransportToCustomer));

                if(remainingAmount == 0)
                {
                    factorySteps.add(new FactoryStep(
                            factory,
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
        if(order.getProduct().item().getItemType().equals(WarehouseItemType.Material))
        {
            factorySteps.addAll(getTransportationFactoryStepsForOneTask(
                    order.getProduct().item(),
                    order.getProduct().amount(),
                    getFittingTransporters(order.getProduct().item()),
                    null));

            return factorySteps;
        }

        var productToProduce = (Product)order.getProduct().item();
        var materialList = this.factory
                .getMaterialPositionsForProductWithRespectOfBatchSize(
                        productToProduce,
                        order.getProduct().amount(), false);

        materialList = condenseMaterialList(materialList);

        for(var materialPosition : materialList)
        {
            //get everything. Also, Stahl and Bauholz
            if(!factory.checkIfItemHasASupplier(materialPosition.item()))
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
    private List<MaterialPosition> condenseMaterialList(List<MaterialPosition> materialList)
    {
        var newMaterialList = new ArrayList<MaterialPosition>();

        for (var item: materialList)
        {
            var position = findMaterialPositionByName(item.item().getName(), newMaterialList);
            if(position == null)
            {
                var newPosition = new MaterialPosition(item.item(), item.amount());
                newMaterialList.add(newPosition);
                continue;
            }

            var newPosition = new MaterialPosition(item.item(), position.amount() + item.amount());
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
    private MaterialPosition findMaterialPositionByName(String name, List<MaterialPosition> materialList)
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
        var processesToProduce = this.factory
                .getProductionProcessesForProduct((Product) order.getProduct().item());

        var fittingProcessPlaningItems = new ProductionPlanningItem[processesToProduce.size()];

        for(int i = 0; i < processesToProduce.size(); i++)
        {
            var process = processesToProduce.get(i);
            for(var processPlaningItem : this.productionPlanningItems)
            {
                if(process.getProduction().equals(processPlaningItem.getProduction()))
                    fittingProcessPlaningItems[i] = processPlaningItem;
            }
        }

        var materialToProduces = this.factory
                .getMaterialPositionsForProductWithRespectOfBatchSize((Product) order.getProduct().item(),
                        order.getProduct().amount(), false);

        for(int i = fittingProcessPlaningItems.length-1; i >= 0; i--)
        {
            var processPlaningItem = fittingProcessPlaningItems[i];
            var process = processesToProduce.get(i);

            if(this.factory.checkIfItemHasASupplier(process.getProductToProduce()))
                continue;

            var amountNeeded = 0;
            var batchSizeOfProduct = 0;
            var nrOfBatchesNeeded = 0;
            for(var productToProduce : materialToProduces)
            {
                if(this.factory.checkIfItemHasASupplier(productToProduce.item()))
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
                for(var step : factoryStepsBefore)
                {
                    if(step.getItemToManipulate().equals(material.item()))
                    {
                        amountMaterialNeeded -= step.getDoTimeStep();
                    }

                    if(amountMaterialNeeded < 0)
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

            processPlaningItem.increaseBlockedTime(startTime);

            for(int j = 0; j < nrOfBatchesNeeded; j++)
            {
                var productionStart = processPlaningItem.getBlockedTime();
                var productionTime = process.getProductionTime();

                var newStep = new FactoryStep(factory,
                        productionStart,
                        process.getProductToProduce(),
                        1,
                        process.getProduction(),
                        FactoryStepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer);
                factorySteps.add(newStep);


                newStep = new FactoryStep(factory,
                        productionStart,
                        process.getProductToProduce(),
                        1,
                        process.getProduction(),
                        FactoryStepTypes.Produce);
                factorySteps.add(newStep);


                newStep = new FactoryStep(factory,
                        productionStart + productionTime,
                        process.getProductToProduce(),
                        1,
                        process.getProduction(),
                        FactoryStepTypes.MoveProductToOutputBuffer);
                factorySteps.add(newStep);

                newStep = new FactoryStep(factory,
                        productionStart + productionTime,
                        process.getProductToProduce(),
                        1,
                        process.getProduction(),
                        FactoryStepTypes.MoveProductFromOutputBufferToWarehouse);
                factorySteps.add(newStep);
            }
        }

        return factorySteps;
    }
}

