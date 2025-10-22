package org.example.jsprr;

import java.util.*;

public class EvaluationMetrics {

    private final List<BaseStationDevice> bases;
    private final List<Link> links;
    private final PlacementResult placement;

    public EvaluationMetrics(List<BaseStationDevice> bases, List<Link> links, PlacementResult placement) {
        this.bases = bases;
        this.links = links;
        this.placement = placement;
    }

    public void printMetrics() {
        System.out.println("\n=== Evaluation Metrics ===");

        // 1. Task offloading
        long edgeCount = placement.getAll().values().stream()
                .filter(b -> b != null && !(b instanceof CloudDevice))
                .count();
        long cloudCount = placement.getAll().values().stream()
                .filter(b -> b instanceof CloudDevice)
                .count();
        long unplaced = placement.getAll().values().stream()
                .filter(Objects::isNull)
                .count();
        System.out.printf("Services placed on Edge: %d\n", edgeCount);
        System.out.printf("Services placed on Cloud: %d\n", cloudCount);
        System.out.printf("Services not placed: %d\n", unplaced);

        // 2. Resource utilization per base
        System.out.println("\nResource utilization per base station:");
        for (BaseStationDevice b : bases) {
            double sUtil = 100.0 * b.getStorageUsed() / b.getStorageCapacity();
            double cUtil = 100.0 * b.getComputeUsed() / b.getComputeCapacity();
            double uUtil = 100.0 * b.getUplinkUsed() / b.getUplinkCapacity();
            double dUtil = 100.0 * b.getDownlinkUsed() / b.getDownlinkCapacity();
            System.out.printf("%s: Storage %.1f%%, Compute %.1f%%, Uplink %.1f%%, Downlink %.1f%%\n",
                    b.getName(), sUtil, cUtil, uUtil, dUtil);
        }

        // 3. Link utilization
        System.out.println("\nLink utilization:");
        for (Link l : links) {
            double bwUtil = 100.0 * l.getUsedBandwidth() / l.getCapacity();
            System.out.printf("%s (%s-%s): %.1f%%\n",
                    l.getId(), l.getA().getName(), l.getB().getName(), bwUtil);
        }

        // 4. Cost / penalty: e.g., cloud penalty
        double cloudPenalty = cloudCount * 5.0; // matches ILP cost penalty
        System.out.printf("\nTotal cloud penalty: %.1f\n", cloudPenalty);

        // 5. Success rate
        double successRate = 100.0 * (edgeCount + cloudCount) / placement.getAll().size();
        System.out.printf("Service placement success rate: %.1f%%\n", successRate);
    }
}
