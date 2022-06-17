package logistikoptimierung.Entities.WarehouseItems;

public class Material extends WarehouseItem {

    private final Location supplierLocation;

    public Material(String name, Location supplierLocation)
    {
        super(name);
        this.supplierLocation = supplierLocation;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    public Location getSupplierLocation() {
        return supplierLocation;
    }
}
