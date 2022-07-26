package logistikoptimierung.Entities.WarehouseItems;

public class Material extends WarehouseItem {

    private final int travelTime;
    private final String area;
    private final String[] transportTypes;
    private final String engine;

    /**
     * creates a material object
     * @param materialId sets material id
     * @param name sets the name of the material
     * @param area sets the area for transportation
     * @param transportTypes sets the transport types for transportation
     * @param engine sets the engine for transportation
     * @param travelTime sets the travel time for transportation
     */
    public Material(String materialId, String name, String area, String[] transportTypes, String engine,  int travelTime)
    {
        super(materialId, name, WarehouseItemType.Material);
        this.travelTime = travelTime;
        this.area = area;
        this.transportTypes = transportTypes;
        this.engine = engine;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    public int getTravelTime() {
        return travelTime;
    }

    public String getEngine() {
        return engine;
    }

    public String getArea() {
        return area;
    }

    public String[] getTransportTypes() {
        return transportTypes;
    }


}
