package logistikoptimierung;

import logistikoptimierung.Entities.*;
import logistikoptimierung.Services.FirstComeFirstServeOptimizer;
import logistikoptimierung.Services.SmallInstanceSolution;
import logistikoptimierung.Services.TestDataService;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
	// write your code here

        //TestSmallInstanceWithSolution();

        //TestSmallInstanceWithFirstComeFirstServer();

        TestMediumInstanceWithFirstComeFirstServer();
    }

    private static void TestSmallInstanceWithSolution()
    {
        System.out.println("Test with Small Instance and Solution");
        var dataService = new TestDataService();
        var instance = dataService.loadData("SmallTestInstance");

        var optimizer = new SmallInstanceSolution(instance.getFactory());

        instance.getFactory().startFactory(optimizer.optimize(instance.getFactory().getOrderList()));

        System.out.println();
        System.out.println("**********************************************");
        System.out.println("End income: " + instance.getFactory().getCurrentIncome());
        System.out.println("**********************************************");
        System.out.println();
    }

    private static void TestSmallInstanceWithFirstComeFirstServer()
    {
        System.out.println("Test with Small Instance and First Come First Serve");
        var dataService = new TestDataService();
        var instance = dataService.loadData("SmallTestInstance");

        var optimizer = new FirstComeFirstServeOptimizer(instance.getFactory());

        instance.getFactory().startFactory(optimizer.optimize(instance.getFactory().getOrderList()));

        System.out.println();
        System.out.println("**********************************************");
        System.out.println("End income: " + instance.getFactory().getCurrentIncome());
        System.out.println("**********************************************");
        System.out.println();
    }

    private static void TestMediumInstanceWithFirstComeFirstServer()
    {
        System.out.println("Test with Medium Instance and First Come First Serve");
        var dataService = new TestDataService();
        var instance = dataService.loadData("MediumTestInstance");

        var optimizer = new FirstComeFirstServeOptimizer(instance.getFactory());

        instance.getFactory().startFactory(optimizer.optimize(instance.getFactory().getOrderList()));

        System.out.println();
        System.out.println("**********************************************");
        System.out.println("End income: " + instance.getFactory().getCurrentIncome());
        System.out.println("**********************************************");
        System.out.println();
    }
}
