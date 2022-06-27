package logistikoptimierung.Services.FirstComeFirstServeOptimizer;

import logistikoptimierung.Contracts.IOptimizationService;
import logistikoptimierung.Entities.FactoryObjects.*;
import logistikoptimierung.Entities.WarehouseItems.*;

import java.util.ArrayList;
import java.util.List;

public class FirstComeFirstServeOptimizerMain implements IOptimizationService {

    private final Factory factory;
    private List<TransporterPlanningItem> transporterPlanningItems;

    public FirstComeFirstServeOptimizerMain(Factory factory)
    {
        this.factory = factory;
        this.transporterPlanningItems = new ArrayList<>();

        for (var transporter: this.factory.getTransporters())
        {
            transporterPlanningItems.add(new TransporterPlanningItem(transporter));
        }
    }

    @Override
    public List<FactoryStep> optimize(List<Order> orderList, int nrOfOrdersToOptimize)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        var orderCount = 0;

        for (var order : orderList)
        {
            if(orderCount == nrOfOrdersToOptimize)
                break;
            factorySteps.addAll(handleOrder(order));
            orderCount++;
        }

        return factorySteps;
    }

    private List<FactoryStep> handleOrder(Order order)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        var productToProduce = order.getProduct().item();

        //Bring Material From Supplier To Warehouse
        //Bring Material From Warehouse to Customer
        if(productToProduce.getItemType().equals(WarehouseItemTypes.Material))
        {
            factorySteps.addAll(splitBomOnTransporters(order));
            factorySteps.addAll(sendOrderToCustomerSteps(order));
            return factorySteps;
        }

        factorySteps.addAll(splitBomOnTransporters(order));
        factorySteps.addAll(splitBomOnMachines(order));
        factorySteps.addAll(sendOrderToCustomerSteps(order));

        return factorySteps;
    }

    private List<FactoryStep> sendOrderToCustomerSteps(Order order)
    {
        var factorySteps = new ArrayList<>(getTransportationFactoryStepsForOneTask(
                order,
                order.getProduct().amount(),
                getFittingTransporters(order)));

        return factorySteps;
    }

    private List<TransporterPlanningItem> getFittingTransporters(WarehouseItem item)
    {
        var fittingTransporters = new ArrayList<TransporterPlanningItem>();

        for (var transporterPlanningItem : this.transporterPlanningItems)
        {
            if(item.getItemType().equals(WarehouseItemTypes.Order))
            {
                if(transporterPlanningItem.getTransporter().areTransportationConstraintsFulfilledForOrder((Order) item))
                    fittingTransporters.add(transporterPlanningItem);
            }

            if(item.getItemType().equals(WarehouseItemTypes.Material))
            {
                if(transporterPlanningItem.getTransporter().areTransportationConstraintsFulfilledForMaterial((Material) item))
                    fittingTransporters.add(transporterPlanningItem);
            }
        }

        return fittingTransporters;
    }

    private List<FactoryStep> getTransportationFactoryStepsForOneTask(WarehouseItem item,
                                                                      int amountOfItems,
                                                                      List<TransporterPlanningItem> fittingTransporters)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        var remainingAmount = amountOfItems;

        if(fittingTransporters.isEmpty())
            return factorySteps;

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

            var factoryStepsToDo = new ArrayList<String>();
            if(item instanceof Material)
            {
                travelTime = ((Material) item).getTravelTime();

                factorySteps.add(new FactoryStep(
                        factory,
                        transporterPlanningItem.getBlockedTime(),
                        item.getName(),
                        transporterAmount,
                        transporterPlanningItem.getTransporter().getName(),
                        FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse));

                factorySteps.add(new FactoryStep(
                        factory,
                        transporterPlanningItem.getBlockedTime() + travelTime,
                        item.getName(),
                        transporterAmount,
                        transporterPlanningItem.getTransporter().getName(),
                        FactoryStepTypes.MoveMaterialFromTransporterToWarehouse));
            }

            if(item instanceof Order)
            {
                travelTime = ((Order) item).getTravelTime();
                factorySteps.add(new FactoryStep(
                        factory,
                        transporterPlanningItem.getBlockedTime(),
                        item.getName(),
                        transporterAmount,
                        transporterPlanningItem.getTransporter().getName(),
                        FactoryStepTypes.ConcludeOrderTransportToCustomer));
            }

            transporterPlanningItem.increaseBlockedTime(travelTime);

            if(remainingAmount == 0)
                break;
        }
        return factorySteps;
    }

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

    private List<FactoryStep> splitBomOnTransporters(Order order)
    {
        var factorySteps = new ArrayList<FactoryStep>();

        //Only get Material from the Supplier
        if(order.getProduct().item().getItemType().equals(WarehouseItemTypes.Material))
        {
            factorySteps.addAll(getTransportationFactoryStepsForOneTask(
                    order.getProduct().item(),
                    order.getProduct().amount(),
                    getFittingTransporters(order.getProduct().item())));

            return factorySteps;
        }

        var productToProduce = (Product)order.getProduct().item();
        var materialList = this.factory
                .getMaterialPositionsForProductWithRespectOfBatchSize(
                        productToProduce,
                        order.getProduct().amount(),
                        true);

        var condensedMaterialList = condenseMaterialList(materialList);

        for(var materialPosition : condensedMaterialList)
        {
            //produce everything. Also, Stahl and Bauholz
            var process = this.factory.getProductionProcessForWarehouseItem(materialPosition.item());
            if(process != null)
                continue;

            factorySteps.addAll(getTransportationFactoryStepsForOneTask(
                    materialPosition.item(),
                    materialPosition.amount(),
                    getFittingTransporters(materialPosition.item())));
        }

        return factorySteps;
    }

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

    private MaterialPosition findMaterialPositionByName(String name, List<MaterialPosition> materialList)
    {
        for (var position : materialList)
        {
            if(position.item().getName().equals(name))
                return position;
        }

        return null;
    }

    private List<FactoryStep> splitBomOnMachines(Order order)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        var processToProduce = this.factory.getProductionProcessesForProduct((Product) order.getProduct().item());

        for(int i = processToProduce.size()-1; i >= 0; i--)
        {
            var process = processToProduce.get(i);
            var amountNeeded = order.getProduct().amount();
            var batchSizeOfProduct = process.getProductionBatchSize();
            var nrOfBatchesNeeded = (int)Math.ceil((double) amountNeeded / (double)batchSizeOfProduct);

            for(int j = 0; j < nrOfBatchesNeeded; j++)
            {
                var stepTypes = new String[]{
                        FactoryStepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer,
                        FactoryStepTypes.Produce,
                        FactoryStepTypes.MoveProductToOutputBuffer,
                        FactoryStepTypes.MoveProductFromOutputBufferToWarehouse
                };

                for(var step : stepTypes)
                {
                    var newStep = new FactoryStep(factory,
                            0,
                            process.getProductToProduce().getName(),
                            1,
                            process.getProduction().getName(),
                            step);

                    factorySteps.add(newStep);
                }
            }
        }

        return factorySteps;
    }
}

