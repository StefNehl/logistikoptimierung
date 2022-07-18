package logistikoptimierung.Services.EnumeratedCalculation;

import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

public record PlanningItem(WarehouseItem item, int amount, PlanningType planningType)
{
    @Override
    public String toString()
    {
        return "Item: " + item + " Amount: " + amount + " " + planningType;
    }
}
