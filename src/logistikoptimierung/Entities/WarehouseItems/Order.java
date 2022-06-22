package logistikoptimierung.Entities.WarehouseItems;

public class Order
{
    private int orderId;
    private String area;
    private final MaterialPosition product;
    private final double income;
    private final String transportType;
    private final String engine;
    private final int travelTime;

    public Order(int orderId, String area,
                 MaterialPosition product,
                 double income, String transportType,
                 String engine, int travelTime)
    {
        this.orderId = orderId;
        this.area = area;
        this.product = product;
        this.income = income;
        this.transportType = transportType;
        this.engine = engine;
        this.travelTime = travelTime;
    }



    public int getOrderId() {
        return orderId;
    }

    public String getArea() {
        return area;
    }

    public MaterialPosition getProduct() {
        return product;
    }

    public double getIncome() {
        return income;
    }

    public String getTransportType() {
        return transportType;
    }

    public String getEngine() {
        return engine;
    }

    public int getTravelTime() {
        return travelTime;
    }
}
