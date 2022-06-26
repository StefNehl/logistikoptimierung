package logistikoptimierung.Entities.WarehouseItems;

public record MaterialPosition(WarehouseItem item, int amount) {

    @Override
    public String toString()
    {
        return item.getName() + " Amount: " + amount;
    }
}
