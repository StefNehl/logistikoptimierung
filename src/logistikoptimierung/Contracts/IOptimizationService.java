package logistikoptimierung.Contracts;
import logistikoptimierung.Entities.FactoryObjects.FactoryStep;

import java.util.List;

/**
 * Interface for an optimization service. The instance which gets optimized are given via the constructor.
 */
public interface IOptimizationService {
    /**
     * Optimize the given instance
     * @param nrOfOrdersToOptimize nr of orders which should be optimized.
     * @return a list of factory steps for the simulation
     */
    List<FactoryStep> optimize(int nrOfOrdersToOptimize);
}
