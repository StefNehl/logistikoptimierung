package logistikoptimierung.Services.EnumeratedCalculation;

import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

/**
 * Record for planning items which is used for the recursive combination in the enumerated calculation
 * @param item to manipulate
 * @param amount amount of items
 * @param planningType type of the manipulation
 */
public record PlanningItem(WarehouseItem item, int amount, PlanningType planningType)
{
    @Override
    public String toString()
    {
        return "Item: " + item + " Amount: " + amount + " " + planningType;
    }
}
