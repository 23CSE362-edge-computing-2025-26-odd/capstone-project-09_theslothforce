package org.example.jsprr;

public class ServiceModule {
    private final String name;
    private final double storageReq;
    private final double computeReq;
    private final double uplinkReq;
    private final double downlinkReq;

    // JSPRR: fractional allocation in LP solution
    private double allocatedFraction;

    public ServiceModule(String name, double storageReq, double computeReq,
                         double uplinkReq, double downlinkReq) {
        this.name = name;
        this.storageReq = storageReq;
        this.computeReq = computeReq;
        this.uplinkReq = uplinkReq;
        this.downlinkReq = downlinkReq;
        this.allocatedFraction = 0.0;
    }

    public String getName() { return name; }
    public double getStorageReq() { return storageReq; }
    public double getComputeReq() { return computeReq; }
    public double getUplinkReq() { return uplinkReq; }
    public double getDownlinkReq() { return downlinkReq; }

    // JSPRR fractional allocation methods
    public double getAllocatedFraction() { return allocatedFraction; }
    public void setAllocatedFraction(double fraction) { 
        this.allocatedFraction = Math.max(0, Math.min(1, fraction)); 
    }

    @Override
    public String toString() {
        return name;
    }
}
