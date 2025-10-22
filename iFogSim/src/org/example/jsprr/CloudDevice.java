package org.example.jsprr;

public class CloudDevice extends BaseStationDevice {
    private final double latencyMs;

    public CloudDevice(String name, double storageCapacity, double computeCapacity,
                       double uplinkCapacity, double downlinkCapacity, double latencyMs) {
        super(name, storageCapacity, computeCapacity, uplinkCapacity, downlinkCapacity);
        this.latencyMs = latencyMs;
    }

    public double getLatencyMs() { return latencyMs; }

    // Helper: checks if Cloud can host the service module (optional max latency check)
    public boolean canDeploy(ServiceModule svc) {
        return getStorageCapacity() - getStorageUsed() >= svc.getStorageReq()
            && getComputeCapacity() - getComputeUsed() >= svc.getComputeReq()
            && getUplinkCapacity() - getUplinkUsed() >= svc.getUplinkReq()
            && getDownlinkCapacity() - getDownlinkUsed() >= svc.getDownlinkReq();
    }

    public boolean isSuitableFor(ServiceModule svc, double maxAllowedLatency) {
        return getLatencyMs() <= maxAllowedLatency && canDeploy(svc);
    }
}
