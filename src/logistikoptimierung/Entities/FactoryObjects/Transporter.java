package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.StepTypes;
import logistikoptimierung.Entities.WarehouseItems.Material;
import logistikoptimierung.Entities.WarehouseItems.MaterialPosition;
import logistikoptimierung.Entities.WarehouseItems.Order;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.ArrayList;

public class Transporter extends FactoryObject
{

    private final String area;
    private final String type;
    private final String engine;
    private final int capacity;
    private double remainingDrivingTime;
    private int blockedUntilTimeStep;

    private MaterialPosition loadedItem;
    private String currentTask;

    public Transporter(String name, String area, String type, String engine, int maxCapacity)
    {
        super(name);
        this.area = area;
        this.type = type;
        this.engine = engine;
        this.capacity = maxCapacity;
        this.blockedUntilTimeStep = 0;
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
                this.loadedItem = getMaterialFromSupplier(amountOfItems, (Material) item);
            case StepTypes.MoveMaterialFromTransporterToWarehouse -> {
                this.getFactory().getWarehouse().addItemToWarehouse(loadedItem);
                this.loadedItem = null;
            }
            case StepTypes.ConcludeOrderTransportToCustomer -> {
                var order = (Order) item;
                if(!isOrderComplete(order))
                {
                    super.getFactory().addLog("Not enough products (" + order.getProduct().item().getName() + ") for order: " + order.getName());
                    return false;
                }

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

    private MaterialPosition getMaterialFromSupplier(int amount, Material material)
    {
        if(amount > this.capacity)
        {
            addCapacityExceededMessage(material, amount);
            return null;
        }

        if(!areTransportationConstraintsFulfilled(material))
        {
            addTransportationConstraintNotFulfilledMessage(material);
            return null;
        }

        var drivingTime = material.getTravelTime() * 2;
        this.blockedUntilTimeStep = this.getFactory().getCurrentTimeStep() + drivingTime;

        var newPosition = new MaterialPosition(material, amount);
        addDriveLogMessage(newPosition);

        return newPosition;
    }

    public boolean areTransportationConstraintsFulfilled(Material material)
    {
        if(!material.getArea().equals(this.area))
            return false;

        if(material.getEngine().equals("x") &&
                !material.getEngine().equals(this.engine))
            return false;

        return !material.checkTransportType(this.type);
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

    private boolean getOrderToCustomer(Order order)
    {
        return false;
    }

    private double getProductsAndSell(Order order)
    {
        return order.getIncome();
    }

    private void addDriveLogMessage(MaterialPosition position)
    {
        var message = super.getName() + " Task: get Material " + position.item().getName() + " Amount: " + position.amount() + " RemainingDrivingTime: " + this.remainingDrivingTime;
        super.getFactory().addLog(message);
    }

    private void addCapacityExceededMessage(Material material, int amount)
    {
        var message = super.getName() + ": Capacity exceeded for " + material.getName() + " amount: " + amount;
        super.getFactory().addLog(message);
    }

    private void addTransportationConstraintNotFulfilledMessage(WarehouseItem material)
    {
        var message = super.getName() + ": Transport constraints not fulfilled for " + material.getName();
        super.getFactory().addLog(message);
    }
}
