package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

public class FactoryObject {

    private final String name;
    private final Factory factory;

    public FactoryObject(String name, Factory factory)
    {
        this.name = name;
        this.factory = factory;
    }

    public Factory getFactory() {
        return factory;
    }

    public String getName() {
        return name;
    }

    public void doWork(WarehouseItem item, int amountOfItems, String stepType)
    {
        factory.addLog("Not Implemented");
    }
}
