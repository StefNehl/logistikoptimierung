package logistikoptimierung.Services.FirstComeFirstServeOptimizer;

import logistikoptimierung.Entities.FactoryObjects.Production;

/**
 * A planning item for the production
 */
public class ProductionPlanningItem
{
    private long blockedTime;
    private final Production production;

    /**
     * Creates a new production planning item
     * @param production production to plan
     */
    public ProductionPlanningItem(Production production)
    {
        this.blockedTime = 0;
        this.production = production;
    }

    /**
     * @return the production which is planned
     */
    public Production getProduction() {
        return production;
    }

    /**
     * @return the blocked time of the planning item
     */
    public long getBlockedTime() {
        return blockedTime;
    }

    /**
     * Increases the blocked time
     * @param timeToAdd increase the blocked time with the time to add
     */
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
