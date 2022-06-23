package logistikoptimierung.Entities.WarehouseItems;

public class WarehouseItem {

    private final String name;
    private final String itemId;
    private boolean isMaterial;

    public WarehouseItem(String itemId, String name, boolean isMaterial)
    {
        this.name = name;
        this.itemId = itemId;
        this.isMaterial = isMaterial;
    }

    public String getName() {
        return name;
    }

    public String getItemId() {
        return itemId;
    }

    public boolean isMaterial() {
        return isMaterial;
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
