package logistikoptimierung.Services;

import logistikoptimierung.Contracts.IOptimizationService;
import logistikoptimierung.Entities.FactoryObjects.Factory;
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
    public List<FactoryStep> optimize(List<Order> orderList)
    {
        var factorySteps = new ArrayList<FactoryStep>();

        for (var order : orderList)
        {
            factorySteps.addAll(splitBomOnTransporters(order));
            factorySteps.addAll(splitBomOnMachines(order));

            factorySteps.add(new FactoryStep(
                    factory,
                    order.getName(),
                    order.getAmount(),
                    factory.getTransporters().get(0).getName(),
                    StepTypes.ConcludeOrderTransportToCustomer));
        }

        return factorySteps;
    }

    private List<FactoryStep> splitBomOnTransporters(Order order)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        var transporterList = this.factory.getTransporters();
        var materialList = new ArrayList<>(order.getProduct().getBillOfMaterial());

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

        return factorySteps;
    }

    private List<FactoryStep> splitBomOnMachines(Order order)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        var remainingProductsToProduce = order.getAmount();
        var machineList = this.factory.getMachines();

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
        }

        return factorySteps;
    }
}

