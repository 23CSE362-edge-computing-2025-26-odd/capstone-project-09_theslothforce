package org.example.jsprr;

import java.util.*;

public class NetworkTopology {
    private final List<BaseStationDevice> nodes = new ArrayList<>();
    private final List<Link> links = new ArrayList<>();
    private final Map<BaseStationDevice, List<Link>> adj = new HashMap<>();

    public synchronized void addNode(BaseStationDevice n) {
        nodes.add(n);
        adj.putIfAbsent(n, new ArrayList<>());
    }

    public synchronized void addLink(Link l) {
        links.add(l);
        adj.get(l.getA()).add(l);
        adj.get(l.getB()).add(l);
    }

    public List<BaseStationDevice> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<Link> getLinks() {
        return Collections.unmodifiableList(links);
    }

    /**
     * Returns the shortest-latency path as a list of links from src to dst.
     * Returns empty list if src == dst.
     * Returns null if no path exists.
     */
    public List<Link> shortestPath(BaseStationDevice src, BaseStationDevice dst) {
        if (src.equals(dst)) return new ArrayList<>();

        Map<BaseStationDevice, Double> dist = new HashMap<>();
        Map<BaseStationDevice, Link> prevLink = new HashMap<>();
        Set<BaseStationDevice> visited = new HashSet<>();
        PriorityQueue<Map.Entry<BaseStationDevice, Double>> pq =
                new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));

        for (BaseStationDevice n : nodes) dist.put(n, Double.POSITIVE_INFINITY);
        dist.put(src, 0.0);
        pq.add(new AbstractMap.SimpleEntry<>(src, 0.0));

        while (!pq.isEmpty()) {
            BaseStationDevice u = pq.poll().getKey();
            if (visited.contains(u)) continue;
            visited.add(u);
            if (u.equals(dst)) break;

            for (Link e : adj.getOrDefault(u, Collections.emptyList())) {
                BaseStationDevice v = e.getA().equals(u) ? e.getB() : e.getA();
                double nd = dist.get(u) + e.getLatencyMs();
                if (nd < dist.get(v)) {
                    dist.put(v, nd);
                    prevLink.put(v, e);
                    pq.add(new AbstractMap.SimpleEntry<>(v, nd));
                }
            }
        }

        if (!prevLink.containsKey(dst)) return null;

        List<Link> path = new ArrayList<>();
        BaseStationDevice cur = dst;
        while (!cur.equals(src)) {
            Link le = prevLink.get(cur);
            if (le == null) break;
            path.add(0, le);
            cur = le.getA().equals(cur) ? le.getB() : le.getA();
        }
        return path;
    }
}

