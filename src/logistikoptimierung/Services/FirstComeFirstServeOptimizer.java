package logistikoptimierung.Services;

import logistikoptimierung.Contracts.IOptimizationService;
import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.FactoryObjects.Transporter;
import logistikoptimierung.Entities.FactoryStep;
import logistikoptimierung.Entities.StepTypes;
import logistikoptimierung.Entities.WarehouseItems.Order;

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
            handleOrder(order);
            orderCount++;
        }

        return factorySteps;
    }

    private List<FactoryStep> handleOrder(Order order)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        var productToProduce = order.getProduct().item();

        if(productToProduce.isMaterial())
        {
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
                StepTypes.ConcludeOrderTransportToCustomer));

        return factorySteps;
    }

    private List<FactoryStep> sendOrderToCustomerSteps(Order order)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        var availableTransporters = this.factory.getTransporters();
        var remainingAmount = order.getProduct().amount();

        var fittingTransporters = new ArrayList<Transporter>();

        for (var transporter : availableTransporters)
        {
            if(transporter.areTransportationConstraintsFulfilledForOrder(order))
            {
                fittingTransporters.add(transporter);
            }
        }

        while(remainingAmount != 0)
        {
            for(var transporter : fittingTransporters)
            {
                var transporterAmount = 0;
                if(transporter.getCapacity() >= order.getProduct().amount())
                    transporterAmount = order.getProduct().amount();
                else
                    transporterAmount = transporter.getCapacity();

                remainingAmount -= transporterAmount;

                factorySteps.add(new FactoryStep(
                        factory,
                        order.getProduct().item().getName(),
                        transporterAmount,
                        transporter.getName(),
                        StepTypes.ConcludeOrderTransportToCustomer));

                if(remainingAmount == 0)
                    break;
            }
        }

        return factorySteps;
    }

    private List<FactoryStep> splitBomOnTransporters(Order order)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        var transporterList = this.factory.getTransporters();

        var productToProduce = order.getProduct().item();

        if(productToProduce.isMaterial())
        {

        }
        //var productionProcess =

/*
        while (!materialList.isEmpty())
        {
            for(int i = 0; i < transporterList.size(); i++)
            {
                if(materialList.isEmpty())
                    break;

                var materialPosition = materialList.get(0);
                materialList.remove(materialPosition);

                factorySteps.add(new FactoryStep(
                        factory,
                        materialPosition.material().getName(),
                        materialPosition.amount() * order.getAmount(),
                        transporterList.get(i).getName(),
                        StepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse));

                factorySteps.add(new FactoryStep(
                        factory,
                        materialPosition.material().getName(),
                        materialPosition.amount() * order.getAmount(),
                        transporterList.get(i).getName(),
                        StepTypes.MoveMaterialFromTransporterToWarehouse));
            }
        }
*/
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

