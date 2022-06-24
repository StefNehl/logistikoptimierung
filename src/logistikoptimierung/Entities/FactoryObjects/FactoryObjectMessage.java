package logistikoptimierung.Entities.FactoryObjects;

public record FactoryObjectMessage(int timeStep, String message, String factoryObjectType) {
    @Override
    public String toString()
    {
        var messageString = timeStep + " " + factoryObjectType + ": " + message;
        return messageString;
    }
}
