package logistikoptimierung.Entities.FactoryObjects;

/**
 * Sets the message type for filtering the factory log messages
 */
public enum LogMessageTypes
{
    /**
     * Messages for drivers
     */
    Driver,
    /**
     * Messages for factory
     */
    Factory,
    /**
     * Messages for factory step
     */
    FactoryStep,
    /**
     * Messages for production
     */
    Production,
    /**
     * Messages for transporters
     */
    Transporter,
    /**
     * Messages for warehouse
     */
    Warehouse,
    /**
     * Messages for warehouse stock changes
     */
    WarehouseStock,
    /**
     * Messages for current warehouse stock (displays the whole stock in the log messages)
     */
    CurrentWarehouseStock;
}
