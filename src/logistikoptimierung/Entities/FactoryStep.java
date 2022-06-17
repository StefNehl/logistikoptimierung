package logistikoptimierung.Entities;

import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.FactoryObjects.FactoryObject;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

public class FactoryStep {

    private WarehouseItem itemToManipulate;
    private FactoryObject factoryObject;
    private final String stepType;
    private final Factory factory;
    private final int amountOfItems;

    public FactoryStep(Factory factory, String itemToManipulate, int amountOfItems, String factoryObjectName, String stepType)
    {
        for(var item : factory.getAvailableWarehouseItems())
        {
            if(item.getName().equals(itemToManipulate))
                this.itemToManipulate = item;
        }

        if(this.itemToManipulate == null)
            factory.addLog("Item " + itemToManipulate + " not found");

        for (var factoryObject :
                factory.getFactoryObject()) {
            if(factoryObject.getName().equals(factoryObjectName))
                this.factoryObject = factoryObject;
        }

        if(factoryObject == null)
            factory.addLog("Factory Object " + factoryObjectName + " not found");

        this.stepType = stepType;
        this.factory = factory;
        this.amountOfItems = amountOfItems;
    }

    public boolean doStep()
    {
        addStepMessage();
        factory.increaseCurrentTimeStep();
        return factoryObject.doWork(factory.getCurrentTimeStep(), itemToManipulate, amountOfItems, stepType);
    }

    private void addStepMessage()
    {
        var message = "Item: " + this.itemToManipulate.getName() + " Amount " + this.amountOfItems + " FO: " + this.factoryObject.getName() + " Step: " + this.stepType;
        factory.addLog(message);
    }
}
