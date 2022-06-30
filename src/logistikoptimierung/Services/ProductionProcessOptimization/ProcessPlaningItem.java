package logistikoptimierung.Services.ProductionProcessOptimization;

import logistikoptimierung.Entities.FactoryObjects.ProductionProcess;

public class ProcessPlaningItem
{
    private ProductionProcess process;
    private int orderNr;
    private int processDepth;
    private int nrOfBatches;
    private long startTimeStamp;
    private long endTimeStamp;


    public ProcessPlaningItem(ProductionProcess process, int orderNr)
    {
        this.process = process;
        this.orderNr = orderNr;
    }

    public ProductionProcess getProcess() {
        return process;
    }

    public int getOrderNr() {
        return orderNr;
    }

    public int getNrOfBatches() {
        return nrOfBatches;
    }

    public void setNrOfBatches(int nrOfBatches) {
        this.nrOfBatches = nrOfBatches;
    }

    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    public void setStartTimeStamp(long startTimeStamp) {
        this.startTimeStamp = startTimeStamp;
    }

    public long getEndTimeStamp() {
        return endTimeStamp;
    }

    public void setEndTimeStamp(long endTimeStamp) {
        this.endTimeStamp = endTimeStamp;
    }

    public int getProcessDepth() {
        return processDepth;
    }

    public void setProcessDepth(int value){
        this.processDepth = value;
    }

    @Override
    public int hashCode()
    {
        var objectString = this.process.getProductToProduce().getName() +
                this.processDepth +
                this.nrOfBatches +
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
                " Nr of B: " +
                this.getNrOfBatches() +
                " Time: " +
                this.getStartTimeStamp() +
                " - " +
                this.getEndTimeStamp();
    }
}
