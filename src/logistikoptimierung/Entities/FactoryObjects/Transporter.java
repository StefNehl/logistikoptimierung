package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.FactoryObjects.FactoryObject;
import logistikoptimierung.Entities.StepTypes;
import logistikoptimierung.Entities.WarehouseItems.Material;
import logistikoptimierung.Entities.WarehouseItems.Order;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.ArrayList;
import java.util.List;

public class Transporter extends FactoryObject
{

    private final String type;
    private final String engine;
    private final int capacity;
    private double remainingDrivingTime;

    public Transporter(String name, String type, String engine, int maxSize, double maxDrivingTime, Factory factory)
    {
        super(name, factory);
        this.type = type;
        this.engine = engine;
        this.capacity = maxSize;
        this.remainingDrivingTime = maxDrivingTime;
    }

    @Override
    public void doWork(WarehouseItem item, int amountOfItems, String stepType)
    {
        switch (stepType)
        {
            case StepTypes.GetMaterialFromSuppliesAndMoveToWarehouse -> {
                var materials = getMaterialFromSupplier(amountOfItems, (Material) item);
                moveItemsToWarehouse(materials);
            }
            case StepTypes.ConcludeOrderTransportToCustomer -> {
                var order = (Order) item;
                if(!getProductsForOrderFromWarehouse(order))
                {
                    super.getFactory().addLog("Not enough products (" + order.getProduct() + ") for order: " + order.getName());
                    return;
                }

                if(!getOrderToCustomer((Order)item))
                {
                    super.getFactory().addLog("Order " + item.getName() + " not completed");
                    return;
                }

                var income = getProductsAndSell((Order) item);
                super.getFactory().increaseIncome(income);

            }
        }
    }

    public String getType() {
        return type;
    }

    public String getEngine() {
        return engine;
    }

    public int getCapacity() {
        return capacity;
    }

    /*
    gets the amount of the specific material and deducts the time for every drive
    returns the items which are possible to get in the remaining driving time of the transporter
     */
    private List<WarehouseItem> getMaterialFromSupplier(int amount, Material material)
    {
        var materialList = new ArrayList<WarehouseItem>();

        while (amount != 0)
        {
            //to and back from the supplier
            var timeToDeduct = material.getSupplierLocation().getTravelTimeToWarehouse() * 2;
            if(remainingDrivingTime < timeToDeduct)
            {
                addDriveTimeReachedException(amount, material);
                return materialList;
            }

            remainingDrivingTime -= timeToDeduct;

            var remainingCap = capacity;
            while (remainingCap != 0)
            {
                materialList.add(material);
                amount--;
                if(amount == 0)
                    break;
                remainingCap--;
            }

            addDriveLogMessage(capacity - remainingCap, material);
        }

        return materialList;
    }

    private boolean getProductsForOrderFromWarehouse(Order order)
    {
        var warehouse = this.getFactory().getWarehouse();

        for(int i = 0; i < order.getAmount(); i++)
        {
            var product = warehouse.removeItemFromWarehouse(order.getProduct());
            if(product == null)
                return false;
        }

        return true;
    }

    private boolean getOrderToCustomer(Order order)
    {
        var amount = order.getAmount();
        while (amount != 0)
        {
            //to and back from the customer
            var timeToDeduct = order.getTargetLocation().getTravelTimeToWarehouse() * 2;
            if(remainingDrivingTime < timeToDeduct)
            {
                addDriveTimeReachedException(amount, order);
                return false;
            }

            remainingDrivingTime -= timeToDeduct;

            var remainingCap = capacity;
            while (remainingCap != 0)
            {
                amount--;
                if(amount == 0)
                    break;
                remainingCap--;
            }

            addDriveLogMessage(capacity - remainingCap, order.getProduct());
        }

        return true;
    }

    private void moveItemsToWarehouse(List<WarehouseItem> materials)
    {
        var warehouse = super.getFactory().getWarehouse();
        for (var m : materials)
        {
            warehouse.addItemToWarehouse(m);
        }
    }

    private double getProductsAndSell(Order order)
    {
        return order.getIncome();
    }

    private void addDriveLogMessage(int amount, WarehouseItem item)
    {
        var message = super.getName() + " Task: get Material " + item.getName() + " Amount: " + amount + " RemainingDrivingTime: " + this.remainingDrivingTime;
        super.getFactory().addLog(message);
    }

    private void addDriveTimeReachedException(int amount, WarehouseItem item)
    {
        var message = super.getName() + " Max drive time reached. " + amount + " from " + item.getName() + " where not delivered";
        super.getFactory().addLog(message);
    }
}
