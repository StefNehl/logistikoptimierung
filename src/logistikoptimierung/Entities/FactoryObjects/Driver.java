package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.Material;
import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

public class Driver extends FactoryObject
{
    private String currentTask;
    private int blockedUntilTimeStep;


    public Driver(String name, int id) {
        super(name, "D" + id, FactoryObjectTypes.Driver);
        blockedUntilTimeStep = 0;
    }

    @Override
    public boolean doWork(long currentTimeStep, WarehouseItem item, int amountOfItems, String stepType)
    {
        if(currentTimeStep < blockedUntilTimeStep)
        {
            super.addBlockMessage(super.getName(), currentTask);
            return false;
        }

        super.addLogMessage("Driving for item: " + item.getName());
        return true;
    }

    public int getBlockedUntilTimeStep() {
        return blockedUntilTimeStep;
    }

    public void setBlockedUntilTimeStep(int blockedUntilTimeStep) {
        this.blockedUntilTimeStep = blockedUntilTimeStep;
    }
}