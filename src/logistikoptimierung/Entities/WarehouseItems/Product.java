package logistikoptimierung.Entities.WarehouseItems;

public class Product extends WarehouseItem {

    private final String machineType;

    /**
     * Creates a new product
     * @param name sets the name
     * @param productId sets the id
     * @param production sets the production where the product is produced
     */
    public Product(String name,
                   String productId,
                   String production)
    {
        super(productId, name, WarehouseItemType.Product);
        this.machineType = production;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    public String getMachineType() {
        return machineType;
    }
}
