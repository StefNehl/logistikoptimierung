package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

/**
 * Creates a factory object. Factory objects are drivers, transporters, factory steps and productions.
 * This class should only be extended.
 */
public class FactoryObject {

    private final String objectId;
    private final String name;
    private FactoryConglomerate factoryConglomerate;
    private final LogMessageTypes factoryObjectType;
    private long blockedUntilTimeStep;

    /**
     * Creates a factory object. Factory objects are drivers, transporters, factory steps and productions.
     * This class should only be extended.
     * @param name name of the object
     * @param objectId id of the object
     * @param factoryObjectType type of the object
     */
    public FactoryObject(String name, String objectId, LogMessageTypes factoryObjectType)
    {
        this.name = name;
        this.objectId = objectId;
        this.factoryObjectType = factoryObjectType;
    }

    /**
     * @return the factory
     */
    public FactoryConglomerate getFactory() {
        return factoryConglomerate;
    }

    /**
     * @param factoryConglomerate sets the factory
     */
    public void setFactory(FactoryConglomerate factoryConglomerate)
    {
        this.factoryConglomerate = factoryConglomerate;
    }

    /**
     * @return the name of the factory object
     */
    public String getName() {
        return this.objectId + " " + name;
    }

    /**
     * Perform the task with the current factory object.
     * @param timeStep time step to perform
     * @param item warehouse item to manipulate
     * @param amountOfItems amount of items to manipulate
     * @param stepType type of manipulation
     * @return true if the step was correct performed, false if not
     */
    public boolean doWork(long timeStep, WarehouseItem item, int amountOfItems, FactoryStepTypes stepType)
    {
        factoryConglomerate.addLog("Not Implemented", factoryObjectType);
        return false;
    }

    /**
     * Gets the time step until this factory object is blocked
     * @return long time step
     */
    public long getBlockedUntilTimeStep() {
        return blockedUntilTimeStep;
    }

    /**
     * sets the time step until this factory object is blocked
     * @param blockedUntilTimeStep time step in seconds until the factory object is blocked
     */
    public void setBlockedUntilTimeStep(long blockedUntilTimeStep) {
        this.blockedUntilTimeStep = blockedUntilTimeStep;
    }

    /**
     * adds a log message to the factory for the current factory object
     * @param message message to log
     */
    public void addLogMessage(String message)
    {
        this.factoryConglomerate.addLog(message, factoryObjectType);
    }

    /**
     * adds an error log message to the factory for the current factory object
     * @param message message to log
     */
    public void addErrorLogMessage(String message)
    {
        this.factoryConglomerate.addLog(message, factoryObjectType);
    }

    /**
     * adds a block log message to the factory for the current factory object
     * @param message message to log
     * @param stepType step types of the step which was performed
     */
    public void addBlockMessage(String message, FactoryStepTypes stepType)
    {
        this.factoryConglomerate.addBlockLog(message, stepType, factoryObjectType);
    }

    @Override
    public String toString()
    {
        return this.objectId + " " + this.name;
    }
}
