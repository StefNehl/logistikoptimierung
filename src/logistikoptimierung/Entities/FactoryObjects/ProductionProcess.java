package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.MaterialPosition;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.List;

public class ProductionProcess
{
    private final WarehouseItem productToProduce;
    private final int productionBatchSize;
    private final int productionTime;

    private final Production production;
    List<MaterialPosition> materialPositions;

    /**
     * Creates the production process for a product to produce
     * @param productToProduce the product to produce
     * @param productionBatchSize the batch size of the product
     * @param productionTime the production time
     * @param production the parent production
     * @param materialPositions list of material position needed for the production
     */
    public ProductionProcess(WarehouseItem productToProduce, int productionBatchSize,
                             int productionTime,
                             Production production,
                             List<MaterialPosition> materialPositions)
    {
        this.productToProduce = productToProduce;
        this.productionBatchSize = productionBatchSize;
        this.productionTime = productionTime;
        this.production = production;
        this.materialPositions = materialPositions;
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
     * @return gets a list of the material position which are needed for the production
     */
    public List<MaterialPosition> getMaterialPositions() {
        return materialPositions;
    }

    /**
     * Returns the amount for a warehouse item which is needed for the production process to produce
     * @param item warehouse item
     * @return the amount, 0 if the process does not need the warehouse item for the production
     */
    public int getAmountFromMaterialPositions(WarehouseItem item)
    {
        for(var position : this.materialPositions)
        {
            if(position.item().equals(item))
                return position.amount();
        }
        return 0;
    }
}
