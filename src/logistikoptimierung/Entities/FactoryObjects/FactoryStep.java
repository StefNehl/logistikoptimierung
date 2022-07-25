package logistikoptimierung.Entities.FactoryObjects;

import logistikoptimierung.Entities.WarehouseItems.WarehouseItem;

import java.util.ArrayList;
import java.util.List;

public class FactoryStep {

    private WarehouseItem itemToManipulate;
    private FactoryObject factoryObject;
    private FactoryStepTypes stepType;
    private Factory factory;
    private int amountOfItems;
    private long doTimeStep;
    private List<FactoryStep> factoryStepsToDoBefore;
    private boolean isCompleted;

    /**
     * Creates a factory step object which should be performed at a specific time step
     * @param factory factory where the step should be performed
     * @param doTimeStep the specific time step when the step should be performed
     * @param itemToManipulate the item which should be manipulated
     * @param amountOfItems the amount of items which should be manipulated
     * @param factoryObject the factory object which should perform the manipulation
     * @param stepType the type of manipulation
     */
    public FactoryStep(Factory factory, long doTimeStep, WarehouseItem itemToManipulate, int amountOfItems, FactoryObject factoryObject, FactoryStepTypes stepType)
    {
        initFactoryStep(factory, doTimeStep, new ArrayList<>(), itemToManipulate, amountOfItems, factoryObject, stepType);
    }

    /**
     * Creates a factory step object which should be performed after a list of factory steps
     * @param factory factory where the step should be performed
     * @param factoryStepsToDoBefore the factory steps which needed to be completed before this step can perform
     * @param itemToManipulate the item which should be manipulated
     * @param amountOfItems the amount of items which should be manipulated
     * @param factoryObject the factory object which should perform the manipulation
     * @param stepType the type of manipulation
     */
    public FactoryStep(Factory factory, List<FactoryStep> factoryStepsToDoBefore, WarehouseItem itemToManipulate,
                       int amountOfItems, FactoryObject factoryObject, FactoryStepTypes stepType)
    {
        var doTimeStamp = 0;
        initFactoryStep(factory, doTimeStamp, factoryStepsToDoBefore, itemToManipulate, amountOfItems, factoryObject, stepType);
    }

    private void initFactoryStep(Factory factory, long doTimeStamp, List<FactoryStep> factoryStepsToDoBefore, WarehouseItem itemToManipulate, int amountOfItems, FactoryObject factoryObject, FactoryStepTypes stepType)
    {
        this.itemToManipulate = itemToManipulate;
        this.factoryObject = factoryObject;

        this.stepType = stepType;
        this.factory = factory;
        this.amountOfItems = amountOfItems;
        this.doTimeStep = doTimeStamp;
        this.factoryStepsToDoBefore = factoryStepsToDoBefore;
    }

    /**
     * The actual "do" of the step
     * @return true if the step was completed, false if not
     */
    public boolean doStep()
    {
        this.isCompleted = factoryObject.doWork(factory.getCurrentTimeStep(), itemToManipulate, amountOfItems, stepType);
        addStepMessage(this.isCompleted);
        return this.isCompleted;
    }

    /**
     * @return true if the step is completed, false if not
     */
    public boolean isStepCompleted()
    {
        return this.isCompleted;
    }

    /**
     * @return a list with steps which needs to be done, before this step can be done
     */
    public List<FactoryStep> getFactoryStepsToDoBefore()
    {
        return this.factoryStepsToDoBefore;
    }

    /**
     * @return true if every step which should be done before this step are completed
     */
    public boolean areAllStepsBeforeCompleted()
    {
        if(this.factoryStepsToDoBefore == null || this.factoryStepsToDoBefore.isEmpty())
            return true;

        for(var factoryStep : this.factoryStepsToDoBefore)
        {
            if(!factoryStep.isStepCompleted())
                return false;
        }

        return true;
    }

    /**
     * get the time step when the factory step should be performed
     * @return time step
     */
    public long getDoTimeStep() {
        return doTimeStep;
    }

    /**
     * @return the factory where the time step should be performed
     */
    public FactoryObject getFactoryObject() {
        return factoryObject;
    }

    /**
     * @return gets the warehouse item which should be manipulated
     */
    public WarehouseItem getItemToManipulate()
    {
        return this.itemToManipulate;
    }

    /**
     * @return gets the type of manipulation
     */
    public FactoryStepTypes getStepType()
    {
        return this.stepType;
    }

    /**
     * @return gets the amount of times
     */
    public int getAmountOfItems()
    {
        return this.amountOfItems;
    }

    private void addStepMessage(boolean completed)
    {
        this.factory.addFactoryStepLog(this + " Completed: " + completed, FactoryObjectMessageTypes.FactoryStep, completed);
    }

    public boolean compareTo(FactoryStep step){
        if(this.itemToManipulate.getItemId().equals(step.itemToManipulate.getItemId())){
            if(this.getStepType().equals(step.getStepType())){
                if(this.getAmountOfItems()==(step.getAmountOfItems())){
                    if(this.factoryObject.getName().equals(step.getFactoryObject().getName()))
                        if(this.getDoTimeStep() == step.getDoTimeStep())
                            return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString()
    {
        return this.doTimeStep + ": Item: " + this.itemToManipulate.getName() + " Amount " + this.amountOfItems + " FO: " + this.factoryObject.getName() + " Step: " + this.stepType;
    }
}
