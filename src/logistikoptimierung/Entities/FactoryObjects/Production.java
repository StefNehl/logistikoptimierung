package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.MaterialPosition;
import logistikoptimierung.Entities.WarehouseItems.Product;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Production extends FactoryObject
{
    private final List<ProductionProcess> productionProcesses;
    private int remainingNrOfInputBufferBatches;
    private int remainingNrOfOutputBufferBatches;

    private Set<ProductionProcess> processesInInputBuffer = new HashSet<>();
    private MaterialPosition productInProduction;
    private Set<MaterialPosition> productsInOutputBuffer = new HashSet<>();

    private String currentTask;
    private long blockedUntilTimeStep;

    public Production(String name,
                      int id,
                      List<ProductionProcess> productionProcesses,
                      int maxNrOfInputBufferBatches,
                      int maxNrOfOutputBufferBatches)
    {
        super(name, "P" + id, FactoryObjectTypes.Production);
        this.productionProcesses = productionProcesses;
        this.remainingNrOfInputBufferBatches = maxNrOfInputBufferBatches;
        this.remainingNrOfOutputBufferBatches = maxNrOfOutputBufferBatches;
        this.blockedUntilTimeStep = 0;
    }

    @Override
    public boolean doWork(long currentTimeStep, WarehouseItem item, int amountOfItems, String stepType)
    {
        if(currentTimeStep < this.blockedUntilTimeStep)
        {
            super.addBlockMessage(super.getName(), currentTask);
            return false;
        }
        currentTask = stepType;
        switch (stepType)
        {
            case FactoryStepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer -> {

                if(remainingNrOfInputBufferBatches == 0)
                {
                    addNotEnoughCapacityInBufferLogMessage(false);
                    return false;
                }

                var productToProduce = (Product) item;
                var process = getProductionProcessForProduct(productToProduce);

                if(process == null)
                {
                    addPProcessNotFoundMessage(productToProduce);
                    return false;
                }


                for(var m : process.getMaterialPositions())
                {
                    var itemForBuffer = getFactory().getWarehouse().removeItemFromWarehouse(m);
                    if(itemForBuffer == null)
                    {
                        super.addLogMessage("Not enough material (" + m.item().getName() + ") for product: " + item.getName() + " in warehouse");
                        return false;
                    }
                }

                processesInInputBuffer.add(process);
                remainingNrOfInputBufferBatches--;
                addBufferLogMessage(item, false, true);
            }
            case FactoryStepTypes.Produce -> {
                if(this.remainingNrOfOutputBufferBatches == 0)
                {
                    addNotEnoughCapacityInBufferLogMessage(true);
                    return false;
                }

                var producedProduct = produce((Product) item);

                if(producedProduct == null)
                    return false;

                productInProduction = producedProduct;
                return true;
            }
            case FactoryStepTypes.MoveProductToOutputBuffer -> {
                if(productInProduction == null)
                {
                    super.addLogMessage("No product in production. " + item.getName());
                    return false;
                }

                remainingNrOfOutputBufferBatches--;
                productsInOutputBuffer.add(productInProduction);
                productInProduction = null;
                addBufferLogMessage(item, true, false);
                return true;
            }
            case FactoryStepTypes.MoveProductFromOutputBufferToWarehouse -> {
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

                getFactory().getWarehouse().addItemToWarehouse(productToMove);
            }
        }

        return true;
    }

    public ProductionProcess getProductionProcessForProduct(WarehouseItem item)
    {
        for(var process : productionProcesses)
        {
            if(item.getName().equals(process.getProductToProduce().getName()))
                return process;
        }
        return null;
    }

    private MaterialPosition produce(Product productToProduce)
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

    private void addPProcessNotFoundMessage(Product product)
    {
        var message = super.getName() + product.getName() + " Process for product not found";
        super.addLogMessage(message);
    }

    private void addProduceItemMessage(Product product)
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
        super.addLogMessage(message);
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
