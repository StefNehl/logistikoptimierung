package logistikoptimierung;

import logistikoptimierung.Entities.FactoryObjects.FactoryMessageSettings;
import logistikoptimierung.Services.CSVDataImportService;
import logistikoptimierung.Services.EnumeratedCalculation.EnumeratedCalculationMain;
import logistikoptimierung.Services.FirstComeFirstServeOptimizer.FirstComeFirstServeOptimizerMain;

import java.util.concurrent.TimeUnit;

public class Main
{
    /**
     * starts the test of the optimization.
     * 1: Inits the factory settings for the simulation
     * 2: Sets the parameter for the instance of the factory and sets the max runtime for the simulation
     * 3: Tests the first come first serve optimization and prints the result in the console
     * 4: Tests the enumerated calculation optimization and prints the result in the console
     * @param args
     */
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

        int nrOfOrderToOptimize = 3;
        String contractList = CSVDataImportService.MERGED_CONTRACTS;
        long maxRuntimeInSeconds = 100000;
        int nrOfDrivers = 6;
        int warehouseCapacity = 1000;

        TestFirstComeFirstServe(factoryMessageSettings, nrOfOrderToOptimize, maxRuntimeInSeconds, nrOfDrivers, warehouseCapacity, contractList);

        TestProductionProcessOptimization(factoryMessageSettings, nrOfOrderToOptimize, maxRuntimeInSeconds, nrOfDrivers, warehouseCapacity, contractList);
    }

    /**
     * Loads the data with the contract list from the parameter, does the first come first serve optimization and
     * tests the first come first serve optimization.
     * @param factoryMessageSettings message settings for the factory
     * @param nrOfOrderToOptimize nr of orders to optimize
     * @param maxRuntimeInSeconds max Runtime for the simulation
     * @param nrOfDrivers nr of drivers in the factory
     * @param warehouseCapacity warehouse capacity in the factory
     * @param contractListName name of the contract list for the instance
     */
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

    /**
     * Loads the data with the contract list from the parameter, does the enumerated calculation optimization and
     * tests the optimization.
     * @param factoryMessageSettings message settings for the factory
     * @param nrOfOrderToOptimize nr of orders to optimize
     * @param maxRuntimeInSeconds max Runtime for the simulation
     * @param nrOfDrivers nr of drivers in the factory
     * @param warehouseCapacity warehouse capacity in the factory
     * @param contractListName name of the contract list for the instance
     */
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

    /**
     * Converts the given seconds to days, hours, minutes and seconds
     * @param seconds
     * @return string with the time
     */
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

    /**
     * Prints the result of the factory
     * @param currentIncome
     * @param currentTimeStep
     */
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
