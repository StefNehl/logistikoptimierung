package logistikoptimierung.Entities.WarehouseItems;

public class Order extends WarehouseItem {

    private final Product product;
    private final int amount;
    private final double income;
    private final Location targetLocation;

    public Order(String name, Product product, int amount, double income, Location targetLocation)
    {
        super(name);
        this.product = product;
        this.amount = amount;
        this.income = income;
        this.targetLocation = targetLocation;
    }

    public Product getProduct() {
        return product;
    }

    public int getAmount() {
        return amount;
    }

    public double getIncome() {
        return income;
    }

    public Location getTargetLocation() {
        return targetLocation;
    }
}
