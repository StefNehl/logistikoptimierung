package logistikoptimierung.Entities.FactoryObjects;

/**
 * Sets the messages which should be printed in the console for the factory
 * @param printDriverMessages true => prints every message from the driver
 * @param printFactoryMessage true => prints messages from the factory
 * @param printFactoryStepMessages true => prints messages from the factory steps
 * @param printOnlyCompletedFactoryStepMessages true => prints messages only from completed factory steps (printFactoryStepMessages have to be set to true)
 * @param printProductionMessages true => prints messages from the production
 * @param printTransportMessages true => prints messages from the transporters
 * @param printWarehouseMessages true => prints messages from the warehouse
 * @param printWarehouseStockChangeMessages true => prints messages from the warehouse if the stock was changed
 * @param printCurrentWarehouseStockAfterChangeMessages true => prints messages from the current warehouse stock if the stock was changed
 */
public record FactoryMessageSettings(
        boolean activateLogging,
        boolean printDriverMessages,
        boolean printFactoryMessage,
        boolean printFactoryStepMessages,
        boolean printOnlyCompletedFactoryStepMessages,
        boolean printProductionMessages,
        boolean printTransportMessages,
        boolean printWarehouseMessages,
        boolean printWarehouseStockChangeMessages,
        boolean printCurrentWarehouseStockAfterChangeMessages) {
}
