package logistikoptimierung.Services.FirstComeFirstServeOptimizer;

import logistikoptimierung.Entities.FactoryObjects.Transporter;

public class TransporterPlanningItem
{
    private long blockedTime = 0;
    private Transporter transporter;

    public TransporterPlanningItem(Transporter transporter)
    {
        this.blockedTime = 0;
        this.transporter = transporter;
    }

    public Transporter getTransporter() {
        return transporter;
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
        return this.getTransporter().getName() + " BlockedUntil: " + this.getBlockedTime();
    }
}
