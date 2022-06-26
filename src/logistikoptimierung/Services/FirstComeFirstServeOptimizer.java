package logistikoptimierung.Services;

import logistikoptimierung.Contracts.IOptimizationService;
import logistikoptimierung.Entities.FactoryObjects.*;
import logistikoptimierung.Entities.WarehouseItems.*;

import java.util.ArrayList;
import java.util.List;

public class FirstComeFirstServeOptimizer implements IOptimizationService {

    private final Factory factory;

    public FirstComeFirstServeOptimizer(Factory factory)
    {
        this.factory = factory;
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
        var stepTypes = new String[]
                {
                        FactoryStepTypes.ConcludeOrderTransportToCustomer
                };
        var factorySteps = new ArrayList<>(getTransportationFactoryStepsForOneTask(
                stepTypes,
                order,
                order.getProduct().amount(),
                getFittingTransporters(order)));

        return factorySteps;
    }

    private List<Transporter> getFittingTransporters(WarehouseItem item)
    {
        var availableTransporters = this.factory.getTransporters();
        var fittingTransporters = new ArrayList<Transporter>();

        for (var transporter : availableTransporters)
        {
            if(item.getItemType().equals(WarehouseItemTypes.Order))
            {
                if(transporter.areTransportationConstraintsFulfilledForOrder((Order) item))
                    fittingTransporters.add(transporter);
            }

            if(item.getItemType().equals(WarehouseItemTypes.Material))
            {
                if(transporter.areTransportationConstraintsFulfilledForMaterial((Material) item))
                    fittingTransporters.add(transporter);
            }
        }

        return fittingTransporters;
    }

    private List<FactoryStep> getTransportationFactoryStepsForOneTask(String[] stepTypes,
                                                                      WarehouseItem item,
                                                                      int amountOfItems,
                                                                      List<Transporter> fittingTransporters)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        var remainingAmount = amountOfItems;

        if(fittingTransporters.isEmpty())
            return factorySteps;

        while(remainingAmount != 0)
        {
            for(var transporter : fittingTransporters)
            {
                var transporterAmount = 0;
                if(transporter.getCapacity() >= remainingAmount)
                    transporterAmount = remainingAmount;
                else
                {
                    transporterAmount = transporter.getCapacity();
                }

                remainingAmount -= transporterAmount;

                for(var stepType : stepTypes)
                {
                    factorySteps.add(new FactoryStep(
                            factory,
                            item.getName(),
                            transporterAmount,
                            transporter.getName(),
                            stepType));
                }

                if(remainingAmount == 0)
                    break;
            }
        }
        return factorySteps;
    }

    private List<FactoryStep> splitBomOnTransporters(Order order)
    {
        var factorySteps = new ArrayList<FactoryStep>();

        //Only get Material from the Supplier
        if(order.getProduct().item().getItemType().equals(WarehouseItemTypes.Material))
        {
            var stepTypes = new String[]{
                    FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse,
                    FactoryStepTypes.MoveMaterialFromTransporterToWarehouse
            };
            factorySteps.addAll(getTransportationFactoryStepsForOneTask(stepTypes,
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

            var stepTypes = new String[]{
                    FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse,
                    FactoryStepTypes.MoveMaterialFromTransporterToWarehouse
            };
            factorySteps.addAll(getTransportationFactoryStepsForOneTask(stepTypes,
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

