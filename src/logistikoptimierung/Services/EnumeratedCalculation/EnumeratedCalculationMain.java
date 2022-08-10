package logistikoptimierung.Services.EnumeratedCalculation;

import logistikoptimierung.Contracts.IOptimizationService;
import logistikoptimierung.Entities.FactoryObjects.*;
import logistikoptimierung.Entities.Instance;
import logistikoptimierung.Entities.WarehouseItems.*;
import logistikoptimierung.Services.FirstComeFirstServeOptimizer.FirstComeFirstServeOptimizerMain;

import java.util.*;

/**
 * Creates an object of the optimizer with an enumeration of the possibilities and combinations for handling the order.
 * For this the algorithm gets every needed step for transportation, production and delivery and orders them in
 * every combination and simulates the factory to find the best result.
 * For the transport and driver constraints a Driver pool in the size of the nr of drivers and if the pool is full the
 * first driver is used again. This ensures with the trying of every combination that different drivers  and transporters
 * are used in the optimization.
 */
public class EnumeratedCalculationMain implements IOptimizationService
{
    private final Factory factory;
    private final List<Order> orderList;
    private FactoryMessageSettings factoryMessageSettings;

    private List<Transporter> sortedAvailableTransportList;
    private List<Driver> availableDrivers;
    private List<DriverPoolItem> driverPoolItems;

    private long bestTimeSolution;
    private List<FactoryStep> bestSolution = new ArrayList<>();
    private long nrOfSimulations = 0;
    private boolean condenseMaterialSupplies;
    private long maxSystemRunTime;
    private long startTime;

    /**
     * Creates an object of the optimizer with an enumeration of the possibilities and combinations for handling the order.
     * For this the algorithm gets every needed step for transportation, production and delivery and orders them in
     * every combination and simulates the factory to find the best result.
     * For the transport and driver constraints a Driver pool in the size of the nr of drivers and if the pool is full the
     * first driver is used again. This ensures with the trying of every combination that different drivers  and transporters
     * are used in the optimization.
     * @param instance with the factory and the orderlist where the optimization should happen
     * @param maxRuntime maximum run time for the optimization
     * @param factoryMessageSettings factory messages settings for the simulating factory
     * @param condenseMaterialSupplies condenses the supplying of the material to
     */
    public EnumeratedCalculationMain(Instance instance,
                                     long maxRuntime,
                                     boolean condenseMaterialSupplies,
                                     FactoryMessageSettings factoryMessageSettings,
                                     long maxSystemRunTimeInNanoSeconds)
    {
        this.factory = instance.factory();
        this.orderList = instance.orderList();
        this.factoryMessageSettings = factoryMessageSettings;

        this.sortedAvailableTransportList = new ArrayList<>(this.factory.getTransporters());
        this.sortedAvailableTransportList.sort(Comparator.comparingInt(Transporter::getCapacity));
        this.driverPoolItems = new ArrayList<>(this.factory.getNrOfDrivers());

        this.bestTimeSolution = maxRuntime;
        this.condenseMaterialSupplies = condenseMaterialSupplies;

        this.maxSystemRunTime = maxSystemRunTimeInNanoSeconds;
    }

    @Override
    public List<FactoryStep> optimize(int nrOfOrdersToOptimize)
    {
        var stepToDo = new ArrayList<FactoryStep>();
        if(nrOfOrdersToOptimize > this.orderList.size())
            return stepToDo;
        var subOrderList = new ArrayList<Order>();
        for(int i = 0; i < nrOfOrdersToOptimize; i++)
        {
            subOrderList.add(this.orderList.get(i));
        }

        var newInstance = new Instance(this.factory, this.orderList);

        var firstComeFirstServeOptimizer = new FirstComeFirstServeOptimizerMain(newInstance);
        this.bestSolution = firstComeFirstServeOptimizer.optimize(nrOfOrdersToOptimize);
        var firstComeFirstServeResult = this.factory.startFactory(this.orderList, this.bestSolution, this.bestTimeSolution, factoryMessageSettings);
        firstComeFirstServeResult++;
        this.bestTimeSolution = firstComeFirstServeResult;
        this.factory.resetFactory();

        var planningItems = getAllNeededFactoryPlanningItemsForOrder(subOrderList);
        System.out.println("Nr of planning items: " + planningItems.size());

        this.nrOfSimulations = 0;
        this.startTime = System.nanoTime();

        //Create copy of driver list
        availableDrivers = new ArrayList<Driver>();
        var idCount = 0;
        for(var driver : this.factory.getDrivers())
        {
            availableDrivers.add(new Driver(driver.getName(), idCount));
            idCount++;
        }

        getPlanningSolutionRecursive(stepToDo, planningItems, new ArrayList<>(this.factory.getProductions()));

        return bestSolution;
    }

    /**
     * @return Returns the nr of Simulations done in the last optimization
     */
    public long getNrOfSimulations()
    {
        return this.nrOfSimulations;
    }

    /**
     * Checks every combination of the planning items and simulate the factory to find the best result.
     * @param stepsToDo Steps to perform before the current step
     * @param planningItems planning item to add the factory steps
     */
    private void getPlanningSolutionRecursive(List<FactoryStep> stepsToDo, List<PlanningItem> planningItems, List<Production> availableProduction)
    {
        if(maxSystemRunTime != 0 && System.nanoTime() > (maxSystemRunTime + startTime))
            return;

        if(planningItems.isEmpty())
        {
            nrOfSimulations++;
            long result = this.factory.startFactory(this.orderList, stepsToDo, bestTimeSolution, factoryMessageSettings);
            var nrOfRemainingSteps = this.factory.getNrOfRemainingSteps();
            this.factory.resetFactory();

            if(nrOfSimulations % 10000 == 0) {
                //System.out.println("Nr of simulations: " + nrOfSimulations + " Result: " + result + " Nr Remaining Steps:" + nrOfRemainingSteps);
            }

            if(result < bestTimeSolution && nrOfRemainingSteps == 0)
            {
                bestTimeSolution = result;
                bestSolution = new ArrayList<>(stepsToDo);
                System.out.println("Nr of simulations: " + nrOfSimulations + " Result: " + result + " Nr Remaining Steps:" + nrOfRemainingSteps);
            }
        }

        var planningItemsToRemove = new ArrayList<PlanningItem>();
        for (var planningItem : planningItems)
        {
            var stepsToAdd = new ArrayList<FactoryStep>();
            switch (planningItem.planningType())
            {
                case Acquire -> {
                    stepsToAdd = getAcquireTransportSteps(stepsToDo, planningItem);
                }
                case Produce -> {
                    stepsToAdd = getProductionSteps(stepsToDo, planningItem);
                }
                case Deliver -> {
                    stepsToAdd = getDeliverTransporters(stepsToDo, planningItem);
                }
            }

            //abort complete solution because no material for the production or delivery in the warehouse
            if(stepsToAdd.isEmpty())
                continue;

            //Check if planning item can be processed in parallel
            if(planningItem.planningType().equals(PlanningType.Produce))
            {
                var productionProcess = this.factory.getProductionProcessForProduct((Product) planningItem.item());
                if(availableProduction.contains(productionProcess.getProduction()))
                {
                    stepsToDo.addAll(stepsToAdd);
                    availableProduction.remove(productionProcess.getProduction());
                    planningItemsToRemove.add(planningItem);
                    continue;
                }
                else
                {
                    availableProduction = new ArrayList<>(this.factory.getProductions());
                }
            }

            var copyOfPlanningItems = new ArrayList<>(planningItems);
            var copyOfSteps = new ArrayList<>(stepsToDo);
            copyOfSteps.addAll(stepsToAdd);
            copyOfPlanningItems.remove(planningItem);
            copyOfPlanningItems.removeAll(planningItemsToRemove);
            getPlanningSolutionRecursive(copyOfSteps, copyOfPlanningItems, availableProduction);

            //Release driver
            if(planningItem.planningType() == PlanningType.Acquire ||
                    planningItem.planningType() == PlanningType.Deliver)
            {
                if(this.driverPoolItems.isEmpty())
                    break;

                //Last item in the list, is the latest added one.
                var poolItem = this.driverPoolItems.remove(this.driverPoolItems.size() - 1);
                var removedDriver = poolItem.driver();

                var driveTime = 0;
                if(planningItem.item() instanceof Order)
                    driveTime = ((Order) planningItem.item()).getTravelTime();
                else if(planningItem.item() instanceof Material)
                    driveTime = ((Material) planningItem.item()).getTravelTime();

                removedDriver.setBlockedUntilTimeStep(removedDriver.getBlockedUntilTimeStep() - driveTime);
                this.availableDrivers.add(poolItem.driver());
                addTransporterToSortedTransportList(poolItem.transporter());
            }
        }
    }

    /**
     * Add the transporter to the sorted transportation list and keeps the order
     * @param transporterToAdd transporter to add
     */
    private void addTransporterToSortedTransportList(Transporter transporterToAdd)
    {
        var indexToAdd = 0;
        for(var transporter : this.sortedAvailableTransportList)
        {
            if(transporter.getCapacity() <= transporterToAdd.getCapacity())
                continue;

            indexToAdd = this.sortedAvailableTransportList.indexOf(transporter);
            break;
        }

        this.sortedAvailableTransportList.add(indexToAdd, transporterToAdd);
    }

    /**
     * Returns a list of factory steps for a planning item with the deliver property. The method checks if the material
     * was produced in the needed amount before. If not an empty list is returned.
     * @param stepsToDo steps which are getting performed before this planning item
     * @param planningItem planning item for the factory steps
     * @return factory steps for the planning item, returns an empty list if no product to deliver is available in the warehouse
     */
    private ArrayList<FactoryStep> getDeliverTransporters(List<FactoryStep> stepsToDo, PlanningItem planningItem)
    {
        var steps = new ArrayList<FactoryStep>();
        var amount = planningItem.amount();

        //Check if product is actually in the warehouse
        var amountOfProductInWarehouse = 0;
        var order = (Order)planningItem.item();

        var stepsToDoBefore = new ArrayList<FactoryStep>();

        for(var step : stepsToDo)
        {
            if(step.getStepType().equals(FactoryStepTypes.Produce))
            {
                //Check if item to deliver was produced
                if(step.getItemToManipulate()
                        .equals(order.getWarehousePosition().item()))
                {
                    var production = (Production)step.getFactoryObject();
                    var process = production.getProductionProcessForProduct((Product) order.getWarehousePosition().item());
                    if(process == null)
                        continue;

                    amountOfProductInWarehouse += process.getProductionBatchSize();
                    stepsToDoBefore.add(step);
                    continue;
                }

                var production = (Production) step.getFactoryObject();
                var process = production.getProductionProcessForProduct((Product) step.getItemToManipulate());

                //Check if any other production process was using the item
                for(var materialPosition : process.getMaterialPositions())
                {
                    if(step.getItemToManipulate().getName().equals(materialPosition.item().getName()))
                    {
                        amountOfProductInWarehouse -= materialPosition.amount();
                        stepsToDoBefore.add(step);
                    }
                }
            }

            if(step.getStepType().equals(FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse))
            {
                if(step.getItemToManipulate().getName()
                        .equals(order.getWarehousePosition().item().getName()))
                {
                    amountOfProductInWarehouse += step.getAmountOfItems();
                    stepsToDoBefore.add(step);
                }
            }

            if(step.getStepType().equals(FactoryStepTypes.ConcludeOrderTransportToCustomer))
            {
                var oldOrder = (Order)step.getItemToManipulate();
                if(oldOrder.getWarehousePosition().item().getName().equals(order.getWarehousePosition().item().getName()))
                {
                    amountOfProductInWarehouse -= step.getAmountOfItems();
                    stepsToDoBefore.add(step);
                }
            }
        }

        if(amountOfProductInWarehouse < order.getWarehousePosition().amount())
        {
            //System.out.println("Abort solution: Not enough products in the Warehouse to deliver");
            return steps;
        }

        //The last transporter needs to close the order
        Transporter lastTransport = null;
        var lastAmountToTransport = 0;
        while (amount > 0)
        {
            //Driver list is empty take release first driver from pool
            if(this.availableDrivers.isEmpty())
            {
                var poolItem = this.driverPoolItems.remove(0);
                this.availableDrivers.add(poolItem.driver());
                addTransporterToSortedTransportList(poolItem.transporter());
            }

            var bestTransporter = findBestTransporterForTheAmount(planningItem.item(), amount);

            if(bestTransporter == null)
                throw new RuntimeException("Should not happen ");

            var amountToTransport = 0;
            if(amount < bestTransporter.getCapacity())
                amountToTransport = amount;
            else
                amountToTransport = bestTransporter.getCapacity();

            var newStep = new FactoryStep(this.factory, stepsToDoBefore,
                    planningItem.item(),
                    amountToTransport,
                    bestTransporter,
                    FactoryStepTypes.ConcludeOrderTransportToCustomer);

            steps.add(newStep);

            var newDriver = this.availableDrivers.remove(0);
            this.sortedAvailableTransportList.remove(bestTransporter);

            var driveTime = 0;
            if(planningItem.item() instanceof Order)
                driveTime = ((Order) planningItem.item()).getTravelTime();

            newDriver.setBlockedUntilTimeStep(newDriver.getBlockedUntilTimeStep() + driveTime);

            var newPoolItem = new DriverPoolItem(newDriver, bestTransporter);
            driverPoolItems.add(newPoolItem);
            amount -= bestTransporter.getCapacity();

            lastTransport  = bestTransporter;
            lastAmountToTransport = amountToTransport;
        }

        var newStep = new FactoryStep(this.factory, stepsToDoBefore,
                planningItem.item(),
                lastAmountToTransport,
                lastTransport,
                FactoryStepTypes.ClosesOrderFromCustomer);

        steps.add(newStep);

        return steps;
    }

    /**
     * Returns a list of factory steps for a planning item with the acquire property.
     * @param planningItem planning item for the factory steps
     * @return factory steps for the planning item
     */
    private ArrayList<FactoryStep> getAcquireTransportSteps(List<FactoryStep> stepToDo, PlanningItem planningItem)
    {
        var steps = new ArrayList<FactoryStep>();
        var amount = planningItem.amount();

        //Removed this cutting plane. Possibility to remove a solution with a high runtime
        //if(checkIfWarehouseIsFull(stepToDo, amount))
        //    return steps;

        while (amount > 0)
        {
            //Driver list is empty take release first driver from pool
            if(this.availableDrivers.isEmpty())
            {
                var poolItem = this.driverPoolItems.remove(0);
                this.availableDrivers.add(poolItem.driver());
                addTransporterToSortedTransportList(poolItem.transporter());
            }

            var bestTransporter = findBestTransporterForTheAmount(planningItem.item(), amount);

            if(bestTransporter == null)
                throw new RuntimeException("Bug! no fitting Transporter ");

            var amountToTransport = 0;
            if(amount < bestTransporter.getCapacity())
                amountToTransport = amount;
            else
                amountToTransport = bestTransporter.getCapacity();

            var newStep = new FactoryStep(this.factory, 0,
                    planningItem.item(),
                    amountToTransport,
                    bestTransporter,
                    FactoryStepTypes.GetMaterialFromSuppliesAndMoveBackToWarehouse);

            steps.add(newStep);

            newStep = new FactoryStep(this.factory, 0,
                    planningItem.item(),
                    amountToTransport,
                    bestTransporter,
                    FactoryStepTypes.MoveMaterialFromTransporterToWarehouse);

            steps.add(newStep);



            var newDriver = this.availableDrivers.remove(0);
            this.sortedAvailableTransportList.remove(bestTransporter);

            var driveTime = 0;
            if (planningItem.item() instanceof Material)
                driveTime = ((Material) planningItem.item()).getTravelTime();

            newDriver.setBlockedUntilTimeStep(newDriver.getBlockedUntilTimeStep() + driveTime);
            var newPoolItem = new DriverPoolItem(newDriver, bestTransporter);

            driverPoolItems.add(newPoolItem);
            amount -= bestTransporter.getCapacity();

        }

        return steps;
    }

    private boolean checkIfWarehouseIsFull(List<FactoryStep> stepsToDo, int amountToAdd)
    {
        var warehouseCapacity = this.factory.getWarehouse().getWarehouseCapacity();
        warehouseCapacity -= amountToAdd;
        for(var step : stepsToDo)
        {
            if(step.getStepType().equals(FactoryStepTypes.MoveMaterialFromTransporterToWarehouse))
            {
                //reduce
                warehouseCapacity -= step.getAmountOfItems();
            }

            if(step.getStepType().equals(FactoryStepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer))
            {
                //add
                var process = this.factory.getProductionProcessForProduct((Product) step.getItemToManipulate());
                warehouseCapacity += process.getProductionBatchSize();
            }

            if(step.getStepType().equals(FactoryStepTypes.MoveProductFromOutputBufferToWarehouse))
            {
                //reduce
                var process = this.factory.getProductionProcessForProduct((Product) step.getItemToManipulate());
                warehouseCapacity -= process.getProductionBatchSize();
            }

            if(step.getStepType().equals(FactoryStepTypes.ConcludeOrderTransportToCustomer))
            {
                //add
                warehouseCapacity = warehouseCapacity + step.getAmountOfItems();

            }

            if(warehouseCapacity < 0)
                return true;
        }
        return false;
    }

    /**
     * Finds the best transporter from the available transporters. If none is available it returns one
     * of the driver pool.
     * @param item item for the transportation constraints
     * @param amount amount of the item
     * @return transporter, null if none was found
     */
    private Transporter findBestTransporterForTheAmount(WarehouseItem item, int amount)
    {
        var bestTransporter = this.getTransporterWithSmallestDifferenceFromAmountAndHigherCapacity(
                this.sortedAvailableTransportList,
                item,
                amount);

        //No Transporter found, need to reuse already used transporter
        if(bestTransporter == null)
        {
            var bestDriverPoolItem = getDriverPoolItemWithDriverWhoIsEarliestBack(item);
            bestTransporter = bestDriverPoolItem.transporter();

            //Release driver from this transporter
            this.driverPoolItems.remove(bestDriverPoolItem);
            this.availableDrivers.add(bestDriverPoolItem.driver());
            addTransporterToSortedTransportList(bestDriverPoolItem.transporter());
        }

        return bestTransporter;
    }

    /**
     * Returns the transporter with the smallest difference of the amount needed. The transporter is equal or higher than the amount needed.
     * If no transporter fits the needed amount. The transporter with the highest capacity gets returned
     * @param availableTransportersSortedByCapacity sorted list of available transporters
     * @param item item to transport
     * @param amount amount of the item
     * @return a fitting transporter. If none is found the method returns null.
     */
    private Transporter getTransporterWithSmallestDifferenceFromAmountAndHigherCapacity(List<Transporter> availableTransportersSortedByCapacity,
                                                                                        WarehouseItem item, int amount)
    {
        Transporter fittingTransporterWithHighestCapacity = null;
        for(var transporter : availableTransportersSortedByCapacity)
        {
            if(item instanceof Material)
            {
                if(!transporter.areTransportationConstraintsFulfilledForMaterial((Material) item))
                    continue;
            }
            else if(item instanceof  Order)
            {
                if(!transporter.areTransportationConstraintsFulfilledForOrder((Order) item))
                    continue;
            }

            if(transporter.getCapacity() >= amount)
                return transporter;

            if(fittingTransporterWithHighestCapacity == null ||
                    fittingTransporterWithHighestCapacity.getCapacity() < transporter.getCapacity())
                fittingTransporterWithHighestCapacity = transporter;
        }

        if(fittingTransporterWithHighestCapacity != null)
            return fittingTransporterWithHighestCapacity;

        return null;
    }

    private DriverPoolItem getDriverPoolItemWithDriverWhoIsEarliestBack(WarehouseItem item)
    {
        DriverPoolItem bestDriverPoolItem = null;

        for(var driverPoolItem : this.driverPoolItems)
        {
            if(item instanceof Material)
            {
                if(!driverPoolItem.transporter().areTransportationConstraintsFulfilledForMaterial((Material) item))
                    continue;
            }
            else if(item instanceof  Order)
            {
                if(!driverPoolItem.transporter().areTransportationConstraintsFulfilledForOrder((Order) item))
                    continue;
            }

            if(bestDriverPoolItem == null)
            {
                bestDriverPoolItem = driverPoolItem;
                continue;
            }

            if(driverPoolItem.driver().getBlockedUntilTimeStep() < bestDriverPoolItem.driver().getBlockedUntilTimeStep())
                bestDriverPoolItem = driverPoolItem;
        }

        return bestDriverPoolItem;
    }

    private DriverPoolItem getDriverPoolItemWithSmallestDifferenceFromAmountAndHigherCapacity(WarehouseItem item, int amount)
    {
        DriverPoolItem bestDriverPoolItem = null;
        DriverPoolItem driverPoolItemWithHighestCapacity = null;
        var minDiff = Integer.MAX_VALUE;
        for(var driverPoolItem : this.driverPoolItems)
        {
            if(item instanceof Material)
            {
                if(!driverPoolItem.transporter().areTransportationConstraintsFulfilledForMaterial((Material) item))
                    continue;
            }
            else if(item instanceof  Order)
            {
                if(!driverPoolItem.transporter().areTransportationConstraintsFulfilledForOrder((Order) item))
                    continue;
            }

            var diff = driverPoolItem.transporter().getCapacity() - amount;

            if(bestDriverPoolItem == null)
            {
                bestDriverPoolItem = driverPoolItem;
                driverPoolItemWithHighestCapacity = driverPoolItem;
                minDiff = diff;
                continue;
            }

            if(driverPoolItem.transporter().getCapacity() >
                    driverPoolItemWithHighestCapacity.transporter().getCapacity())
                driverPoolItemWithHighestCapacity = driverPoolItem;

            if(diff < 0)
                continue;

            if(diff < minDiff)
            {
                bestDriverPoolItem = driverPoolItem;
                minDiff = diff;
            }

        }

        if(bestDriverPoolItem.transporter().getCapacity() >= amount)
            return bestDriverPoolItem;

        return driverPoolItemWithHighestCapacity;
    }

    /**
     * Returns a list of factory steps for a planning item with the produce property. The method checks if the material
     * was transported in the needed amount before. If not an empty list is returned.
     * @param stepsToDo steps which are getting performed before this planning item
     * @param planningItem planning item for the factory steps
     * @return factory steps for the planning item, returns an empty list if no material is available in the warehouse
     */
    private ArrayList<FactoryStep> getProductionSteps(List<FactoryStep> stepsToDo, PlanningItem planningItem)
    {
        var steps = new ArrayList<FactoryStep>();
        var process = this
                .factory
                .getProductionProcessForProduct((Product) planningItem.item());

        var stepsToDoBefore = new ArrayList<FactoryStep>();

        //Check if material is actually in the warehouse
        for (var materialPosition : process.getMaterialPositions())
        {
            var materialInWarehouse = 0;
            for(var step : stepsToDo)
            {
                if(step.getStepType().equals(FactoryStepTypes.MoveMaterialFromTransporterToWarehouse))
                {
                    //Check if transporter got the needed material before
                    if(step.getItemToManipulate().equals(materialPosition.item()))
                    {
                        stepsToDoBefore.add(step);
                        materialInWarehouse += step.getAmountOfItems();
                    }
                }

                if(step.getStepType().equals(FactoryStepTypes.Produce))
                {
                    //Check if product was produced
                    if(step.getItemToManipulate().equals(materialPosition.item()))
                    {
                        var subProcess = this.factory.getProductionProcessForProduct((Product) step.getItemToManipulate());
                        materialInWarehouse += subProcess.getProductionBatchSize();
                        stepsToDoBefore.add(step);
                    }
                }

                //Check if any other production consumed the material before
                if(step.getStepType().equals(FactoryStepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer))
                {
                    var productionProcess = this.factory.getProductionProcessForProduct((Product) step.getItemToManipulate());
                    for(var materialPositionForProcess : productionProcess.getMaterialPositions())
                    {
                        if(materialPositionForProcess.item().equals(materialPosition.item()))
                        {
                            materialInWarehouse -= materialPositionForProcess.amount();
                            stepsToDoBefore.add(step);
                        }
                    }
                }
            }

            if(materialInWarehouse < materialPosition.amount())
            {
                //System.out.println("Abort solution: Not enough material in the Warehouse to produce");
                return steps;
            }
        }

        var newStep = new FactoryStep(this.factory, stepsToDoBefore,
                planningItem.item(),
                1,
                process.getProduction(),
                FactoryStepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer);

        steps.add(newStep);

        stepsToDoBefore = new ArrayList<>();
        stepsToDoBefore.add(newStep);
        newStep = new FactoryStep(this.factory, stepsToDoBefore,
                planningItem.item(),
                1,
                process.getProduction(),
                FactoryStepTypes.Produce);

        steps.add(newStep);

        stepsToDoBefore = new ArrayList<>();
        stepsToDoBefore.add(newStep);
        newStep = new FactoryStep(this.factory, stepsToDoBefore,
                planningItem.item(),
                1,
                process.getProduction(),
                FactoryStepTypes.MoveProductToOutputBuffer);

        steps.add(newStep);


        stepsToDoBefore = new ArrayList<>();
        stepsToDoBefore.add(newStep);
        newStep = new FactoryStep(this.factory, stepsToDoBefore,
                planningItem.item(),
                planningItem.amount(),
                process.getProduction(),
                FactoryStepTypes.MoveProductFromOutputBufferToWarehouse);

        steps.add(newStep);

        return steps;
    }

    /**
     * Returns a list of planning items which are needed to fulfill the orders in the subOrder list
     * This algorithm could be improved heavily, but time constraints and the reason that this code runs only once per test
     * was the reason we did not improve the code.
     * @param subOrderList order list for the planning items
     * @return list of planning items needed
     */
    private List<PlanningItem> getAllNeededFactoryPlanningItemsForOrder(List<Order> subOrderList)
    {
        var productionPlanningItems = createProcessList(subOrderList);
        setProcessDepthForPlanningItems(productionPlanningItems);
        removeDoubleEntriesFromPlaningItemList(productionPlanningItems);
        getProcessesForEveryBatchAndOrderAfterDepth(subOrderList, productionPlanningItems);
        var acquiringMaterialPositions = addAcquiringPlaningItemsForEveryBatch(productionPlanningItems);
        acquiringMaterialPositions.addAll(addAcquiringPlaningItemsForDirectDelivery(subOrderList));
        var deliverMaterialPositions = addDeliveryPlanningItems(subOrderList);

        var planningItems = new ArrayList<PlanningItem>();

        if(this.condenseMaterialSupplies)
        {
            acquiringMaterialPositions = this.condenseMaterialList(acquiringMaterialPositions);
        }

        var idCount = 1;

        for(var item : acquiringMaterialPositions)
            planningItems.add(new PlanningItem(idCount++, item.item(), item.amount(), PlanningType.Acquire));

        var sortedFlatProductionProcessList = new ArrayList<>(this.getFlatProductionProcessList(productionPlanningItems));
        sortedFlatProductionProcessList.sort(Comparator.comparingInt(value -> value.getProcessDepth()));
        for (var item : sortedFlatProductionProcessList)
            planningItems.add(new PlanningItem(idCount++, item.getProcess().getProductToProduce(),
                    1,
                    PlanningType.Produce));

        for(var item : deliverMaterialPositions)
            planningItems.add(new PlanningItem(idCount++, item.item(), item.amount(), PlanningType.Deliver));

        return planningItems;
    }

    /**
     * Returns production planning items for the orders without products which can be supplied
     * @param orderList
     * @return list of production planning items
     */
    private ArrayList<ProductionPlanningItem> createProcessList(List<Order> orderList)
    {
        var productionItems = new ArrayList<ProductionPlanningItem>();
        for (var production : this.factory.getProductions())
        {
            productionItems.add(new ProductionPlanningItem(production));
        }

        var orderCount = 1;
        for (var order : orderList)
        {
            var processes = this.factory.getProductionProcessesForProduct(
                    order.getWarehousePosition().item());

            var filteredProcesses = new ArrayList<ProductionProcess>();
            for(var process : processes)
            {
                if(this.factory.checkIfItemHasASupplier(process.getProductToProduce()))
                    continue;
                filteredProcesses.add(process);
            }

            for (var process : filteredProcesses)
            {
                var planningItem = getProductionPlanningItemForProcess(process, productionItems);
                planningItem.getProcessPlanningItems()
                        .add(new ProcessPlaningItem(process, orderCount));
            }
            orderCount++;
        }

        return productionItems;
    }

    private ProductionPlanningItem getProductionPlanningItemForProcess(ProductionProcess process, List<ProductionPlanningItem> planningItems)
    {
        for (var item : planningItems)
        {
            var prodItem = item.getProduction().getProductionProcessForProduct((Product)
                    process.getProductToProduce());
            if(prodItem != null)
                return item;
        }

        return null;
    }

    private void setProcessDepthForPlanningItems(List<ProductionPlanningItem> planningItems)
    {
        for(var planingItem : this.getFlatProductionProcessList(planningItems))
        {
            var depth = this.getProcessDepthRecursive(planingItem.getProcess().getProductToProduce());
            planingItem.setProcessDepth(depth);
        }

    }

    private int getProcessDepthRecursive(WarehouseItem item)
    {
        if(this.factory.checkIfItemHasASupplier(item))
            return 0;

        var process = this.factory.getProductionProcessForProduct((Product) item);
        for (var position : process.getMaterialPositions())
        {
            return getProcessDepthRecursive(position.item()) + 1;
        }
        return 0;
    }

    /**
     * Returns every process batch and orders it after the depth
     * @param orderList order list to do
     * @param planningItems list of the planning items which should be processed
     */
    private void getProcessesForEveryBatchAndOrderAfterDepth(List<Order> orderList, List<ProductionPlanningItem> planningItems)
    {
        var flatProcessList = getFlatProductionProcessList(planningItems);
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
                orderMap.put(parentItem.getOrderNr(), amountToProduce);
            }

            for(var order : orderList)
            {
                if(planningItem.getProcess().getProductToProduce().equals(order.getWarehousePosition().item()))
                {
                    amountToProduce += order.getWarehousePosition().amount();
                    orderMap.put(order.getOrderNr(), order.getWarehousePosition().amount());
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
        for(var production : planningItems)
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

    /**
     * Returns a list flat list from all production processes.
     * @return list with process planning items
     */
    private List<ProcessPlaningItem> getFlatProductionProcessList(List<ProductionPlanningItem> planningItems)
    {
        var result = new ArrayList<ProcessPlaningItem>();

        for (var production : planningItems)
        {
            for (var process : production.getProcessPlanningItems())
            {
                result.add(process);
            }
        }
        return result;
    }

    /**
     * Remove double entries for the planning production list
     * @param productionPlanningItems
     */
    private void removeDoubleEntriesFromPlaningItemList(List<ProductionPlanningItem> productionPlanningItems)
    {
        for(var production : productionPlanningItems)
        {
            var hashSet = new HashSet<>(production.getProcessPlanningItems());
            production.getProcessPlanningItems().clear();
            production.getProcessPlanningItems().addAll(hashSet);
            production.getProcessPlanningItems().sort((i1, i2) -> Integer.compare(i1.getOrderNr(), i2.getOrderNr()));
        }
    }

    /**
     * Returns a list of material positions which are needed for the production
     * @param productionPlanningItems production processes for the orders to optimize
     * @return list of material positions
     */
    private List<WarehousePosition> addAcquiringPlaningItemsForEveryBatch(List<ProductionPlanningItem> productionPlanningItems)
    {
        var materialPositionsToAcquire = new ArrayList<WarehousePosition>();
        for(var processPlaningItem : getFlatProductionProcessList(productionPlanningItems))
        {
            var materialPositions = processPlaningItem.getProcess().getMaterialPositions();

            for(var materialPosition : materialPositions)
            {
                if(!this.factory.checkIfItemHasASupplier(materialPosition.item()))
                    continue;

                materialPositionsToAcquire.add(new WarehousePosition(materialPosition.item(), materialPosition.amount()));
            }
        }

        return materialPositionsToAcquire;
    }

    /**
     * Returns a condensed material position list
     * @param materialList not condensed material position list
     * @return condensed material position list
     */
    private List<WarehousePosition> condenseMaterialList(List<WarehousePosition> materialList)
    {
        var newMaterialList = new ArrayList<WarehousePosition>();

        for (var item: materialList)
        {
            var position = findMaterialPositionByName(item.item().getName(), newMaterialList);
            if(position == null)
            {
                var newPosition = new WarehousePosition(item.item(), item.amount());
                newMaterialList.add(newPosition);
                continue;
            }

            var newPosition = new WarehousePosition(item.item(), position.amount() + item.amount());
            newMaterialList.remove(position);
            newMaterialList.add(newPosition);
        }

        return newMaterialList;
    }

    /**
     * Finds the material from the material list by name
     * @param name name to search
     * @param materialList material list to search
     * @return material position
     */
    private WarehousePosition findMaterialPositionByName(String name, List<WarehousePosition> materialList)
    {
        for (var position : materialList)
        {
            if(position.item().getName().equals(name))
                return position;
        }

        return null;
    }

    /**
     * Returns a list of material position which are delivered directly to the customer
     * @param orderList orders to check for direct delivery
     * @return a list of material positions
     */
    private List<WarehousePosition> addAcquiringPlaningItemsForDirectDelivery(List<Order> orderList)
    {
        var materialPositionsToAcquire = new ArrayList<WarehousePosition>();
        for(var order : orderList)
        {
            if(this.factory.checkIfItemHasASupplier(order.getWarehousePosition().item()))
                materialPositionsToAcquire.add(new WarehousePosition(order.getWarehousePosition().item(), order.getWarehousePosition().amount()));
        }
        return materialPositionsToAcquire;
    }

    /**
     * Returns a list of material positions to deliver to the customer
     * @param subOrderList orders to check
     * @return list of material positions
     */
    private List<WarehousePosition> addDeliveryPlanningItems(List<Order> subOrderList)
    {
        var deliveryItems = new ArrayList<WarehousePosition>();
        for (var order : subOrderList)
        {
            deliveryItems.add(new WarehousePosition(order,
                    order.getWarehousePosition().amount()));
        }
        return deliveryItems;
    }
}