package logistikoptimierung.Services.FirstComeFirstServeOptimizer;

import logistikoptimierung.Entities.FactoryObjects.Factory;

/**
 * A planning item for the production
 */
public class ProductionPlanningItem
{
    private long blockedTime;
    private final Factory factory;

    /**
     * Creates a new production planning item
     * @param factory production to plan
     */
    public ProductionPlanningItem(Factory factory)
    {
        this.blockedTime = 0;
        this.factory = factory;
    }

    /**
     * @return the production which is planned
     */
    public Factory getProduction() {
        return factory;
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
