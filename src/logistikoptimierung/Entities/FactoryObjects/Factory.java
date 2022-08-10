package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class creates an object for the factory. The factory is the main object and contains the warehouse, transporters,
 * drivers, and productions. The factory also starts the simulation.
 */
public class  Factory {

    private final String name;
    private long currentTimeStep;
    private double currentIncome;
    private long startTime;
    private int nrOfRemainingSteps;
    private int timeStepToJump;

    private final Warehouse warehouse;
    private final List<Production> productions;
    private final List<Transporter> transporters;
    private final List<Driver> drivers;
    private final List<Material> suppliedMaterials;
    private final List<Product> availableProducts;

    private List<Order> workingOrderList;

    private final List<FactoryObjectMessage> logMessages = new ArrayList<>();
    private FactoryMessageSettings factoryMessageSettings;

    /**
     * This class creates an object for the factory. The factory is the main object and contains the warehouse, transporters,
     * drivers, and productions. The factory also starts the simulation.
     * @param name sets the name
     * @param warehouseCapacity sets the capacity of the warehouse
     * @param productions sets the productions with the production processes
     * @param nrOfDrivers sets the nr of available drivers
     * @param transporters sets the transporters
     * @param suppliedMaterials sets the materials for supply
     * @param availableProducts sets the available productions
     */
    public Factory(String name,
                   int warehouseCapacity,
                   List<Production> productions,
                   int nrOfDrivers,
                   List<Transporter> transporters,
                   List<Material> suppliedMaterials,
                   List<Product> availableProducts) {
        this.name = name;
        this.currentTimeStep = 0;
        this.currentIncome = 0;

        this.startTime = 0;
        this.warehouse = new Warehouse("WH", warehouseCapacity, this);
        this.productions = new ArrayList<>(productions);
        for(var production : this.productions)
            production.setFactory(this);

        this.transporters = new ArrayList<>(transporters);
        for(var transporter : this.transporters)
            transporter.setFactory(this);

        drivers = new ArrayList<>();
        for(int i = 0; i < nrOfDrivers; i++)
        {
            drivers.add(new Driver(i + "", i));
        }

        this.suppliedMaterials = new ArrayList<>(suppliedMaterials);
        this.availableProducts = new ArrayList<>(availableProducts);

        this.timeStepToJump = Integer.MAX_VALUE;
        //get smallest time step from production and supplied material
        for(var material : suppliedMaterials)
        {
            if(this.timeStepToJump > material.getTravelTime())
                this.timeStepToJump = material.getTravelTime();
        }

        for(var production : productions)
        {
            for(var process : production.getProductionProcesses())
            if(this.timeStepToJump > process.getProductionTime())
                this.timeStepToJump = process.getProductionTime();
        }

    }

    /**
     * Starts the simulation. The simulation iterates and every step creates a new event time step. At every event time step the simulation
     * tries to perform every task in the factory steps every iteration. The simulation stops if it reaches the max run time, if no factory step is left to perform
     * or no event time step is left.
     * The factory message settings sets the amount of messages which are printed in the console while the simulation. The not printed
     * messages are stored in the object.
     * @param orderList sets the orders
     * @param factorySteps sets the factory steps to perform
     * @param maxRunTime sets the maximum runtime after the simulation stops
     * @param factoryMessageSettings sets the amount of printed messages in the console
     * @return the time step after the factory stops (in seconds)
     */
    public long startFactory(List<Order> orderList,
                             List<FactoryStep> factorySteps,
                             long maxRunTime,
                             FactoryMessageSettings factoryMessageSettings)
    {
        //Working Order List is used for storing the remaining amount of items in an Order
        //This list changes => copy the list
        this.workingOrderList = new ArrayList<>();
        for(var order : orderList)
        {
            var copyOfOrder = order.createCopyOfOrder();
            workingOrderList.add(copyOfOrder);

            //Checks every order if there is a smaller time step
            if(this.timeStepToJump > copyOfOrder.getTravelTime())
                this.timeStepToJump = copyOfOrder.getTravelTime();
        }

        logMessages.clear();
        nrOfRemainingSteps = 0;
        this.factoryMessageSettings = factoryMessageSettings;

        int hourCount = 1;
        addLog("Hour: " + hourCount, FactoryObjectMessageTypes.Factory);

        var copyOfSteps = new ArrayList<>(factorySteps);
        var stepsDone = new ArrayList<FactoryStep>();

        var eventTimeSteps = new ArrayList<Long>();
        long starTime = 0;
        eventTimeSteps.add(starTime);

        while (this.currentTimeStep <= maxRunTime)
        {
            this.currentTimeStep = eventTimeSteps.get(0);
            //System.out.println("Handled: " + this.currentTimeStep);
            var remainingSteps = new ArrayList<>(copyOfSteps);

            for (var step : remainingSteps)
            {
                if(step.getDoTimeStep() > this.currentTimeStep)
                    continue;

                if(!step.areAllStepsBeforeCompleted())
                    continue;

                if(!step.doStep())
                    continue;

                copyOfSteps.remove(step);
                stepsDone.add(step);

                //One time step after the blocked until time
                var newEventTimeStep = step.getFactoryObject().getBlockedUntilTimeStep() + 1;
                if(eventTimeSteps.contains(newEventTimeStep))
                    continue;

                //System.out.println("Added: " + newEventTimeStep);
                for(var timeStep : eventTimeSteps)
                {
                    var currentIndex = eventTimeSteps.indexOf(timeStep);
                    if(timeStep > newEventTimeStep)
                    {
                        eventTimeSteps.add(currentIndex, newEventTimeStep);
                        break;
                    }
                    else if(currentIndex == eventTimeSteps.size() - 1)
                    {
                        eventTimeSteps.add(currentIndex + 1, newEventTimeStep);
                        break;
                    }
                }
            }

            eventTimeSteps.remove(this.currentTimeStep);

            if(eventTimeSteps.isEmpty() && !copyOfSteps.isEmpty())
            {
                //System.out.println("Error");
                //eventTimeSteps.add(this.currentTimeStep);
                break;
            }

            if(eventTimeSteps.isEmpty())
                break;
        }

        nrOfRemainingSteps = copyOfSteps.size();
        this.getWarehouse().addCurrentWarehouseStockMessage();
        return this.currentTimeStep;
    }

    /**
     * @return returns after the simulation, if the max runtime is reached the nr of remaining steps
     */
    public int getNrOfRemainingSteps()
    {
        return this.nrOfRemainingSteps;
    }

    /**
     * Resets the factory and every factory object to its initial state.
     */
    public void resetFactory()
    {
        this.currentTimeStep = 0;
        this.currentIncome = 0;
        this.startTime = 0;

        this.warehouse.resetWarehouse();

        for (var production : productions)
        {
            production.resetProduction();
        }

        for(var transporter : transporters)
        {
            transporter.resetTransporter();
        }

        for(var driver : drivers)
        {
            driver.resetDriver();
        }

        this.workingOrderList.clear();
    }

    /**
     * @return the current time step of the simulation
     */
    public long getCurrentTimeStep() {
        return currentTimeStep;
    }

    /**
     * @return the name of the factory
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return a list of transporters in the factory
     */
    public List<Transporter> getTransporters() {
        return transporters;
    }

    /**
     * @return a list of productions in the factory
     */
    public List<Production> getProductions()
    {
        return productions;
    }

    /**
     * @return the nr of drivers
     */
    public int getNrOfDrivers()
    {
        return drivers.size();
    }

    /**
     * @return a list of the drivers
     */
    public List<Driver> getDrivers()
    {
        return this.drivers;
    }

    /**
     * Returns a list of drivers which are not blocked by a task
     * @return a list of drivers or null if no driver is available
     */
    public Driver getNotBlockedDriver()
    {
        for(var driver : drivers)
        {
            if(currentTimeStep >= driver.getBlockedUntilTimeStep())
                return driver;
        }

        return null;
    }

    /**
     * Returns a list of factory objects which are transporters and productions
     * @return a list of factory objects
     */
    public List<FactoryObject> getFactoryObject() {
        var result = new ArrayList<FactoryObject>();
        result.addAll(transporters);
        result.addAll(productions);

        return result;
    }

    /**
     * @return the warehouse object
     */
    public Warehouse getWarehouse() {
        return this.warehouse;
    }

    /**
     * Gets the every production processes for a specific product to produce.
     * @param product the specific product to produce
     * @return a list or production processes. Returns an empty list if no production process was found for the product.
     */
    public List<ProductionProcess> getProductionProcessesForProduct(WarehouseItem product)
    {
        var processList = new ArrayList<ProductionProcess>();
        addProcessesRecursiveToList(product, processList);
        return processList;
    }

    private void addProcessesRecursiveToList(WarehouseItem product, List<ProductionProcess> productionProcesses)
    {
        if(!(product instanceof Product))
            return;

        var process = getProductionProcessForProduct((Product) product);
        if(process != null)
        {
            productionProcesses.add(process);
            for(var subProduct : process.getMaterialPositions())
            {
                addProcessesRecursiveToList(subProduct.item(), productionProcesses);
            }
        }
    }

    /**
     * Gets the every warehouse item for the production processes for a specific product to produce with the respect of the batch size.
     * The list contains every batch which needs to be produced to fulfill the amount of the product.
     * @param product the product to produce
     * @param amount the amount needed for the product to produce
     * @param produceEverything true if everything gets produced also Stahl and Bauholz (both have also a supplier)
     * @return list with warehouse position, empty list if no production process was found for the item.
     */
    public List<WarehousePosition> getWarehousePositionsForProductWithRespectOfBatchSize(WarehouseItem product, int amount, boolean produceEverything)
    {
        var materialList = new ArrayList<WarehousePosition>();
        addWarehousePositionsRecursiveToListWithRespectOfBatchSize(product,1,  amount, produceEverything,  materialList);
        return materialList;
    }

    private void addWarehousePositionsRecursiveToListWithRespectOfBatchSize(WarehouseItem item,
                                                                            int batchSize,
                                                                            int nrOfBatches,
                                                                            boolean produceEverything,
                                                                            List<WarehousePosition> warehousePositions)
    {
        var process = getProductionProcessForProduct((Product) item);

        if(!produceEverything && checkIfItemHasASupplier(item))
        {
            var subMaterialPosition = new WarehousePosition(item, batchSize * nrOfBatches);
            warehousePositions.add(subMaterialPosition);
            return;
        }

        if(process != null)
        {
            var processBatchSizeForProduct = process.getProductionBatchSize();
            var amountNeeded = batchSize * nrOfBatches;
            var nrOfBatchesToProduce = (int) Math.ceil((double) amountNeeded / (double) processBatchSizeForProduct);
            var amountToProduce = nrOfBatchesToProduce * processBatchSizeForProduct;

            var newMaterialPosition = new WarehousePosition(item, amountToProduce);
            warehousePositions.add(newMaterialPosition);

            for (var subItem : process.getMaterialPositions())
            {
                if(subItem.item() instanceof Product)
                {
                    addWarehousePositionsRecursiveToListWithRespectOfBatchSize(subItem.item(),
                            subItem.amount(),
                            nrOfBatchesToProduce,
                            produceEverything,
                            warehousePositions);
                }
                else if(subItem.item() instanceof Material)
                {
                    var subMaterialPosition = new WarehousePosition(subItem.item(), subItem.amount() * nrOfBatchesToProduce);
                    warehousePositions.add(subMaterialPosition);
                }
            }
        }
    }

    /**
     * Returns the production process for the product.
     * @param item product
     * @return the production process, null if no process was found
     */
    public ProductionProcess getProductionProcessForProduct(Product item)
    {
        for (var production : productions)
        {
            var process = production.getProductionProcessForProduct(item);
            if(process != null)
                return process;
        }
        return null;
    }

    /**
     * Checks if the specific warehouse item (product or material) has a supplier.
     * @param item warehouse item to check
     * @return true if the item has a supplier, false if not
     */
    public boolean checkIfItemHasASupplier(WarehouseItem item)
    {
        for (var suppliedMaterial : this.suppliedMaterials)
        {
            if(suppliedMaterial.getName().equals(item.getName()))
                return true;
        }
        return false;
    }

    /**
     * Returns a list of every warehouse item (products, materials, orders)
     * @return list of warehouse items
     */
    public List<WarehouseItem> getAvailableWarehouseItems()
    {
        var result = new ArrayList<WarehouseItem>();
        result.addAll(suppliedMaterials);
        result.addAll(availableProducts);
        result.addAll(workingOrderList);

        return result;
    }

    /**
     * Increase the income of the factory
     * @param additionalIncome the income which should be added to the current factory income
     */
    public void increaseIncome(double additionalIncome)
    {
        this.currentIncome += additionalIncome;
        var message = "Income increase, new income: " + additionalIncome;
        addLog(message, FactoryObjectMessageTypes.Factory);
    }

    /**
     * @return the current income
     */
    public double getCurrentIncome() {
        return currentIncome;
    }

    /**
     * adds a log message to the factory and print the message in the console if the settings are set for the message
     * @param message message to log
     * @param factoryObjectType the object which has the message
     * @param completed if the factory step was completed
     */
    private void addLog(String message, FactoryObjectMessageTypes factoryObjectType, boolean completed)
    {
        if(!factoryMessageSettings.activateLogging())
            return;

        var newMessage = new FactoryObjectMessage(currentTimeStep, message, factoryObjectType);
        this.logMessages.add(newMessage);
        switch (factoryObjectType)
        {
            case Driver -> {
                if(this.factoryMessageSettings.printDriverMessages())
                    System.out.println(newMessage);
            }
            case Factory -> {
                if(this.factoryMessageSettings.printFactoryMessage())
                    System.out.println(newMessage);
            }
            case FactoryStep -> {
                if(!this.factoryMessageSettings.printFactoryStepMessages())
                    break;

                if(this.factoryMessageSettings.printOnlyCompletedFactoryStepMessages())
                {
                    if(completed)
                        System.out.println(newMessage);
                    break;
                }

                System.out.println(newMessage);
            }
            case Production -> {
                if(this.factoryMessageSettings.printProductionMessages())
                    System.out.println(newMessage);
            }
            case Transporter -> {
                if(this.factoryMessageSettings.printTransportMessages())
                    System.out.println(newMessage);
            }
            case WarehouseStock ->{
                if(this.factoryMessageSettings.printWarehouseStockChangeMessages())
                    System.out.println(newMessage);
            }
            case Warehouse -> {
                if(this.factoryMessageSettings.printWarehouseMessages())
                    System.out.println(newMessage);
            }
            case CurrentWarehouseStock -> {
                if(this.factoryMessageSettings.printCurrentWarehouseStockAfterChangeMessages())
                    System.out.println(newMessage);
            }
        }
    }

    /**
     * adds a log message to the factory and print the message in the console if the settings are set for the message
     * @param message message to log
     * @param factoryObjectType the object which has the message
     */
    public void addLog(String message, FactoryObjectMessageTypes factoryObjectType)
    {
        addLog(message, factoryObjectType, true);
    }

    /**
     * adds a log message for a factory step to the factory and print the message in the console if the settings are set for the message
     * @param message message to log
     * @param factoryObjectType the object which has the message
     * @param completed if the factory step was completed
     */
    public void addFactoryStepLog(String message, FactoryObjectMessageTypes factoryObjectType, boolean completed)
    {
        addLog(message, factoryObjectType, completed);
    }

    /**
     * adds a log message if a factory object was blocked for a task.
     * @param name name of the factory object
     * @param stepType step type which the object wanted to perform
     * @param factoryObjectType type of the factory object
     */
    public void addBlockLog(String name, FactoryStepTypes stepType, FactoryObjectMessageTypes factoryObjectType)
    {
        addLog(name + " is blocked from Task: " + stepType, factoryObjectType);
    }

    /**
     * prints every log message
     */
    public void printAllLogMessage() {
        printLogMessageFromTo(-1, -1);
    }

    /**
     * prints every log message in the time between the from and to time step
     * @param fromTimeStamp start for the messages
     * @param toTimeStamp end for the messages
     */
    public void printLogMessageFromTo(long fromTimeStamp, long toTimeStamp)
    {
        if(logMessages.isEmpty()) {
            System.out.println("No logs");
            return;
        }

        if(fromTimeStamp == -1)
            fromTimeStamp = startTime;

        if(toTimeStamp == -1)
        {
            var indexOfLastItem = logMessages.size() -1;
            var lastItem = logMessages.get(indexOfLastItem);
            toTimeStamp = lastItem.timeStep();
        }

        for (var message : this.logMessages)
        {
            if(message.timeStep() > fromTimeStamp && message.timeStep() < toTimeStamp)
            System.out.println(message);
        }
    }

    /**
     * Returns the working order which is used for storing the state of the current order for the specific order
     * @param order for which the working order is needed
     * @return the working order, returns null if the order was not found
     */
    public Order getWorkingOrderForOrder(Order order)
    {
        for (var workingOrder : workingOrderList)
        {
            if(workingOrder.getOrderNr() == order.getOrderNr())
                return workingOrder;
        }

        return null;
    }
}
