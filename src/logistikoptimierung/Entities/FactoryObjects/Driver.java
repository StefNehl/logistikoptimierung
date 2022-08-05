package logistikoptimierung.Entities.FactoryObjects;

/**
 * This class creates a Driver for a Transporter. It does not support any specific tasks.
 * If the driver is assigned to a Transporter the transporter object sets the blocked time to the same value as the
 * transporter.
 */
public class Driver extends FactoryObject
{
    /**
     * This class creates a Driver for a Transporter. It does not support any specific tasks.
     * If the driver is assigned to a Transporter the transporter object sets the blocked time to the same value as the
     * transporter.
     * @param name sets the name
     * @param id sets the id
     */
    public Driver(String name, int id) {
        super(name, "D" + id, FactoryObjectMessageTypes.Driver);
    }

    /**
     * Resets the object and sets the blocked until time step back to 0.
     */
    public void resetDriver()
    {
        super.setBlockedUntilTimeStep(0);
    }

}
