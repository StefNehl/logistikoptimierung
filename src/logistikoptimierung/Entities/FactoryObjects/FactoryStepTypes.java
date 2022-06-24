package logistikoptimierung.Entities.FactoryObjects;

public class FactoryStepTypes {

    public static final String GetMaterialFromSuppliesAndMoveBackToWarehouse = "Get material from supplier and move back to warehouse";
    public static final String MoveMaterialFromTransporterToWarehouse = "Move material from transporter to warehouse";
    public static final String MoveMaterialsForProductFromWarehouseToInputBuffer = "Move materials for product from warehouse to input buffer";
    public static final String Produce = "Produce";
    public static final String MoveProductToOutputBuffer = "Move product to output buffer";
    public static final String MoveProductFromOutputBufferToWarehouse = "Move product from output buffer to warehouse";
    public static final String ConcludeOrderTransportToCustomer = "Conclude the order and transport products for order from warehouse to customer";

}
