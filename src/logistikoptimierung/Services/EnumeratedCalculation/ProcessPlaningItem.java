package logistikoptimierung.Services.EnumeratedCalculation;

import logistikoptimierung.Entities.FactoryObjects.ProductionProcess;

/**
 * Creates a process planning item for the enumerated calculation
 */
public class ProcessPlaningItem
{
    private ProductionProcess process;
    private final int orderNr;
    private int processDepth;
    private long startTimeStamp;
    private long endTimeStamp;


    /**
     * Process planning item
     * @param process process to plan
     * @param orderNr nr of the order where the product for the process comes from
     */
    public ProcessPlaningItem(ProductionProcess process, int orderNr)
    {
        this.process = process;
        this.orderNr = orderNr;
    }

    /**
     * @return the production process
     */
    public ProductionProcess getProcess() {
        return process;
    }

    /**
     * @return the nr of the order
     */
    public int getOrderNr() {
        return orderNr;
    }

    /**
     * @return the start time stamp of the process
     */
    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    /**
     * @param startTimeStamp sets the start time stamp of the process
     */
    public void setStartTimeStamp(long startTimeStamp) {
        this.startTimeStamp = startTimeStamp;
    }

    /**
     * @return the end time step of the process
     */
    public long getEndTimeStamp() {
        return endTimeStamp;
    }

    /**
     * @param endTimeStamp sets the end time step
     */
    public void setEndTimeStamp(long endTimeStamp) {
        this.endTimeStamp = endTimeStamp;
    }

    /**
     * @return the process depth (how many production steps are needed before this process can start)
     */
    public int getProcessDepth() {
        return processDepth;
    }

    /**
     * @param value sets the process depth
     */
    public void setProcessDepth(int value){
        this.processDepth = value;
    }

    @Override
    public int hashCode()
    {
        var objectString = this.process.getProductToProduce().getName() +
                this.processDepth +
                this.startTimeStamp +
                this.endTimeStamp;

        return objectString.hashCode();
    }

    @Override
    public boolean equals(Object a)
    {
        if(!(a instanceof ProcessPlaningItem))
            return false;

        if(a.hashCode() != this.hashCode())
            return false;

        return true;
    }

    @Override
    public String toString()
    {
        return this.process.getProductToProduce().getName() +
                " Order: " +
                this.orderNr +
                " Depth: " +
                this.getProcessDepth() +
                " Time: " +
                this.getStartTimeStamp() +
                " - " +
                this.getEndTimeStamp();
    }
}
