package dankmap.navigation;

import dankmap.model.Bounds;
import dankmap.model.Location;
import dankmap.model.XYSupplier;
import dankmap.util.StringUtil;
import dankmap.util.VectorMath;
import dankmap.util.collections.tree.KDTreeMap;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static dankmap.util.cartography.MapConstants.sphericalDistance;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Double.isInfinite;
import static java.util.Arrays.fill;

public class Graph implements Serializable {
    private static final long serialVersionUID = 2587550312943675670L;

    /**
     * Edges of the graph
     */
    private class Edge implements Serializable {
        private static final long serialVersionUID = -6716360098830361977L;

        float length;
        int startIndex, endIndex;
        Road road;
        Vertex to, from;

        Edge(Road road, Vertex from, Vertex to, int startIndex, int endIndex) {
            this.road = road;
            this.to = to;
            this.from = from;
            this.startIndex = startIndex;
            this.endIndex = endIndex;

            float[] path = path();

            float length = 0;
            for (int i = 0; i < path.length - 2; i += 2) {
                length += sphericalDistance(path[i], path[i + 1], path[i + 2], path[i + 3]);
            }

            this.length = length;
        }

        float[] path() {
            return road.subPath(startIndex, endIndex);
        }

        boolean isTraversableBy(Vehicle vehicle) {
            boolean isForward = startIndex < endIndex;
            if (isForward) return road.isVehicleForward(vehicle);
            else return road.isVehicleBackward(vehicle);
        }

        float getTime() {
            return length / getSpeedLimit();
        }

        short getSpeedLimit() {
            return road.getSpeedLimit();
        }
    }

    /**
     * Vertices of the graph
     */
    private class Vertex extends Location implements Serializable {
        private static final long serialVersionUID = -177128828005209192L;

        int id;

        Vertex(XYSupplier p, int id) {
            super(p.getX(), p.getY());
            this.id = id;
        }


        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        boolean isAccessibleBy(Vehicle vehicle) {
            for (Edge edge : adjacent.get(this)) {
                if (edge.to.id > adjacent.size() || edge.from.id > adjacent.size()) {
                    return false;
                }
                if (edge.isTraversableBy(vehicle)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public float getCenterX() {
            return 0;
        }

        @Override
        public float getCenterY() {
            return 0;
        }
    }

    // The outgoing edges of each vertex
    private final KDTreeMap<Vertex, List<Edge>> adjacent;

    public Graph(Map<XYSupplier, List<GraphBuilder.Connection>> connections) {
        Map<XYSupplier, Vertex> toVertex = new HashMap<>(connections.size());
        // Keeps track of which ID to assign next vertex
        int vID = 0;
        for (var key : connections.keySet()) {
            toVertex.put(key, new Vertex(key, vID++));
        }

        Map<Vertex, List<Edge>> remapped = new HashMap<>(vID);

        for (var entry : connections.entrySet()) {
            List<Edge> adj = entry.getValue()
                    .stream()
                    .map((c) -> new Edge(c.road, toVertex.get(c.from), toVertex.get(c.to), c.startIndex, c.endIndex))
                    .collect(Collectors.toList());
            remapped.put(toVertex.get(entry.getKey()), adj);
        }

        adjacent = new KDTreeMap<>(remapped);
    }

    /**
     * @return <code>null</code>
     */
    public Route getRoute(Vehicle vehicle, XYSupplier from, XYSupplier to, boolean fastest) {
        Vertex start = nearestTraversableBy(from, vehicle);
        Vertex end = nearestTraversableBy(to, vehicle);
        return start != null && end != null ?
                new Route(vehicle, start, end, fastest) :
                null;
    }

    private Vertex nearestTraversableBy(XYSupplier start, Vehicle vehicle) {
        Collection<Vertex> vertices = adjacent.nearestKeys(start, 150);
        return vertices.stream()
                .filter(v -> v.isAccessibleBy(vehicle))
                .min(Comparator.comparingDouble(v -> sphericalDistance(start, v)))
                .orElse(null);
    }

    private class Route implements dankmap.navigation.Route, Serializable {
        private static final long serialVersionUID = -5882298756245317862L;
        public static final float CYCLING_SPEED = 15f;
        public static final float WALKING_SPEED = 5f;

        Vertex start;
        Vertex end;
        Vehicle vehicle;
        PriorityQueue<Vertex> pq;
        Edge[] edgeTo;
        double[] distTo;
        double[]  heuristic;
        boolean fastest;

        Route(Vehicle vehicle, Vertex start, Vertex end, boolean fastest) {
            this.vehicle = vehicle;
            this.start = start;
            this.end = end;
            this.fastest = fastest;
            edgeTo = new Edge[adjacent.size()];
            heuristic = new double[adjacent.size()];
            distTo = new double[adjacent.size()];

            fill(distTo, POSITIVE_INFINITY);
            fill(heuristic, POSITIVE_INFINITY);
            distTo[this.start.id] = 0f;
            pq = new PriorityQueue<>(adjacent.size(), Comparator.comparingDouble(v -> distTo[v.id] + heuristic[v.id]));
            pq.add(this.start);
            while (!pq.isEmpty()) {
                relaxVertex(pq.remove());
            }
        }

        private void relaxVertex(Vertex v) {
            if (v.id == (this.end.id)) return;
            List<Edge> edges = adjacent.get(v);
            if (edges == null) return;
            for (Edge e : edges) {
                if (e.isTraversableBy(vehicle)) {
                    relaxEdge(e);
                }
            }
        }

        /**
         * Relaxes edge based on the sum of the actual cost to the
         * end vertex and the cheapest possible cost to the endpoint
         */
        private void relaxEdge(Edge e) {
            if (e.to == null) {
                return;
            }
            double dist = sphericalDistance(e.to, end);
            double hCost = fastest ? dist / 130 : dist;
            double edgeWeight = fastest ? e.getTime() : e.length;
            boolean cheaper = distTo[e.to.id] > distTo[e.from.id] + edgeWeight;

            if (isInfinite(distTo[e.to.id]) || cheaper) {
                heuristic[e.to.id] = hCost;
                distTo[e.to.id] = distTo[e.from.id] + edgeWeight;
                edgeTo[e.to.id] = e;
                if (e.to.id == this.end.id) {
                    pq.clear();
                } else {
                    pq.add(e.to);
                }
            }
        }

        public void setVehicle(Vehicle vehicle) {
            this.vehicle = vehicle;
        }


        /**
         * @return the total distance of the route represented
         * by this <code>Route</code> object
         */
        public float getDistance() {
            List<Edge> path = getPathTo(end);
            if (path.isEmpty()) return -1f;
            return path.stream()
                    .map((e) -> e.length)
                    .reduce(Float::sum).get();
        }


        public float getTravelTime() {
            float time = 0f;

            switch (vehicle) {
                case MOTOR: {
                    for (Edge edge : getPathTo(end)) {
                        time += edge.getTime();
                    }
                    break;
                }
                case BIKE: {
                    time += getDistance() / CYCLING_SPEED;
                    break;
                }
                case PEDESTRIAN: {
                    time += getDistance() / WALKING_SPEED;
                    break;
                }
            }


            return time;
        }


        public String getDescription() {
            //TODO fix empty street names in description

            List<Edge> paths = getPathTo(end);
            boolean alternator = true;
            boolean inRoundabout = false;

            if (paths.isEmpty()) return "No route";
            if (paths.size() == 1) {
                var path = paths.get(0);
                return path.road.getStreetName() + " : " + path.length;
            }

            StringBuilder desc = new StringBuilder();

            desc.append("Total distance: ")
                    .append(StringUtil.kilometersToString(getDistance()))
                    .append("\n");
            desc.append("Estimated time: ")
                    .append(StringUtil.hoursToString(getTravelTime()))
                    .append("\n\n");


            desc.append("Follow ");

            Edge previous = paths.remove(0);
            float distance = previous.length;
            int roundaboutCounter = 0;

            String previousStreetName = "";
            String currentStreetName = "";

            while (!paths.isEmpty()) {
                Edge current = paths.remove(0);
                currentStreetName = !current.road.getStreetName().isEmpty() ? current.road.getStreetName() : "Unknown road";
                previousStreetName = !previous.road.getStreetName().isEmpty() ? previous.road.getStreetName() : "Unknown road";

                boolean sameStreetAsPrev = current.road.getStreetName().equals(previous.road.getStreetName());

                if (current.road.isRoundabout()) {

                    if (!inRoundabout) {
                        desc.append(previousStreetName);
                        inRoundabout = true;
                    }

                    List<Edge> r = adjacent.get(current.to).stream().filter(e -> e.road.isVehicleForward(vehicle) && !e.road.getStreetName().isEmpty()).collect(Collectors.toList());


                    for (var edge : r) {
                        if (edge.road.isVehicleBackward(vehicle)) {
                            roundaboutCounter++;
                        } else if (Math.abs(edge.road.getPath()[0] - current.to.getX()) <= 0.00001 && Math.abs(edge.road.getPath()[1] - current.to.getY()) <= 0.00001) {
                            roundaboutCounter++;
                        }
                    }

                    previous = current;
                    continue;
                } else if (previous.road.isRoundabout()) {
                    desc.append(" and take exit ").append(roundaboutCounter)
                            .append(" in the roundabout")
                            .append(" onto ")
                            .append(currentStreetName)
                            .append(", follow ");
                    roundaboutCounter = 0;
                    inRoundabout = false;
                    previous = current;
                    continue;
                }

                var p = previous.path();
                //short for normal vector from
                double[] nvf = new double[]{
                        -(p[p.length - 1] - p[p.length - 3]),
                        (p[p.length - 2] - p[p.length - 4])
                };
                var c = current.path();
                //short for vector to
                double[] vt = new double[]{
                        c[2] - c[0],
                        c[3] - c[1]
                };
                double cos = VectorMath.dot(nvf, vt) / (VectorMath.length(nvf) * VectorMath.length(vt));


                if (sameStreetAsPrev && Math.abs(cos) < 0.8) {
                    distance += current.length;
                } else {
                    desc.append(previousStreetName);
                    desc.append(" for ");
                    desc.append(StringUtil.kilometersToString(distance));
                    if (alternator) {
                        desc.append(" then");
                    } else {
                        desc.append(".\n\n");
                    }
                    alternator = !alternator;
                    if (sameStreetAsPrev && cos < -0.2) {
                        if (alternator) {
                            desc.append("C");
                        } else {
                            desc.append(" c");
                        }
                        desc.append("ontinue right on ");
                    } else if (sameStreetAsPrev && cos > 0.2) {
                        if (alternator) {
                            desc.append("C");
                        } else {
                            desc.append(" c");
                        }
                        desc.append("ontinue left on ");
                    } else {
                        distance = current.length;
                        if (cos < -0.2) {
                            if (alternator) {
                                desc.append("T");
                            } else {
                                desc.append(" t");
                            }
                            desc.append("urn left onto ");
                        } else if (cos > 0.2) {
                            if (alternator) {
                                desc.append("T");
                            } else {
                                desc.append(" t");
                            }
                            desc.append("urn right onto ");
                        } else {
                            if (alternator) {
                                desc.append("F");
                            } else {
                                desc.append(" f");
                            }
                            desc.append("ollow ");
                        }
                    }
                }
                previous = current;
            }
            desc.append(previousStreetName);
            desc.append(" for ");
            desc.append(StringUtil.kilometersToString(distance));
            desc.append(" to arrive at your destination at ");
            return desc.toString();
        }

        public float[][] getPaths() {
            // TODO: 20/04/2020 make iterable instead
            return getPathTo(end).stream()
                    .map((Edge::path)).toArray(float[][]::new);
        }

        private List<Edge> getPathTo(Vertex end) {
            List<Edge> path = new LinkedList<>();

            var edge = edgeTo[end.id];

            while (edge != null) {
                path.add(0, edge);
                edge = edgeTo[edge.from.id];
            }

            return path;
        }

        @Override
        public float getArea() {
            return getBounds().getArea();
        }

        @Override
        public Bounds getBounds() {
            return new Bounds(getPaths());
        }
    }

    public static class GraphBuilder implements Serializable {
        private static final long serialVersionUID = 2049163598935302541L;

        private Map<XYSupplier, List<Connection>> connections = new HashMap<>();

        public void addConnection(XYSupplier from, XYSupplier to, Road way, int startIndex, int endIndex) {
            if (from == null || to == null || way == null) {
                throw new IllegalArgumentException("null connection");
            }

            Location start = new Location(from.getX(), from.getY());
            Location end = new Location(to.getX(), to.getY());

            connections.computeIfAbsent(start, k -> new ArrayList<>());
            connections.get(start).add(new Connection(start, end, way, startIndex, endIndex));

            // We need an entry for the terminal vertex
            connections.computeIfAbsent(end, k -> new ArrayList<>());
        }

        public Graph build() {
            return new Graph(connections);
        }

        static class Connection {
            Road road;
            short startIndex, endIndex;
            XYSupplier from, to;

            private Connection(XYSupplier from, XYSupplier to, Road road, int startIndex, int endIndex) {
                this.road = road;
                this.from = from;
                this.to = to;
                this.startIndex = (short) startIndex;
                this.endIndex = (short) endIndex;
            }
        }
    }
}