package logistikoptimierung.Entities.WarehouseItems;

/**
 * Creates the warehouse type order
 */
public class Order extends WarehouseItem
{
    private final String area;
    private MaterialPosition product;
    private final double income;
    private final String transportType;
    private final String engine;
    private final int travelTime;
    private final int orderNr;

    /**
     * Creates a new object for the order.
     * @param orderId sets the order id
     * @param orderNr sets the order nr
     * @param area sets the area for transportation
     * @param product sets the production with the amount which is needed as material position
     * @param income sets the income of the
     * @param transportType sets the transport type for transportation
     * @param engine sets the engine for transportation
     * @param travelTime sets the travel time for transportation
     */
    public Order(String orderId,
                 int orderNr,
                 String area,
                 MaterialPosition product,
                 double income,
                 String transportType,
                 String engine,
                 int travelTime)
    {
        super(orderId, orderId, WarehouseItemType.Order);
        this.orderNr = orderNr;
        this.area = area;
        this.product = product;
        this.income = income;
        this.transportType = transportType;
        this.engine = engine;
        this.travelTime = travelTime;
    }

    /**
     * @return the order nr
     */
    public int getOrderNr() {
        return orderNr;
    }

    /**
     * @return the area
     */
    public String getArea() {
        return area;
    }

    /**
     * @return the product as material position with the needed amount
     */
    public MaterialPosition getProduct() {
        return product;
    }

    /**
     * Reduce the material position needed by the amount to deduct
     * @param amountToDeduct amount to deduct
     */
    public void deductProductAmount(int amountToDeduct)
    {
        this.product = new MaterialPosition(this.product.item(), this.product.amount() - amountToDeduct);
    }

    /**
     * @return the income of the order
     */
    public double getIncome() {
        return income;
    }

    /**
     * @return the transport type
     */
    public String getTransportType() {
        return transportType;
    }

    /**
     * @return the engine
     */
    public String getEngine() {
        return engine;
    }

    /**
     * @return the travel time needed for the transporter
     */
    public int getTravelTime() {
        return travelTime;
    }

    /**
     * Creates a full copy (with a copy of the material position) of the order
     * @return a new copied order
     */
    public Order createCopyOfOrder()
    {

        var materialPosition = new MaterialPosition(getProduct().item(),
                getProduct().amount());

        return new Order(super.getItemId(), this.orderNr, this.area, materialPosition, this.income, this.transportType, this.engine, this.travelTime);
    }
}
