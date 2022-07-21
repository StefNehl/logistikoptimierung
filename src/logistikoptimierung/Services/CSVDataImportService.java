package logistikoptimierung.Services;

import logistikoptimierung.Contracts.IDataService;
import logistikoptimierung.Entities.FactoryObjects.*;
import logistikoptimierung.Entities.Instance;
import logistikoptimierung.Entities.WarehouseItems.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CSVDataImportService implements IDataService
{
    private static final String TRANSPORTER_FILENAME = "Transportmittel.csv";
    private static final String MATERIAL_WITH_TRANSPORTER_FILENAME = "RohstoffeTransportmittel.csv";
    private static final String PRODUCTS_FILENAME = "Products.csv";
    private static final String PRODUCTIONS_FILENAME = "FabrikenMitPuffer.csv";

    public static final String CONTRACT_1 = "Aufträge1.csv";
    public static final String CONTRACT_2 = "Aufträge2.csv";
    public static final String CONTRACT_3 = "Aufträge3.csv";
    public static final String CONTRACT_4 = "Aufträge4.csv";
    public static final String CONTRACT_5 = "Aufträge5.csv";

    private static final String DATA_PATH = "data\\";
    private static final String DELIMITER = ";";

    private int nrOfDrivers = 0;
    private int warehouseCapacity = 0;

    /**
     * Create a object for the CSV Import to load the data and create an instance with the given nr of drivers and warehouse capacity.
     * @param nrOfDrivers nr of drivers in the simulation
     * @param warehouseCapacity capacity of the warehouse in the simulation
     */
    public CSVDataImportService(int nrOfDrivers, int warehouseCapacity)
    {
        this.nrOfDrivers = nrOfDrivers;
        this.warehouseCapacity = warehouseCapacity;
    }

    /**
     * Load the data from the given CSV files and creates the instance for the simulation.
     * @param filename
     * @return
     */
    @Override
    public Instance loadData(String filename)
    {
        var path = System.getProperty("user.dir") + "\\" + DATA_PATH;

        try
        {
            var materials = loadMaterials(
                    loadCsv(path + MATERIAL_WITH_TRANSPORTER_FILENAME));

            var products = loadProducts(
                    loadCsv(path + PRODUCTS_FILENAME));

            var transporters = loadTransporters(
                    loadCsv(path + TRANSPORTER_FILENAME));

            var availableItems = new ArrayList<WarehouseItem>(materials);
            availableItems.addAll(products);

            var productions = loadProduction(
                    loadCsv(path + PRODUCTIONS_FILENAME),
                    availableItems);

            var orders = loadOrders(
                    loadCsv(path + filename), availableItems);


            var factory = new Factory("Test 1",
                    warehouseCapacity,
                    productions,
                    nrOfDrivers,
                    transporters,
                    materials,
                    products,
                    orders);
            return new Instance(factory);
        }
        catch (Exception ex)
        {
            System.out.println(ex);
            ex.printStackTrace();
            return null;
        }
    }

    private List<String[]> loadCsv(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        boolean isFirstLine = true;

        var dataList = new ArrayList<String[]>();

        while ((line = reader.readLine()) != null)
        {
            if(isFirstLine)
            {
                isFirstLine = false;
                continue;
            }

            String[] data = line.split(DELIMITER);
            dataList.add(data);
        }

        reader.close();
        return dataList;
    }

    private List<Transporter> loadTransporters(List<String[]> data)
    {
        var transporters = new ArrayList<Transporter>();

        var idCount = 0;
        for (var dataItem : data)
        {
            var area = dataItem[0];
            var type = dataItem[1];
            var eng = dataItem[2];
            var capacityString = dataItem[3];
            var capacity = Integer.parseInt(capacityString);
            var name = area + "_" + type + "_" + eng + "_" + capacityString;

            var newTransporter = new Transporter(
                    name,
                    idCount,
                    area,
                    type,
                    eng,
                    capacity);
            transporters.add(newTransporter);
            idCount++;
        }

        return transporters;
    }

    private List<Material> loadMaterials(List<String[]> data)
    {
        var materials = new ArrayList<Material>();

        for(var dataItem : data)
        {
            var transportConstraints = dataItem[0].split(",");
            var area = transportConstraints[0];
            var transportTypes = transportConstraints[1]
                    .replace("(", "")
                    .replace(")", "")
                    .split(" ");
            var engine = transportConstraints[2];
            var materialId = dataItem[1].trim();
            var name = dataItem[2].trim();
            var transportTimeString = dataItem[3];
            var transportTime = convertStringToSeconds(transportTimeString);

            var newMaterial = new Material(materialId, name, area, transportTypes, engine, transportTime);
            materials.add(newMaterial);
        }

        return materials;
    }

    private List<Product> loadProducts(List<String[]> data)
    {
        var products = new ArrayList<Product>();

        var productType = "";

        for (var dataItem : data)
        {
            var productId = dataItem[0].trim();
            if(productId.isBlank())
            {
                productType = dataItem[1];
                continue;
            }

            var productName = dataItem[1].trim();
            var newProduct = new Product(productName, productId, productType);
            products.add(newProduct);
        }

        return products;
    }

    private List<Production> loadProduction(List<String[]> data, List<WarehouseItem> items)
    {
        var productionList = new ArrayList<Production>();
        Production currentProduction = null;
        var currentProductionProcesses = new ArrayList<ProductionProcess>();
        var idCount = 0;

        for(var dataItem : data)
        {
            if(dataItem.length == 1)
                continue;
            var productionName = dataItem[0];
            if(!productionName.isBlank())
            {
                var bufferStrings = dataItem[1].split("/");
                var bufferInput = Integer.parseInt(bufferStrings[0].substring(0, 1));
                var bufferOutput = Integer.parseInt(bufferStrings[1].substring(0, 1));

                currentProductionProcesses = new ArrayList<>();
                currentProduction = new Production(
                        productionName,
                        idCount,
                        currentProductionProcesses,
                        bufferInput,
                        bufferOutput);

                productionList.add(currentProduction);
                idCount++;
            }

            var bom = new ArrayList<MaterialPosition>();
            var productName = dataItem[3];
            var productBatchSize = Integer.parseInt(dataItem[2]);
            var productionTimeString = dataItem[4];
            var productionTime = convertStringToSeconds(productionTimeString);

            var products = findWarehouseItem(productName, items);
            for(var product : products)
            {
                var productionProcess = new ProductionProcess(
                        product,
                        productBatchSize,
                        productionTime,
                        currentProduction,
                        bom);

                int startCount = 5;
                while (startCount < dataItem.length && !dataItem[startCount].isBlank())
                {
                    var materialBatchSize = Integer.parseInt(dataItem[startCount]);
                    var materialName = dataItem[startCount + 1];

                    var materials = findWarehouseItem(materialName, items);
                    for(var material : materials )
                    {
                        var materialPosition = new MaterialPosition(material, materialBatchSize);
                        bom.add(materialPosition);
                        startCount = startCount + 2;
                    }
                }

                currentProductionProcesses.add(productionProcess);
            }
        }
        return productionList;
    }

    private List<Order> loadOrders(List<String[]> data, List<WarehouseItem> items)
    {
        var orders = new ArrayList<Order>();
        var count = 1;
        for(var dataItem : data)
        {
            var area = dataItem[0];
            var productName = dataItem[2].trim();
            var product = findWarehouseItem(productName, items);

            var amount = Integer.parseInt(dataItem[3]);
            var materialPosition = new MaterialPosition(product.get(0), amount);

            var income = Integer.parseInt(dataItem[4]);
            var transportType = dataItem[5];
            var engine = dataItem[6];

            //Convert minutes to seconds
            var transportTime = Integer.parseInt(dataItem[7]) * 60;

            var order = new Order("Order " + count, count, area, materialPosition,
                    income, transportType, engine, transportTime);

            orders.add(order);
            count++;
        }

        return orders;
    }

    private List<WarehouseItem> findWarehouseItem(String name, List<WarehouseItem> items)
    {
        var result = new ArrayList<WarehouseItem>();
        for(var item : items)
        {
            if(name.equals(item.getName()))
                result.add(item);
        }
        return result;
    }

    private int convertStringToSeconds(String timeString)
    {
        var time = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm:ss"));
        var seconds = time.toSecondOfDay();

        return seconds;
    }


}
