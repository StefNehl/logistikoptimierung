package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.ArrayList;
import java.util.List;

public class Warehouse
{
    private final List<WarehouseItem> warehouseItems;
    private int remainingWarehouseCapacity;
    private final String name;
    private final Factory factory;

    public Warehouse(String name, int warehouseCapacity, Factory factory)
    {
        this.name = name;
        this.factory = factory;
        this.warehouseItems = new ArrayList<>();
        this.remainingWarehouseCapacity = warehouseCapacity;
    }

    public void addItemToWarehouse(WarehouseItem item)
    {
        if(remainingWarehouseCapacity == 0)
        {
            addCapacityReachedMessage();
            return;
        }

        remainingWarehouseCapacity--;
        warehouseItems.add(item);
        addAddItemMessage(item);
    }

    public WarehouseItem removeItemFromWarehouse(WarehouseItem itemToGet)
    {
        for (var item : warehouseItems)
        {
            if(item.getName().equals(itemToGet.getName()))
            {
                warehouseItems.remove(item);
                remainingWarehouseCapacity++;
                addItemRemovedMessage(itemToGet);
                return item;
            }
        }

        addItemNotFoundMessage(itemToGet);
        return null;
    }

    public String getName() {
        return name;
    }

    public List<WarehouseItem> getWarehouseItems() {
        return warehouseItems;
    }

    private void addCapacityReachedMessage()
    {
        var message = this.name + " Capacity reached";
        this.factory.addLog(message);
    }

    private void addAddItemMessage(WarehouseItem item)
    {
        var message = this.name + " Task: add item " + item.getName() + " RC: " + this.remainingWarehouseCapacity;
        this.factory.addLog(message);
    }

    private void addItemNotFoundMessage(WarehouseItem item)
    {
        var message = this.name + " " + item.getName() + " not found in warehouse";
        this.factory.addLog(message);
    }

    private void addItemRemovedMessage(WarehouseItem item)
    {
        var message = this.name + " Task: remove " + item.getName() + " RC: " + this.remainingWarehouseCapacity;
        this.factory.addLog(message);
    }
}
