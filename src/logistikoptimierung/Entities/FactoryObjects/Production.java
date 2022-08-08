
package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.WarehousePosition;
import logistikoptimierung.Entities.WarehouseItems.Product;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class create an object of the production. The production can have several production processes for different items.
 * The possible tasks for the production are:
 * - Move the materials to the input buffer
 * - Produce a specific product
 * - Move the produced product to the output buffer
 * - Move the produced product from the output buffer to the warehouse
 * Furthermore, the class simulates the blocked time, and the size of the input/output buffer and checks
 * if the material for the production is available.
 */
public class Production extends FactoryObject
{
    private final List<ProductionProcess> productionProcesses;
    private int remainingNrOfInputBufferBatches;
    private int remainingNrOfOutputBufferBatches;
    private final int maxNrOfInputBufferBatches;
    private final int maxNrOfOutputBufferBatches;

    private final Set<ProductionProcess> processesInInputBuffer = new HashSet<>();
    private WarehousePosition productInProduction;
    private final Set<WarehousePosition> productsInOutputBuffer = new HashSet<>();

    private FactoryStepTypes currentTask;

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
        if(currentTimeStep < super.getBlockedUntilTimeStep())
        {
            super.addBlockMessage(super.getName(), currentTask);
            return false;
        }
        this.currentTask = stepType;
        this.setBlockedUntilTimeStep(currentTimeStep);
        switch (stepType)
        {
            case MoveMaterialsForProductFromWarehouseToInputBuffer -> {
                if(remainingNrOfInputBufferBatches == 0)
                {
                    addNotEnoughCapacityInBufferLogMessage(false);
                    return false;
                }

                var process = getProductionProcessForProduct((Product) item);

                if(process == null)
                {
                    addProcessNotFoundMessage(item);
                    return false;
                }


                for(var m : process.getMaterialPositions())
                {
                    //Check if material is available
                    if(!this.getFactory().getWarehouse().checkIfMaterialIsAvailable(m.item(), m.amount()))
                    {
                        super.addErrorLogMessage("Material: " + item + " in the amount: " + amountOfItems + " not available");
                        return false;
                    }
                }

                for(var m : process.getMaterialPositions())
                {
                    var itemForBuffer = getFactory().getWarehouse().removeItemFromWarehouse(m);
                    if(itemForBuffer == null)
                        throw new RuntimeException();
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

                ProductionProcess processInInput = null;
                for(var process : processesInInputBuffer)
                {
                    if(process.getProductToProduce().equals(item))
                        processInInput = process;
                }

                if(processInInput == null)
                {
                    addItemNotInBufferLogMessage(item, false);
                    return false;
                }

                var producedProduct = produce(processInInput);

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
                WarehousePosition productToMove = null;
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
                {
                    productsInOutputBuffer.add(productToMove);
                    remainingNrOfOutputBufferBatches--;
                    return false;
                }
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
    public ProductionProcess getProductionProcessForProduct(Product item)
    {
        for(var process : productionProcesses)
        {
            if(item.getName().equals(process.getProductToProduce().getName()))
                return process;
        }
        return null;
    }

    /**
     * Produce a product. Simulates the production of a product
     * @param processInInput process for the production
     * @return a warehouse position with the product and the batch size as amount
     */
    private WarehousePosition produce(ProductionProcess processInInput)
    {
        processesInInputBuffer.remove(processInInput);
        remainingNrOfInputBufferBatches++;

        addBufferLogMessage(processInInput.getProductToProduce(), false, true);
        var blockedUntilTimeStep = this.getFactory().getCurrentTimeStep() + processInInput.getProductionTime();
        super.setBlockedUntilTimeStep(blockedUntilTimeStep);
        addProduceItemMessage(processInInput.getProductToProduce());

        return new WarehousePosition(processInInput.getProductToProduce(), processInInput.getProductionBatchSize());
    }

    /**
     * @return returns a list of every process of this production
     */
    public List<ProductionProcess> getProductionProcesses() {
        return productionProcesses;
    }

    /**
     * Resets the production. The input/output buffers are set back to empty. The blocking time step is set back to 0.
     * Current task to None. And the item which is in production gets removed.
     */
    public void resetProduction()
    {
        this.remainingNrOfInputBufferBatches = this.maxNrOfInputBufferBatches;
        this.remainingNrOfOutputBufferBatches = this.maxNrOfOutputBufferBatches;
        super.setBlockedUntilTimeStep(0);
        this.currentTask = FactoryStepTypes.None;
        this.productInProduction = null;
        this.productsInOutputBuffer.clear();
        this.processesInInputBuffer.clear();

    }

    private void addProcessNotFoundMessage(WarehouseItem product)
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
