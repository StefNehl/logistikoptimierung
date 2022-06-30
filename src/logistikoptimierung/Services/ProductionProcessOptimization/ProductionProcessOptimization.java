package logistikoptimierung.Services.ProductionProcessOptimization;

import logistikoptimierung.Contracts.IOptimizationService;
import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.FactoryObjects.FactoryStep;
import logistikoptimierung.Entities.FactoryObjects.ProductionProcess;
import logistikoptimierung.Entities.WarehouseItems.Order;
import logistikoptimierung.Entities.WarehouseItems.Product;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.*;

public class ProductionProcessOptimization implements IOptimizationService
{
    private Factory factory;
    private List<ProductionPlanningItem> productionPlanningItems;

    public ProductionProcessOptimization(Factory factory)
    {
        this.factory = factory;
        this.productionPlanningItems = new ArrayList<>();
    }

    @Override
    public List<FactoryStep> optimize(List<Order> orderList, int nrOfOrdersToOptimize)
    {
        productionPlanningItems.clear();

        var factorySteps = new ArrayList<FactoryStep>();
        var subOrderList = new ArrayList<Order>();

        for(int i = 0; i < nrOfOrdersToOptimize; i++)
        {
            subOrderList.add(orderList.get(i));
        }

        createProcessList(subOrderList);
        setProcessDepthForPlanningItems();
        optimizeBatches(subOrderList);
        removeDoubleEntriesFromPlaningItemList();
        calculateStartAndEndTimes();


        return factorySteps;
    }

    private void createProcessList(List<Order> orderList)
    {
        for (var production : this.factory.getProductions())
        {
            productionPlanningItems.add(new ProductionPlanningItem(production));
        }

        var orderCount = 1;
        for (var order : orderList)
        {
            var processes = this.factory.getProductionProcessesForProduct(
                    order.getProduct().item());

            var filteredProcesses = new ArrayList<ProductionProcess>();
            for(var process : processes)
            {
                if(this.factory.checkIfItemHasASupplier(process.getProductToProduce()))
                    continue;
                filteredProcesses.add(process);
            }

            for (var process : filteredProcesses)
            {
                var planningItem = getProductionPlanningItemForProcess(process);
                planningItem.getProcessPlanningItems()
                        .add(new ProcessPlaningItem(process, orderCount));
            }
            orderCount++;
        }
    }

    private ProductionPlanningItem getProductionPlanningItemForProcess(ProductionProcess process)
    {
        for (var item : this.productionPlanningItems)
        {
            var prodItem = item.getProduction().getProductionProcessForProduct(
                    process.getProductToProduce());
            if(prodItem != null)
                return item;
        }

        return null;
    }

    private void setProcessDepthForPlanningItems()
    {
        for(var planingItem : this.getFlatProcessList())
        {
            var depth = this.getProcessDepthRecursive(planingItem.getProcess().getProductToProduce());
            planingItem.setProcessDepth(depth);
        }

    }

    private int getProcessDepthRecursive(WarehouseItem item)
    {
        if(this.factory.checkIfItemHasASupplier(item))
            return 0;

        var process = this.factory.getProductionProcessForWarehouseItem(item);
        for (var position : process.getMaterialPositions())
        {
            return getProcessDepthRecursive(position.item()) + 1;

        }
        return 0;
    }

    private void optimizeBatches(List<Order> orderList)
    {
        var flatProcessList = getFlatProcessList();
        flatProcessList.sort(Comparator.comparingInt(ProcessPlaningItem::getProcessDepth));

        for(int i = flatProcessList.size() - 1; i >= 0; i--)
        {
            var planningItem = flatProcessList.get(i);
            var amountToProduce = 0;
            var nrOfBatchesFromParentProduct = 0;

            var parentPlanningItems = getParentProcessesPlanningItemFromProduct(planningItem
                    .getProcess()
                    .getProductToProduce(), flatProcessList);

            for (var parentItem : parentPlanningItems)
            {
                nrOfBatchesFromParentProduct += parentItem.getNrOfBatches();
            }

            //nrOfBatchesFromParentProduct == 0  no other item has been processed
            amountToProduce += nrOfBatchesFromParentProduct * planningItem.getProcess().getProductionBatchSize();

            for(var order : orderList)
            {
                if(planningItem.getProcess().getProductToProduce().equals(order.getProduct().item()))
                    amountToProduce += order.getProduct().amount();
            }

            var nrOfBatches = (int)Math.ceil((double) amountToProduce / (double) planningItem.getProcess().getProductionBatchSize());
            planningItem.setNrOfBatches(nrOfBatches);
        }
    }

    private List<ProcessPlaningItem> getParentProcessesPlanningItemFromProduct(WarehouseItem warehouseItem, List<ProcessPlaningItem> flatList)
    {
        var parentProcesses = new ArrayList<ProcessPlaningItem>();
        for (var planningItem : flatList)
        {
            var bom = planningItem.getProcess().getMaterialPositions();
            for(var item : bom)
            {
                if(item.item().equals(warehouseItem))
                    parentProcesses.add(planningItem);
            }
        }

        return parentProcesses;
    }

    private List<ProcessPlaningItem> getFlatProcessList()
    {
        var result = new ArrayList<ProcessPlaningItem>();

        for (var production : this.productionPlanningItems)
        {
            for (var process : production.getProcessPlanningItems())
            {
                result.add(process);

            }
        }
        return result;
    }

    private void removeDoubleEntriesFromPlaningItemList()
    {
        for(var production : this.productionPlanningItems)
        {
            var hashSet = new HashSet<ProcessPlaningItem>(production.getProcessPlanningItems());
            production.getProcessPlanningItems().clear();
            production.getProcessPlanningItems().addAll(hashSet);
            production.getProcessPlanningItems().sort((i1, i2) -> Integer.compare(i1.getOrderNr(), i2.getOrderNr()));
        }
    }

    private void calculateStartAndEndTimes()
    {
    }
}
