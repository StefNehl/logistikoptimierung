package logistikoptimierung.Entities;

import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.WarehouseItems.Order;

import java.util.List;

/**
 * Record which stores the object for an instance to optimize
 * @param factory for which the optimization should be done
 * @param orderList for which the optimization should be done
 */
public record Instance (Factory factory, List<Order> orderList)
{

}
