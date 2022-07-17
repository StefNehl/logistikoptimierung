package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.Material;
import logistikoptimierung.Entities.WarehouseItems.MaterialPosition;
import logistikoptimierung.Entities.WarehouseItems.Order;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

public class Transporter extends FactoryObject
{
    private final String area;
    private final String type;
    private final String engine;
    private final int capacity;
    private long blockedUntilTimeStep;

    private MaterialPosition loadedItem;
    private String currentTask;

    public Transporter(String name, int id, String area, String type, String engine, int maxCapacity)
    {
        super(name, "T" + id, FactoryObjectTypes.Transporter);
        this.area = area;
        this.type = type;
        this.engine = engine;
        this.capacity = maxCapacity;
        this.blockedUntilTimeStep = 0;
    }

    @Override
    public boolean doWork(long currentTimeStep, WarehouseItem item, int amountOfItems, String stepType)
    {
        if(currentTimeStep < blockedUntilTimeStep)
        {
            super.addBlockMessage(super.getName(), currentTask);
            return false;
        }

        currentTask = stepType;
        switch (stepType)
        {
            case FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse -> {
                var driver = this.getFactory().getNotBlockedDriver();
                if(driver == null)
                {
                    addNoAvailableDriverLogMessage();
                    return false;
                }
                this.loadedItem = getMaterialFromSupplier(amountOfItems, (Material) item);
            }
            case FactoryStepTypes.MoveMaterialFromTransporterToWarehouse -> {
                this.getFactory().getWarehouse().addItemToWarehouse(loadedItem);
                this.loadedItem = null;
            }
            case FactoryStepTypes.ConcludeOrderTransportToCustomer -> {

                //Check if material is available
                if(!this.getFactory().getWarehouse().checkIfMaterialIsAvailable(((Order)item).getProduct().item(), amountOfItems))
                {
                    super.addLogMessage("Material: " + item + " in the amount: " + amountOfItems + " not available");
                    return false;
                }

                var driver = this.getFactory().getNotBlockedDriver();
                if(driver == null)
                {
                    addNoAvailableDriverLogMessage();
                    return false;
                }

                return getSpecificAmountOfItemsFromOrderToCustomer((Order)item, amountOfItems);
            }
            case FactoryStepTypes.TransporterClosesOrderFromCustomer -> {

            }
        }
        return true;
    }

    public void resetTransporter()
    {
        this.blockedUntilTimeStep = 0;
        this.currentTask = "";
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

        if(!areTransportationConstraintsFulfilledForMaterial(material))
        {
            addTransportationConstraintNotFulfilledMessage(material);
            return null;
        }

        var drivingTime = material.getTravelTime();
        this.blockedUntilTimeStep = this.getFactory().getCurrentTimeStep() + drivingTime;

        var newPosition = new MaterialPosition(material, amount);
        addDriveLogMessage(newPosition);

        return newPosition;
    }

    public boolean areTransportationConstraintsFulfilledForMaterial(Material material)
    {
        var area = material.getArea();
        var engine = material.getEngine();
        var transportTypes = material.getTransportTypes();

        return areTransportationConstraintsFulfilled(area, engine, transportTypes);
    }

    public boolean areTransportationConstraintsFulfilledForOrder(Order order)
    {
        var area = order.getArea();
        var engine = order.getEngine();
        var transportTypes = new String[]{order.getTransportType()};

        return areTransportationConstraintsFulfilled(area, engine, transportTypes);
    }

    private boolean areTransportationConstraintsFulfilled(String area, String engine,
                                                         String[] transportTypes)
    {
        if(!area.equals(this.area))
            return false;

        if(!engine.equals("x") &&
                !engine.equals(this.engine))
            return false;

        return checkTransportType(transportTypes);
    }

    private boolean checkTransportType(String[] transportTypes)
    {
        if(transportTypes[0].equals("x"))
            return true;

        for (String transportType : transportTypes) {
            if (transportType.equals(type))
                return true;
        }

        return false;
    }

    private boolean getSpecificAmountOfItemsFromOrderToCustomer(Order order, int amountOfItems)
    {
        if(amountOfItems > this.capacity)
        {
            addCapacityExceededMessage(order.getProduct().item(), amountOfItems);
            return false;
        }

        if(!areTransportationConstraintsFulfilledForOrder(order))
        {
            addTransportationConstraintNotFulfilledMessage(order);
            return false;
        }

        blockedUntilTimeStep = order.getTravelTime();
        this.getFactory().getWarehouse().removeItemFromWarehouse(
                new MaterialPosition(order.getProduct().item(), amountOfItems));
        order.deductProductAmount(amountOfItems);

        if(order.getProduct().amount() <= 0)
            this.getFactory().increaseIncome(order.getIncome());
        return true;
    }

    public long getBlockedUntilTimeStep() {
        return blockedUntilTimeStep;
    }



    private void addNoAvailableDriverLogMessage()
    {
        var message = super.getName() + ": no driver available for task: " + currentTask;
        super.addLogMessage(message);
    }

    private void addDriveLogMessage(MaterialPosition position)
    {
        var message = super.getName() + " Task: get Material " + position.item().getName() + " Amount: " + position.amount();
        super.addLogMessage(message);
    }

    private void addCapacityExceededMessage(WarehouseItem item, int amount)
    {
        var message = super.getName() + ": Capacity exceeded for " + item.getName() + " amount: " + amount;
        super.addLogMessage(message);
    }

    private void addTransportationConstraintNotFulfilledMessage(WarehouseItem material)
    {
        var message = super.getName() + ": Transport constraints not fulfilled for " + material.getName();
        super.addLogMessage(message);
    }
}
