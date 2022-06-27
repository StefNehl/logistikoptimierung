package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

public class FactoryStep {

    private WarehouseItem itemToManipulate;
    private FactoryObject factoryObject;
    private final String stepType;
    private final Factory factory;
    private final int amountOfItems;
    private final long doTimeStep;

    public FactoryStep(Factory factory, long doTimeStamp, String itemToManipulate, int amountOfItems, String factoryObjectName, String stepType)
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

    private void addStepMessage(boolean completed)
    {
        factory.addLog(this.toString() + " Completed: " + completed, FactoryObjectTypes.FactoryStep, completed);
    }

    @Override
    public String toString()
    {
        return "Item: " + this.itemToManipulate.getName() + " Amount " + this.amountOfItems + " FO: " + this.factoryObject.getName() + " Step: " + this.stepType;
    }
}
