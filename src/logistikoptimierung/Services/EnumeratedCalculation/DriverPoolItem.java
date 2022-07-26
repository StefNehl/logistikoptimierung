package logistikoptimierung.Services.EnumeratedCalculation;

import logistikoptimierung.Entities.FactoryObjects.Driver;
import logistikoptimierung.Entities.FactoryObjects.Transporter;

/**
 * Record for the driver pool which is used in the enumerated calculation
 * @param driver driver in the pool item
 * @param transporter transporter in the pool item
 */
public record DriverPoolItem(Driver driver, Transporter transporter) {
}
