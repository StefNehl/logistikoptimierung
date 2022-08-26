package logistikoptimierung.Entities.WarehouseItems;

/**
 * Create a material position which handles the warehouse item and the amount.
 * Material Positions are used for transport and in the warehouse
 * @param item the item in the material position
 * @param amount the amount of the item in the material postion
 */
public record MaterialPosition(WarehouseItem item, int amount) {

    @Override
    public String toString()
    {
        return item.getName() + " Amount: " + amount;
    }
}
