package logistikoptimierung.Entities.FactoryObjects;

/**
 * Different factory steps which the factory can perform
 */
public enum FactoryStepTypes
{
    /**
     * Do nothing
     */
    None,
    /**
     * Get material from supplier and move back to warehouse
     */
    GetMaterialFromSuppliesAndMoveBackToWarehouse,
    /**
     * Move material from transporter to warehouse
     */
    MoveMaterialFromTransporterToWarehouse,
    /**
     * Move materials for product from warehouse to input buffer
     */
    MoveMaterialsForProductFromWarehouseToInputBuffer,
    /**
     * Produce
     */
    Produce,
    /**
     * Move product to output buffer
     */
    MoveProductToOutputBuffer,
    /**
     * Move product from output buffer to warehouse
     */
    MoveProductFromOutputBufferToWarehouse,
    /**
     * Move the order and transport products for order from warehouse to customer
     */
    ConcludeOrderTransportToCustomer,
    /**
     * Transporter closes order from the customer
     */
    ClosesOrderFromCustomer;
}
