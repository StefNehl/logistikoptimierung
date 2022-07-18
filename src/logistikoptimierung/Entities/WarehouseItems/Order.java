package logistikoptimierung.Entities.WarehouseItems;

public class Order extends WarehouseItem
{
    private final String area;
    private MaterialPosition product;
    private final double income;
    private final String transportType;
    private final String engine;
    private final int travelTime;
    private final int orderNr;

    public Order(String orderId,
                 int orderNr,
                 String area,
                 MaterialPosition product,
                 double income,
                 String transportType,
                 String engine,
                 int travelTime)
    {
        super(orderId, orderId, WarehouseItemTypes.Order);
        this.orderNr = orderNr;
        this.area = area;
        this.product = product;
        this.income = income;
        this.transportType = transportType;
        this.engine = engine;
        this.travelTime = travelTime;
    }

    public int getOrderNr() {
        return orderNr;
    }

    public String getArea() {
        return area;
    }

    public MaterialPosition getProduct() {
        return product;
    }

    public void deductProductAmount(int amountToDeduct)
    {
        this.product = new MaterialPosition(this.product.item(), this.product.amount() - amountToDeduct);
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

    public Order createCopyOfOrder()
    {

        var materialPosition = new MaterialPosition(getProduct().item(),
                getProduct().amount());

        var newOrder = new Order(super.getItemId(), this.orderNr, this.area, materialPosition, this.income, this.transportType, this.engine, this.travelTime);
        return newOrder;
    }
}
