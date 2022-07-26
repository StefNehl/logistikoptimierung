package logistikoptimierung.Services.FirstComeFirstServeOptimizer;

import logistikoptimierung.Entities.FactoryObjects.Transporter;

/**
 * Planning item for the transporters
 */
public class TransporterPlanningItem
{
    private long blockedTime = 0;
    private Transporter transporter;

    /**
     * Creates a planning item for the transporter
     * @param transporter transporter which is planned
     */
    public TransporterPlanningItem(Transporter transporter)
    {
        this.blockedTime = 0;
        this.transporter = transporter;
    }

    /**
     * @return the transporter
     */
    public Transporter getTransporter() {
        return transporter;
    }

    /**
     * @return the blocked time
     */
    public long getBlockedTime() {
        return blockedTime;
    }

    /**
     * @param timeToAdd increases the blocked time of the planning item
     */
    public void increaseBlockedTime(long timeToAdd)
    {
        blockedTime += timeToAdd;
    }

    @Override
    public String toString()
    {
        return this.getTransporter().getName() + " BlockedUntil: " + this.getBlockedTime();
    }
}
