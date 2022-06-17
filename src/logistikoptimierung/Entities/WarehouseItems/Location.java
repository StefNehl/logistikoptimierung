package logistikoptimierung.Entities.WarehouseItems;

public class Location {

    private final String name;
    private final boolean isCustomer;
    private final int travelTimeToWarehouse;

    public Location(String name, boolean isCustomer, int travelTimeToWarehouse)
    {
        this.name = name;
        this.isCustomer = isCustomer;
        this.travelTimeToWarehouse = travelTimeToWarehouse;
    }

    public String getName() {
        return name;
    }

    public boolean isCustomer() {
        return isCustomer;
    }

    public int getTravelTimeToWarehouse() {
        return travelTimeToWarehouse;
    }
}
