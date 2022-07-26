package logistikoptimierung.Entities.WarehouseItems;

/**
 * Indicates a warehouse item in the simulation (Material, Product, Order)
 * This class should only  be extended by a different class.
 */
public class WarehouseItem {

    private final String name;
    private final String itemId;
    private final WarehouseItemType itemType;

    /**
     * Creates a warehouse item with an id, name and the item type
     * @param itemId indicates the id
     * @param name indicates the name
     * @param itemType indicates the item type
     */
    public WarehouseItem(String itemId, String name, WarehouseItemType itemType)
    {
        this.name = name;
        this.itemId = itemId;
        this.itemType = itemType;
    }

    /**
     * @return the name of the warehouse item
     */
    public String getName() {
        return name;
    }

    /**
     * @return the id of the item
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * @return the item type of the warehouse item
     */
    public WarehouseItemType getItemType() {
        return itemType;
    }

    @Override
    public String toString()
    {
        return this.name;
    }

    @Override
    public boolean equals(Object o)
    {
        if(!(o instanceof WarehouseItem))
            return false;

        return this.name.equals(((WarehouseItem) o).getName());
    }
}
