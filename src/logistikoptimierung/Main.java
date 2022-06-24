package logistikoptimierung;

import logistikoptimierung.Services.CSVDataImportService;
import logistikoptimierung.Services.FirstComeFirstServeOptimizer;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
	// write your code here
        TestCSVImport();
    }

    private static void TestCSVImport()
    {
        System.out.println("Test with csv import");
        var dataService = new CSVDataImportService();
        var instance = dataService.loadData(CSVDataImportService.CONTRACT_4);

        var optimizer = new FirstComeFirstServeOptimizer(instance.getFactory());
        var factoryTaskList = optimizer.optimize(instance.getFactory().getOrderList(),1);

        instance.getFactory().startFactory(factoryTaskList);

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

        var timeString = "Day " + days + " Hour " + hours + " Minute " + minutes +
                " Seconds " + realSeconds;

        return timeString;
    }

}
