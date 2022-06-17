package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.FactoryStep;
import logistikoptimierung.Entities.StepTypes;
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
    private int runTime;
    private final boolean printLogs;

    private final Warehouse warehouse;
    private final List<Machine> machines;
    private final List<Transporter> transporters;
    private final List<Material> suppliedMaterials;
    private final List<Product> availableProducts;
    private final List<Order> orderList;


    public Factory(String name,
                   int warehouseCapacity,
                   int nrOfMachines,
                   int maxCapacityInputBuffer,
                   int maxCapacityOutputBuffer,
                   int nrOfTransporters,
                   int transportCapacity,
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
        this.runTime = runTime;

        this.warehouse = new Warehouse("WH", warehouseCapacity, this);

        this.machines = new ArrayList<>();
        for (int i = 0; i < nrOfMachines; i++)
            machines.add(
                    new Machine("M" + (i + 1),
                            maxCapacityInputBuffer,
                            maxCapacityOutputBuffer,
                            runTime,
                            this
                    ));

        this.transporters = new ArrayList<>();
        for (int i = 0; i < nrOfTransporters; i++)
            transporters.add(new Transporter("T" + (i + 1),
                    "",
                    "",
                    transportCapacity,
                    runTime,
                    this));

        this.suppliedMaterials = suppliedMaterials;
        this.availableProducts = availableProducts;
        this.orderList = orderList;
    }

    public void startFactory(List<FactoryStep> factorySteps)
    {
        for(int i = 0; i < runTime; i++)
        {
            var copyOfSteps = new ArrayList<>(factorySteps);
            for (var step : copyOfSteps)
            {
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

    public void increaseCurrentTimeStep() {
        this.currentTimeStep++;
    }

    public String getName() {
        return this.name;
    }

    public List<Transporter> getTransporters() {
        return transporters;
    }

    public List<FactoryObject> getFactoryObject() {
        var result = new ArrayList<FactoryObject>();
        result.addAll(transporters);
        result.addAll(machines);

        return result;
    }

    public Warehouse getWarehouse() {
        return this.warehouse;
    }

    public List<Machine> getMachines() {
        return machines;
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
