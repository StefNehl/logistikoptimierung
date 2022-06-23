package logistikoptimierung.Entities.WarehouseItems;

import java.util.List;

public class Product extends WarehouseItem {

    private final String machineType;

    public Product(String name,
                   String productId,
                   String machineType)
    {
        super(productId, name, WarehouseItemTypes.Product);
        this.machineType = machineType;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    public String getMachineType() {
        return machineType;
    }
}
