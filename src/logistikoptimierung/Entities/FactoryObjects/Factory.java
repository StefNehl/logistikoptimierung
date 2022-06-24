package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.*;

import java.util.ArrayList;
import java.util.List;

public class Factory {

    private final String name;
    private long currentTimeStep;
    private double currentIncome;
    private final long startTime;
    private final long runTime;

    private final Warehouse warehouse;
    private final List<Production> productions;
    private final List<Transporter> transporters;
    private final List<Driver> drivers;
    private final List<Material> suppliedMaterials;
    private final List<Product> availableProducts;
    private final List<Order> orderList;

    private final List<FactoryObjectMessage> logMessages = new ArrayList<>();
    private final boolean printDriverMessages;
    private final boolean printProductionMessages;
    private final boolean printTransportMessages;
    private final boolean printWarehouseMessages;
    private final boolean printFactoryMessages;
    private final boolean printFactoryStepMessages;
    private final boolean printOnlyCompletedFactoryStepMessages;

    private final int oneHourInSeconds = 3600;

    public Factory(String name,
                   int warehouseCapacity,
                   List<Production> productions,
                   int nrOfDrivers,
                   List<Transporter> transporters,
                   List<Material> suppliedMaterials,
                   List<Product> availableProducts,
                   List<Order> orderList,
                   long runTime,
                   boolean printDriverMessages,
                   boolean printFactoryMessage,
                   boolean printFactoryStepMessages,
                   boolean printOnlyCompletedFactoryStepMessages,
                   boolean printProductionMessages,
                   boolean printTransportMessages,
                   boolean printWarehouseMessages) {
        this.name = name;
        this.currentTimeStep = 0;
        this.currentIncome = 0;
        this.printDriverMessages = printDriverMessages;
        this.printProductionMessages = printProductionMessages;
        this.printTransportMessages = printTransportMessages;
        this.printWarehouseMessages = printWarehouseMessages;
        this.printFactoryMessages = printFactoryMessage;
        this.printFactoryStepMessages = printFactoryStepMessages;
        this.printOnlyCompletedFactoryStepMessages = printOnlyCompletedFactoryStepMessages;

        this.startTime = 1;
        this.runTime = runTime;
        this.warehouse = new Warehouse("WH", warehouseCapacity, this);
        this.productions = new ArrayList<>(productions);
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

    public void startFactory(List<FactoryStep> factorySteps)
    {
        int hourCount = 1;
        addLog("Hour: " + hourCount, FactoryObjectTypes.Factory);

        for(long i = startTime; i <= runTime; i++)
        {
            if(i % oneHourInSeconds == 0)
            {
                hourCount++;
                addLog("Hour: " + hourCount, FactoryObjectTypes.Factory);
            }

            this.currentTimeStep = i;
            var handledFactoryObject = new ArrayList<FactoryObject>();
            var copyOfSteps = new ArrayList<>(factorySteps);
            for (var step : copyOfSteps)
            {
                if(handledFactoryObject.contains(step.getFactoryObject()))
                    continue;
                handledFactoryObject.add(step.getFactoryObject());
                if(step.doStep())
                {
                    factorySteps.remove(step);
                    if(factorySteps.isEmpty())
                        return;
                }
            }
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

    public int getNrOfDrivers()
    {
        return drivers.size();
    }

    public Driver getNotBlockedDriver()
    {
        for(var driver : drivers)
        {
            if(currentTimeStep > driver.getBlockedUntilTimeStep())
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

    public List<ProductionProcess> getProductionProcessesForProduct(Product product)
    {
        var processList = new ArrayList<ProductionProcess>();
        addProcessesRecursiveToList(product, processList);
        return processList;
    }

    public void addProcessesRecursiveToList(Product product, List<ProductionProcess> productionProcesses)
    {
        for (var production : productions)
        {
            var process = production.getProductionProcessForProduct(product);
            if(process != null)
            {
                productionProcesses.add(process);
                for(var subProduct : process.getMaterialPositions())
                {
                    if(subProduct.item().getItemType().equals(WarehouseItemTypes.Product))
                        addProcessesRecursiveToList((Product) subProduct.item(), productionProcesses);
                }
            }
        }
    }

    public List<Material> getSuppliedMaterials() {
        return suppliedMaterials;
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
            case FactoryObjectTypes.Warehouse -> {
                if(this.printWarehouseMessages)
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
        for (var message : this.logMessages)
        {
            System.out.println(message);
        }
    }

}
