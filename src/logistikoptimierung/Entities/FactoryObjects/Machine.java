package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.StepTypes;
import logistikoptimierung.Entities.WarehouseItems.Material;
import logistikoptimierung.Entities.WarehouseItems.Product;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.ArrayList;
import java.util.List;

public class Machine extends FactoryObject {

    private final List<WarehouseItem> outputBuffer;
    private int remainingCapacityOutputBuffer;
    private final List<WarehouseItem> inputBuffer;
    private int remainingCapacityInputBuffer;
    private WarehouseItem itemInProduction;
    private String currentTask;
    private double remainingAssemblyTime;
    private int blockedUntilTimeStep;

    public Machine(String name, int maxAmountInPutBuffer, int maxAmountOutPutBuffer, double maxAssemblyTime, Factory factory)
    {
        super(name);
        outputBuffer = new ArrayList<>();
        inputBuffer = new ArrayList<>();
        this.remainingCapacityInputBuffer = maxAmountInPutBuffer;
        this.remainingCapacityOutputBuffer = maxAmountOutPutBuffer;
        this.remainingAssemblyTime = maxAssemblyTime;
        this.blockedUntilTimeStep = 0;
    }

    //Only returns item if you use Remove from Buffer step
    @Override
    public boolean doWork(int currentTimeStep, WarehouseItem item, int amountOfItems, String stepType)
    {
        if(currentTimeStep < this.blockedUntilTimeStep)
        {
            super.getFactory().addBlockLog(super.getName(), currentTask);
            return false;
        }
        currentTask = stepType;
        switch (stepType)
        {
            case StepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer ->
            {
                /*
                var itemsToAddToBuffer = ((Product)item).getBillOfMaterial();

                for(var m : itemsToAddToBuffer)
                {
                    for (int i = 0; i < m.amount(); i++)
                    {
                        var itemForBuffer = getFactory().getWarehouse().removeItemFromWarehouse(m.material());
                        if(itemForBuffer == null)
                        {
                            super.getFactory().addLog("Not enough material (" + m.material().getName() + ") for product: " + item.getName() + " in warehouse");
                            return false;
                        }
                        addItemToInputBuffer((Material) itemForBuffer);
                    }
                }

                 */
            }
            case StepTypes.Produce -> {
                if(remainingCapacityOutputBuffer == 0)
                {
                    addNotEnoughCapacityInBufferLogMessage(true);
                    return false;
                }
                if(!itemsForProductAreAvailableInInputBuffer((Product) item))
                {
                    addItemNotInBufferLogMessage(item, true);
                    return false;
                }
                /*
                for (var m : ((Product) item).getBillOfMaterial())
                {
                    removeItemFromBuffer(m.material(), false);
                }

                 */
                itemInProduction = produceProduct((Product) item);

            }
            case StepTypes.MoveProductToOutputBuffer -> {
                if(itemInProduction == null)
                {
                    addProduceItemMessage((Product) item);
                    return false;
                }
                return addItemToOutputBuffer(itemInProduction);
            }
            case StepTypes.MoveProductFromOutputBufferToWarehouse ->
            {
                var product = removeItemFromBuffer(item, true);
                if(product == null)
                {
                    addItemNotInBufferLogMessage(item, true);
                    return false;
                }
                //this.getFactory().getWarehouse().addItemToWarehouse(product);
            }
        }

        return true;
    }

    private void addItemToInputBuffer(Material item)
    {
        if(remainingCapacityInputBuffer == 0)
        {
            addNotEnoughCapacityInBufferLogMessage(false);
            return;
        }

        inputBuffer.add(item);
        remainingCapacityInputBuffer--;
        addBufferLogMessage(item, false, false);
    }

    private boolean addItemToOutputBuffer(WarehouseItem item)
    {
        outputBuffer.add(item);
        remainingCapacityOutputBuffer--;
        addBufferLogMessage(item, true, false);
        return true;
    }

    private WarehouseItem removeItemFromBuffer(WarehouseItem item, boolean isOutputBuffer)
    {
        var buffer = inputBuffer;
        if(isOutputBuffer)
            buffer = outputBuffer;

        for(var itemInBuffer : buffer)
        {
            if(itemInBuffer.getName().equals(item.getName()))
            {
                if(isOutputBuffer)
                    remainingCapacityOutputBuffer++;
                else
                    remainingCapacityInputBuffer++;

                buffer.remove(itemInBuffer);
                addBufferLogMessage(itemInBuffer, isOutputBuffer, true);
                return itemInBuffer;
            }
        }
        addItemNotInBufferLogMessage(item, isOutputBuffer);
        return null;
    }

    private boolean itemsForProductAreAvailableInInputBuffer(Product product)
    {
        /*
        var materialList = product.getBillOfMaterial();
        var copyOfIB = new ArrayList<>(this.inputBuffer);

        for (var materialInOrder : materialList)
        {
            var itemFound = false;
            for(var materialInInput : copyOfIB)
            {
                if(materialInOrder.material().getName().equals(materialInInput.getName()))
                {
                    copyOfIB.remove(materialInInput);
                    itemFound = true;
                    break;
                }
            }
            if(!itemFound)
            {
                addItemNotInBufferLogMessage(materialInOrder.material(), false);
                return false;
            }
        }

         */
        return true;
    }

    private WarehouseItem produceProduct(Product product)
    {
        /*
        if(product.getAssemblyTime() > remainingAssemblyTime)
        {
            addNotEnoughAssemblyTimeRemainingMessage(product);
            return null;
        }

        remainingAssemblyTime -= product.getAssemblyTime();
        blockedUntilTimeStep = this.getFactory().getCurrentTimeStep() + product.getAssemblyTime();
        addProduceItemMessage(product);

         */
        return product;
    }

    private void addNotEnoughAssemblyTimeRemainingMessage(Product product)
    {
        var message = super.getName() + " Task: produce " + product.getName() + " not enough time";
        super.getFactory().addLog(message);
    }

    private void addProduceItemMessage(Product product)
    {
        var message = super.getName() + " Task: produce " + product.getName() + " RT: " + this.remainingAssemblyTime;
        super.getFactory().addLog(message);
    }

    private void addNotEnoughCapacityInBufferLogMessage(boolean isOutputBuffer)
    {
        var bufferName = "InputBuffer";
        if(isOutputBuffer)
            bufferName = "OutputBuffer";

        var message = super.getName() + " Not enough capacity in " + bufferName;
        super.getFactory().addLog(message);
    }

    private void addItemNotInBufferLogMessage(WarehouseItem item, boolean isOutputBuffer)
    {
        var bufferName = "InputBuffer";
        if(isOutputBuffer)
            bufferName = "OutputBuffer";

        var message = super.getName() + " item " + item.getName() + " not in " + bufferName;
        super.getFactory().addLog(message);
    }

    private void addBufferLogMessage(WarehouseItem item, boolean isOutputBuffer, boolean isRemoveOperation)
    {
        var bufferName = "InputBuffer";
        var remCapacity = this.remainingCapacityInputBuffer;
        if(isOutputBuffer)
        {
            bufferName = "OutputBuffer";
            remCapacity = this.remainingCapacityOutputBuffer;
        }

        var operationName = "moved";
        var fromTo = "to";
        if(isRemoveOperation)
        {
            operationName = "removed";
            fromTo = "from";
        }

        var message = super.getName() + " " + operationName + " item " + item.getName() + " " + fromTo + " " + bufferName + " RC: " + remCapacity;
        super.getFactory().addLog(message);
    }

}
