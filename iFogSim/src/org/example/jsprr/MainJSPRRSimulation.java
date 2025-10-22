package org.example.jsprr;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * JSPRR Simulation that reads all inputs from dataset CSV files.
 */
public class MainJSPRRSimulation {

    public static void main(String[] args) throws Exception {
        String datasetDir = "data/jsprr_dataset_extended"; // path where CSVs are extracted

        // ========= 1. Load Base Stations =========
        List<BaseStationDevice> bases = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(datasetDir, "bs_nodes_extended.csv"))) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                String id = v[0].trim();
                double cpu = Double.parseDouble(v[1]);
                double mem = Double.parseDouble(v[2]);
                double energy = Double.parseDouble(v[3]);
                double latency = Double.parseDouble(v[4]);
                BaseStationDevice bs = new BaseStationDevice(id, cpu, mem, energy, latency);
                bases.add(bs);
            }
        }

        // ========= 2. Load Network Topology =========
        NetworkTopology topo = new NetworkTopology();
        for (BaseStationDevice b : bases) topo.addNode(b);

        try (BufferedReader br = Files.newBufferedReader(Paths.get(datasetDir, "links.csv"))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                String id = v[0];
                String srcId = v[1];
                String dstId = v[2];
                double cap = Double.parseDouble(v[3]);
                double lat = Double.parseDouble(v[4]);
                BaseStationDevice src = findBase(bases, srcId);
                BaseStationDevice dst = findBase(bases, dstId);
                if (src != null && dst != null)
                    topo.addLink(new Link(id, src, dst, cap, lat));
            }
        }

        // ========= 3. Load Services =========
        List<ServiceModule> services = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(datasetDir, "services_extended.csv"))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                String id = v[0];
                double cpu = Double.parseDouble(v[1]);
                double mem = Double.parseDouble(v[2]);
                double cost = Double.parseDouble(v[7]); // Execution_Cost
                double latency = Double.parseDouble(v[4]); // VM_Fixed_Latency_ms
                ServiceModule svc = new ServiceModule(id, cpu, mem, cost, latency);
                services.add(svc);
            }
        }

        // ========= 4. Load User Requests (optional demonstration) =========
        // You can later extend this to model per-user demands, offloading decisions, etc.
        List<String> userLines = Files.readAllLines(Paths.get(datasetDir, "user_requests_extended.csv"));
        System.out.println("Loaded " + (userLines.size() - 1) + " user requests");

        // ========= 5. Generate simple communication demands =========
        List<CommDemand> commDemands = new ArrayList<>();
        Random rand = new Random(42);
        for (int i = 0; i < services.size() - 1; i++) {
            ServiceModule sA = services.get(i);
            ServiceModule sB = services.get(i + 1);
            commDemands.add(new CommDemand(sA, sB, rand.nextInt(50) + 10));
        }

        // ========= 6. Solve JSPRR (ILP + Rounding) =========
        ILPFormulation ilp = new ILPFormulation();
        double[][] x = ilp.solveLP(bases, services);

        System.out.println("LP fractional solution:");
        for (int i = 0; i < services.size(); i++) {
            System.out.println(services.get(i).getName() + ": " + Arrays.toString(x[i]));
        }

        RandomizedRounding rr = new RandomizedRounding();
        PlacementResult placement = rr.roundWithRouting(x, bases, services, commDemands, topo);

        // ========= 7. Print final placement =========
        System.out.println("\nFinal placement:");
        for (ServiceModule s : services) {
            BaseStationDevice b = placement.getBase(s);
            System.out.println(s.getName() + " -> " + (b != null ? b.getName() : "NOT PLACED"));
        }

        // ========= 8. Print link usage =========
        System.out.println("\nLink usages:");
        for (Link l : topo.getLinks()) {
            System.out.printf("%s (%s-%s): used %.2f / cap %.2f%n",
                    l.getId(), l.getA().getName(), l.getB().getName(),
                    l.getUsedBandwidth(), l.getCapacity());
        }
    }

    private static BaseStationDevice findBase(List<BaseStationDevice> list, String id) {
        for (BaseStationDevice b : list)
            if (b.getName().equalsIgnoreCase(id))
                return b;
        return null;
    }
}
