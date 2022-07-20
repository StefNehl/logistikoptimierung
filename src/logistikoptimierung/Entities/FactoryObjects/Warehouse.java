package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.MaterialPosition;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Warehouse extends FactoryObject
{
    private final List<MaterialPosition> warehouseItems;
    private int remainingWarehouseCapacity;
    private final Factory factory;

    public Warehouse(String name, int warehouseCapacity, Factory factory)
    {
        super(name, name, FactoryObjectTypes.Warehouse);
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

        MaterialPosition warehousePosition = null;
        var indexToRemove = 0;

        for(var wp : warehouseItems)
        {
            if(Objects.equals(wp.item().getName(), materialPosition.item().getName()))
            {
                warehousePosition = new MaterialPosition(materialPosition.item(),
                        wp.amount() + materialPosition.amount());
                indexToRemove = warehouseItems.indexOf(wp);
            }
        }

        if(warehousePosition != null)
        {
            warehouseItems.remove(indexToRemove);
            warehouseItems.add(warehousePosition);
        }
        else
        {
            warehouseItems.add(materialPosition);
        }

        addAddItemMessage(materialPosition);
        addCompleteWarehouseStockMessage();
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
                addCompleteWarehouseStockMessage();
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

    public void resetWarehouse()
    {
        this.warehouseItems.clear();
    }

    public List<MaterialPosition> getWarehouseItems() {
        return warehouseItems;
    }

    private void addCapacityReachedMessage()
    {
        var message = super.getName() + " Capacity reached";
        this.factory.addLog(message, FactoryObjectTypes.Warehouse);
    }

    private void addAddItemMessage(MaterialPosition item)
    {
        var message = super.getName() + " Task: add item " + item.item().getName() +" amount: " + item.amount() + " RC: " + this.remainingWarehouseCapacity;
        this.factory.addLog(message, FactoryObjectTypes.WarehouseStock);
    }

    private void addItemNotFoundMessage(WarehouseItem item)
    {
        var message = super.getName() + " " + item.getName() + " not found or not enough amount in warehouse";
        this.factory.addLog(message, FactoryObjectTypes.Warehouse);
    }

    private void addItemRemovedMessage(MaterialPosition item)
    {
        var message = super.getName() + " Task: remove " + item.item().getName() +" amount: " + item.amount() + " RC: " + this.remainingWarehouseCapacity;
        this.factory.addLog(message, FactoryObjectTypes.WarehouseStock);
    }

    public void addCompleteWarehouseStockMessage()
    {
        if(this.warehouseItems.isEmpty())
            return;

        var message = listToString(this.warehouseItems);
        this.factory.addLog(message, FactoryObjectTypes.CompleteWarehouseStock);
    }

    private String listToString(List<MaterialPosition> list)
    {
        StringBuilder stringResult = new StringBuilder("\n" + list.get(0).toString());

        for(int i = 1; i < list.size(); i++)
        {
            stringResult.append("\n").append(list.get(i).toString());
        }

        return stringResult.toString();
    }
}
