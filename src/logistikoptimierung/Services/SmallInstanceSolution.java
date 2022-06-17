package logistikoptimierung.Services;

import logistikoptimierung.Contracts.IOptimizationService;
import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.FactoryStep;
import logistikoptimierung.Entities.StepTypes;
import logistikoptimierung.Entities.WarehouseItems.Order;

import java.util.ArrayList;
import java.util.List;

public class SmallInstanceSolution implements IOptimizationService
{
    private Factory factory;
    public SmallInstanceSolution(Factory factory)
    {
        this.factory = factory;
    }

    @Override
    public List<FactoryStep> optimize(List<Order> orderList)
    {
        var factorySteps = new ArrayList<FactoryStep>();
        factorySteps.add(new FactoryStep(factory, "M1", 4, "T1", StepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse));
        factorySteps.add(new FactoryStep(factory, "M1", 1, "T1", StepTypes.MoveMaterialFromTransporterToWarehouse));
        factorySteps.add(new FactoryStep(factory, "P1", 1, "M1", StepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer));
        factorySteps.add(new FactoryStep(factory, "P1", 1, "M1", StepTypes.Produce));
        factorySteps.add(new FactoryStep(factory, "P1", 1, "M1", StepTypes.MoveProductToOutputBuffer));
        factorySteps.add(new FactoryStep(factory, "P1", 1, "M1", StepTypes.MoveProductFromOutputBufferToWarehouse));
        //factorySteps.add(new FactoryStep(factory, "P1", 1, "M1", StepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer));
        //factorySteps.add(new FactoryStep(factory, "P1", 1, "M1", StepTypes.Produce));
        //factorySteps.add(new FactoryStep(factory, "P1", 1, "M1", StepTypes.MoveProductFromBufferToWarehouse));
        //factorySteps.add(new FactoryStep(factory, "O1", 1, "T1", StepTypes.ConcludeOrderTransportToCustomer));

        return factorySteps;
    }
}
