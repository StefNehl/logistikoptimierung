package logistikoptimierung.Entities;

import logistikoptimierung.Entities.FactoryObjects.Factory;

public class Instance {

    private final Factory factory;

    public Instance(Factory factory)
    {
        this.factory = factory;
    }

    public Factory getFactory() {
        return factory;
    }
}
