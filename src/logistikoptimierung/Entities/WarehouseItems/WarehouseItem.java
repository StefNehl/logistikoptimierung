package logistikoptimierung.Entities.WarehouseItems;

public class WarehouseItem {

    private final String name;
    private final String itemId;

    public WarehouseItem(String itemId, String name)
    {
        this.name = name;
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public String getItemId() {
        return itemId;
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
