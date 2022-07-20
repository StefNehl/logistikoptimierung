package logistikoptimierung.Entities.FactoryObjects;

public class Driver extends FactoryObject
{
    private long blockedUntilTimeStep;

    /**
     * This class creates a Driver for a Transporter. It does not support any specific tasks.
     * If the driver is assigned to a Transporter the transporter object sets the blocked time to the same value as the
     * transporter.
     * @param name sets the name
     * @param id sets the id
     */
    public Driver(String name, int id) {
        super(name, "D" + id, FactoryObjectTypes.Driver);
        blockedUntilTimeStep = 0;
    }

    /**
     * Resets the object and sets the blocked until time step back to 0.
     */
    public void resetDriver()
    {
        this.blockedUntilTimeStep = 0;
    }

    /**
     * @return returns the time step until the driver is blocked
     */
    public long getBlockedUntilTimeStep() {
        return blockedUntilTimeStep;
    }

    /**
     * @param blockedUntilTimeStep Sets the time step until the driver is blocked
     */
    public void setBlockedUntilTimeStep(long blockedUntilTimeStep)
    {
        this.blockedUntilTimeStep = blockedUntilTimeStep;
    }
}
