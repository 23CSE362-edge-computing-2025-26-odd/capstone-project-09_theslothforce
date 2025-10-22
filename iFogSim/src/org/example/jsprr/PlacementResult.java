package org.example.jsprr;

import java.util.*;

public class PlacementResult {
    private final Map<ServiceModule, BaseStationDevice> assignment = new HashMap<>();

    // Thread-safe assignment
    public synchronized void assign(ServiceModule svc, BaseStationDevice base) {
        assignment.put(svc, base);
    }

    // Retrieve assigned base
    public synchronized BaseStationDevice getBase(ServiceModule svc) {
        return assignment.get(svc);
    }

    // Return unmodifiable view
    public synchronized Map<ServiceModule, BaseStationDevice> getAll() {
        return Collections.unmodifiableMap(new HashMap<>(assignment));
    }

    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        assignment.forEach((svc, base) -> sb.append(svc.getName())
                                           .append(" -> ")
                                           .append(base == null ? "null" : base.getName())
                                           .append("\n"));
        return sb.toString();
    }
}

