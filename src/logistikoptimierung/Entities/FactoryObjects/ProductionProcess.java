package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.WarehousePosition;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.List;

/**
 * The production process for a product
 */
public class ProductionProcess
{
    private final WarehouseItem productToProduce;
    private final int productionBatchSize;
    private final int productionTime;

    private final Production production;
    List<WarehousePosition> warehousePositions;

    /**
     * Creates the production process for a product to produce.
     * @param productToProduce the product to produce
     * @param productionBatchSize the batch size of the product
     * @param productionTime the production time
     * @param production the parent production
     * @param warehousePositions list of warehouse positions (materials or products) needed for the production
     */
    public ProductionProcess(WarehouseItem productToProduce, int productionBatchSize,
                             int productionTime,
                             Production production,
                             List<WarehousePosition> warehousePositions)
    {
        this.productToProduce = productToProduce;
        this.productionBatchSize = productionBatchSize;
        this.productionTime = productionTime;
        this.production = production;
        this.warehousePositions = warehousePositions;
    }

    /**
     * @return the product which is produced
     */
    public WarehouseItem getProductToProduce() {
        return productToProduce;
    }

    /**
     * @return the batch size
     */
    public int getProductionBatchSize() {
        return productionBatchSize;
    }

    /**
     * @return the production time
     */
    public int getProductionTime() {
        return productionTime;
    }

    /**
     * @return gets the parent production
     */
    public Production getProduction() {
        return production;
    }

    /**
     * @return gets a list of the warehouse positions (material, product) which are needed for the production
     */
    public List<WarehousePosition> getMaterialPositions() {
        return warehousePositions;
    }

    /**
     * Returns the amount for a warehouse item which is needed for the production process to produce
     * @param item warehouse item
     * @return the amount, 0 if the process does not need the warehouse item for the production
     */
    public int getAmountFromMaterialPositions(WarehouseItem item)
    {
        for(var position : this.warehousePositions)
        {
            if(position.item().equals(item))
                return position.amount();
        }
        return 0;
    }

    @Override
    public String toString()
    {
        return "Product " + getProductToProduce().toString() + " BatchSize: " + getProductionBatchSize();
    }
}
