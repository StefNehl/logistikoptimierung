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

    private final int oneHourInSeconds = 3600;

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
    }

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
        this.printCompleteWarehouseStockAfterChangeMessages = factoryMessageSettings.printCompleteWarehouseStockAfterChangeMessages();

        int hourCount = 1;
        addLog("Hour: " + hourCount, FactoryObjectTypes.Factory);

        var copyOfSteps = new ArrayList<>(factorySteps);
        for(long i = startTime; i <= maxRunTime; i++)
        {
            if(i % oneHourInSeconds == 0)
            {
                hourCount++;
                addLog("Hour: " + hourCount, FactoryObjectTypes.Factory);
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
    }

    public long getCurrentTimeStep() {
        return currentTimeStep;
    }

    public String getName() {
        return this.name;
    }

    public List<Transporter> getTransporters() {
        return transporters;
    }

    public List<Production> getProductions()
    {
        return productions;
    }

    public int getNrOfDrivers()
    {
        return drivers.size();
    }

    public List<Driver> getDrivers()
    {
        return this.drivers;
    }

    public Driver getNotBlockedDriver()
    {
        for(var driver : drivers)
        {
            if(currentTimeStep >= driver.getBlockedUntilTimeStep())
                return driver;
        }

        return null;
    }

    public List<FactoryObject> getFactoryObject() {
        var result = new ArrayList<FactoryObject>();
        result.addAll(transporters);
        result.addAll(productions);

        return result;
    }

    public Warehouse getWarehouse() {
        return this.warehouse;
    }

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

    public List<Material> getSuppliedMaterials() {
        return suppliedMaterials;
    }


    public boolean checkIfItemHasASupplier(WarehouseItem item)
    {
        for (var suppliedMaterial : this.suppliedMaterials)
        {
            if(suppliedMaterial.getName().equals(item.getName()))
                return true;
        }
        return false;
    }

    public List<Product> getAvailableProducts() {
        return availableProducts;
    }

    public List<Order> getOrderList() {
        return orderList;
    }

    public List<WarehouseItem> getAvailableWarehouseItems()
    {
        var result = new ArrayList<WarehouseItem>();
        result.addAll(suppliedMaterials);
        result.addAll(availableProducts);
        result.addAll(orderList);

        return result;
    }

    public void increaseIncome(double additionalIncome)
    {
        this.currentIncome += additionalIncome;
        var message = "Income increase, new income: " + additionalIncome;
        addLog(message, FactoryObjectTypes.Factory);
    }

    public double getCurrentIncome() {
        return currentIncome;
    }

    public void addLog(String message, String factoryObjectType, boolean completed)
    {
        var newMessage = new FactoryObjectMessage(currentTimeStep, message, factoryObjectType);
        this.logMessages.add(newMessage);
        switch (factoryObjectType)
        {
            case FactoryObjectTypes.Driver -> {
                if(this.printDriverMessages)
                    System.out.println(newMessage);
            }
            case FactoryObjectTypes.Factory -> {
                if(this.printFactoryMessages)
                    System.out.println(newMessage);
            }
            case FactoryObjectTypes.FactoryStep -> {
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
            case FactoryObjectTypes.Production -> {
                if(this.printProductionMessages)
                    System.out.println(newMessage);
            }
            case FactoryObjectTypes.Transporter -> {
                if(this.printTransportMessages)
                    System.out.println(newMessage);
            }
            case FactoryObjectTypes.WarehouseStock ->{
                if(this.printWarehouseStockChangeMessages)
                    System.out.println(newMessage);
            }
            case FactoryObjectTypes.Warehouse -> {
                if(this.printWarehouseMessages)
                    System.out.println(newMessage);
            }
            case FactoryObjectTypes.CompleteWarehouseStock -> {
                if(this.printCompleteWarehouseStockAfterChangeMessages)
                    System.out.println(newMessage);
            }
        }
    }

    public void addLog(String message, String factoryObjectType)
    {
        addLog(message, factoryObjectType, true);
    }


    public void addBlockLog(String name, String stepType, String factoryObjectType)
    {
        addLog(name + " is blocked from Task: " + stepType, factoryObjectType);
    }

    public void printAllLogMessage() {
        printLogMessageFromTo(-1, -1);
    }

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
