package org.example.jsprr;

import java.util.*;

public class PlacementManager {
    private final ILPFormulation ilp = new ILPFormulation();
    private final RandomizedRounding rr = new RandomizedRounding();

    /**
     * Performs JSPRR-style placement:
     * 1. LP relaxation
     * 2. Randomized rounding
     * 3. Routing with fallback
     *
     * @param bases        list of BaseStationDevice
     * @param services     list of ServiceModule
     * @param commDemands  inter-service communication demands
     * @param topology     network topology (links & latencies)
     * @return PlacementResult mapping services to bases
     * @throws Exception if LP solving fails
     */
    public PlacementResult placeServices(List<BaseStationDevice> bases,
                                         List<ServiceModule> services,
                                         List<CommDemand> commDemands,
                                         NetworkTopology topology) throws Exception {
        // Solve LP relaxation
        double[][] fractional;
        try {
            fractional = ilp.solveLP(bases, services);
        } catch (Exception e) {
            throw new Exception("LP formulation failed: " + e.getMessage(), e);
        }

        // Optional: log fractional solution for debugging
        System.out.println("LP fractional solution:");
        for (int i = 0; i < services.size(); i++) {
            System.out.println(services.get(i).getName() + ": " + Arrays.toString(fractional[i]));
        }

        // Randomized rounding + routing
        return rr.roundWithRouting(fractional, bases, services, commDemands, topology);
    }
}

