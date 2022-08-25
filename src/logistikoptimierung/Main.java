package logistikoptimierung;

import logistikoptimierung.Entities.FactoryObjects.*;
import logistikoptimierung.Entities.Instance;
import logistikoptimierung.Entities.WarehouseItems.Product;
import logistikoptimierung.Entities.WarehouseItems.WarehousePosition;
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

        String contractList = CSVDataImportService.PARALLEL_ORDERS;
        var dataService = new CSVDataImportService();
        var instance = dataService.loadDataAndCreateInstance(contractList);

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

        instance.setLogSettings(logSettings);
        instance.setNrOfDrivers(6);
        instance.setWarehouseCapacity(1000);

        int nrOfOrderToOptimize = 5;
        long maxRuntimeInSeconds = 10000000;
        var maxSystemRunTimeInSeconds = 1800;
        boolean fillWarehouseWith20PercentOfNeededMaterials = false;

        //testTheCalculationOfNrOfOrders(maxRuntimeInSeconds, maxSystemRunTimeInSeconds, instance);
        TestFirstComeFirstServe(nrOfOrderToOptimize, maxRuntimeInSeconds, fillWarehouseWith20PercentOfNeededMaterials, instance);
        TestProductionProcessOptimization(nrOfOrderToOptimize, maxRuntimeInSeconds, maxSystemRunTimeInSeconds, fillWarehouseWith20PercentOfNeededMaterials, instance);
    }

    /**
     * Loads the data with the contract list from the parameter, does the first come first serve optimization and
     * tests the first come first serve optimization.
     * @param nrOfOrderToOptimize nr of orders to optimize
     * @param maxRuntimeInSeconds max Runtime for the simulation
     * @param fillWarehouseWith20PercentOfNeededMaterials true if the warehouse should be filled with 50 percent of the needed materials
     * @param instance instance for the simulation
     */
    private static void TestFirstComeFirstServe(int nrOfOrderToOptimize,
                                                long maxRuntimeInSeconds,
                                                boolean fillWarehouseWith20PercentOfNeededMaterials,
                                                Instance instance)
    {
        var startTime = System.nanoTime();

        var optimizer = new FirstComeFirstServeOptimizerMain(instance);
        var factoryTaskList = optimizer.optimize(nrOfOrderToOptimize);

        if(fillWarehouseWith20PercentOfNeededMaterials)
            fillWarehouseWith20PercentOfMaterialsNeeded(factoryTaskList, instance.getFactoryConglomerate().getWarehouse());

        var result = instance.getFactoryConglomerate().startSimulation(instance.getOrderList(), factoryTaskList, maxRuntimeInSeconds);
        var endTime = System.nanoTime();

        printResult(factoryTaskList, instance.getFactoryConglomerate().getCurrentIncome(), result, convertNanoSecondsToSeconds(endTime - startTime));
        instance.getFactoryConglomerate().resetFactory();
    }

    /**
     * Loads the data with the contract list from the parameter, does the enumerated calculation optimization and
     * tests the optimization.
     * @param nrOfOrderToOptimize nr of orders to optimize
     * @param maxRuntimeInSeconds max Runtime for the simulation
     * @param fillWarehouseWith20PercentOfNeededMaterials true if the warehouse should be filled with 50 percent of the needed materials
     * @param maxSystemRunTimeInSeconds max real runtime for the calculation (will abort the calculation after the nr of seconds)
     * @param instance instance for the simulation
     */
    private static void TestProductionProcessOptimization(int nrOfOrderToOptimize,
                                                          long maxRuntimeInSeconds,
                                                          long maxSystemRunTimeInSeconds,
                                                          boolean fillWarehouseWith20PercentOfNeededMaterials,
                                                          Instance instance)
    {
        var startTime = System.nanoTime();

        var optimizer = new EnumeratedCalculationMain(instance,
                maxRuntimeInSeconds,
                false,
                convertSecondsToNanoSeconds(maxSystemRunTimeInSeconds));


        var factoryTaskList = optimizer.optimize(nrOfOrderToOptimize);

        if(fillWarehouseWith20PercentOfNeededMaterials)
            fillWarehouseWith20PercentOfMaterialsNeeded(factoryTaskList, instance.getFactoryConglomerate().getWarehouse());

        var result = instance.getFactoryConglomerate().startSimulation(instance.getOrderList(), factoryTaskList, maxRuntimeInSeconds);
        var endTime = System.nanoTime();

        printResult(factoryTaskList, instance.getFactoryConglomerate().getCurrentIncome(), result, convertNanoSecondsToSeconds(endTime - startTime));
        System.out.println("Nr of Simulations: " + optimizer.getNrOfSimulations());
        instance.getFactoryConglomerate().resetFactory();
    }

    private static void fillWarehouseWith20PercentOfMaterialsNeeded(List<FactoryStep> factorySteps, Warehouse warehouse)
    {
        var warehousePositions = new ArrayList<WarehousePosition>();

        for(var step : factorySteps)
        {
            var amount = get20PercentOfAmountOfStep(step);
            if(amount == 0)
                continue;
            warehousePositions.add(new WarehousePosition(step.getItemToManipulate(), amount));
        }

        for(var position : warehousePositions)
            warehouse.addItemToWarehouse(position);
    }

    private static int get20PercentOfAmountOfStep(FactoryStep step)
    {
        if(step.getStepType() == FactoryStepTypes.Produce)
        {
            var getFactory = (Factory)step.getFactoryObject();
            var productionProcess = getFactory.getProductionProcessForProduct((Product) step.getItemToManipulate());
            return productionProcess.getProductionBatchSize() / 5;

        }

        if(step.getStepType() == FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse)
            return step.getAmountOfItems() / 5;

        return 0;
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
     * @param runtimeInSeconds Runtime for the orders
     */
    private static void testTheCalculationOfNrOfOrders(long runtimeInSeconds,
                                                       long maxSystemRunTimeInSeconds,
                                                       Instance instance)
    {
        calculateMaxNrOfOrders(runtimeInSeconds, instance, convertSecondsToNanoSeconds(maxSystemRunTimeInSeconds));
    }

    /**
     * Calculates the maximum of orders which are possible in the given time
     * @param runTimeInSeconds the max runtime in seconds
     */
    private static void calculateMaxNrOfOrders(long runTimeInSeconds,
                                               Instance instance,
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

            instance.getFactoryConglomerate().startSimulation(instance.getOrderList(), factorySteps, runTimeInSeconds);

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
                    convertSecondsToNanoSeconds(maxSystemRunTimeInSeconds));
            var factorySteps = enumCalculation.optimize(nrOfOrders);

            //Not possible
            if(factorySteps.isEmpty())
                break;

            instance.getFactoryConglomerate().startSimulation(instance.getOrderList(), factorySteps, runTimeInSeconds);
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
