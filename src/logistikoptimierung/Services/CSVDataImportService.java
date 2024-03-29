package logistikoptimierung.Services;

import logistikoptimierung.Contracts.IDataService;
import logistikoptimierung.Entities.FactoryObjects.*;
import logistikoptimierung.Entities.Instance;
import logistikoptimierung.Entities.WarehouseItems.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for loading the data of the csv files and creates the instance to work with
 */
public class CSVDataImportService implements IDataService
{
    private static final String TRANSPORTER_FILENAME = "Transportmittel.csv";
    private static final String MATERIAL_WITH_TRANSPORTER_FILENAME = "RohstoffeTransportmittel.csv";
    private static final String PRODUCTS_FILENAME = "Produkte.csv";
    private static final String PRODUCTIONS_FILENAME = "FabrikenMitPuffer.csv";

    /**
     * loads the contract of the merged and mixed up orders
     */
    public static final String MERGED_ORDERS = "VermischteAuftraege.csv";

    /**
     * Loads the contract of the test Auftraege
     */
    public static final String TEST_ORDERS = "TestAuftraege.csv";

    /**
     * Loads the contract with similar products
     */
    public static final String SIMILAR_ORDERS = "similarProducts.csv";

    /**
     * Loads the contract with products which can be produced in parallel
     */
    public static final String PARALLEL_ORDERS = "paraProc.csv";

    private static final String DATA_PATH = "data\\";
    private static final String DELIMITER = ";";


    /**
     * Create a object for the CSV Import to load the data and create an instance with the given nr of drivers and warehouse capacity.

     */
    public CSVDataImportService()
    {
    }

    /**
     * Load the data from the given CSV files and creates the instance for the simulation.
     * @param filename file name of the orders to load
     * @return the instance with the factory and the order
     */

    @Override
    public Instance loadDataAndCreateInstance(String filename)
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


            var factory = new FactoryConglomerate("Test 1",
                    productions,
                    transporters,
                    materials,
                    products);
            return new Instance(factory,
                    orders);
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
            var newProduct = new Product(productName, productId);
            products.add(newProduct);
        }

        return products;
    }

    private List<Factory> loadProduction(List<String[]> data, List<WarehouseItem> items)
    {
        var productionList = new ArrayList<Factory>();
        Factory currentFactory = null;
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
                currentFactory = new Factory(
                        productionName,
                        idCount,
                        currentProductionProcesses,
                        bufferInput,
                        bufferOutput);

                productionList.add(currentFactory);
                idCount++;
            }

            var bom = new ArrayList<WarehousePosition>();
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
                        currentFactory,
                        bom);

                int startCount = 5;
                while (startCount < dataItem.length && !dataItem[startCount].isBlank())
                {
                    var materialBatchSize = Integer.parseInt(dataItem[startCount]);
                    var materialName = dataItem[startCount + 1];

                    var materials = findWarehouseItem(materialName, items);
                    for(var material : materials )
                    {
                        var materialPosition = new WarehousePosition(material, materialBatchSize);
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
            var materialPosition = new WarehousePosition(product.get(0), amount);

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
