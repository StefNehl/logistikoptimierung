package logistikoptimierung.Entities.WarehouseItems;

public class Material extends WarehouseItem {

    private final int travelTime;

    public Material(String name, int travelTime)
    {
        super(name);
        this.travelTime = travelTime;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    public int getTravelTime() {
        return travelTime;
    }
}
