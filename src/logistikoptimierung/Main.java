package logistikoptimierung;

import logistikoptimierung.Services.CSVDataImportService;
import logistikoptimierung.Services.FirstComeFirstServeOptimizer;

public class Main {

    public static void main(String[] args) {
	// write your code here

        //TestSmallInstanceWithSolution();

        //TestSmallInstanceWithFirstComeFirstServer();

        //TestMediumInstanceWithFirstComeFirstServer();

        TestCSVImport();
    }

    private static void TestCSVImport()
    {
        System.out.println("Test with csv import");
        var dataService = new CSVDataImportService();
        var instance = dataService.loadData(CSVDataImportService.CONTRACT_1);

        var optimizer = new FirstComeFirstServeOptimizer(instance.getFactory());
        var factoryTaskList = optimizer.optimize(instance.getFactory().getOrderList(),5);

        instance.getFactory().startFactory(factoryTaskList);

        System.out.println();
        System.out.println("**********************************************");
        System.out.println("End income: " + instance.getFactory().getCurrentIncome());
        System.out.println("**********************************************");
        System.out.println();

    }

}
