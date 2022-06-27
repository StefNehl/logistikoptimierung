package logistikoptimierung.Services.FirstComeFirstServeOptimizer;

import logistikoptimierung.Entities.FactoryObjects.Production;

public class ProductionPlanningItem
{
    private long blockedTime = 0;
    private Production production;

    public ProductionPlanningItem(Production production)
    {
        this.blockedTime = 0;
        this.production = production;
    }

    public Production getProduction() {
        return production;
    }

    public long getBlockedTime() {
        return blockedTime;
    }

    public void increaseBlockedTime(long timeToAdd)
    {
        blockedTime += timeToAdd;
    }

    @Override
    public String toString()
    {
        return this.getProduction().getName() + " BlockedUntil: " + this.getBlockedTime();
    }
}
