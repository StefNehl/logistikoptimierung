
package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.MaterialPosition;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Production extends FactoryObject
{
    private final List<ProductionProcess> productionProcesses;
    private int remainingNrOfInputBufferBatches;
    private int remainingNrOfOutputBufferBatches;
    private final int maxNrOfInputBufferBatches;
    private final int maxNrOfOutputBufferBatches;

    private final Set<ProductionProcess> processesInInputBuffer = new HashSet<>();
    private MaterialPosition productInProduction;
    private final Set<MaterialPosition> productsInOutputBuffer = new HashSet<>();

    private FactoryStepTypes currentTask;
    private long blockedUntilTimeStep;

    /**
     * This class create an object of the production. The production can have several production processes for different items.
     * The possible tasks for the production are:
     * - Move the materials to the input buffer
     * - Produce a specific product
     * - Move the produced product to the output buffer
     * - Move the produced product from the output buffer to the warehouse
     *
     * Furthermore, the class simulates the blocked time, and the size of the input/output buffer and checks
     * if the material for the production is available.
     *
     * @param name sets the name
     * @param id sets the unique id
     * @param productionProcesses sets the different production processes
     * @param maxNrOfInputBufferBatches sets the maximum amount of items in the input buffer
     * @param maxNrOfOutputBufferBatches sets the maximum amount of items in the output buffer
     */
    public Production(String name,
                      int id,
                      List<ProductionProcess> productionProcesses,
                      int maxNrOfInputBufferBatches,
                      int maxNrOfOutputBufferBatches)
    {
        super(name, "P" + id, FactoryObjectMessageTypes.Production);
        this.productionProcesses = productionProcesses;
        this.remainingNrOfInputBufferBatches = maxNrOfInputBufferBatches;
        this.remainingNrOfOutputBufferBatches = maxNrOfOutputBufferBatches;
        this.maxNrOfInputBufferBatches = maxNrOfInputBufferBatches;
        this.maxNrOfOutputBufferBatches = maxNrOfOutputBufferBatches;
        this.blockedUntilTimeStep = 0;
    }

    /**
     * Performs a task with the production.
     * Task types are:
     * - MoveMaterialsForProductFromWarehouseToInputBuffer => gets the material for the specific product to produce from the warehouse
     * - Produce => produce the product
     * - MoveProductToOutputBuffer => move the product from the production to the output buffer
     * - MoveProductFromOutputBufferToWarehouse => moves the product form the output buffer to the warehouse
     * @param currentTimeStep sets the current timeStep
     * @param item which warehouse item should be manipulated
     * @param amountOfItems the amount of items which should be manipulated
     * @param stepType  what is the task to do
     * @return return true if the task was successfully or false if not
     */
    @Override
    public boolean doWork(long currentTimeStep, WarehouseItem item, int amountOfItems, FactoryStepTypes stepType)
    {
        if(currentTimeStep < this.blockedUntilTimeStep)
        {
            super.addBlockMessage(super.getName(), currentTask);
            return false;
        }
        currentTask = stepType;
        switch (stepType)
        {
            case MoveMaterialsForProductFromWarehouseToInputBuffer -> {
                if(remainingNrOfInputBufferBatches == 0)
                {
                    addNotEnoughCapacityInBufferLogMessage(false);
                    return false;
                }

                var process = getProductionProcessForProduct(item);

                if(process == null)
                {
                    addPProcessNotFoundMessage(item);
                    return false;
                }

                for(var m : process.getMaterialPositions())
                {
                    var itemForBuffer = getFactory().getWarehouse().removeItemFromWarehouse(m);
                    if(itemForBuffer == null)
                    {
                        super.addErrorLogMessage("Not enough material (" + m.item().getName() + ") for product: " + item.getName() + " in warehouse");
                        return false;
                    }
                }

                processesInInputBuffer.add(process);
                remainingNrOfInputBufferBatches--;
                addBufferLogMessage(item, false, true);
            }
            case Produce -> {
                if(this.remainingNrOfOutputBufferBatches == 0)
                {
                    addNotEnoughCapacityInBufferLogMessage(true);
                    return false;
                }

                var producedProduct = produce(item);

                if(producedProduct == null)
                {
                    super.addErrorLogMessage("Not able to produce product");
                    return false;
                }

                productInProduction = producedProduct;
                return true;
            }
            case MoveProductToOutputBuffer -> {
                if(productInProduction == null)
                {
                    super.addErrorLogMessage("No product in production. " + item.getName());
                    return false;
                }

                if(remainingNrOfOutputBufferBatches == 0)
                {
                    super.addErrorLogMessage("Not enough space in the output buffer");
                    return false;
                }

                remainingNrOfOutputBufferBatches--;
                productsInOutputBuffer.add(productInProduction);
                productInProduction = null;
                addBufferLogMessage(item, true, false);
                return true;
            }
            case MoveProductFromOutputBufferToWarehouse -> {
                MaterialPosition productToMove = null;
                for(var product : productsInOutputBuffer)
                {
                    if(product.item().equals(item))
                        productToMove = product;
                }

                if(productToMove == null)
                {
                    addItemNotInBufferLogMessage(item, true);
                    return false;
                }

                productsInOutputBuffer.remove(productToMove);
                remainingNrOfOutputBufferBatches++;
                addBufferLogMessage(item, true, true);

                var result = getFactory().getWarehouse().addItemToWarehouse(productToMove);
                if(!result)
                    return false;
            }
            default -> throw new IllegalStateException("Unexpected value: " + stepType);
        }

        return true;
    }

    /**
     * Return the production process for the product to produce.
     * @param item the Product which should be produce
     * @return the production process for the product to produce. Returns null if the production does not include a
     * process for the product.
     */
    public ProductionProcess getProductionProcessForProduct(WarehouseItem item)
    {
        for(var process : productionProcesses)
        {
            if(item.getName().equals(process.getProductToProduce().getName()))
                return process;
        }
        return null;
    }

    private MaterialPosition produce(WarehouseItem productToProduce)
    {
        ProductionProcess processInInput = null;
        for(var process : processesInInputBuffer)
        {
            if(process.getProductToProduce().equals(productToProduce))
                processInInput = process;
        }

        if(processInInput == null)
        {
            addItemNotInBufferLogMessage(productToProduce, false);
            return null;
        }

        processesInInputBuffer.remove(processInInput);
        remainingNrOfInputBufferBatches++;

        addBufferLogMessage(productToProduce, false, true);
        blockedUntilTimeStep = this.getFactory().getCurrentTimeStep() + processInInput.getProductionTime();
        addProduceItemMessage(productToProduce);

        return new MaterialPosition(productToProduce, processInInput.getProductionBatchSize());
    }

    /**
     * Resets the production. The input/output buffers are set back to empty. The blocking time step is set back to 0.
     * Current task to None. And the item which is in production gets removed.
     */
    public void resetProduction()
    {
        this.remainingNrOfInputBufferBatches = this.maxNrOfInputBufferBatches;
        this.remainingNrOfOutputBufferBatches = this.maxNrOfOutputBufferBatches;
        this.blockedUntilTimeStep = 0;
        this.currentTask = FactoryStepTypes.None;
        this.productInProduction = null;
        this.productsInOutputBuffer.clear();
        this.processesInInputBuffer.clear();

    }

    private void addPProcessNotFoundMessage(WarehouseItem product)
    {
        var message = super.getName() + product.getName() + " Process for product not found";
        super.addErrorLogMessage(message);
    }

    private void addProduceItemMessage(WarehouseItem product)
    {
        var message = super.getName() + " Task: produce " + product.getName();
        super.addLogMessage(message);
    }

    private void addNotEnoughCapacityInBufferLogMessage(boolean isOutputBuffer)
    {
        var bufferName = "InputBuffer";
        if(isOutputBuffer)
            bufferName = "OutputBuffer";

        var message = super.getName() + " Not enough capacity in " + bufferName;
        super.addErrorLogMessage(message);
    }

    private void addItemNotInBufferLogMessage(WarehouseItem item, boolean isOutputBuffer)
    {
        var bufferName = "InputBuffer";
        if(isOutputBuffer)
            bufferName = "OutputBuffer";

        var message = super.getName() + " item " + item.getName() + " not in " + bufferName;
        super.addLogMessage(message);
    }

    private void addBufferLogMessage(WarehouseItem item, boolean isOutputBuffer, boolean isRemoveOperation)
    {
        var bufferName = "InputBuffer";
        var remCapacity = this.remainingNrOfInputBufferBatches;
        if(isOutputBuffer)
        {
            bufferName = "OutputBuffer";
            remCapacity = this.remainingNrOfOutputBufferBatches;
        }

        var operationName = "moved";
        var fromTo = "to";
        if(isRemoveOperation)
        {
            operationName = "removed";
            fromTo = "from";
        }

        var message = super.getName() + " " + operationName + " item " + item.getName() + " " + fromTo + " " + bufferName + " RC: " + remCapacity;
        super.addLogMessage(message);
    }
}
