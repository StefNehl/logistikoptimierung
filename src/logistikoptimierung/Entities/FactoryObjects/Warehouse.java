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
    private final int warehouseCapacity;
    private final Factory factory;

    /**
     * Create an object of the warehouse. This object simulates the add and remove of warehouse items from the warehouse.
     * It has a certain capacity.
     * @param name sets name of the warehouse
     * @param warehouseCapacity sets the capacity
     * @param factory sets the factory
     */
    public Warehouse(String name, int warehouseCapacity, Factory factory)
    {
        super(name, name, FactoryObjectMessageTypes.Warehouse);
        this.factory = factory;
        this.warehouseItems = new ArrayList<>();
        this.warehouseCapacity = warehouseCapacity;
        this.remainingWarehouseCapacity = warehouseCapacity;
    }

    /**
     * Adds an item as material position to the warehouse
     * @param materialPosition the material position with the warehouse item and the amount to add
     * @return returns true if the add was possible, false if not
     */
    public boolean addItemToWarehouse(MaterialPosition materialPosition)
    {
        if(remainingWarehouseCapacity < materialPosition.amount())
        {
            addCapacityReachedMessage();
            return false;
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
        addCurrentWarehouseStockMessage();
        return true;
    }

    /**
     * removes a material position from the warehouse
     * @param materialPosition the material position to remove (with warehouse item and amount)
     * @return the material position if possible, returns null if not
     */
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

                remainingWarehouseCapacity = remainingWarehouseCapacity + materialPosition.amount();
                var newPosition = new MaterialPosition(materialPosition.item(),
                        itemToOverwrite.amount() - materialPosition.amount());
                warehouseItems.set(index, newPosition);
                addItemRemovedMessage(materialPosition);
                addCurrentWarehouseStockMessage();
                return item;
            }
        }

        addItemNotFoundMessage(materialPosition.item());
        return null;
    }

    /**
     * checks if an item in a certain amount is in the warehouse
     * @param warehouseItem the warehouse item to check
     * @param amount the needed amount of the item
     * @return true if the item in the amount is available, return false if not
     */
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

    /**
     * resets the warehouse to it's initial state, remove every item in the warehouse
     */
    public void resetWarehouse()
    {
        this.warehouseItems.clear();
        this.remainingWarehouseCapacity = this.warehouseCapacity;
    }

    /**
     * returns every item which is available in the warehouse
     * @return
     */
    public List<MaterialPosition> getWarehouseItems() {
        return warehouseItems;
    }

    private void addCapacityReachedMessage()
    {
        var message = super.getName() + " Capacity reached";
        this.factory.addLog(message, FactoryObjectMessageTypes.Warehouse);
    }

    private void addAddItemMessage(MaterialPosition item)
    {
        var message = super.getName() + " Task: add item " + item.item().getName() +" amount: " + item.amount() + " RC: " + this.remainingWarehouseCapacity;
        this.factory.addLog(message, FactoryObjectMessageTypes.WarehouseStock);
    }

    private void addItemNotFoundMessage(WarehouseItem item)
    {
        var message = super.getName() + " " + item.getName() + " not found or not enough amount in warehouse";
        this.factory.addLog(message, FactoryObjectMessageTypes.Warehouse);
    }

    private void addItemRemovedMessage(MaterialPosition item)
    {
        var message = super.getName() + " Task: remove " + item.item().getName() +" amount: " + item.amount() + " RC: " + this.remainingWarehouseCapacity;
        this.factory.addLog(message, FactoryObjectMessageTypes.WarehouseStock);
    }

    public void addCurrentWarehouseStockMessage()
    {
        if(this.warehouseItems.isEmpty())
            return;

        var message = listToString(this.warehouseItems);
        this.factory.addLog(message, FactoryObjectMessageTypes.CurrentWarehouseStock);
        this.factory.addLog("Remaining warehouse capacity: " + this.remainingWarehouseCapacity, FactoryObjectMessageTypes.CurrentWarehouseStock);
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
