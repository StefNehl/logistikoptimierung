package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

public class FactoryObject {

    private final String objectId;
    private final String name;
    private Factory factory;
    private final String factoryObjectType;

    public FactoryObject(String name, String objectId, String factoryObjectType)
    {
        this.name = name;
        this.objectId = objectId;
        this.factoryObjectType = factoryObjectType;
    }

    public Factory getFactory() {
        return factory;
    }

    public void setFactory(Factory factory)
    {
        this.factory = factory;
    }

    public String getName() {
        return this.objectId + " " + name;
    }

    public boolean doWork(int timeStep, WarehouseItem item, int amountOfItems, String stepType)
    {
        factory.addLog("Not Implemented", factoryObjectType);
        return false;
    }

    public void addLogMessage(String message)
    {
        this.factory.addLog(message, factoryObjectType);
    }

    public void addBlockMessage(String message, String stepType)
    {
        this.factory.addBlockLog(message, stepType, factoryObjectType);
    }

    @Override
    public String toString()
    {
        return this.objectId + " " + this.name;
    }
}
