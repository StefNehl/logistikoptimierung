package logistikoptimierung.Entities;

import logistikoptimierung.Entities.FactoryObjects.FactoryConglomerate;
import logistikoptimierung.Entities.FactoryObjects.LogSettings;
import logistikoptimierung.Entities.WarehouseItems.Order;

import java.util.List;

/**
 * Class which stores the object for an instance to optimize. The instance contains the factory conglomerate, order list,
 * warehouse capacity and nr of drivers. Default values for the warehouse capacity is 1000 and for the drivers 6.
 */
public class Instance
{
    private FactoryConglomerate factoryConglomerate;
    private List<Order> orderList;
    private int warehouseCapacity = 1000;
    private int nrOfDrivers = 6;

    /**
     * Creates the instance, needs the factory conglomerate and the order list.
     * @param factoryConglomerate for which the optimization should be done
     * @param orderList for which the optimization should be done
     */
    public Instance(FactoryConglomerate factoryConglomerate, List<Order> orderList)
    {
        this.factoryConglomerate = factoryConglomerate;
        this.factoryConglomerate.setNrOfDrivers(this.nrOfDrivers);
        this.factoryConglomerate.getWarehouse().setWarehouseCapacity(this.warehouseCapacity);
        this.orderList = orderList;
    }

    /**
     * Returns a list with the available orders
     * @return list with orders
     */
    public List<Order> getOrderList() {
        return orderList;
    }

    /**
     * Returns the factory conglomerate with the simulation
     * @return the factory conglomerate object
     */
    public FactoryConglomerate getFactoryConglomerate() {
        return factoryConglomerate;
    }

    /**
     * @return the nr of drivers for the simulation
     */
    public int getNrOfDrivers() {
        return nrOfDrivers;
    }

    /**
     * Sets the nr of drivers for the simuulation
     * @param nrOfDrivers
     */
    public void setNrOfDrivers(int nrOfDrivers)
    {
        this.nrOfDrivers = nrOfDrivers;
        this.factoryConglomerate.setNrOfDrivers(nrOfDrivers);
    }

    /**
     * @return the warehouse capacity for the simulation
     */
    public int getWarehouseCapacity() {
        return warehouseCapacity;
    }

    /**
     * sets the warehouse capacity for the simulation
     * @param warehouseCapacity
     */
    public void setWarehouseCapacity(int warehouseCapacity) {
        this.warehouseCapacity = warehouseCapacity;
        this.factoryConglomerate.getWarehouse().setWarehouseCapacity(warehouseCapacity);
    }

    /**
     * Sets the messaging in the console of the factory conglomerate
     * @param logSettings log settings for the factory conglomerate
     */
    public void setLogSettings(LogSettings logSettings)
    {
        this.factoryConglomerate.setLogSettings(logSettings);
    }
}
