package org.example.jsprr;

import java.util.*;

public class RoutingManager {

    private final NetworkTopology topology;

    public RoutingManager(NetworkTopology topology) {
        this.topology = topology;
    }

    /**
     * Route all communication demands given a placement.
     * Uses probabilities from LP fractional solution to guide reassignment if path reservation fails.
     *
     * @param placement   Current placement mapping ServiceModule -> BaseStationDevice
     * @param demands     List of communication demands
     * @param bases       List of all base stations
     * @param probabilities Map of ServiceModule -> double[] of LP fractional allocations
     * @return true if all demands routed successfully
     */
    public boolean routeAll(PlacementResult placement,
                            List<CommDemand> demands,
                            List<BaseStationDevice> bases,
                            Map<ServiceModule, double[]> probabilities) {

        for (CommDemand d : demands) {
            ServiceModule s = d.getSrc();
            ServiceModule t = d.getDst();
            BaseStationDevice bs = placement.getBase(s);
            BaseStationDevice bt = placement.getBase(t);

            if (bs == null || bt == null) return false; // some service not placed

            if (bs.equals(bt)) continue; // intra-node communication, no link needed

            List<Link> path = topology.shortestPath(bs, bt);
            if (path != null && canReservePath(path, d.getBandwidth())) {
                reservePath(path, d.getBandwidth());
                continue;
            }

            // attempt reassignment guided by LP probabilities
            boolean routed = tryReassignAndRoute(s, t, d.getBandwidth(), placement, bases, probabilities);
            if (!routed) return false; // routing failed
        }

        return true;
    }

    private boolean canReservePath(List<Link> path, double bw) {
        for (Link l : path) {
            if (!l.canReserve(bw)) return false;
        }
        return true;
    }

    private void reservePath(List<Link> path, double bw) {
        for (Link l : path) {
            boolean ok = l.reserve(bw);
            if (!ok) {
                // rollback in case of unexpected failure
                for (Link r : path) {
                    if (r.getUsedBandwidth() >= bw) r.free(bw);
                }
                break;
            }
        }
    }

    private boolean tryReassignAndRoute(ServiceModule s, ServiceModule t, double bw,
                                        PlacementResult placement, List<BaseStationDevice> bases,
                                        Map<ServiceModule, double[]> probabilities) {
        // Try reassign source
        for (BaseStationDevice candidate : orderBasesByProb(bases, probabilities.get(s))) {
            if (candidate.equals(placement.getBase(s))) continue;
            if (!fitsNode(candidate, s)) continue;
            BaseStationDevice old = placement.getBase(s);
            placement.assign(s, candidate);

            List<Link> path = topology.shortestPath(candidate, placement.getBase(t));
            if (path != null && canReservePath(path, bw)) {
                reservePath(path, bw);
                return true;
            }
            placement.assign(s, old); // revert
        }

        // Try reassign destination
        for (BaseStationDevice candidate : orderBasesByProb(bases, probabilities.get(t))) {
            if (candidate.equals(placement.getBase(t))) continue;
            if (!fitsNode(candidate, t)) continue;
            BaseStationDevice old = placement.getBase(t);
            placement.assign(t, candidate);

            List<Link> path = topology.shortestPath(placement.getBase(s), candidate);
            if (path != null && canReservePath(path, bw)) {
                reservePath(path, bw);
                return true;
            }
            placement.assign(t, old); // revert
        }

        return false;
    }

    private boolean fitsNode(BaseStationDevice base, ServiceModule svc) {
        // check capacity slack
        return (base.getStorageUsed() + svc.getStorageReq() <= base.getStorageCapacity()) &&
               (base.getComputeUsed() + svc.getComputeReq() <= base.getComputeCapacity()) &&
               (base.getUplinkUsed() + svc.getUplinkReq() <= base.getUplinkCapacity()) &&
               (base.getDownlinkUsed() + svc.getDownlinkReq() <= base.getDownlinkCapacity());
    }

    private List<BaseStationDevice> orderBasesByProb(List<BaseStationDevice> bases, double[] probs) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < bases.size(); i++) idx.add(i);
        idx.sort((i,j) -> Double.compare(probs[j], probs[i])); // descending
        List<BaseStationDevice> ordered = new ArrayList<>();
        for (int i : idx) ordered.add(bases.get(i));
        return ordered;
    }
}

