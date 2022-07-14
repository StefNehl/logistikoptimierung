package logistikoptimierung.Services.EnumeratedCalculation;

import logistikoptimierung.Entities.FactoryObjects.Driver;
import logistikoptimierung.Entities.FactoryObjects.Transporter;

public record DriverPoolItem(Driver driver, Transporter transporter) {
}
