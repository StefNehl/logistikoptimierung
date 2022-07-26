package logistikoptimierung;

import logistikoptimierung.Entities.FactoryObjects.FactoryMessageSettings;
import logistikoptimierung.Services.CSVDataImportService;
import logistikoptimierung.Services.EnumeratedCalculation.EnumeratedCalculationMain;
import logistikoptimierung.Services.FirstComeFirstServeOptimizer.FirstComeFirstServeOptimizerMain;

import java.util.concurrent.TimeUnit;

public class Main
{
    public static void main(String[] args) {
	// write your code here

        var factoryMessageSettings = new FactoryMessageSettings(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        int nrOfOrderToOptimize = 5;
        String contractList = CSVDataImportService.MERGED_CONTRACTS;
        long maxRuntimeInSeconds = 100000;
        int nrOfDrivers = 6;
        int warehouseCapacity = 1000;

        TestFirstComeFirstServe(factoryMessageSettings, nrOfOrderToOptimize, maxRuntimeInSeconds, nrOfDrivers, warehouseCapacity, contractList);

        TestProductionProcessOptimization(factoryMessageSettings, nrOfOrderToOptimize, maxRuntimeInSeconds, nrOfDrivers, warehouseCapacity, contractList);
    }

    private static void TestFirstComeFirstServe(FactoryMessageSettings factoryMessageSettings,
                                                int nrOfOrderToOptimize,
                                                long maxRuntimeInSeconds,
                                                int nrOfDrivers,
                                                int warehouseCapacity,
                                                String contractListName)
    {
        var dataService = new CSVDataImportService(nrOfDrivers, warehouseCapacity);
        var instance = dataService.loadData(contractListName);

        var optimizer = new FirstComeFirstServeOptimizerMain(instance);
        var factoryTaskList = optimizer.optimize(nrOfOrderToOptimize);

        instance.factory().startFactory(instance.orderList(), factoryTaskList, maxRuntimeInSeconds, factoryMessageSettings);
        printResult(instance.factory().getCurrentIncome(), instance.factory().getCurrentTimeStep());
        instance.factory().resetFactory();
    }

    private static void TestProductionProcessOptimization(FactoryMessageSettings factoryMessageSettings,
                                                          int nrOfOrderToOptimize,
                                                          long maxRuntimeInSeconds,
                                                          int nrOfDrivers,
                                                          int warehouseCapacity,
                                                          String contractListName)
    {
        var dataService = new CSVDataImportService(nrOfDrivers, warehouseCapacity);
        var instance = dataService.loadData(contractListName);

        var optimizer = new EnumeratedCalculationMain(instance, maxRuntimeInSeconds, factoryMessageSettings);
        var factoryTaskList = optimizer.optimize(nrOfOrderToOptimize);

        instance.factory().startFactory(instance.orderList(), factoryTaskList, maxRuntimeInSeconds, factoryMessageSettings);
        printResult(instance.factory().getCurrentIncome(), instance.factory().getCurrentTimeStep());
        instance.factory().resetFactory();

    }

    private static String ConvertSecondsToTime(long seconds)
    {
        long days = TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (days * 24);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
        long realSeconds = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

        var timeString = days + " Days " + hours + " Hours " + minutes +
                " Minutes " + realSeconds + " Seconds";

        return timeString;
    }

    private static void printResult(double currentIncome, long currentTimeStep)
    {
        System.out.println();
        System.out.println("**********************************************");
        System.out.println("End income: " + currentIncome);
        System.out.println("Runtime: " +  currentTimeStep);
        System.out.println("Runtime: " +  ConvertSecondsToTime(currentTimeStep));
        System.out.println("**********************************************");
        System.out.println();
    }

}
