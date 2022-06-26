package logistikoptimierung.Entities.FactoryObjects;

public record FactoryMessageSettings(                   boolean printDriverMessages,
                                                        boolean printFactoryMessage,
                                                        boolean printFactoryStepMessages,
                                                        boolean printOnlyCompletedFactoryStepMessages,
                                                        boolean printProductionMessages,
                                                        boolean printTransportMessages,
                                                        boolean printWarehouseMessages,
                                                        boolean printWarehouseStockChangeMessages,
                                                        boolean printCompleteWarehouseStockAfterChangeMessages) {
}
