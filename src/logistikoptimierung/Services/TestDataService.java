package logistikoptimierung.Services;

import logistikoptimierung.Contracts.IDataService;
import logistikoptimierung.Entities.*;
import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.FactoryObjects.Machine;
import logistikoptimierung.Entities.FactoryObjects.Transporter;
import logistikoptimierung.Entities.WarehouseItems.*;

import java.util.ArrayList;

public class TestDataService implements IDataService {

    @Override
    public Instance loadData(String filename) {

        if(filename.equals("SmallTestInstance"))
            return createSmallTestInstance();

        if(filename.equals("MediumTestInstance"))
            return createMediumTestInstance();

        return null;
    }

    private Instance createSmallTestInstance()
    {
        var runTimeInMinutes = 100;//5 * 8 * 60;
        int warehouseCapacity = 100;

        int nrOfMachines = 1;
        int capacityInputBuffer = 5;
        int capacityOutputBuffer = 5;

        int nrOfDrivers = 1;
        int capacityOfTransporter = 5;

        var machines = new ArrayList<Machine>();
        var transporters = new ArrayList<Transporter>();

        var location = new Location("S1", false, 10);
        var material = new Material("M1", location);
        var materials = new ArrayList<Material>();
        materials.add(material);

        var bom = new ArrayList<MaterialPosition>();
        bom.add(new MaterialPosition(material, 2));
        var productToProduce = new Product("P1", bom ,10);
        var products = new ArrayList<Product>();
        products.add(productToProduce);

        var orders = new ArrayList<Order>();
        var customerLocation = new Location("C1", true, 10);
        orders.add(new Order("O1", productToProduce, 2, 1000, customerLocation));

        var factory = new Factory("Test 1",
                warehouseCapacity,
                machines,
                nrOfDrivers,
                transporters,
                materials,
                products,
                orders,
                runTimeInMinutes,
                true);

        return new Instance(factory);
    }

    private Instance createMediumTestInstance()
    {
        var runTimeInMinutes = 5 * 8 * 60;
        int warehouseCapacity = 100;

        int nrOfMachines = 3;
        int capacityInputBuffer = 5;
        int capacityOutputBuffer = 5;

        int nrOfDrivers = 7;
        int capacityOfTransporter = 5;

        var machines = new ArrayList<Machine>();
        var transporters = new ArrayList<Transporter>();


        var materials = new ArrayList<Material>();
        var location = new Location("S1", false, 10);
        var material1 = new Material("M1", location);
        materials.add(material1);

        location = new Location("S2", false, 10);
        var material2 = new Material("M2", location);
        materials.add(material2);

        location = new Location("S3", false, 10);
        var material3 = new Material("M3", location);
        materials.add(material3);

        var products = new ArrayList<Product>();
        var bom = new ArrayList<MaterialPosition>();
        bom.add(new MaterialPosition(material1, 2));
        bom.add(new MaterialPosition(material2, 2));
        bom.add(new MaterialPosition(material3, 1));
        var product1 = new Product("P1", bom, 10);
        products.add(product1);

        bom = new ArrayList<>();
        bom.add(new MaterialPosition(material1, 3));
        bom.add(new MaterialPosition(material3, 1));
        var product2 = new Product("P1", bom, 10);
        products.add(product2);

        var orders = new ArrayList<Order>();
        var customerLocation1 = new Location("C1", true, 10);
        orders.add(new Order("O1", product1, 2, 1000, customerLocation1));

        var customerLocation2 = new Location("C2", true, 100);
        orders.add(new Order("O2", product2, 2, 2000, customerLocation2));

        orders.add(new Order("O3", product2, 5, 1500, customerLocation1));

        var factory = new Factory("Test 1",
                warehouseCapacity,
                machines,
                nrOfDrivers,
                transporters,
                materials,
                products,
                orders,
                runTimeInMinutes,
                true);

        return new Instance(factory);
    }
}
