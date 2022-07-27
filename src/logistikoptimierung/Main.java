package logistikoptimierung;

import logistikoptimierung.Entities.FactoryObjects.FactoryMessageSettings;
import logistikoptimierung.Entities.FactoryObjects.FactoryStep;
import logistikoptimierung.Entities.Instance;
import logistikoptimierung.Services.CSVDataImportService;
import logistikoptimierung.Services.EnumeratedCalculation.EnumeratedCalculationMain;
import logistikoptimierung.Services.FirstComeFirstServeOptimizer.FirstComeFirstServeOptimizerMain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Main class for testing the optimization
 */
public class Main
{
    /**
     * starts the test of the optimization.
     * 1: Inits the factory settings for the simulation
     * 2: Sets the parameter for the instance of the factory and sets the max runtime for the simulation
     * 3: Tests the first come first serve optimization and prints the result in the console
     * 4: Tests the enumerated calculation optimization and prints the result in the console
     * @param args String args (No available start parameters)
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

        int nrOfOrderToOptimize = 5;
        String contractList = CSVDataImportService.PARALLEL_CONTRACTS;
        long maxRuntimeInSeconds = 100000;
        int nrOfDrivers = 6;
        int warehouseCapacity = 1000;

        var maxSystemRunTimeInSeconds = 1800;

        //testTheCalculationOfNrOfOrders(factoryMessageSettings, 22000, nrOfDrivers, warehouseCapacity, contractList);
        TestFirstComeFirstServe(factoryMessageSettings, nrOfOrderToOptimize, maxRuntimeInSeconds, nrOfDrivers, warehouseCapacity, contractList);
        TestProductionProcessOptimization(factoryMessageSettings, nrOfOrderToOptimize, maxRuntimeInSeconds, nrOfDrivers, warehouseCapacity, contractList, maxSystemRunTimeInSeconds);
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
        var startTime = System.nanoTime();
        var dataService = new CSVDataImportService(nrOfDrivers, warehouseCapacity);
        var instance = dataService.loadData(contractListName);

        var optimizer = new FirstComeFirstServeOptimizerMain(instance);
        var factoryTaskList = optimizer.optimize(nrOfOrderToOptimize);

        instance.factory().startFactory(instance.orderList(), factoryTaskList, maxRuntimeInSeconds, factoryMessageSettings);
        var endTime = System.nanoTime();

        printResult(factoryTaskList, instance.factory().getCurrentIncome(), instance.factory().getCurrentTimeStep(), convertNanoSecondsToSeconds(endTime - startTime));
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
                                                          String contractListName,
                                                          long maxSystemRunTimeInSeconds)
    {
        var startTime = System.nanoTime();
        var dataService = new CSVDataImportService(nrOfDrivers, warehouseCapacity);
        var instance = dataService.loadData(contractListName);

        var optimizer = new EnumeratedCalculationMain(instance,
                maxRuntimeInSeconds,
                false,
                factoryMessageSettings,
                convertSecondsToNanoSeconds(maxSystemRunTimeInSeconds));
        var factoryTaskList = optimizer.optimize(nrOfOrderToOptimize);

        instance.factory().startFactory(instance.orderList(), factoryTaskList, maxRuntimeInSeconds, factoryMessageSettings);

        var endTime = System.nanoTime();

        printResult(factoryTaskList, instance.factory().getCurrentIncome(), instance.factory().getCurrentTimeStep(), convertNanoSecondsToSeconds(endTime - startTime));
        System.out.println("Nr of Simulations: " + optimizer.getNrOfSimulations());
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
    private static void printResult(List<FactoryStep> stepList, double currentIncome, long currentTimeStep, long runTimeReal)
    {
        System.out.println();
        System.out.println("**********************************************");
        System.out.println();
        for(var step : stepList)
            System.out.println(step);
        System.out.println();
        System.out.println("**********************************************");
        System.out.println("End income: " + currentIncome);
        System.out.println("Simulation Runtime: " +  currentTimeStep);
        System.out.println("Simulation Runtime: " +  ConvertSecondsToTime(currentTimeStep));
        System.out.println("System Runtime: " +  ConvertSecondsToTime(runTimeReal));
        System.out.println("**********************************************");
        System.out.println();
    }

    /**
     * Tests the calculation of nr of orders
     * @param factoryMessageSettings message settings for the factory
     * @param runtimeInSeconds Runtime for the orders
     * @param nrOfDrivers nr of drivers in the factory
     * @param warehouseCapacity warehouse capacity in the factory
     * @param contractListName name of the contract list for the instance
     */
    private static void testTheCalculationOfNrOfOrders(FactoryMessageSettings factoryMessageSettings,
                                                       long runtimeInSeconds,
                                                       int nrOfDrivers,
                                                       int warehouseCapacity,
                                                       String contractListName,
                                                       long maxSystemRunTimeInSeconds)
    {
        var dataService = new CSVDataImportService(nrOfDrivers, warehouseCapacity);
        var instance = dataService.loadData(contractListName);
        calculateMaxNrOfOrders(runtimeInSeconds, instance, factoryMessageSettings, convertSecondsToNanoSeconds(maxSystemRunTimeInSeconds));
    }

    /**
     * Calculates the maximum of orders which are possible in the given time
     * @param runTimeInSeconds the max runtime in seconds
     */
    private static void calculateMaxNrOfOrders(long runTimeInSeconds,
                                               Instance instance,
                                               FactoryMessageSettings factoryMessageSettings,
                                               long maxSystemRunTimeInSeconds)
    {
        var bestSteps = new ArrayList<FactoryStep>();
        var startTime = System.nanoTime();
        int nrOfOrders = 1;
        while (true)
        {
            var firstComeFirstServe = new FirstComeFirstServeOptimizerMain(instance);
            var factorySteps = firstComeFirstServe.optimize(nrOfOrders);

            //Not possible
            if(factorySteps.isEmpty())
                break;

            instance.factory().startFactory(instance.orderList(), factorySteps, runTimeInSeconds, factoryMessageSettings);

            var nrOfRemainingSteps = instance.factory().getNrOfRemainingSteps();
            instance.factory().resetFactory();
            if(nrOfRemainingSteps > 0)
                break;
            nrOfOrders++;
        }

        while (true)
        {
            var enumCalculation = new EnumeratedCalculationMain(instance,
                    runTimeInSeconds,
                    false,
                    factoryMessageSettings,
                    convertSecondsToNanoSeconds(maxSystemRunTimeInSeconds));
            var factorySteps = enumCalculation.optimize(nrOfOrders);

            //Not possible
            if(factorySteps.isEmpty())
                break;

            instance.factory().startFactory(instance.orderList(), factorySteps, runTimeInSeconds, factoryMessageSettings);
            var nrOfRemainingSteps = instance.factory().getNrOfRemainingSteps();

            if(nrOfRemainingSteps > 0)
                break;

            instance.factory().resetFactory();
            nrOfOrders++;
            bestSteps = new ArrayList<>(factorySteps);
        }

        var endTime = System.nanoTime();
        printResult(bestSteps, instance.factory().getCurrentIncome(), instance.factory().getCurrentTimeStep(), convertNanoSecondsToSeconds(endTime - startTime));
        System.out.println("Nr of orders done: " + nrOfOrders);
    }

    /**
     * Converts nano seconds to seconds
     * @param nanoseconds nano seconds
     * @return seconds
     */
    private static long convertNanoSecondsToSeconds(long nanoseconds)
    {
        return (long)(nanoseconds / Math.pow(10, 9));
    }

    /**
     * Converts nano seconds to seconds
     * @param seconds seconds
     * @return nano seconds
     */
    private static long convertSecondsToNanoSeconds(double seconds)
    {
        return (long) (seconds * Math.pow(10, 9));
    }

}
