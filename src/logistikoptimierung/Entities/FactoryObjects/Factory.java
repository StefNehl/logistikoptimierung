package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.*;

import java.util.ArrayList;
import java.util.List;

public class Factory {

    private final String name;
    private long currentTimeStep;
    private double currentIncome;
    private long startTime;

    private final Warehouse warehouse;
    private final List<Production> productions;
    private final List<Transporter> transporters;
    private final List<Driver> drivers;
    private final List<Material> suppliedMaterials;
    private final List<Product> availableProducts;
    private final List<Order> workingOrderList;
    private final List<Order> orderList;

    private final List<FactoryObjectMessage> logMessages = new ArrayList<>();
    private boolean printDriverMessages;
    private boolean printProductionMessages;
    private boolean printTransportMessages;
    private boolean printWarehouseMessages;
    private boolean printWarehouseStockChangeMessages;
    private boolean printFactoryMessages;
    private boolean printFactoryStepMessages;
    private boolean printOnlyCompletedFactoryStepMessages;
    private boolean printCompleteWarehouseStockAfterChangeMessages;

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
     * @param orderList sets the orders
     */
    public Factory(String name,
                   int warehouseCapacity,
                   List<Production> productions,
                   int nrOfDrivers,
                   List<Transporter> transporters,
                   List<Material> suppliedMaterials,
                   List<Product> availableProducts,
                   List<Order> orderList) {
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
        this.orderList = orderList;

        //Working Order List is used for storing the remaining amount of items in an Order
        //This list changes => copy the list
        this.workingOrderList = new ArrayList<>();
        for(var order : orderList)
        {
            var copyOfOrder = order.createCopyOfOrder();
            workingOrderList.add(copyOfOrder);
        }
    }

    /**
     * Starts the simulation. The simulation iterates from 0 to the max run time, in seconds,  and tries to perform every task in the
     * factory steps every iteration. The simulation stops if it reaches the max run time or if no factory step is left to perform.
     * The factory message settings sets the amount of messages which are printed in the console while the simulation. The not printed
     * messages are stored in the object.
     * @param factorySteps sets the factory steps to perform
     * @param maxRunTime sets the maximum runtime after the simulation stops
     * @param factoryMessageSettings sets the amount of printed messages in the console
     * @return the time step after the factory stops (in seconds)
     */
    public long startFactory(List<FactoryStep> factorySteps, long maxRunTime, FactoryMessageSettings factoryMessageSettings)
    {
        logMessages.clear();
        this.printDriverMessages = factoryMessageSettings.printDriverMessages();
        this.printProductionMessages = factoryMessageSettings.printProductionMessages();
        this.printTransportMessages = factoryMessageSettings.printTransportMessages();
        this.printWarehouseMessages = factoryMessageSettings.printWarehouseMessages();
        this.printFactoryMessages = factoryMessageSettings.printFactoryMessage();
        this.printFactoryStepMessages = factoryMessageSettings.printFactoryStepMessages();
        this.printOnlyCompletedFactoryStepMessages = factoryMessageSettings.printOnlyCompletedFactoryStepMessages();
        this.printWarehouseStockChangeMessages = factoryMessageSettings.printWarehouseStockChangeMessages();
        this.printCompleteWarehouseStockAfterChangeMessages = factoryMessageSettings.printCurrentWarehouseStockAfterChangeMessages();

        int hourCount = 1;
        addLog("Hour: " + hourCount, FactoryObjectMessageTypes.Factory);

        var copyOfSteps = new ArrayList<>(factorySteps);
        for(long i = startTime; i <= maxRunTime; i++)
        {
            int oneHourInSeconds = 3600;
            if(i % oneHourInSeconds == 0)
            {
                hourCount++;
                addLog("Hour: " + hourCount, FactoryObjectMessageTypes.Factory);
            }

            this.currentTimeStep = i;
            var remainingSteps = new ArrayList<>(copyOfSteps);
            for (var step : remainingSteps)
            {
                if(step.getDoTimeStep() > this.currentTimeStep)
                    continue;

                if(step.doStep())
                    copyOfSteps.remove(step);
            }

            if(copyOfSteps.isEmpty())
                return i;

        }
        this.getWarehouse().addCompleteWarehouseStockMessage();
        return maxRunTime;
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
        for(var order : orderList)
        {
            var copyOfOrder = order.createCopyOfOrder();
            workingOrderList.add(copyOfOrder);
        }
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
        var process = getProductionProcessForWarehouseItem(product);
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
     * Gets the every production processes for a specific product to produce with the respect of the batch size.
     * The list contains every batch which needs to be produced to fulfill the amount of the product.
     * @param product the product to produce
     * @param amount the amount needed for the product to produce
     * @return list with production processes, empty list if no production process was found for the item.
     */
    public List<MaterialPosition> getMaterialPositionsForProductWithRespectOfBatchSize(WarehouseItem product, int amount)
    {
        var materialList = new ArrayList<MaterialPosition>();
        addMaterialPositionRecursiveToListWithRespectOfBatchSize(product,1,  amount, materialList);
        return materialList;
    }

    private void addMaterialPositionRecursiveToListWithRespectOfBatchSize(WarehouseItem item,
                                                    int batchSize,
                                                    int nrOfBatches,
                                                    List<MaterialPosition> materialPositions)
    {
        var process = getProductionProcessForWarehouseItem(item);
        if(process != null)
        {
            var processBatchSizeForProduct = process.getProductionBatchSize();
            var amountNeeded = batchSize * nrOfBatches;
            var nrOfBatchesToProduce = (int) Math.ceil((double) amountNeeded / (double) processBatchSizeForProduct);
            var amountToProduce = nrOfBatchesToProduce * processBatchSizeForProduct;

            var newMaterialPosition = new MaterialPosition(item, amountToProduce);
            materialPositions.add(newMaterialPosition);

            for (var subItem : process.getMaterialPositions())
            {
                var subProcess = getProductionProcessForWarehouseItem(subItem.item());
                if(subProcess != null)
                {
                    addMaterialPositionRecursiveToListWithRespectOfBatchSize(subItem.item(),
                            subItem.amount(),
                            nrOfBatchesToProduce,
                            materialPositions);
                }
                else
                {
                    var subMaterialPosition = new MaterialPosition(subItem.item(), subItem.amount() * nrOfBatchesToProduce);
                    materialPositions.add(subMaterialPosition);
                }
            }
        }
    }

    /**
     * Returns the production process for the product.
     * @param item product
     * @return the production process, null if no process was found
     */
    public ProductionProcess getProductionProcessForWarehouseItem(WarehouseItem item)
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
     * @return the order list
     */
    public List<Order> getOrderList() {
        return orderList;
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
        var newMessage = new FactoryObjectMessage(currentTimeStep, message, factoryObjectType);
        this.logMessages.add(newMessage);
        switch (factoryObjectType)
        {
            case Driver -> {
                if(this.printDriverMessages)
                    System.out.println(newMessage);
            }
            case Factory -> {
                if(this.printFactoryMessages)
                    System.out.println(newMessage);
            }
            case FactoryStep -> {
                if(!this.printFactoryStepMessages)
                    break;

                if(this.printOnlyCompletedFactoryStepMessages)
                {
                    if(completed)
                        System.out.println(newMessage);
                    break;
                }

                System.out.println(newMessage);
            }
            case Production -> {
                if(this.printProductionMessages)
                    System.out.println(newMessage);
            }
            case Transporter -> {
                if(this.printTransportMessages)
                    System.out.println(newMessage);
            }
            case WarehouseStock ->{
                if(this.printWarehouseStockChangeMessages)
                    System.out.println(newMessage);
            }
            case Warehouse -> {
                if(this.printWarehouseMessages)
                    System.out.println(newMessage);
            }
            case CurrentWarehouseStock -> {
                if(this.printCompleteWarehouseStockAfterChangeMessages)
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
     * @param fromTimeStamp
     * @param toTimeStamp
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

}
