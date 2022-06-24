package logistikoptimierung.Services;

import logistikoptimierung.Contracts.IOptimizationService;
import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.FactoryObjects.Transporter;
import logistikoptimierung.Entities.FactoryObjects.FactoryStep;
import logistikoptimierung.Entities.FactoryObjects.FactoryStepTypes;
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

        factorySteps.add(new FactoryStep(
                factory,
                order.getProduct().item().getName(),
                order.getProduct().amount(),
                factory.getTransporters().get(0).getName(),
                FactoryStepTypes.ConcludeOrderTransportToCustomer));

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
        var materialList = this.factory.getMaterialPositionsForProduct(productToProduce);

        for(var materialPosition : materialList)
        {
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

    private List<FactoryStep> splitBomOnMachines(Order order)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        /*var remainingProductsToProduce = order.getAmount();
        var machineList = this.factory.getProductions();

        while (remainingProductsToProduce != 0)
        {
            for(int i = 0; i < machineList.size(); i++)
            {
                if(remainingProductsToProduce == 0)
                    break;

                factorySteps.add(new FactoryStep(
                        factory,
                        order.getProduct().getName(),
                        1,
                        machineList.get(i).getName(),
                        StepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer));

                factorySteps.add(new FactoryStep(
                        factory,
                        order.getProduct().getName(),
                        1,
                        machineList.get(i).getName(),
                        StepTypes.Produce));

                factorySteps.add(new FactoryStep(
                        factory,
                        order.getProduct().getName(),
                        1,
                        machineList.get(i).getName(),
                        StepTypes.MoveProductToOutputBuffer));

                factorySteps.add(new FactoryStep(
                        factory,
                        order.getProduct().getName(),
                        1,
                        machineList.get(i).getName(),
                        StepTypes.MoveProductFromOutputBufferToWarehouse));

                remainingProductsToProduce--;
            }
        }*/

        return factorySteps;
    }
}

