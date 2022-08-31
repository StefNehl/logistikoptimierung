package logistikoptimierung.Entities.FactoryObjects;

/**
 * Different factory steps which the factory conglomerate can perform
 */
public enum FactoryStepTypes
{
    /**
     * Do nothing
     */
    None,

    /**
     * Get material from supplier and move back to warehouse. Needs a Factory Object Transporter, the warehouse item material to gather
     * and the amount to gather from the material.
     */
    GetMaterialFromSuppliesAndMoveBackToWarehouse,

    /**
     * Move material from transporter to warehouse. Needs a Factory Object Transporter, the warehouse item material to move,
     * and the amount, to move the material from the Transporter to the warehouse. The Transporter needs the warehouse item stored with the amount and the capacity
     * in the warehouse needs to be higher or equal than the amount. Otherwise, the step will fail.
     */
    MoveMaterialFromTransporterToWarehouse,

    /**
     * Move materials for product from warehouse to input buffer. Needs a Factory Object Production, the product which this production
     * should produce and the amount of batches (currently only 1 is supported). Every material needed for the production needs to
     * be in the warehouse, otherwise the step will fail
     */
    MoveMaterialsForProductFromWarehouseToInputBuffer,

    /**
     * Produce. Need a Factory object Production, and alle the materials for the production in the Input buffer of this production.
     * If the material is not in the input buffer or there is no remaining space in the output buffer, this step will fail. 
     * Amount is the nr of batches (currently only 1 is supported).
     */
    Produce,

    /**
     * Move product to output buffer. Needs the Factory Object Production, the produced product and the nr of Batches (only 1).
     * The output buffer needs to have the space for the product and the factory needs to be free, no production in progress,
     * otherwise the step will fail.
     */
    MoveProductToOutputBuffer,

    /**
     * Move product from output buffer to warehouse. Needs the Factory Object Production, the product in the output buffer and the nr of Batches (only 1).
     * The warehouse needs to have remaining capacity, otherwise the step will fail.
     */
    MoveProductFromOutputBufferToWarehouse,

    /**
     * Move the order and transport products for order from warehouse to customer. Needs the Factory Object Transporter,
     * the warehouse item Order which needs to be handled and the amount of the needed product or material in the order.
     * The amount of the material or product needs to be in the warehouse, otherwise this step will fail.
     */
    ConcludeOrderTransportToCustomer,

    /**
     * Transporter closes order from the customer. Needs the Factory Object Transporter, and the warehouse item Order to close.
     * The parameter Amount is ignored in the step. This step will also increase the income of the factory. This step will fail,
     * if the needed amount of the Order > 0. Furthermore, if the amount in the Order was not deduct to zero by the ConcludeOrderTransportToCustomer
     * Step.
     */
    ClosesOrderFromCustomer;
}
