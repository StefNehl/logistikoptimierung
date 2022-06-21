package logistikoptimierung.Services;

import logistikoptimierung.Contracts.IDataService;
import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.FactoryObjects.Machine;
import logistikoptimierung.Entities.FactoryObjects.Transporter;
import logistikoptimierung.Entities.Instance;
import logistikoptimierung.Entities.WarehouseItems.Material;
import logistikoptimierung.Entities.WarehouseItems.Order;
import logistikoptimierung.Entities.WarehouseItems.Product;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class CSVDataImportService implements IDataService
{
    private static final String TRANSPORTER_FILENAME = "Transportmittel.csv";
    private static final String FACTORIES_WITH_BUFFERS_FILENAME = "FabrikenMitPuffer.csv";
    private static final String MATERIAL_WITH_TRANSPORTER_FILENAME = "RohstoffeTransportmittel.csv";
    private static final String PRODUCTS_FILENAME = "Products.csv";

    public static final String CONTRACT_1 = "Aufträge1.csv";
    public static final String CONTRACT_2 = "Aufträge2.csv";
    public static final String CONTRACT_3 = "Aufträge3.csv";
    public static final String CONTRACT_4 = "Aufträge4.csv";
    public static final String CONTRACT_5 = "Aufträge5.csv";

    private static final String DATA_PATH = "data\\";
    private static final String DELIMITER = ";";

    private Instance instance;

    public CSVDataImportService()
    {

    }

    @Override
    public Instance loadData(String filename)
    {
        var path = System.getProperty("user.dir") + "\\" + DATA_PATH;

        try
        {
            var runTimeInMinutes = 1000;//5 * 8 * 60;
            int warehouseCapacity = 100;
            int nrOfDrivers = 7;

            var orders = new ArrayList<Order>();

            var machines = new ArrayList<Machine>();

            var materials = loadMaterials(
                    loadCsv(path + MATERIAL_WITH_TRANSPORTER_FILENAME));

            var products = loadProducts(
                    loadCsv(path + PRODUCTS_FILENAME));

            var transporters = loadTransporters(
                    loadCsv(path + TRANSPORTER_FILENAME));

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
            instance = new Instance(factory);

            return instance;
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
        String line = "";
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
                    area,
                    type,
                    eng,
                    capacity);
            transporters.add(newTransporter);
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
            var materialId = dataItem[1];
            var name = dataItem[2];
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
            var productId = dataItem[0];
            if(productId.isBlank())
            {
                productType = dataItem[1];
                continue;
            }

            var productName = dataItem[1];
            var newProduct = new Product(productName, productId, productType);
            products.add(newProduct);
        }

        return products;
    }

    private int convertStringToSeconds(String timeString)
    {
        var time = LocalTime.parse(timeString);
        var seconds = time.getSecond();

        return seconds;
    }


}
