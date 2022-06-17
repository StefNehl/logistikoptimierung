package logistikoptimierung.Entities.WarehouseItems;

public class Location {

    private final String name;
    private final boolean isCustomer;
    private final double travelTimeToWarehouse;

    public Location(String name, boolean isCustomer, double travelTimeToWarehouse)
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

    public double getTravelTimeToWarehouse() {
        return travelTimeToWarehouse;
    }
}
