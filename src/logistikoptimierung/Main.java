package logistikoptimierung;

import logistikoptimierung.Entities.FactoryObjects.FactoryStep;
import logistikoptimierung.Entities.FactoryObjects.ProductionProcess;
import logistikoptimierung.Entities.FactoryObjects.StepTypes;
import logistikoptimierung.Entities.WarehouseItems.Product;
import logistikoptimierung.Services.CSVDataImportService;
import logistikoptimierung.Services.FirstComeFirstServeOptimizer;

import java.util.ArrayList;

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
        var instance = dataService.loadData(CSVDataImportService.CONTRACT_4);

        var optimizer = new FirstComeFirstServeOptimizer(instance.getFactory());
        //var factoryTaskList = optimizer.optimize(instance.getFactory().getOrderList(),1);

        var factoryTaskList = new ArrayList<FactoryStep>();
        var productToProduce = (Product) instance.getFactory().getOrderList().get(0).getProduct().item();

        var processes = new ArrayList<ProductionProcess>();
        processes.addAll(instance.getFactory().getProductionProcessesForProduct(productToProduce));

        productToProduce = (Product) instance.getFactory().getOrderList().get(1).getProduct().item();
        processes.addAll(instance.getFactory().getProductionProcessesForProduct(productToProduce));

        factoryTaskList.add(new FactoryStep(instance.getFactory(), "Holz", 50, "T1", StepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse));

        instance.getFactory().startFactory(factoryTaskList);

        System.out.println();
        System.out.println("**********************************************");
        System.out.println("End income: " + instance.getFactory().getCurrentIncome());
        System.out.println("**********************************************");
        System.out.println();

    }

}
