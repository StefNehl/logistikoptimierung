package logistikoptimierung.Entities.FactoryObjects;

public class StepTypes {

    public static final String GetMaterialFromSuppliesAndMoveBackToWarehouse = "Get Material From Supplies And Move back to Warehouse";
    public static final String MoveMaterialFromTransporterToWarehouse = "Move Material From Transporter To Warehouse";
    public static final String MoveMaterialsForProductFromWarehouseToInputBuffer = "Move Materials For Product From Warehouse To InputBuffer";
    public static final String Produce = "Produce";
    public static final String MoveProductToOutputBuffer = "Move Product To Output Buffer";
    public static final String MoveProductFromOutputBufferToWarehouse = "Move Product From Output Buffer To Warehouse";
    public static final String ConcludeOrderTransportToCustomer = "Conclude the order and transport products from order from warehouse to customer";

}
