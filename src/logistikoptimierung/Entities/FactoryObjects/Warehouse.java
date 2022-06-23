package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.WarehouseItems.MaterialPosition;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.ArrayList;
import java.util.List;

public class Warehouse
{
    private final List<MaterialPosition> warehouseItems;
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

    public void addItemToWarehouse(MaterialPosition materialPosition)
    {
        if(remainingWarehouseCapacity < materialPosition.amount())
        {
            addCapacityReachedMessage();
            return;
        }

        remainingWarehouseCapacity = remainingWarehouseCapacity - materialPosition.amount();

        if(warehouseItems.contains(materialPosition.item()))
        {
            var index = warehouseItems.indexOf(materialPosition.item());
            var itemToOverwrite = warehouseItems.get(index);
            var newPosition = new MaterialPosition(materialPosition.item(),
                    itemToOverwrite.amount() + materialPosition.amount());
            warehouseItems.set(index, newPosition);
        }
        else
        {
            warehouseItems.add(materialPosition);
        }
        addAddItemMessage(materialPosition);
    }

    public MaterialPosition removeItemFromWarehouse(MaterialPosition materialPosition)
    {
        for (var item : warehouseItems)
        {
            if(item.item().equals(materialPosition.item()))
            {
                var index = warehouseItems.indexOf(item);
                var itemToOverwrite = warehouseItems.get(index);

                if(itemToOverwrite.amount() < materialPosition.amount())
                {
                    addItemNotFoundMessage(materialPosition.item());
                    return null;
                }

                remainingWarehouseCapacity = remainingWarehouseCapacity + item.amount();
                var newPosition = new MaterialPosition(materialPosition.item(),
                        itemToOverwrite.amount() - materialPosition.amount());
                warehouseItems.set(index, newPosition);
                addItemRemovedMessage(materialPosition);
                return item;
            }
        }

        addItemNotFoundMessage(materialPosition.item());
        return null;
    }

    public boolean checkIfMaterialIsAvailable(WarehouseItem warehouseItem, int amount)
    {
        for (var item : warehouseItems)
        {
            if(item.item().equals(warehouseItem))
            {
                var index = warehouseItems.indexOf(item);
                var itemToOverwrite = warehouseItems.get(index);

                if(itemToOverwrite.amount() < amount)
                {
                    addItemNotFoundMessage(warehouseItem);
                    return false;
                }
                else
                    return true;

            }
        }

        addItemNotFoundMessage(warehouseItem);
        return false;
    }

    public String getName() {
        return name;
    }

    public List<MaterialPosition> getWarehouseItems() {
        return warehouseItems;
    }

    private void addCapacityReachedMessage()
    {
        var message = this.name + " Capacity reached";
        this.factory.addLog(message);
    }

    private void addAddItemMessage(MaterialPosition item)
    {
        var message = this.name + " Task: add item " + item.item().getName() +" amount: " + item.amount() + " RC: " + this.remainingWarehouseCapacity;
        this.factory.addLog(message);
    }

    private void addItemNotFoundMessage(WarehouseItem item)
    {
        var message = this.name + " " + item.getName() + " not found or no enough amount in warehouse";
        this.factory.addLog(message);
    }

    private void addItemRemovedMessage(MaterialPosition item)
    {
        var message = this.name + " Task: remove " + item.item().getName() +" amount: " + item.amount() + " RC: " + this.remainingWarehouseCapacity;
        this.factory.addLog(message);
    }
}
