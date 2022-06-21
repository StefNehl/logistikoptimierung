package logistikoptimierung.Entities.WarehouseItems;

import java.util.List;

public class Product extends WarehouseItem {

    private final List<MaterialPosition> billOfMaterial;
    private final int assemblyTime;
    private final String machineType;
    private final int batchSize;

    public Product(String name,
                   String productId,
                   List<MaterialPosition> billOfMaterial,
                   int assemblyTime,
                   int batchSize,
                   String machineType)
    {
        super(productId, name);
        this.billOfMaterial = billOfMaterial;
        this.assemblyTime = assemblyTime;
        this.machineType = machineType;
        this.batchSize = batchSize;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    public List<MaterialPosition> getBillOfMaterial()
    {
        return billOfMaterial;
    }


    public int getAssemblyTime() {
        return assemblyTime;
    }

    public String getMachineType() {
        return machineType;
    }

    public int getBatchSize() {
        return batchSize;
    }
}
