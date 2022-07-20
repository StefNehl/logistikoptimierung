package logistikoptimierung.Entities.FactoryObjects;

/**
 * Creates a log message for the factory.
 * @param timeStep time step when the log happened
 * @param message message to log
 * @param factoryObjectType type of log
 */
public record FactoryObjectMessage(long timeStep, String message, FactoryObjectMessageTypes factoryObjectType) {
    @Override
    public String toString()
    {
        return timeStep + " " + factoryObjectType + ": " + message;
    }
}
