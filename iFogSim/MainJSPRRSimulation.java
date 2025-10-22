package org.example.jsprr;

import java.io.*;
import java.util.*;

public class MainJSPRRSimulation {

    public static void main(String[] args) throws Exception {
        // ========================
        // 1. Load Base Stations and Cloud from CSV
        // ========================
        Map<String, BaseStationDevice> deviceMap = new HashMap<>();
        List<BaseStationDevice> bases = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("base_stations.csv"))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String id = parts[0];
                int storage = Integer.parseInt(parts[1]);
                int compute = Integer.parseInt(parts[2]);
                int ram = Integer.parseInt(parts[3]);
                int bw = Integer.parseInt(parts[4]);
                boolean isCloud = Boolean.parseBoolean(parts[5]);
                int cloudLatency = Integer.parseInt(parts[6]);

                BaseStationDevice dev;
                if (isCloud) {
                    dev = new CloudDevice(id, storage, compute, ram, bw, cloudLatency);
                } else {
                    dev = new BaseStationDevice(id, storage, compute, ram, bw);
                }
                bases.add(dev);
                deviceMap.put(id, dev);
            }
        }

        // ========================
        // 2. Load Network Topology
        // ========================
        NetworkTopology topo = new NetworkTopology();
        for (BaseStationDevice b : bases) topo.addNode(b);

        try (BufferedReader br = new BufferedReader(new FileReader("links.csv"))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String id = parts[0];
                BaseStationDevice from = deviceMap.get(parts[1]);
                BaseStationDevice to = deviceMap.get(parts[2]);
                double cap = Double.parseDouble(parts[3]);
                double lat = Double.parseDouble(parts[4]);
                topo.addLink(new Link(id, from, to, cap, lat));
            }
        }

        // ========================
        // 3. Load Services
        // ========================
        List<ServiceModule> services = new ArrayList<>();
        Map<String, ServiceModule> serviceMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader("services.csv"))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                ServiceModule s = new ServiceModule(parts[0],
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3]),
                        Integer.parseInt(parts[4]));
                services.add(s);
                serviceMap.put(s.getName(), s);
            }
        }

        // ========================
        // 4. Load Communication Demands
        // ========================
        List<CommDemand> commDemands = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("demands.csv"))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                ServiceModule from = serviceMap.get(parts[0]);
                ServiceModule to = serviceMap.get(parts[1]);
                int bw = Integer.parseInt(parts[2]);
                commDemands.add(new CommDemand(from, to, bw));
            }
        }

        // ========================
        // 5. Solve LP and Randomized Rounding
        // ========================
        ILPFormulation ilp = new ILPFormulation();
        double[][] x = ilp.solveLP(bases, services);

        System.out.println("LP fractional solution:");
        for (int i = 0; i < services.size(); i++) {
            System.out.println(services.get(i).getName() + ": " + Arrays.toString(x[i]));
        }

        RandomizedRounding rr = new RandomizedRounding();
        PlacementResult placement = rr.roundWithRouting(x, bases, services, commDemands, topo);

        // ========================
        // 6. Print Final Placement
        // ========================
        System.out.println("\nFinal placement:");
        for (ServiceModule s : services) {
            BaseStationDevice b = placement.getBase(s);
            System.out.println(s.getName() + " -> " + (b != null ? b.getName() : "NOT PLACED"));
        }

        // ========================
        // 7. Print Link Usage
        // ========================
        System.out.println("\nLink usages:");
        for (Link l : topo.getLinks()) {
            System.out.printf("%s (%s-%s): used %.2f / cap %.2f\n",
                    l.getId(), l.getA().getName(), l.getB().getName(),
                    l.getUsedBandwidth(), l.getCapacity());
        }

        // ========================
        // 8. Evaluation Metrics
        // ========================
        EvaluationMetrics metrics = new EvaluationMetrics(bases, topo.getLinks(), placement);
        metrics.printMetrics();
    }
}
