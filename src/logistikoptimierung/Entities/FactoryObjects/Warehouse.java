package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.WarehousePosition;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Warehouse simulates the stock with the add and remove operation.
 */
public class Warehouse extends FactoryObject
{
    private final List<WarehousePosition> warehouseItems;
    private int remainingWarehouseCapacity;
    private int warehouseCapacity;
    private final FactoryConglomerate factoryConglomerate;

    /**
     * Create an object of the warehouse. This object simulates the add and remove of warehouse items from the warehouse.
     * It has a certain capacity.
     * @param name sets name of the warehouse
     * @param factoryConglomerate sets the factory
     */
    public Warehouse(String name, FactoryConglomerate factoryConglomerate)
    {
        super(name, name, LogMessageTypes.Warehouse);
        this.factoryConglomerate = factoryConglomerate;
        this.warehouseItems = new ArrayList<>();
        this.remainingWarehouseCapacity = warehouseCapacity;
    }

    /**
     * @return the warehouse capacity
     */
    public int getWarehouseCapacity()
    {
        return warehouseCapacity;
    }

    /**
     * warehouseCapacity sets the capacity
     * @param warehouseCapacity sets the warehouse capacity
     */
    public void setWarehouseCapacity(int warehouseCapacity)
    {
        this.warehouseCapacity = warehouseCapacity;
        this.remainingWarehouseCapacity = warehouseCapacity;
    }

    /**
     * Adds an item as warehouse position to the warehouse
     * @param warehousePosition the warehouse position with the warehouse item and the amount to add
     * @return returns true if the add was possible, false if not
     */
    public boolean addItemToWarehouse(WarehousePosition warehousePosition)
    {
        if(remainingWarehouseCapacity < warehousePosition.amount())
        {
            addCapacityReachedMessage();
            return false;
        }

        remainingWarehouseCapacity = remainingWarehouseCapacity - warehousePosition.amount();

        WarehousePosition positionToOverwrite = null;
        var indexToRemove = 0;

        for(var wp : warehouseItems)
        {
            if(wp.item().getName().equals(warehousePosition.item().getName()))
            {
                positionToOverwrite = new WarehousePosition(warehousePosition.item(),
                        wp.amount() + warehousePosition.amount());
                indexToRemove = warehouseItems.indexOf(wp);
            }
        }

        if(positionToOverwrite != null)
        {
            warehouseItems.remove(indexToRemove);
            warehouseItems.add(positionToOverwrite);
        }
        else
        {
            warehouseItems.add(warehousePosition);
        }

        addAddItemMessage(warehousePosition);
        addCurrentWarehouseStockMessage();
        return true;
    }

    /**
     * removes a warehouse position from the warehouse
     * @param warehousePosition the warehouse position to remove (with warehouse item and amount)
     * @return the warehouse position if possible, returns null if not
     */
    public WarehousePosition removeItemFromWarehouse(WarehousePosition warehousePosition)
    {
        for (var item : warehouseItems)
        {
            if(item.item().equals(warehousePosition.item()))
            {
                var index = warehouseItems.indexOf(item);
                var itemToOverwrite = warehouseItems.get(index);

                if(itemToOverwrite.amount() < warehousePosition.amount())
                {
                    addItemNotFoundMessage(warehousePosition.item());
                    return null;
                }

                remainingWarehouseCapacity = remainingWarehouseCapacity + warehousePosition.amount();
                var newPosition = new WarehousePosition(warehousePosition.item(),
                        itemToOverwrite.amount() - warehousePosition.amount());
                warehouseItems.set(index, newPosition);
                addItemRemovedMessage(warehousePosition);
                addCurrentWarehouseStockMessage();
                return item;
            }
        }

        addItemNotFoundMessage(warehousePosition.item());
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
                    //Take care. Performance impact in the simulation
                    //addItemNotFoundMessage(warehouseItem);
                    return false;
                }
                else
                    return true;

            }
        }

        //Take care. Performance impact in the simulation
        //addItemNotFoundMessage(warehouseItem);
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
     * Creates a copy of the current warehouse, with a copy of all the position currently in the warehouse.
     * With the same factory conglomerate.
     * @return a copy of the Warehouse
     */
    public Warehouse copy()
    {
        var newWarehouse = new Warehouse(this.getName(), this.factoryConglomerate);
        newWarehouse.setWarehouseCapacity(this.warehouseCapacity);

        for(var warehousePosition : this.getWarehouseItems())
        {
            var copyOfPosition = warehousePosition.copy();
            newWarehouse.addItemToWarehouse(copyOfPosition);
        }

        return newWarehouse;
    }

    /**
     * @return every item which is available in the warehouse
     */
    public List<WarehousePosition> getWarehouseItems() {
        return warehouseItems;
    }

    private void addCapacityReachedMessage()
    {
        var message = super.getName() + " Capacity reached";
        this.factoryConglomerate.addLog(message, LogMessageTypes.Warehouse);
    }

    private void addAddItemMessage(WarehousePosition item)
    {
        var message = super.getName() + " Task: add item " + item.item().getName() +" amount: " + item.amount() + " RC: " + this.remainingWarehouseCapacity;
        this.factoryConglomerate.addLog(message, LogMessageTypes.WarehouseStock);
    }

    private void addItemNotFoundMessage(WarehouseItem item)
    {
        var message = super.getName() + " " + item.getName() + " not found or not enough amount in warehouse";
        this.factoryConglomerate.addLog(message, LogMessageTypes.Warehouse);
    }

    private void addItemRemovedMessage(WarehousePosition item)
    {
        var message = super.getName() + " Task: remove " + item.item().getName() +" amount: " + item.amount() + " RC: " + this.remainingWarehouseCapacity;
        this.factoryConglomerate.addLog(message, LogMessageTypes.WarehouseStock);
    }

    /**
     * Adds a log message to with the current warehouse stock
     */
    public void addCurrentWarehouseStockMessage()
    {
        if(this.warehouseItems.isEmpty())
            return;

        var message = listToString(this.warehouseItems);
        this.factoryConglomerate.addLog(message, LogMessageTypes.CurrentWarehouseStock);
        this.factoryConglomerate.addLog("Remaining warehouse capacity: " + this.remainingWarehouseCapacity, LogMessageTypes.CurrentWarehouseStock);
    }

    private String listToString(List<WarehousePosition> list)
    {
        StringBuilder stringResult = new StringBuilder("\n" + list.get(0).toString());

        for(int i = 1; i < list.size(); i++)
        {
            stringResult.append("\n").append(list.get(i).toString());
        }

        return stringResult.toString();
    }
}
