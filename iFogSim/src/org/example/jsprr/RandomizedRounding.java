package org.example.jsprr;

import java.util.*;

public class RandomizedRounding {
    private final Random rng = new Random();

    /**
     * Perform randomized rounding based on LP fractional assignments.
     * Also attempts routing of communication demands using RoutingManager.
     */
    public PlacementResult roundWithRouting(double[][] x,
                                            List<BaseStationDevice> bases,
                                            List<ServiceModule> services,
                                            List<CommDemand> commDemands,
                                            NetworkTopology topology) {

        int m = services.size();
        int n = bases.size();
        PlacementResult placement = new PlacementResult();

        // Track node resource usage locally before committing
        double[] sUsed = new double[n];
        double[] cUsed = new double[n];
        double[] uUsed = new double[n];
        double[] dUsed = new double[n];

        // Randomized rounding + fallback
        for (int s = 0; s < m; s++) {
            ServiceModule svc = services.get(s);
            double[] probs = Arrays.copyOf(x[s], n);

            // Normalize probabilities
            double sum = Arrays.stream(probs).sum();
            if (sum <= 0) {
                fallbackPlacement(placement, svc, bases, sUsed, cUsed, uUsed, dUsed);
                continue;
            }
            for (int i = 0; i < n; i++) probs[i] /= sum;

            boolean placed = attemptRandomizedPlacement(placement, svc, probs, bases, sUsed, cUsed, uUsed, dUsed);
            if (!placed) fallbackPlacement(placement, svc, bases, sUsed, cUsed, uUsed, dUsed);
        }

        // Commit resource reservations to BaseStationDevice objects
        for (BaseStationDevice b : bases) {
            if (b != null) b.resetUsage();
        }

        for (Map.Entry<ServiceModule, BaseStationDevice> e : placement.getAll().entrySet()) {
            ServiceModule sMod = e.getKey();
            BaseStationDevice b = e.getValue();
            if (b == null) {
                System.err.println("Warning: Service " + sMod.getName() + " was not placed.");
                continue;
            }

            // Deploy service (updates storage)
            boolean deployed = b.deploy(sMod);
            if (!deployed) {
                System.err.println("Warning: Service " + sMod.getName() + " could not be deployed on " + b.getName());
                continue;
            }

            // Reserve compute, uplink, and downlink safely
            synchronized (b) {
                b.reserveCompute(sMod.getComputeReq());
                b.reserveUplink(sMod.getUplinkReq());
                b.reserveDownlink(sMod.getDownlinkReq());
            }
        }

        // Prepare probability map for reassignment guidance
        Map<ServiceModule, double[]> probMap = new HashMap<>();
        for (int i = 0; i < services.size(); i++) {
            probMap.put(services.get(i), Arrays.copyOf(x[i], x[i].length));
        }

        // Attempt routing
        RoutingManager rm = new RoutingManager(topology);
        boolean routed = rm.routeAll(placement, commDemands, bases, probMap);
        if (!routed) System.err.println("RoutingManager: failed to route all demands.");

        return placement;
    }

    private boolean attemptRandomizedPlacement(PlacementResult placement,
                                               ServiceModule svc,
                                               double[] probs,
                                               List<BaseStationDevice> bases,
                                               double[] sUsed, double[] cUsed,
                                               double[] uUsed, double[] dUsed) {

        double[] working = Arrays.copyOf(probs, probs.length);
        for (int t = 0; t < 5; t++) {
            int chosen = sampleIndex(working);
            if (chosen < 0 || chosen >= bases.size()) break;
            if (fits(bases.get(chosen), sUsed[chosen], cUsed[chosen], uUsed[chosen], dUsed[chosen], svc)) {
                assign(placement, svc, bases.get(chosen), chosen, sUsed, cUsed, uUsed, dUsed);
                return true;
            } else {
                working[chosen] = 0;
                double rem = Arrays.stream(working).sum();
                if (rem > 0) for (int i = 0; i < working.length; i++) working[i] /= rem;
            }
        }
        return false;
    }

    private void fallbackPlacement(PlacementResult placement,
                                   ServiceModule svc,
                                   List<BaseStationDevice> bases,
                                   double[] sUsed, double[] cUsed,
                                   double[] uUsed, double[] dUsed) {
        int best = -1;
        double bestSlack = -Double.MAX_VALUE;
        for (int b = 0; b < bases.size(); b++) {
            if (!fits(bases.get(b), sUsed[b], cUsed[b], uUsed[b], dUsed[b], svc)) continue;
            double slack = (bases.get(b).getStorageCapacity() - sUsed[b] - svc.getStorageReq())
                         + (bases.get(b).getComputeCapacity() - cUsed[b] - svc.getComputeReq());
            if (slack > bestSlack) {
                bestSlack = slack;
                best = b;
            }
        }

        if (best >= 0) assign(placement, svc, bases.get(best), best, sUsed, cUsed, uUsed, dUsed);
        else placeOnCloudOrNull(placement, svc, bases, sUsed, cUsed, uUsed, dUsed);
    }

    private void assign(PlacementResult res,
                        ServiceModule svc,
                        BaseStationDevice base,
                        int idx,
                        double[] sU, double[] cU, double[] uU, double[] dU) {
        res.assign(svc, base);
        sU[idx] += svc.getStorageReq();
        cU[idx] += svc.getComputeReq();
        uU[idx] += svc.getUplinkReq();
        dU[idx] += svc.getDownlinkReq();
    }

    private boolean fits(BaseStationDevice base, double sU, double cU, double uU, double dU, ServiceModule svc) {
        return base != null
            && sU + svc.getStorageReq() <= base.getStorageCapacity()
            && cU + svc.getComputeReq() <= base.getComputeCapacity()
            && uU + svc.getUplinkReq() <= base.getUplinkCapacity()
            && dU + svc.getDownlinkReq() <= base.getDownlinkCapacity();
    }

    private int sampleIndex(double[] probs) {
        double r = rng.nextDouble();
        double acc = 0.0;
        for (int i = 0; i < probs.length; i++) {
            acc += probs[i];
            if (r <= acc) return i;
        }
        for (int i = probs.length - 1; i >= 0; i--) if (probs[i] > 0) return i;
        return -1;
    }

    private void placeOnCloudOrNull(PlacementResult placement,
                                    ServiceModule svc,
                                    List<BaseStationDevice> bases,
                                    double[] sU, double[] cU,
                                    double[] uU, double[] dU) {
        for (int b = 0; b < bases.size(); b++) {
            if (bases.get(b) instanceof CloudDevice
                && fits(bases.get(b), sU[b], cU[b], uU[b], dU[b], svc)) {
                assign(placement, svc, bases.get(b), b, sU, cU, uU, dU);
                return;
            }
        }
        placement.assign(svc, null); // unable to place
    }
}



