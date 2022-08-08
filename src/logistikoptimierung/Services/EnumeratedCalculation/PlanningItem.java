package logistikoptimierung.Services.EnumeratedCalculation;

import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

/**
 * Record for planning items which is used for the recursive combination in the enumerated calculation
 * @param id unique id of the planning item (every planning item needs a unique id for comparison)
 * @param item to manipulate
 * @param amount amount of items
 * @param planningType type of the manipulation
 */
public record PlanningItem(int id, WarehouseItem item, int amount, PlanningType planningType)
{
    @Override
    public String toString()
    {
        return id + " Item: " + item + " Amount: " + amount + " " + planningType;
    }
}
