package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.MaterialPosition;
import logistikoptimierung.Entities.WarehouseItems.Product;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.List;

public class ProductionProcess
{
    private WarehouseItem productToProduce;
    private int productionBatchSize;
    private int productionTime;

    List<MaterialPosition> materialPositions;

    public ProductionProcess(WarehouseItem productToProduce, int productionBatchSize,
                             int productionTime, List<MaterialPosition> materialPositions)
    {
        this.productToProduce = productToProduce;
        this.productionBatchSize = productionBatchSize;
        this.productionTime = productionTime;
        this.materialPositions = materialPositions;
    }

    public WarehouseItem getProductToProduce() {
        return productToProduce;
    }

    public int getProductionBatchSize() {
        return productionBatchSize;
    }

    public int getProductionTime() {
        return productionTime;
    }

    public List<MaterialPosition> getMaterialPositions() {
        return materialPositions;
    }
}
