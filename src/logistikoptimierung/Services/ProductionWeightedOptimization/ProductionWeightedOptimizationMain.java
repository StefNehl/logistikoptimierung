package logistikoptimierung.Services.ProductionWeightedOptimization;

import logistikoptimierung.Contracts.IOptimizationService;
import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.FactoryObjects.FactoryStep;
import logistikoptimierung.Entities.FactoryObjects.ProductionProcess;
import logistikoptimierung.Entities.WarehouseItems.Order;
import logistikoptimierung.Entities.WarehouseItems.Product;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.ArrayList;
import java.util.List;

public class ProductionWeightedOptimizationMain implements IOptimizationService
{
    private Factory factory;

    public ProductionWeightedOptimizationMain(Factory factory)
    {
        this.factory = factory;
    }

    @Override
    public List<FactoryStep> optimize(List<Order> orderList, int nrOfOrdersToOptimize)
    {
        var factorySteps = new ArrayList<FactoryStep>();

        var subOrderList = new ArrayList<Order>();

        for(int i = 0; i < nrOfOrdersToOptimize; i++)
        {
            subOrderList.add(orderList.get(i));
        }

        factorySteps.addAll(optimizeProduction(subOrderList));


        return factorySteps;
    }

    private List<FactoryStep> optimizeProduction(List<Order> orderList)
    {
        var factorySteps = new ArrayList<FactoryStep>();

        var rankingList = new ArrayList<ProductionRankingItem>();

        //aggregate amount
        for(var order : orderList)
        {
            var rankingItem = getWarehouseItemFromList(rankingList, order.getProduct().item());
            if(rankingItem == null)
            {
                rankingList.add(new ProductionRankingItem(order.getProduct().item(), order.getProduct().amount()));
                continue;
            }

            rankingItem.setAmount(rankingItem.getAmount() + order.getProduct().amount());
        }

        //calculate nr of batches
        for(var rankingItem : rankingList)
        {
            var process = this.factory.getProductionProcessForWarehouseItem(rankingItem.getProduct());
            var batchSize = process.getProductionBatchSize();
            var nrOfBatches = (int) Math.ceil((double) rankingItem.getAmount() / (double) batchSize);
            rankingItem.setNrOfBatches(nrOfBatches);
        }

        //calculate startTime
        for(var rankingItem : rankingList)
        {
            var materialList = this.factory.getMaterialPositionsForProductWithRespectOfBatchSize(rankingItem.getProduct(), rankingItem.getAmount());
            if(materialList.isEmpty())
                continue;

            var mainProduct = materialList.get(0).item();
            if(this.factory.checkIfItemHasASupplier(mainProduct))
                continue;

            if(rankingItem.getProduct() == mainProduct)
                materialList.remove(0);

            long maxProcessTime = 0;

            for(var material : materialList)
            {
                var materialAmount = material.amount();
                if(this.factory.checkIfItemHasASupplier(material.item()))
                    continue;

                var processList = this.factory.getProductionProcessesForProduct(material.item());
                var process = this.factory.getProductionProcessForWarehouseItem(material.item());
                if(process == null)
                    continue;

                var nrOfSubProductBatches = materialAmount / process.getProductionBatchSize();
                var productionTime = calculateProductionTimeWithSubProducts(processList);
                var prodTimeSubProduct = (nrOfSubProductBatches * productionTime) / rankingItem.getNrOfBatches();
                if(maxProcessTime < prodTimeSubProduct)
                    maxProcessTime = prodTimeSubProduct;
            }

            rankingItem.setTime(maxProcessTime);
        }

        rankingList.sort((o1, o2) -> Long.compare(o1.getTime(), o2.getTime()));

        var count = 1;
        for (var rankingItem : rankingList)
        {
            rankingItem.setRanking(count);
            count++;
        }

        for(var rankingItem : rankingList)
        {
            if(this.factory.checkIfItemHasASupplier(rankingItem.getProduct()))
                continue;

            var processes = this.factory.getProductionProcessForWarehouseItem(rankingItem.getProduct());

            for(var position : processes.getMaterialPositions())
            {
                var subItem = getRankingItemFromList(position.item(), rankingList);
                if(subItem == null)
                    continue;
                rankingItem.setTime(subItem.getTime() + rankingItem.getTime());

            }
        }

        rankingList.sort((o1, o2) -> Long.compare(o1.getTime(), o2.getTime()));
        count = 1;
        for (var rankingItem : rankingList)
        {
            rankingItem.setRanking(count);
            count++;
        }

        return factorySteps;
    }

    private ProductionRankingItem getRankingItemFromList(WarehouseItem itemToSearch, List<ProductionRankingItem> items)
    {
        for(var item : items)
        {
            if(item.getProduct().getName().equals(itemToSearch.getName()))
                return item;

        }
        return null;
    }

    private long calculateProductionTimeWithSubProducts(List<ProductionProcess> productionProcesses)
    {
        long result = 0;
        for(var process : productionProcesses)
        {
            if(this.factory.checkIfItemHasASupplier(process.getProductToProduce()))
                continue;

            result += process.getProductionTime();
        }

        return result;
    }

    private ProductionRankingItem getWarehouseItemFromList(List<ProductionRankingItem> rankingItems, WarehouseItem item)
    {
        for(var rankingItem : rankingItems)
        {
            if(rankingItem.getProduct().equals(item))
                return rankingItem;
        }
        return  null;
    }
}
