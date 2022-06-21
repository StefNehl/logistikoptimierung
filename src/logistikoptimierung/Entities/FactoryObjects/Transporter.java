package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.StepTypes;
import logistikoptimierung.Entities.WarehouseItems.Material;
import logistikoptimierung.Entities.WarehouseItems.Order;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.ArrayList;
import java.util.List;

public class Transporter extends FactoryObject
{

    private final String area;
    private final String type;
    private final String engine;
    private final int capacity;
    private double remainingDrivingTime;
    private int blockedUntilTimeStep;

    private List<WarehouseItem> loadedItems;
    private String currentTask;

    public Transporter(String name, String area, String type, String engine, int maxSize, double maxDrivingTime, Factory factory)
    {
        super(name, factory);
        this.area = area;
        this.type = type;
        this.engine = engine;
        this.capacity = maxSize;
        this.remainingDrivingTime = maxDrivingTime;
        this.blockedUntilTimeStep = 0;
        this.loadedItems = new ArrayList<>();
    }

    @Override
    public boolean doWork(int currentTimeStep, WarehouseItem item, int amountOfItems, String stepType)
    {
        if(currentTimeStep < blockedUntilTimeStep)
        {
            super.getFactory().addBlockLog(super.getName(), currentTask);
            return false;
        }
        currentTask = stepType;
        switch (stepType)
        {
            case StepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse ->
                this.loadedItems.addAll(getMaterialFromSupplier(amountOfItems, (Material) item));
            case StepTypes.MoveMaterialFromTransporterToWarehouse -> {
                var items = new ArrayList<>(this.loadedItems);
                this.loadedItems.clear();
                moveItemsToWarehouse(items);
            }
            case StepTypes.ConcludeOrderTransportToCustomer -> {
                var order = (Order) item;
                if(!isOrderComplete(order))
                {
                    super.getFactory().addLog("Not enough products (" + order.getProduct().getName() + ") for order: " + order.getName());
                    return false;
                }

                var items = getProductsForOrderFromWarehouse(order);
                if(!getOrderToCustomer((Order)item))
                {
                    super.getFactory().addLog("Order " + item.getName() + " not completed");
                    return false;
                }

                var income = getProductsAndSell((Order) item);
                super.getFactory().increaseIncome(income);

            }
        }
        return true;
    }

    public String getArea() { return area; }

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
            if(this.remainingDrivingTime < timeToDeduct)
            {
                addDriveTimeReachedException(amount, material);
                return materialList;
            }

            this.remainingDrivingTime -= timeToDeduct;
            this.blockedUntilTimeStep = this.getFactory().getCurrentTimeStep() + timeToDeduct;

            var remainingCap = this.capacity;
            while (remainingCap != 0)
            {
                materialList.add(material);
                amount--;
                if(amount == 0)
                    break;
                remainingCap--;
            }

            addDriveLogMessage(this.capacity - remainingCap, material);
        }

        return materialList;
    }

    private boolean isOrderComplete(Order order)
    {
        var warehouseItems = new ArrayList<>(this.getFactory().getWarehouse().getWarehouseItems());

        for(int i = 0; i < order.getAmount(); i++)
        {
            var productAvailable = warehouseItems.remove(order.getProduct());
            if(!productAvailable)
                return false;
        }

        return true;
    }

    private List<WarehouseItem> getProductsForOrderFromWarehouse(Order order)
    {
        var warehouse = this.getFactory().getWarehouse();
        var result = new ArrayList<WarehouseItem>();

        for(int i = 0; i < order.getAmount(); i++)
        {
            var product = warehouse.removeItemFromWarehouse(order.getProduct());
            result.add(product);
        }

        return result;
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
