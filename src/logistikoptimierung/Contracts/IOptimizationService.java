package logistikoptimierung.Contracts;
import logistikoptimierung.Entities.FactoryObjects.FactoryStep;

import java.util.List;

public interface IOptimizationService {
    List<FactoryStep> optimize(int nrOfOrdersToOptimize);
}
