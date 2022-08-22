package logistikoptimierung.Entities.WarehouseItems;

/**
 * Create a warehouse position which handles a warehouse item and the amount.
 * Warehouse Positions are used for transportation, production, order handling and in the warehouse
 * @param item the item in the material position
 * @param amount the amount of the item in the material postion
 */
public record WarehousePosition(WarehouseItem item, int amount) {

    @Override
    public String toString()
    {
        return item.getName() + " Amount: " + amount;
    }

    public WarehousePosition copy()
    {
        return new WarehousePosition(this.item, this.amount);
    }
}
