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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CSVDataImportService implements IDataService
{
    private static final String TRANSPORTER_FILENAME = "Transportmittel.csv";
    private static final String FACTORIES_WITH_BUFFERS_FILENAME = "FabrikenMitPuffer.csv";
    private static final String MATERIAL_WITH_TRANSPORTER_FILENAME = "RohstoffeTransportmittel.csv";

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

            var materials = new ArrayList<Material>();
            var products = new ArrayList<Product>();

            var transporters = loadTransporter(loadCsv(path + TRANSPORTER_FILENAME));

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

    private List<Transporter> loadTransporter(List<String[]> data)
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



}