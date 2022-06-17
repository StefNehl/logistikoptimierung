package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.FactoryObjects.FactoryObject;
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
    private double remainingAssemblyTime;
    private int blockedUntilTimeStep;

    public Machine(String name, int maxAmountInPutBuffer, int maxAmountOutPutBuffer, double maxAssemblyTime, Factory factory)
    {
        super(name, factory);
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
            super.getFactory().addBlockLog(super.getName(), stepType);
            return false;
        }
        switch (stepType)
        {
            case StepTypes.MoveMaterialsForProductFromWarehouseToInputBuffer ->
            {
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
            }
            case StepTypes.Produce -> {
                var items = getItemsForProductFromInputBuffer((Product) item);
                if(items == null)
                {
                    super.getFactory().addLog("Not enough materials for product: " + item.getName() + " in inputBuffer");
                    return false;
                }

                produceProduct((Product) item, items);
            }
            case StepTypes.MoveProductToOutputBuffer -> addItemToOutputBuffer(item);
            case StepTypes.MoveProductFromBufferToWarehouse ->
            {
                var product = removeOneItemFromOutputBuffer();
                if(product == null)
                {
                    super.getFactory().addLog("Product " + item.getName() + " not in output buffer");
                    return false;
                }
                this.getFactory().getWarehouse().addItemToWarehouse(product);
            }
        }

        return true;
    }

    private void addItemToInputBuffer(Material item)
    {
        if(remainingCapacityInputBuffer == 0)
        {
            addNoInputCapacityMessage(false);
            return;
        }

        inputBuffer.add(item);
        remainingCapacityInputBuffer--;
        addAddItemMessage(item, false);
    }

    private void addItemToOutputBuffer(WarehouseItem item)
    {
        if(remainingCapacityOutputBuffer == 0)
        {
            addNoInputCapacityMessage(true);
            return;
        }

        outputBuffer.add(item);
        remainingCapacityOutputBuffer--;
        addAddItemMessage(item, true);
    }

    private void addAddItemMessage(WarehouseItem item, boolean isOutputBuffer)
    {
        var bufferName = "IB";
        var remainingCapacity = this.remainingCapacityInputBuffer;
        if(isOutputBuffer)
        {
            bufferName = "OB";
            remainingCapacity = this.remainingCapacityOutputBuffer;
        }

        var message = super.getName() + " Task: add item " + item.getName() + " to " + bufferName + "  RC: " + remainingCapacity;
        super.getFactory().addLog(message);
    }

    private void addNoInputCapacityMessage(boolean isOutputBuffer)
    {
        var bufferName = "IB";
        if(isOutputBuffer)
            bufferName = "OB";

        var message = super.getName() + " " + bufferName + " full";
        super.getFactory().addLog(message);
    }

    private WarehouseItem removeItemFromInputBuffer(WarehouseItem item)
    {
        for(var itemInBuffer : inputBuffer)
        {
            if(itemInBuffer.getName().equals(item.getName()))
            {
                remainingCapacityInputBuffer++;
                inputBuffer.remove(itemInBuffer);
                addRemovedItemFromBufferMessage(itemInBuffer, false);
                return itemInBuffer;
            }
        }
        addItemNotInBufferMessage(item, false);
        return null;
    }

    private WarehouseItem removeOneItemFromOutputBuffer()
    {
        if(outputBuffer.isEmpty())
        {
            addNoItemInBufferMessage(true);
            return null;
        }

        var firstItem = outputBuffer.remove(0);
        remainingCapacityOutputBuffer++;
        addRemovedItemFromBufferMessage(firstItem, true);

        return firstItem;
    }

    private void addNoItemInBufferMessage(boolean isOutputBuffer)
    {
        var bufferName = "IB";
        if(isOutputBuffer)
            bufferName = "OB";

        var message = super.getName() + " " + bufferName + " empty";
        super.getFactory().addLog(message);
    }

    private void addItemNotInBufferMessage(WarehouseItem item, boolean isOutputBuffer)
    {
        var bufferName = "IB";
        if(isOutputBuffer)
            bufferName = "OB";

        var message = super.getName() + " " + bufferName + " item " + item.getName() + " not found";
        super.getFactory().addLog(message);
    }

    private void addRemovedItemFromBufferMessage(WarehouseItem item, boolean isOutputBuffer)
    {
        var bufferName = "IB";
        if(isOutputBuffer)
            bufferName = "OB";

        var message = super.getName() + " Task: removed item " + item.getName() + " from " + bufferName + " RC: " + this.remainingCapacityInputBuffer;
        super.getFactory().addLog(message);
    }

    private List<WarehouseItem> getItemsForProductFromInputBuffer(Product product)
    {
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
                addItemNotInInputBufferMessage(materialInOrder.material(), product);
                return null;
            }
        }

        var items = new ArrayList<WarehouseItem>();
        for(var materialPosition : materialList)
        {
            for(int i = 0; i < materialPosition.amount(); i++)
            {
                items.add(removeItemFromInputBuffer(materialPosition.material()));
            }
        }
        return items;
    }

    private void produceProduct(Product product, List<WarehouseItem> items)
    {
        if(product.getAssemblyTime() > remainingAssemblyTime)
        {
            addNotEnoughAssemblyTimeRemainingMessage(product);
            return;
        }

        remainingAssemblyTime -= product.getAssemblyTime();
        blockedUntilTimeStep += this.getFactory().getCurrentTimeStep() + product.getAssemblyTime();
        addProduceItemMessage(product);
    }

    private void addItemNotInInputBufferMessage(Material material, Product product)
    {
        var message = super.getName() + " Task: produce " + product.getName() + " material " + material.getName() + " not found";
        super.getFactory().addLog(message);
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

}
