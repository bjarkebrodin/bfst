package dankmap.model;

import dankmap.drawing.ZoomLevel;
import dankmap.model.elements.IslandElement;
import dankmap.model.elements.MapElement;
import dankmap.navigation.Graph;
import dankmap.navigation.Road;
import dankmap.navigation.Route;
import dankmap.navigation.Vehicle;
import dankmap.util.VectorMath;
import dankmap.util.cartography.MapConstants;
import dankmap.util.collections.tree.LayeredKDTree;
import dankmap.util.collections.trie.RadixTree;
import javafx.geometry.Point2D;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class DataModel implements Serializable {
    private static final long serialVersionUID = -7162776117678391033L;

    private final Bounds bounds;
    private final Graph graph;
    private final LayeredKDTree mapElements;
    private final LayeredKDTree roadElements;
    private final Collection<IslandElement> islands;
    private final List<PointOfInterest> pointOfInterests;
    private final RadixTree<Location> addressRegistry;

    private transient List<Runnable> onDataUpdateListeners;


    public DataModel(Bounds bounds, Graph graph, LayeredKDTree mapElements, LayeredKDTree roadElements, Collection<IslandElement> islands, RadixTree<Location> addressRegistry) {
        this.bounds = bounds;
        this.graph = graph;
        this.mapElements = mapElements;
        this.roadElements = roadElements;
        this.islands = islands;
        this.addressRegistry = addressRegistry;
        this.pointOfInterests = new ArrayList<>();
        onDataUpdateListeners = new ArrayList<>();
    }


    /////////// Setters //////////

    public void addPointOfInterest(PointOfInterest poi) {
        pointOfInterests.add(poi);
        notifyOnDataModelUpdateListeners();
    }

    public void removePointOfInterest(PointOfInterest poi) {
        pointOfInterests.remove(poi);
        notifyOnDataModelUpdateListeners();
    }

    public void removePointOfInterest(Address address) {
        PointOfInterest poi = null;
        for (PointOfInterest p : pointOfInterests) {
            if (p.getAddress().equals(address.getAddress())) {
                poi = p;
            }
        }
        if (poi != null) {
            pointOfInterests.remove(poi);
            notifyOnDataModelUpdateListeners();
        }
    }

    public void clearPointsOfInterest() {
        pointOfInterests.clear();
        notifyOnDataModelUpdateListeners();
    }


    /////////// Getters //////////

    public List<PointOfInterest> getPointsOfInterest() {
        return pointOfInterests;
    }

    public Bounds getBounds() {
        return bounds;
    }

    public Collection<IslandElement> getIslands() {
        return islands;
    }

    public Collection<MapElement> getRoads(Bounds outerBounds) {
        double c = ZoomLevel.getCurrent().getPanBuffer();
        Bounds preventLoss = new Bounds(outerBounds.getMinX() - c, outerBounds.getMinY() - c, outerBounds.getMaxX() + c, outerBounds.getMaxY() + c);
        return roadElements.rangeSearch(ZoomLevel.getCurrent(), preventLoss);
    }


    public Collection<MapElement> getMapElements(Bounds outerBounds) {
        return mapElements.rangeSearch(ZoomLevel.getCurrent(), outerBounds);
    }

    public Collection<String> getAddressMatches(String prefix) {
        return addressRegistry.searchPrefix(prefix);
    }

    public Address getAddressMatch(String prefix) {
        var matches = ((List<String>) addressRegistry.searchPrefix(prefix.strip().toLowerCase()));
        if (matches.isEmpty()) return null;
        return getAddress(matches.get(0));
    }

    public Route getRoute(Vehicle vehicle, XYSupplier from, XYSupplier to, boolean fastest) {
        return graph.getRoute(vehicle, from, to, fastest);
    }


    public Road getNearestRoad(XYSupplier point) {
        ArrayList<MapElement> possible = new ArrayList<>();
        roadElements.nearest(possible, point);
        possible.removeIf(e -> ((Road) e).getStreetName().isEmpty());
        Road nearest = (Road) possible.get(0);
        double bestDist = Double.POSITIVE_INFINITY;
        for (MapElement element : possible) {
            Road road = (Road) element;
            float[] path = road.getPath();
                for (int i = 0; i < path.length - 2; i += 2) {
                        Point2D centerPoint = new Point2D((path[i] + path[i + 2]) / 2, (path[i + 1] + path[i + 3]) / 2);
                        if (VectorMath.sqDist(centerPoint.getX(), centerPoint.getY(), point.getX(), point.getY()) < bestDist) {
                            bestDist = VectorMath.sqDist(centerPoint.getX(), centerPoint.getY(), point.getX(), point.getY());
                            nearest = road;
                        }

                    if (VectorMath.sqDist(path[i], path[i + 1], point.getX(), point.getY()) < bestDist) {
                        bestDist = VectorMath.sqDist(path[i], path[i + 1], point.getX(), point.getY());
                        nearest = road;
                    }

                }


        }
        return nearest;
    }

    public Address getAddress(String address) {
        Location loc = addressRegistry.get(address.strip().toLowerCase());
        if (loc == null) return null;
        return new Address(loc, address);
    }

    public Address getNearestAddress(Location location) {
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(address ->
                MapConstants.sphericalDistance(addressRegistry.get(address), location)
        ));

        var possible = addressRegistry.searchPrefix((getNearestRoad(location)).getStreetName().toLowerCase());
        pq.addAll(possible);

        if (pq.isEmpty()) return null;
        return getAddress(pq.remove());
    }


    /////////// Observers //////////

    public void addOnDataUpdateListener(Runnable listener) {
        onDataUpdateListeners.add(listener);
    }

    private void notifyOnDataModelUpdateListeners() {
        onDataUpdateListeners.forEach(Runnable::run);
    }


    /////////// Misc //////////

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        onDataUpdateListeners = new ArrayList<>();
    }
}