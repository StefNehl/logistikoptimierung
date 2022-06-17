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

    public Machine(String name, int maxAmountInPutBuffer, int maxAmountOutPutBuffer, double maxAssemblyTime, Factory factory)
    {
        super(name, factory);
        outputBuffer = new ArrayList<>();
        inputBuffer = new ArrayList<>();
        this.remainingCapacityInputBuffer = maxAmountInPutBuffer;
        this.remainingCapacityOutputBuffer = maxAmountOutPutBuffer;
        this.remainingAssemblyTime = maxAssemblyTime;
    }

    //Only returns item if you use Remove from Buffer step
    @Override
    public void doWork(WarehouseItem item, int amountOfItems, String stepType)
    {
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
                            return;
                        addItemToInputBuffer((Material) itemForBuffer);
                    }
                }
            }
            case StepTypes.Produce -> produceProduct((Product) item);
            case StepTypes.MoveProductFromBufferToWarehouse ->
            {
                var product = removeOneItemFromOutputBuffer();
                if(product == null)
                    return;
                this.getFactory().getWarehouse().addItemToWarehouse(product);
            }
        }
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

    private void removeItemFromInputBuffer(WarehouseItem item)
    {
        for(var itemInBuffer : inputBuffer)
        {
            if(itemInBuffer.getName().equals(item.getName()))
            {
                remainingCapacityInputBuffer++;
                inputBuffer.remove(itemInBuffer);
                addRemovedItemFromBufferMessage(itemInBuffer, false);
                return;
            }
        }
        addItemNotInBufferMessage(item, false);
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

    private void produceProduct(Product product)
    {
        var materialList = product.getBillOfMaterial();
        var copyOfIB = new ArrayList<>(inputBuffer);

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
                return;
            }
        }

        if(product.getAssemblyTime() > remainingAssemblyTime)
        {
            addNotEnoughAssemblyTimeRemainingMessage(product);
            return;
        }

        for(var materialPosition : materialList)
        {
            for(int i = 0; i < materialPosition.amount(); i++)
            {
                removeItemFromInputBuffer(materialPosition.material());
            }
        }

        remainingAssemblyTime -= product.getAssemblyTime();
        addProduceItemMessage(product);
        addItemToOutputBuffer(product);
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
