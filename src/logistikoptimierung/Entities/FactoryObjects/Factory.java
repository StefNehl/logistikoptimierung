package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.FactoryStep;
import logistikoptimierung.Entities.WarehouseItems.Material;
import logistikoptimierung.Entities.WarehouseItems.Order;
import logistikoptimierung.Entities.WarehouseItems.Product;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.ArrayList;
import java.util.List;

public class Factory {

    private final List<String> log;
    private final String name;
    private int currentTimeStep;
    private double currentIncome;
    private final int startTime;
    private final int runTime;
    private final boolean printLogs;

    private final Warehouse warehouse;
    private final List<Production> productions;
    private final List<Transporter> transporters;
    private final List<Driver> drivers;
    private final List<Material> suppliedMaterials;
    private final List<Product> availableProducts;
    private final List<Order> orderList;


    public Factory(String name,
                   int warehouseCapacity,
                   List<Production> productions,
                   int nrOfDrivers,
                   List<Transporter> transporters,
                   List<Material> suppliedMaterials,
                   List<Product> availableProducts,
                   List<Order> orderList,
                   int runTime,
                   boolean printLog) {
        this.name = name;
        this.log = new ArrayList<>();
        this.currentTimeStep = 0;
        this.currentIncome = 0;
        this.printLogs = printLog;

        this.startTime = 1;
        this.runTime = runTime;
        this.warehouse = new Warehouse("WH", warehouseCapacity, this);
        this.productions = new ArrayList<>(productions);
        this.transporters = new ArrayList<>(transporters);

        drivers = new ArrayList<>();
        for(int i = 0; i < nrOfDrivers; i++)
        {
            drivers.add(new Driver(i + ""));
        }

        this.suppliedMaterials = new ArrayList<>(suppliedMaterials);
        this.availableProducts = new ArrayList<>(availableProducts);
        this.orderList = orderList;
    }

    public void startFactory(List<FactoryStep> factorySteps)
    {
        for(int i = startTime; i <= runTime; i++)
        {
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
                }
            }
        }
    }

    public int getCurrentTimeStep() {
        return currentTimeStep;
    }

    public String getName() {
        return this.name;
    }

    public List<Transporter> getTransporters() {
        return transporters;
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

    public List<Production> getProductions() {
        return productions;
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

        return result;
    }

    public void increaseIncome(double additionalIncome)
    {
        this.currentIncome += additionalIncome;
        var message = "Income increase, new income: " + additionalIncome;
        addLog(message);
    }

    public double getCurrentIncome() {
        return currentIncome;
    }

    public void addLog(String message)
    {
        message = this.currentTimeStep + ": " + message;
        log.add(message);
        if(this.printLogs)
            System.out.println(message);
    }

    public void addBlockLog(String name, String stepType)
    {
        addLog(name + " is blocked from Task: " + stepType);
    }

    public void printLog() {
        for (var message :
                log) {
            System.out.println(message);

        }
    }

}
