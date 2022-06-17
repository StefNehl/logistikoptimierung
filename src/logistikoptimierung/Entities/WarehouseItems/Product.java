package logistikoptimierung.Entities.WarehouseItems;

import java.util.List;

public class Product extends WarehouseItem {

    private final List<MaterialPosition> billOfMaterial;
    private final double assemblyTime;

    public Product(String name, List<MaterialPosition> billOfMaterial, double assemblyTime)
    {
        super(name);
        this.billOfMaterial = billOfMaterial;
        this.assemblyTime = assemblyTime;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    public List<MaterialPosition> getBillOfMaterial()
    {
        return billOfMaterial;
    }

    public double getAssemblyTime() {
        return assemblyTime;
    }
}
