package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.ArrayList;
import java.util.List;

public class FactoryStep {

    private WarehouseItem itemToManipulate;
    private FactoryObject factoryObject;
    private String stepType;
    private Factory factory;
    private int amountOfItems;
    private long doTimeStep;
    private List<FactoryStep> factoryStepsToDoBefore;

    public FactoryStep(Factory factory, long doTimeStamp, String itemToManipulate, int amountOfItems, String factoryObjectName, String stepType)
    {
        initFactoryStep(factory, doTimeStamp, new ArrayList<>(), itemToManipulate, amountOfItems, factoryObjectName, stepType);
    }

    public FactoryStep(Factory factory, List<FactoryStep> factoryStepsToDoBefore, String itemToManipulate, int amountOfItems, String factoryObjectName, String stepType)
    {
        var doTimeStamp = 0;
        initFactoryStep(factory, doTimeStamp, factoryStepsToDoBefore, itemToManipulate, amountOfItems, factoryObjectName, stepType);
    }

    private void initFactoryStep(Factory factory, long doTimeStamp, List<FactoryStep> factoryStepsToDoBefore, String itemToManipulate, int amountOfItems, String factoryObjectName, String stepType)
    {
        for(var item : factory.getAvailableWarehouseItems())
        {
            if(item.getName().equals(itemToManipulate))
                this.itemToManipulate = item;
        }

        if(this.itemToManipulate == null)
            factory.addLog("Item " + itemToManipulate + " not found", FactoryObjectTypes.FactoryStep);

        for (var factoryObject :
                factory.getFactoryObject()) {
            if(factoryObject.getName().equals(factoryObjectName))
                this.factoryObject = factoryObject;
        }

        if(factoryObject == null)
            factory.addLog("Factory Object " + factoryObjectName + " not found", FactoryObjectTypes.FactoryStep);

        this.stepType = stepType;
        this.factory = factory;
        this.amountOfItems = amountOfItems;
        this.doTimeStep = doTimeStamp;
        this.factoryStepsToDoBefore = factoryStepsToDoBefore;
    }

    public boolean doStep()
    {
        var completed = factoryObject.doWork(factory.getCurrentTimeStep(), itemToManipulate, amountOfItems, stepType);
        addStepMessage(completed);
        return completed;
    }

    public long getDoTimeStep() {
        return doTimeStep;
    }

    public FactoryObject getFactoryObject() {
        return factoryObject;
    }

    public WarehouseItem getItemToManipulate()
    {
        return this.itemToManipulate;
    }

    public String getStepType()
    {
        return this.stepType;
    }

    public int getAmountOfItems()
    {
        return this.amountOfItems;
    }

    private void addStepMessage(boolean completed)
    {
        this.factory.addLog(this.toString() + " Completed: " + completed, FactoryObjectTypes.FactoryStep, completed);
    }

    @Override
    public String toString()
    {
        return this.doTimeStep + ": Item: " + this.itemToManipulate.getName() + " Amount " + this.amountOfItems + " FO: " + this.factoryObject.getName() + " Step: " + this.stepType;
    }
}
