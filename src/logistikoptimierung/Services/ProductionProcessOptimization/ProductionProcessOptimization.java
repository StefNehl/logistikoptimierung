package logistikoptimierung.Services.ProductionProcessOptimization;

import logistikoptimierung.Contracts.IOptimizationService;
import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.FactoryObjects.FactoryStep;
import logistikoptimierung.Entities.FactoryObjects.ProductionProcess;
import logistikoptimierung.Entities.WarehouseItems.Order;
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
        removeDoubleEntriesFromPlaningItemList();
        optimizeBatches(subOrderList);
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

        var newProcessPlaningItemList = new ArrayList<ProcessPlaningItem>();

        for(int i = flatProcessList.size() - 1; i >= 0; i--)
        {
            var planningItem = flatProcessList.get(i);
            var amountToProduce = 0;

            var parentPlanningItems = getParentProcessesPlanningItemFromProduct(planningItem
                    .getProcess()
                    .getProductToProduce(),
                    newProcessPlaningItemList);

            var orderMap = new HashMap<Integer, Integer>();

            for (var parentItem : parentPlanningItems)
            {
                var amountForParentItem = parentItem.getProcess().getAmountFromMaterialPositions(
                        planningItem.getProcess().getProductToProduce());
                amountToProduce += amountForParentItem;
                orderMap.put(parentItem.getOrderNr(), amountForParentItem);
            }

            for(var order : orderList)
            {
                if(planningItem.getProcess().getProductToProduce().equals(order.getProduct().item()))
                {
                    amountToProduce += order.getProduct().amount();
                    orderMap.put(order.getOrderNr(), order.getProduct().amount());
                }
            }

            var nrOfBatches = (int)Math.ceil((double) amountToProduce / (double) planningItem.getProcess().getProductionBatchSize());

            var batchCount = 0;
            for(var orderKey : orderMap.keySet())
            {
                var orderAmount = orderMap.get(orderKey);
                var amountFromBatch = 0;
                while (orderAmount > amountFromBatch )
                {
                    if(batchCount == nrOfBatches)
                        break;

                    var batchSize = planningItem.getProcess().getProductionBatchSize();
                    var newPlaningItem = new ProcessPlaningItem(planningItem.getProcess(), orderKey);
                    newPlaningItem.setProcessDepth(planningItem.getProcessDepth());
                    newProcessPlaningItemList.add(newPlaningItem);
                    amountFromBatch += batchSize;
                    batchCount++;
                }
            }
        }

        //merge new planing list to old one
        for(var production : productionPlanningItems)
        {
            production.getProcessPlanningItems().clear();
            for(var newProcessItem : newProcessPlaningItemList)
            {
                if(newProcessItem.getProcess().getProduction().equals(production.getProduction()))
                    production.getProcessPlanningItems().add(newProcessItem);
            }
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
        var processesToDo = new ArrayList<>(this.getFlatProcessList());

        while (!processesToDo.isEmpty())
        {
            var lowestItem = getPlaningItemWithLowestOrderAndDepth(processesToDo);

            long assemblyTime = lowestItem.getProcess().getProductionTime();
            long startTime = 0;
            var productionPlanningItem = getProductionPlanningItemForProcess(lowestItem
                    .getProcess());
            var lastProcessInProduction = getProcessPlaningItemWithHighestEndTimeGreaterThanZero(
                    productionPlanningItem.getProcessPlanningItems());

            if(lastProcessInProduction != null)
                startTime = lastProcessInProduction.getEndTimeStamp();


            if(lowestItem.getProcessDepth() > 1)
            {
                var preProcessItem = getPreProcessPlaningItemWithHighestEndTime(lowestItem);
                startTime = Math.max(startTime, preProcessItem.getEndTimeStamp());
            }

            startTime++;
            lowestItem.setStartTimeStamp(startTime);
            lowestItem.setEndTimeStamp(startTime + assemblyTime);
            processesToDo.remove(lowestItem);

        }
    }

    private ProcessPlaningItem getPlaningItemWithLowestOrderAndDepth(List<ProcessPlaningItem> processPlaningItems)
    {
        if(processPlaningItems.isEmpty())
            return null;

        ProcessPlaningItem lowestItem = processPlaningItems.get(0);
        for(var item : processPlaningItems)
        {
            if(item.getProcessDepth() < lowestItem.getProcessDepth())
                lowestItem = item;

            if(item.getProcessDepth() <= lowestItem.getProcessDepth() &&
                item.getOrderNr() < lowestItem.getOrderNr())
                lowestItem = item;
        }
        return lowestItem;
    }

    private ProcessPlaningItem getProcessPlaningItemWithHighestEndTimeGreaterThanZero(List<ProcessPlaningItem> processPlaningItems)
    {
        ProcessPlaningItem highestEndTimeProcess = null;
        for(var item : processPlaningItems)
        {
            if(highestEndTimeProcess == null && item.getEndTimeStamp() > 0)
                highestEndTimeProcess = item;

            if(highestEndTimeProcess != null && item.getEndTimeStamp() > highestEndTimeProcess.getEndTimeStamp())
                highestEndTimeProcess = item;
        }

        return highestEndTimeProcess;
    }

    private ProcessPlaningItem getPreProcessPlaningItemWithHighestEndTime(ProcessPlaningItem processPlaningItem)
    {
        ProcessPlaningItem itemWithHighestEndTime = null;
        for(var product : processPlaningItem.getProcess().getMaterialPositions())
        {
            for(var item : getFlatProcessList())
            {
                if(item.getProcess().getProductToProduce().equals(product.item()))
                {
                    if(itemWithHighestEndTime == null)
                    {
                        itemWithHighestEndTime = item;
                        continue;
                    }

                    if(item.getEndTimeStamp() > itemWithHighestEndTime.getEndTimeStamp())
                        itemWithHighestEndTime = item;

                }
            }
        }

        return itemWithHighestEndTime;
    }
}
