package logistikoptimierung;

import logistikoptimierung.Contracts.IDataService;
import logistikoptimierung.Entities.FactoryObjects.LogSettings;
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

        var logSettings = new LogSettings(
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
        String contractList = CSVDataImportService.PARALLEL_ORDERS;
        long maxRuntimeInSeconds = 10000000;
        int nrOfDrivers = 6;
        int warehouseCapacity = 1000;

        var maxSystemRunTimeInSeconds = 1800;

        var dataService = new CSVDataImportService();
        var instance = dataService.loadDataAndCreateInstance(contractList);
        instance.setNrOfDrivers(nrOfDrivers);
        instance.setWarehouseCapacity(warehouseCapacity);

        //testTheCalculationOfNrOfOrders(logSettings, 22000, nrOfDrivers, warehouseCapacity, contractList);
        TestFirstComeFirstServe(logSettings, nrOfOrderToOptimize, maxRuntimeInSeconds, instance);
        TestProductionProcessOptimization(logSettings, nrOfOrderToOptimize, maxRuntimeInSeconds, maxSystemRunTimeInSeconds, instance);
    }

    /**
     * Loads the data with the contract list from the parameter, does the first come first serve optimization and
     * tests the first come first serve optimization.
     * @param logSettings message settings for the factory
     * @param nrOfOrderToOptimize nr of orders to optimize
     * @param maxRuntimeInSeconds max Runtime for the simulation
     */
    private static void TestFirstComeFirstServe(LogSettings logSettings,
                                                int nrOfOrderToOptimize,
                                                long maxRuntimeInSeconds,
                                                Instance instance)
    {
        var startTime = System.nanoTime();

        var optimizer = new FirstComeFirstServeOptimizerMain(instance);
        var factoryTaskList = optimizer.optimize(nrOfOrderToOptimize);

        var result = instance.getFactoryConglomerate().startSimulation(instance.getOrderList(), factoryTaskList, maxRuntimeInSeconds, logSettings);
        var endTime = System.nanoTime();

        printResult(factoryTaskList, instance.getFactoryConglomerate().getCurrentIncome(), result, convertNanoSecondsToSeconds(endTime - startTime));
        instance.getFactoryConglomerate().resetFactory();
    }

    /**
     * Loads the data with the contract list from the parameter, does the enumerated calculation optimization and
     * tests the optimization.
     * @param logSettings message settings for the factory
     * @param nrOfOrderToOptimize nr of orders to optimize
     * @param maxRuntimeInSeconds max Runtime for the simulation
     * @param maxSystemRunTimeInSeconds max real runtime for the calculation (will abort the calculation after the nr of seconds)
     */
    private static void TestProductionProcessOptimization(LogSettings logSettings,
                                                          int nrOfOrderToOptimize,
                                                          long maxRuntimeInSeconds,
                                                          long maxSystemRunTimeInSeconds,
                                                          Instance instance)
    {
        var startTime = System.nanoTime();

        var optimizer = new EnumeratedCalculationMain(instance,
                maxRuntimeInSeconds,
                true,
                logSettings,
                convertSecondsToNanoSeconds(maxSystemRunTimeInSeconds));
        var factoryTaskList = optimizer.optimize(nrOfOrderToOptimize);

        var result = instance.getFactoryConglomerate().startSimulation(instance.getOrderList(), factoryTaskList, maxRuntimeInSeconds, logSettings);
        var endTime = System.nanoTime();

        printResult(factoryTaskList, instance.getFactoryConglomerate().getCurrentIncome(), result, convertNanoSecondsToSeconds(endTime - startTime));
        System.out.println("Nr of Simulations: " + optimizer.getNrOfSimulations());
        instance.getFactoryConglomerate().resetFactory();
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
     * @param logSettings message settings for the factory
     * @param runtimeInSeconds Runtime for the orders
     */
    private static void testTheCalculationOfNrOfOrders(LogSettings logSettings,
                                                       long runtimeInSeconds,
                                                       long maxSystemRunTimeInSeconds,
                                                       Instance instance)
    {
        calculateMaxNrOfOrders(runtimeInSeconds, instance, logSettings, convertSecondsToNanoSeconds(maxSystemRunTimeInSeconds));
    }

    /**
     * Calculates the maximum of orders which are possible in the given time
     * @param runTimeInSeconds the max runtime in seconds
     */
    private static void calculateMaxNrOfOrders(long runTimeInSeconds,
                                               Instance instance,
                                               LogSettings logSettings,
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

            instance.getFactoryConglomerate().startSimulation(instance.getOrderList(), factorySteps, runTimeInSeconds, logSettings);

            var nrOfRemainingSteps = instance.getFactoryConglomerate().getNrOfRemainingSteps();
            instance.getFactoryConglomerate().resetFactory();
            if(nrOfRemainingSteps > 0)
                break;
            nrOfOrders++;
        }

        while (true)
        {
            var enumCalculation = new EnumeratedCalculationMain(instance,
                    runTimeInSeconds,
                    false,
                    logSettings,
                    convertSecondsToNanoSeconds(maxSystemRunTimeInSeconds));
            var factorySteps = enumCalculation.optimize(nrOfOrders);

            //Not possible
            if(factorySteps.isEmpty())
                break;

            instance.getFactoryConglomerate().startSimulation(instance.getOrderList(), factorySteps, runTimeInSeconds, logSettings);
            var nrOfRemainingSteps = instance.getFactoryConglomerate().getNrOfRemainingSteps();

            if(nrOfRemainingSteps > 0)
                break;

            instance.getFactoryConglomerate().resetFactory();
            nrOfOrders++;
            bestSteps = new ArrayList<>(factorySteps);
        }

        var endTime = System.nanoTime();
        printResult(bestSteps, instance.getFactoryConglomerate().getCurrentIncome(), instance.getFactoryConglomerate().getCurrentTimeStep(), convertNanoSecondsToSeconds(endTime - startTime));
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
