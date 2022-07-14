package logistikoptimierung.Services.EnumeratedCalculation;

import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

public record PlanningItem(WarehouseItem item, int amount) {
}
