package logistikoptimierung.Contracts;

import logistikoptimierung.Entities.Instance;

/**
 * Interface for the implementation of different data loading services. These are used to generate or load data for an
 * instance to optimize.
 */
public interface IDataService {
    /**
     * Loads the data with the data service
     * @param filename gives the file name of the contract list
     * @return an instance for the optimization and simulation
     */
    Instance loadData(String filename);
}
