package logistikoptimierung.Services.EnumeratedCalculation;

import logistikoptimierung.Entities.FactoryObjects.Factory;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a production planning item which is used in the enumerated calculation.
 */
public class ProductionPlanningItem
{
    private final Factory factory;
    private final List<ProcessPlaningItem> processPlanningItems;

    /**
     * creates the object for the production planning item
     * @param factory production to plan
     */
    public ProductionPlanningItem(Factory factory)
    {
        this.factory = factory;
        this.processPlanningItems = new ArrayList<>();
    }

    /**
     * @return the production which was planned
     */
    public Factory getProduction() {
        return factory;
    }

    /**
     * @return the process list of the planned production
     */
    public List<ProcessPlaningItem> getProcessPlanningItems() {
        return processPlanningItems;
    }
}
