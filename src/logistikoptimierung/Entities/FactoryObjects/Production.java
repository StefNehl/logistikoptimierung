package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.List;

public class Production extends FactoryObject
{
    private final String productionType;
    private final List<ProductionProcess> productionProcesses;
    private final int nrOfInputBufferBatches;
    private final int nrOfOutputBufferBatches;

    public Production(String name, String productionType,
                      List<ProductionProcess> productionProcesses,
                      int nrOfInputBufferBatches,
                      int nrOfOutputBufferBatches)
    {
        super(name);
        this.productionProcesses = productionProcesses;
        this.productionType = productionType;
        this.nrOfInputBufferBatches = nrOfInputBufferBatches;
        this.nrOfOutputBufferBatches = nrOfOutputBufferBatches;
    }

    @Override
    public boolean doWork(int currentTimeStep, WarehouseItem item, int amountOfItems, String stepType)
    {
        return false;
    }
}
