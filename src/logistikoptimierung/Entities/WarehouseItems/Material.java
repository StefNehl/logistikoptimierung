package logistikoptimierung.Entities.WarehouseItems;

public class Material extends WarehouseItem {

    private final int travelTime;
    private final String area;
    private final String[] transportTypes;
    private final String engine;

    public Material(String materialId, String name, String area, String[] transportTypes, String engine,  int travelTime)
    {
        super(materialId, name, WarehouseItemTypes.Material);
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
