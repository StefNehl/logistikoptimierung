package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

public class FactoryObject {

    private final String name;
    private Factory factory;

    public FactoryObject(String name)
    {
        this.name = name;
    }

    public Factory getFactory() {
        return factory;
    }

    public void setFactory(Factory factory)
    {
        this.factory = factory;
    }

    public String getName() {
        return name;
    }

    public boolean doWork(int timeStep, WarehouseItem item, int amountOfItems, String stepType)
    {
        factory.addLog("Not Implemented");
        return false;
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
