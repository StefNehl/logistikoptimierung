package logistikoptimierung.Entities.FactoryObjects;

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
    }

    public boolean doStep()
    {
        addStepMessage();
        return factoryObject.doWork(factory.getCurrentTimeStep(), itemToManipulate, amountOfItems, stepType);
    }

    public FactoryObject getFactoryObject() {
        return factoryObject;
    }

    private void addStepMessage()
    {
        factory.addLog(this.toString(), FactoryObjectTypes.FactoryStep);
    }

    @Override
    public String toString()
    {
        return "Item: " + this.itemToManipulate.getName() + " Amount " + this.amountOfItems + " FO: " + this.factoryObject.getName() + " Step: " + this.stepType;
    }
}
