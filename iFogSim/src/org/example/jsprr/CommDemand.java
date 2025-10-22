package org.example.jsprr;

public class CommDemand {
    private final ServiceModule src;
    private final ServiceModule dst;
    private final double bandwidth; // bandwidth requirement
    private double allocatedFraction; // fractional allocation for LP/JSPRR

    public CommDemand(ServiceModule src, ServiceModule dst, double bandwidth) {
        this.src = src;
        this.dst = dst;
        this.bandwidth = bandwidth;
        this.allocatedFraction = 0.0;
    }

    public ServiceModule getSrc() { return src; }
    public ServiceModule getDst() { return dst; }
    public double getBandwidth() { return bandwidth; }

    public double getAllocatedFraction() { return allocatedFraction; }
    public void setAllocatedFraction(double fraction) { 
        this.allocatedFraction = Math.max(0, Math.min(1, fraction)); 
    }
}

