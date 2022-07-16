package logistikoptimierung;

import logistikoptimierung.Entities.FactoryObjects.FactoryMessageSettings;
import logistikoptimierung.Services.CSVDataImportService;
import logistikoptimierung.Services.EnumeratedCalculation.EnumeratedCalculationMain;
import logistikoptimierung.Services.FirstComeFirstServeOptimizer.FirstComeFirstServeOptimizerMain;
import logistikoptimierung.Services.ProductionProcessOptimization.ProductionProcessOptimization;
import logistikoptimierung.Services.ProductionWeightedOptimization.ProductionWeightedOptimizationMain;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
	// write your code here
        //TestCSVImport();

        TestProductionProcessOptimization();
    }

    private static void TestCSVImport()
    {
        System.out.println("Test with csv import");
        var dataService = new CSVDataImportService();
        var instance = dataService.loadData(CSVDataImportService.CONTRACT_3);

        var optimizer = new FirstComeFirstServeOptimizerMain(instance.getFactory());
        var factoryTaskList = optimizer.optimize(instance.getFactory().getOrderList(),
                1);

        var factoryMessageSettings = new FactoryMessageSettings(
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true
                );

        //var runTimeInSeconds = 100; //One week 60 * 60 * 24 * 5 = 144 000
        var runTimeInSeconds = 10000;
        instance.getFactory().startFactory(factoryTaskList, runTimeInSeconds, factoryMessageSettings);

        System.out.println();
        System.out.println("**********************************************");
        System.out.println("End income: " + instance.getFactory().getCurrentIncome());
        System.out.println("Runtime: " +  ConvertSecondsToTime(instance.getFactory().getCurrentTimeStep()));
        System.out.println("**********************************************");
        System.out.println();

        //instance.getFactory().printLogMessageFromTo(2017, 2200);
    }

    private static void TestProductionProcessOptimization()
    {
        System.out.println("Test with csv import");
        var dataService = new CSVDataImportService();
        var instance = dataService.loadData(CSVDataImportService.CONTRACT_3);

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

        var optimizer = new EnumeratedCalculationMain(instance.getFactory(), factoryMessageSettings);
        //var optimizer = new ProductionProcessOptimization(instance.getFactory());
        var factoryTaskList = optimizer.optimize(instance.getFactory()
                .getOrderList(),
                1);

        System.out.println();
        System.out.println("**********************************************");
        System.out.println("End income: " + instance.getFactory().getCurrentIncome());
        System.out.println("Runtime: " +  ConvertSecondsToTime(instance.getFactory().getCurrentTimeStep()));
        System.out.println("**********************************************");
        System.out.println();
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

}
