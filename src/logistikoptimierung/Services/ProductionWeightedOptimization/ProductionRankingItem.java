package logistikoptimierung.Services.ProductionWeightedOptimization;

import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

public class ProductionRankingItem
{
    private WarehouseItem product;
    private int amount;
    private int nrOfBatches;
    private long time;
    private int ranking;

    public ProductionRankingItem(WarehouseItem product, int amount)
    {
        this.product = product;
        this.amount = amount;
    }

    public WarehouseItem getProduct() {
        return product;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount)
    {
        this.amount = amount;
    }

    public int getNrOfBatches() {
        return nrOfBatches;
    }

    public void setNrOfBatches(int nrOfBatches) {
        this.nrOfBatches = nrOfBatches;
    }

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString()
    {
        return product.getName() + " Amount: " + this.amount +  " Nr of batches: " + nrOfBatches + " T: " + time + " R: " + ranking;
    }
}
