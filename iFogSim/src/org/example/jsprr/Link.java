package org.example.jsprr;

public class Link {
    private final String id;
    private final BaseStationDevice a;
    private final BaseStationDevice b;
    private final double capacity; // total bandwidth
    private final double latencyMs;

    private double usedBandwidth;

    public Link(String id, BaseStationDevice a, BaseStationDevice b, double capacity, double latencyMs) {
        this.id = id;
        this.a = a;
        this.b = b;
        this.capacity = capacity;
        this.latencyMs = latencyMs;
        this.usedBandwidth = 0;
    }

    public String getId() { return id; }
    public BaseStationDevice getA() { return a; }
    public BaseStationDevice getB() { return b; }
    public double getCapacity() { return capacity; }
    public double getLatencyMs() { return latencyMs; }

    public synchronized boolean canReserve(double bw) {
        return usedBandwidth + bw <= capacity;
    }

    // JSPRR-style: reserve fractional bandwidth
    public synchronized boolean reserve(double bw) {
        if (canReserve(bw)) {
            usedBandwidth += bw;
            return true;
        }
        return false;
    }

    // Free reserved bandwidth
    public synchronized void free(double bw) {
        usedBandwidth = Math.max(0, usedBandwidth - bw);
    }

    public synchronized double getUsedBandwidth() { return usedBandwidth; }

    // JSPRR helper: fraction of bandwidth available
    public synchronized double getAvailableFraction() {
        return Math.max(0, (capacity - usedBandwidth) / capacity);
    }
}


