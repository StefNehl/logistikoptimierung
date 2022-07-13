package logistikoptimierung.Services.EnumeratedCalculation;

import logistikoptimierung.Entities.FactoryObjects.Production;

import java.util.ArrayList;
import java.util.List;

public class ProductionPlanningItem
{
    private Production production;
    private List<ProcessPlaningItem> processPlanningItems;

    public ProductionPlanningItem(Production production)
    {
        this.production = production;
        this.processPlanningItems = new ArrayList<>();
    }

    public Production getProduction() {
        return production;
    }

    public List<ProcessPlaningItem> getProcessPlanningItems() {
        return processPlanningItems;
    }
}
