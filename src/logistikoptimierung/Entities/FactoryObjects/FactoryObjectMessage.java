package logistikoptimierung.Entities.FactoryObjects;

public record FactoryObjectMessage(long timeStep, String message, String factoryObjectType) {
    @Override
    public String toString()
    {
        return timeStep + " " + factoryObjectType + ": " + message;
    }
}
