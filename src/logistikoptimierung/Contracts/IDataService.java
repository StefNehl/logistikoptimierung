package logistikoptimierung.Contracts;

import logistikoptimierung.Entities.Instance;

public interface IDataService {
    Instance loadData(String filename);
}
