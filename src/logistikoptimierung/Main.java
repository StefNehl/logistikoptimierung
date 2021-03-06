package logistikoptimierung;

import logistikoptimierung.Entities.FactoryObjects.FactoryMessageSettings;
import logistikoptimierung.Services.CSVDataImportService;
import logistikoptimierung.Services.EnumeratedCalculation.EnumeratedCalculationMain;
import logistikoptimierung.Services.FirstComeFirstServeOptimizer.FirstComeFirstServeOptimizerMain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main
{
    public static void main(String[] args) {
	// write your code here

        var factoryMessageSettings = new FactoryMessageSettings(
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false
        );

        int nrOfOrderToOptimize = 2;
        String contractList = CSVDataImportService.CONTRACT_3;
        long maxRuntimeInSeconds = 100000000;

        //testRecursion();
        TestFirstComeFirstServe(factoryMessageSettings, nrOfOrderToOptimize, maxRuntimeInSeconds, contractList);

        TestProductionProcessOptimization(factoryMessageSettings, nrOfOrderToOptimize, maxRuntimeInSeconds, contractList);
    }

    private static void testRecursion()
    {
        var steps = new ArrayList<Integer>();
        for(int i = 1; i <= 3; i++)
            steps.add(i);

        var stepToDo = new ArrayList<Integer>();
        rec(stepToDo, steps);
    }

    private static int count = 0;
    private static void rec(List<Integer> stepsToDo, List<Integer> steps)
    {
        if(steps.isEmpty())
        {
            count++;
            for(var step : stepsToDo)
                System.out.println(step);

            System.out.println("********************");
            System.out.println("C: " + count);
            System.out.println("********************");
        }

        for(var step : steps)
        {
            if(step == 3 && !stepsToDo.contains(2))
                return;
            stepsToDo.add(step);
            var copy = new ArrayList<>(steps);
            copy.remove(step);
            rec(stepsToDo, copy);
            stepsToDo.remove(step);
        }
    }

    private static void TestFirstComeFirstServe(FactoryMessageSettings factoryMessageSettings,
                                                int nrOfOrderToOptimize,
                                                long maxRuntimeInSeconds,
                                                String contractList)
    {
        var dataService = new CSVDataImportService(7, 1000);
        var instance = dataService.loadData(contractList);

        var optimizer = new FirstComeFirstServeOptimizerMain(instance.getFactory());
        var factoryTaskList = optimizer.optimize(instance.getFactory().getOrderList(),
                nrOfOrderToOptimize);

        instance.getFactory().startFactory(factoryTaskList, maxRuntimeInSeconds, factoryMessageSettings);
        printResult(instance.getFactory().getCurrentIncome(), instance.getFactory().getCurrentTimeStep());
        instance.getFactory().resetFactory();

        //instance.getFactory().printLogMessageFromTo(2017, 2200);
    }

    private static void TestProductionProcessOptimization(FactoryMessageSettings factoryMessageSettings,
                                                          int nrOfOrderToOptimize,
                                                          long maxRuntimeInSeconds,
                                                          String contractList)
    {
        var dataService = new CSVDataImportService(7, 1000);
        var instance = dataService.loadData(contractList);

        var firstComeFirstServeOptimizer = new FirstComeFirstServeOptimizerMain(instance.getFactory());
        var factoryTaskList = firstComeFirstServeOptimizer.optimize(instance.getFactory().getOrderList(),
                nrOfOrderToOptimize);

        var result = instance.getFactory().startFactory(factoryTaskList, maxRuntimeInSeconds, factoryMessageSettings);

        instance.getFactory().resetFactory();
        var optimizer = new EnumeratedCalculationMain(instance.getFactory(), (result + 1), factoryMessageSettings);
        factoryTaskList = optimizer.optimize(instance.getFactory()
                .getOrderList(),
                nrOfOrderToOptimize);

        instance.getFactory().startFactory(factoryTaskList, maxRuntimeInSeconds, factoryMessageSettings);
        printResult(instance.getFactory().getCurrentIncome(), instance.getFactory().getCurrentTimeStep());
        instance.getFactory().resetFactory();

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
        //System.out.println("Runtime: " +  ConvertSecondsToTime(currentTimeStep));
        System.out.println("Runtime: " +  currentTimeStep);
        System.out.println("**********************************************");
        System.out.println();
    }

}
