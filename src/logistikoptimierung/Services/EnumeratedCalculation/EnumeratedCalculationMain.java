package logistikoptimierung.Services.EnumeratedCalculation;

import logistikoptimierung.Contracts.IOptimizationService;
import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.FactoryObjects.FactoryStep;
import logistikoptimierung.Entities.FactoryObjects.ProductionProcess;
import logistikoptimierung.Entities.WarehouseItems.MaterialPosition;
import logistikoptimierung.Entities.WarehouseItems.Order;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.*;

public class EnumeratedCalculationMain implements IOptimizationService
{
    private final Factory factory;
    private List<ProductionPlanningItem> productionItem;
    private List<MaterialPosition> acquiringPlanningItems;
    private List<MaterialPosition> deliverPlanningItems;
    private List<MaterialPosition> allPlanningItems;

    public EnumeratedCalculationMain(Factory factory)
    {
        this.factory = factory;
        this.productionItem = new ArrayList<>();
        this.acquiringPlanningItems = new ArrayList<>();
        this.deliverPlanningItems = new ArrayList<>();
        this.allPlanningItems = new ArrayList<>();
    }

    @Override
    public List<FactoryStep> optimize(List<Order> orderList, int nrOfOrdersToOptimize)
    {
        var subOrderList = new ArrayList<Order>();

        for(int i = 0; i < nrOfOrdersToOptimize; i++)
        {
            subOrderList.add(orderList.get(i));
        }

        getAllNeededFactoryPlanningItemsForOrder(subOrderList);

        return null;
    }

    private void getAllNeededFactoryPlanningItemsForOrder(List<Order> subOrderList)
    {
        createProcessList(subOrderList);
        setProcessDepthForPlanningItems();
        removeDoubleEntriesFromPlaningItemList();
        getProcessesForEveryBatchAndOrderAfterDepth(subOrderList);
        addAcquiringPlaningItemsForEveryBatch();
        addDeliveryPlanningItems(subOrderList);

        for(var item : this.acquiringPlanningItems)
            this.allPlanningItems.add(item);

        for (var item : this.getFlatProcessList())
            this.allPlanningItems.add(new MaterialPosition(item.getProcess().getProductToProduce(),
                    1));

        for(var item : this.deliverPlanningItems)
            this.allPlanningItems.add(item);
    }

    private void createProcessList(List<Order> orderList)
    {
        for (var production : this.factory.getProductions())
        {
            productionItem.add(new ProductionPlanningItem(production));
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
        for (var item : this.productionItem)
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

    private void getProcessesForEveryBatchAndOrderAfterDepth(List<Order> orderList)
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
        for(var production : productionItem)
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

        for (var production : this.productionItem)
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
        for(var production : this.productionItem)
        {
            var hashSet = new HashSet<ProcessPlaningItem>(production.getProcessPlanningItems());
            production.getProcessPlanningItems().clear();
            production.getProcessPlanningItems().addAll(hashSet);
            production.getProcessPlanningItems().sort((i1, i2) -> Integer.compare(i1.getOrderNr(), i2.getOrderNr()));
        }
    }

    private void addAcquiringPlaningItemsForEveryBatch()
    {
        for(var processPlaningItem : getFlatProcessList())
        {
            var materialPositions = processPlaningItem.getProcess().getMaterialPositions();

            for(var materialPosition : materialPositions)
            {
                if(!this.factory.checkIfItemHasASupplier(materialPosition.item()))
                    continue;

                acquiringPlanningItems.add(new MaterialPosition(materialPosition.item(), materialPosition.amount()));
            }
        }
    }

    private void addDeliveryPlanningItems(List<Order> subOrderList)
    {
        for (var order : subOrderList)
        {
            deliverPlanningItems.add(new MaterialPosition(order.getProduct().item(),
                    order.getProduct().amount()));
        }
    }

    private void createFactoryStepsRecursive(List<FactoryStep> factorySteps)
    {
        for(var planningItem : productionItem)
        {
            //Add one item for every production

        }
    }



}
