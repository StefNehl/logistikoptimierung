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
    private FactoryStepTypes currentTask;

    /**
     * This class creates a Transporter for the factory. This transporter handles the Tasks:
     * - Get material from supplier and drive back to warehouse
     * - Move the material from the transporter to the warehouse
     * - Conclude and bring the product to sell to the customer (from a specific order)
     * - Close the order after every amount of a specific product was delivered
     *
     * Furthermore, the class simulates the blocked time, and the specific properties of the transporter.
     * The properties are: Area, type, engine and capacity
     * @param name set the name of the transporter
     * @param id set the unique ID
     * @param area set the property area
     * @param type set the property type
     * @param engine set the property engine
     * @param maxCapacity set the property max capacity of the transporter
     */
    public Transporter(String name, int id, String area, String type, String engine, int maxCapacity)
    {
        super(name, "T" + id, FactoryObjectMessageTypes.Transporter);
        this.area = area;
        this.type = type;
        this.engine = engine;
        this.capacity = maxCapacity;
        this.blockedUntilTimeStep = 0;
    }

    /**
     * Performs the task with the transporter
     * Task Types are:
     * - GetMaterialFromSuppliesAndMoveBackToWarehouse => Get material from supplier and drive back to warehouse
     * - MoveMaterialFromTransporterToWarehouse => Move the material from the transporter to the warehouse
     * - ConcludeOrderTransportToCustomer => Conclude and bring the product to sell to the customer (from a specific order)
     * - ClosesOrderFromCustomer => Close the order after every amount of a specific product was delivered
     * @param currentTimeStep The current time step of the simulation
     * @param item which warehouse item should be manipulated
     * @param amountOfItems the amount of items which should be manipulated
     * @param stepType what is the task to do
     * @return return true if the task was successfully or false if not
     */
    @Override
    public boolean doWork(long currentTimeStep, WarehouseItem item, int amountOfItems, FactoryStepTypes stepType)
    {
        if(currentTimeStep < blockedUntilTimeStep)
        {
            super.addBlockMessage(super.getName(), currentTask);
            return false;
        }

        currentTask = stepType;
        switch (stepType)
        {
            case GetMaterialFromSuppliesAndMoveBackToWarehouse -> {
                var driver = this.getFactory().getNotBlockedDriver();
                if(driver == null)
                {
                    addNoAvailableDriverLogMessage();
                    return false;
                }
                super.addLogMessage("Free Driver found: " + driver.getName());
                var itemToLoad = getMaterialFromSupplier(amountOfItems, (Material) item, driver);
                if(itemToLoad == null)
                {
                    super.addErrorLogMessage("Could not load item");
                    return false;
                }
                this.loadedItem = itemToLoad;
            }
            case MoveMaterialFromTransporterToWarehouse -> {
                if(this.loadedItem == null)
                {
                    super.addErrorLogMessage("No item to unload");
                    return false;
                }
                var result = this.getFactory().getWarehouse().addItemToWarehouse(loadedItem);
                if(!result)
                    return false;
                this.loadedItem = null;
            }
            case ConcludeOrderTransportToCustomer -> {

                //Check if material is available
                if(!this.getFactory().getWarehouse().checkIfMaterialIsAvailable(((Order)item).getProduct().item(), amountOfItems))
                {
                    super.addErrorLogMessage("Material: " + item + " in the amount: " + amountOfItems + " not available");
                    return false;
                }

                var driver = this.getFactory().getNotBlockedDriver();
                if(driver == null)
                {
                    addNoAvailableDriverLogMessage();
                    return false;
                }

                var workingOrder = this.getFactory().getWorkingOrderForOrder((Order)item);
                if(workingOrder == null)
                {
                    super.addErrorLogMessage("Working order for order not found " + item.getName());
                    return false;
                }

                return getSpecificAmountOfItemsFromOrderToCustomer(workingOrder, amountOfItems, driver);
            }
            case ClosesOrderFromCustomer -> {

                var workingOrder = this.getFactory().getWorkingOrderForOrder((Order)item);
                if(workingOrder == null)
                {
                    super.addErrorLogMessage("Working order for order not found " + item.getName());
                    return false;
                }

                if(workingOrder.getProduct().amount() > 0)
                {
                    super.addErrorLogMessage("Order can't be closed. " + workingOrder.getProduct().amount() + " of " + workingOrder.getProduct().item() + " is still to deliver");
                    return false;
                }
                this.getFactory().increaseIncome(workingOrder.getIncome());
            }
        }
        return true;
    }

    /**
     * Resets the Transporter. The blocked until time step is set to 0 and the current task to the FactoryStepType None
     */
    public void resetTransporter()
    {
        this.blockedUntilTimeStep = 0;
        this.currentTask = FactoryStepTypes.None;
        this.loadedItem = null;
    }

    /**
     * @return the capacity of the transporter
     */
    public int getCapacity() {
        return capacity;
    }

    private MaterialPosition getMaterialFromSupplier(int amount, Material material, Driver driver)
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
        driver.setBlockedUntilTimeStep(this.blockedUntilTimeStep);

        var newPosition = new MaterialPosition(material, amount);
        addDriveLogMessage(newPosition);

        return newPosition;
    }

    /**
     * Checks if the transport constraints of the material are fulfilled from the transporter
     * @param material the material to check
     * @return true for fulfilled and false if this is not the case
     */
    public boolean areTransportationConstraintsFulfilledForMaterial(Material material)
    {
        var area = material.getArea();
        var engine = material.getEngine();
        var transportTypes = material.getTransportTypes();

        return areTransportationConstraintsFulfilled(area, engine, transportTypes);
    }

    /**
     * Checks if the transport constraints of the order are fulfilled from the transporter
     * @param order the order to check
     * @return true for fulfilled and false if this is not the case
     */
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

    private boolean getSpecificAmountOfItemsFromOrderToCustomer(Order order, int amountOfItems, Driver driver)
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

        blockedUntilTimeStep = this.getFactory().getCurrentTimeStep() + order.getTravelTime();
        driver.setBlockedUntilTimeStep(blockedUntilTimeStep);
        this.getFactory().getWarehouse().removeItemFromWarehouse(
                new MaterialPosition(order.getProduct().item(), amountOfItems));
        order.deductProductAmount(amountOfItems);

        return true;
    }

    private void addNoAvailableDriverLogMessage()
    {
        var message = super.getName() + ": no driver available for task: " + currentTask;
        super.addErrorLogMessage(message);
    }

    private void addDriveLogMessage(MaterialPosition position)
    {
        var message = super.getName() + " Task: get Material " + position.item().getName() + " Amount: " + position.amount();
        super.addLogMessage(message);
    }

    private void addCapacityExceededMessage(WarehouseItem item, int amount)
    {
        var message = super.getName() + ": Capacity exceeded for " + item.getName() + " amount: " + amount;
        super.addErrorLogMessage(message);
    }

    private void addTransportationConstraintNotFulfilledMessage(WarehouseItem material)
    {
        var message = super.getName() + ": Transport constraints not fulfilled for " + material.getName();
        super.addErrorLogMessage(message);
    }
}
