package logistikoptimierung.Contracts;
import logistikoptimierung.Entities.FactoryObjects.Factory;
import logistikoptimierung.Entities.FactoryStep;
import logistikoptimierung.Entities.WarehouseItems.Order;

import java.util.List;

public interface IOptimizationService {
    List<FactoryStep> optimize(List<Order> orderList, int nrOfOrdersToOptimize);
}
