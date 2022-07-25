package logistikoptimierung.Entities;

import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.WarehouseItems.Order;

import java.util.List;

public record Instance (Factory factory, List<Order> orderList)
{

}
