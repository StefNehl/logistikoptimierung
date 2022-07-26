package logistikoptimierung.Services.EnumeratedCalculation;

import logistikoptimierung.Entities.FactoryObjects.Production;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a production planning item which is used in the enumerated calculation.
 */
public class ProductionPlanningItem
{
    private final Production production;
    private final List<ProcessPlaningItem> processPlanningItems;

    /**
     * creates the object for the production planning item
     * @param production production to plan
     */
    public ProductionPlanningItem(Production production)
    {
        this.production = production;
        this.processPlanningItems = new ArrayList<>();
    }

    /**
     * @return the production which was planned
     */
    public Production getProduction() {
        return production;
    }

    /**
     * @return the process list of the planned production
     */
    public List<ProcessPlaningItem> getProcessPlanningItems() {
        return processPlanningItems;
    }
}
