package logistikoptimierung.Entities;

import logistikoptimierung.Entities.FactoryObjects.FactoryConglomerate;
import logistikoptimierung.Entities.WarehouseItems.Order;

import java.util.List;

/**
 * Record which stores the object for an instance to optimize
 * @param factoryConglomerate for which the optimization should be done
 * @param orderList for which the optimization should be done
 */
public record Instance (FactoryConglomerate factoryConglomerate, List<Order> orderList)
{

}
