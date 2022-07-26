package logistikoptimierung.Entities.WarehouseItems;

public class WarehouseItem {

    private final String name;
    private final String itemId;
    private WarehouseItemType itemType;

    public WarehouseItem(String itemId, String name, WarehouseItemType itemType)
    {
        this.name = name;
        this.itemId = itemId;
        this.itemType = itemType;
    }

    public String getName() {
        return name;
    }

    public String getItemId() {
        return itemId;
    }

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

        if(this.name.equals(((WarehouseItem) o).getName()))
            return true;

        return false;
    }
}
