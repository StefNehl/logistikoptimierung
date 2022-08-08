package logistikoptimierung.Entities.WarehouseItems;

/**
 * Creates the warehouse type order
 */
public class Order extends WarehouseItem
{
    private final String area;
    private WarehousePosition warehousePosition;
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
     * @param warehousePosition sets the warehouse with the amount which is needed as warehouse position
     * @param income sets the income of the
     * @param transportType sets the transport type for transportation
     * @param engine sets the engine for transportation
     * @param travelTime sets the travel time for transportation
     */
    public Order(String orderId,
                 int orderNr,
                 String area,
                 WarehousePosition warehousePosition,
                 double income,
                 String transportType,
                 String engine,
                 int travelTime)
    {
        super(orderId, orderId, WarehouseItemType.Order);
        this.orderNr = orderNr;
        this.area = area;
        this.warehousePosition = warehousePosition;
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
     * @return the product as warehouse position with the needed item and amount
     */
    public WarehousePosition getWarehousePosition() {
        return warehousePosition;
    }

    /**
     * Reduce the material position needed by the amount to deduct
     * @param amountToDeduct amount to deduct
     */
    public void deductProductAmount(int amountToDeduct)
    {
        this.warehousePosition = new WarehousePosition(this.warehousePosition.item(), this.warehousePosition.amount() - amountToDeduct);
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

        var materialPosition = new WarehousePosition(getWarehousePosition().item(),
                getWarehousePosition().amount());

        return new Order(super.getItemId(), this.orderNr, this.area, materialPosition, this.income, this.transportType, this.engine, this.travelTime);
    }
}
