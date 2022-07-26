package logistikoptimierung.Entities.WarehouseItems;

/**
 * Creates a product
 */
public class Product extends WarehouseItem {

    /**
     * Creates a new product
     * @param name sets the name
     * @param productId sets the id
     */
    public Product(String name,
                   String productId)
    {
        super(productId, name, WarehouseItemType.Product);
    }

    @Override
    public String getName() {
        return super.getName();
    }
}
