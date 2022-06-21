package logistikoptimierung.Entities.WarehouseItems;

public class Order extends WarehouseItem {

    private final Product product;
    private final int amount;
    private final double income;
    private final int travelTime;

    public Order(String name, Product product, int amount, double income, int travelTime)
    {
        super(name);
        this.product = product;
        this.amount = amount;
        this.income = income;
        this.travelTime = travelTime;
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

    public int getTravelTime() {
        return travelTime;
    }
}
