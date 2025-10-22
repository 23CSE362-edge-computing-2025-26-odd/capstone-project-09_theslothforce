package org.example.jsprr;

public class BaseStationDevice {
    private final String name;
    private final double storageCapacity;
    private final double computeCapacity;
    private final double uplinkCapacity;
    private final double downlinkCapacity;

    private double storageUsed;
    private double computeUsed;
    private double uplinkUsed;
    private double downlinkUsed;

    public BaseStationDevice(String name, double storageCapacity, double computeCapacity,
                             double uplinkCapacity, double downlinkCapacity) {
        this.name = name;
        this.storageCapacity = storageCapacity;
        this.computeCapacity = computeCapacity;
        this.uplinkCapacity = uplinkCapacity;
        this.downlinkCapacity = downlinkCapacity;
        this.storageUsed = 0;
        this.computeUsed = 0;
        this.uplinkUsed = 0;
        this.downlinkUsed = 0;
    }

    // --- Getters ---
    public String getName() { return name; }
    public double getStorageCapacity() { return storageCapacity; }
    public double getComputeCapacity() { return computeCapacity; }
    public double getUplinkCapacity() { return uplinkCapacity; }
    public double getDownlinkCapacity() { return downlinkCapacity; }

    public double getStorageUsed() { return storageUsed; }
    public double getComputeUsed() { return computeUsed; }
    public double getUplinkUsed() { return uplinkUsed; }
    public double getDownlinkUsed() { return downlinkUsed; }

    // --- Resource management ---
    public synchronized boolean deploy(ServiceModule svc) {
        if (storageUsed + svc.getStorageReq() <= storageCapacity) {
            storageUsed += svc.getStorageReq();
            return true;
        }
        return false;
    }

    public synchronized void reserveCompute(double req) {
        computeUsed += req;
    }

    public synchronized void reserveUplink(double req) {
        uplinkUsed += req;
    }

    public synchronized void reserveDownlink(double req) {
        downlinkUsed += req;
    }

    public synchronized void freeResources(ServiceModule svc) {
        storageUsed = Math.max(0, storageUsed - svc.getStorageReq());
        computeUsed = Math.max(0, computeUsed - svc.getComputeReq());
        uplinkUsed = Math.max(0, uplinkUsed - svc.getUplinkReq());
        downlinkUsed = Math.max(0, downlinkUsed - svc.getDownlinkReq());
    }

    public synchronized void resetUsage() {
        storageUsed = 0;
        computeUsed = 0;
        uplinkUsed = 0;
        downlinkUsed = 0;
    }
}
