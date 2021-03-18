package dankmap.osm;

import dankmap.drawing.DrawType;
import dankmap.drawing.DrawTypeMap;
import dankmap.model.Bounds;
import dankmap.model.DataModel;
import dankmap.model.Location;
import dankmap.model.XYSupplier;
import dankmap.model.elements.*;
import dankmap.navigation.Graph;
import dankmap.navigation.Graph.GraphBuilder;
import dankmap.navigation.Road;
import dankmap.util.cartography.MapConstants;
import dankmap.util.collections.IDSortedArrayList;
import dankmap.util.collections.tree.LayeredKDTree;
import dankmap.util.collections.trie.RadixTree;
import javafx.util.Pair;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import static dankmap.osm.OSMHelper.*;
import static dankmap.util.cartography.CoordinateConversion.*;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * Parses .osm files and returns a <code>dankmap.model.DataModel</code> object
 */
public class OSMParser implements AutoCloseable {
    private XMLStreamReader read;
    private DrawTypeMap types;

    // Universe of input
    private Bounds bounds;

    // Map elements
    private LayeredKDTree mapElements = new LayeredKDTree();
    private LayeredKDTree roads = new LayeredKDTree();
    private Collection<IslandElement> islands = new ArrayList<>();
    private RadixTree<Location> addressLookup = new RadixTree<>();
    private Graph graph;

    // Temporary elements for construction of map elements
    private IDSortedArrayList<Long, OSMNode> nodes = new IDSortedArrayList<>();
    private IDSortedArrayList<Long, OSMWay> ways = new IDSortedArrayList<>();
    private IDSortedArrayList<Long, OSMRelation> relations = new IDSortedArrayList<>();
    private Map<OSMNode, OSMWay> nodeToCoast = new HashMap<>();
    private Map<Road, OSMWay> roadToWay = new HashMap<>();
    private Map<OSMNode, Integer> degreeOfNode = new HashMap<>();

    /**
     * @param file a .osm or .osm.zip file containing the data
     *             to be parsed.
     */
    public OSMParser(File file) throws XMLStreamException, IOException {
        read = XMLInputFactory.newFactory().createXMLStreamReader(toStream(file));
        types = DrawType.getDrawTypeMap();
    }

    public DataModel load() throws EOFException, XMLStreamException, InterruptedException {
        initializeAndValidate();
        parseBounds();// Parse bounds, set coordinate conversion offsets
        parseNodes();
        parseWays();
        parseRelations();

        if (mapElements.size() > 0)
            mapElements.build();

        if (roads.size() > 0)
            roads.build();

        return new DataModel(bounds, graph, mapElements, roads, islands, addressLookup);
    }

    private void initializeAndValidate() throws EOFException, XMLStreamException {
        if (!read.hasNext())
            throw new InputMismatchException("corrupted/invalid osm file");

        String element = nextElement();
        if (!"osm".equals(element))
            throw new InputMismatchException("corrupted/invalid osm file");

        while (!"bounds".equals(element))
            element = nextElement();
    }

    /**
     * Close this reader
     */
    @Override
    public void close() throws Exception {
        read.close();
    }

    private void parseBounds() throws XMLStreamException, EOFException {
        bounds = new Bounds(
                floatOf("minlon"),
                floatOf("minlat"),
                floatOf("maxlon"),
                floatOf("maxlat")
        );

        MapConstants.setLatCenter(bounds.getCenterY());
        MapConstants.setLonCenter(bounds.getCenterX());

        bounds = convertBounds(bounds);
    }

    private void parseNodes() throws XMLStreamException, EOFException {
        List<String> tags = new ArrayList<>();
        String k, v, address, element;

        element = nextElement();
        while (element.equals("node")) {
            tags.clear();

            long id = longOf("id");
            float lon = TO_X.convert(floatOf("lon"));
            float lat = TO_Y.convert(floatOf("lat"));

            element = nextElement();
            while (element.equals("tag")) {
                k = getAttribute("k");
                v = getAttribute("v");

                if (isPoint(k, v)) {
                    var drawType = types.get(k, v);
                    if (drawType != null)
                        mapElements.add(new PointElement(lon, lat, drawType));
                }

                tags.add(k);
                tags.add(v);
                element = nextElement();
            }

            address = getAddress(tags);
            if (!address.isEmpty()) {
                Location loc = new Location(lon, lat);
                addressLookup.put(address, loc);
            }

            nodes.add(new OSMNode(id, lon, lat));
        }

    }

    private void parseWays() throws XMLStreamException, EOFException {
        List<OSMNode> wayNodes = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        String k, v, element = element();
        DrawType type;
        long id;
        OSMNode nd;
        OSMWay way;
        float[] path;

        while (element.equals("way")) {

            // Renitialize local vars
            tags.clear();
            wayNodes.clear();
            id = longOf("id");

            // Parse <nd> elements
            element = nextElement();
            while (element.equals("nd")) {
                nd = nodes.get(longOf("ref"));
                if (nd != null) wayNodes.add(nd);

                element = nextElement();
            }

            // Parse <tag> elements
            while (element.equals("tag")) {
                k = getAttribute("k");
                v = getAttribute("v");
                tags.add(k);
                tags.add(v);

                element = nextElement();
            }

            if (wayNodes.isEmpty()) continue;

            way = new OSMWay(wayNodes, id);

            if (isCoastline(tags)) {
                addCoastline(way);
            } else if (isRoad(tags)) {
                String highwayTag = getValue("highway", tags);
                addRoad(way, tags);
            } else {
                // Extract map elements from way
                var matching_types = types.getRange(tags).keySet();
                if (!matching_types.isEmpty()) {
                    path = way.getPath();
                    for (Pair<String, String> tag : matching_types) {
                        type = types.get(tag.getKey(), tag.getValue());
                        if (type == null) continue;
                        if (isPath(tags, wayNodes)) {
                            mapElements.add(new PathElement(path, type));
                        } else if (isPolygon(tags, wayNodes)) {
                            mapElements.add(new PolygonElement(path, type));
                        } else {
                            mapElements.add(new PolygonElement(path, true, type));
                        }
                    }
                }
            }

            ways.add(way);
        }
        buildGraph();
        processIslands();
    }

    private void parseRelations() throws XMLStreamException, EOFException {
        String k, v, element;
        long id, ref;
        List<String> tags = new ArrayList<>();
        OSMRelation relation;
        DrawType type;

        element = element();
        while (element.equals("relation")) {
            tags.clear();
            id = longOf("id");
            relation = new OSMRelation(id);

            // Collect members
            element = nextElement();
            while (element.equals("member")) {
                ref = longOf("ref");
                switch (getAttribute("type")) {
                    case "node":
                        var node = nodes.get(ref);
                        if (node != null) relation.addNode(node);
                        break;
                    case "way":
                        var way = ways.get(ref);
                        if (way != null) relation.addWay(way);
                        break;
                    case "relation":
                        var rel = relations.get(ref);
                        if (rel != null) relation.addRelation(rel);
                        break;
                }

                element = nextElement();
            }

            // Parse <tag> elements
            while (element.equals("tag")) {
                k = getAttribute("k");
                v = getAttribute("v");
                tags.add(k);
                tags.add(v);

                element = nextElement();
            }

            if (relation.isEmpty()) continue;

            float[][] paths = relation.getPaths();

            type = types.get(tags);

            if (type != null) {
                if (OSMHelper.isMultiPolygon(tags, relation)) {
                    mapElements.add(new MultiPolygonElement(paths, type));
                } else if (isPath(tags)) {
                    mapElements.add(new PathElement(paths[0], type));
                } else if (isPolygon(tags)) {
                    mapElements.add(new PolygonElement(paths[0], type));
                } else {
                    mapElements.add(new PolygonElement(paths[0], true, type));
                }
            }
            relations.add(relation);
        }
    }

    private void processIslands() {
        for (var entry : nodeToCoast.entrySet()) {
            if (entry.getKey() == entry.getValue().last()) {
                islands.add(new IslandElement(entry.getValue().getPath()));
            }
        }
        nodeToCoast = null;
    }

    private void buildGraph() {
        GraphBuilder builder = new GraphBuilder();
        if (degreeOfNode.size() == 0 || roadToWay.size() == 0) builder.build();

        Set<OSMNode> junctions = degreeOfNode.entrySet()
                .stream()
                .filter((k) -> k.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        degreeOfNode.clear();

        roadToWay.forEach((road, way) -> {
            roads.add(road);
            int lastStart = -1;
            for (int i = 0; i < way.size(); i++) {
                if (lastStart == -1) lastStart = i;
                else if (way.get(i) != null && junctions.contains(way.get(i)) || i == way.size() - 1) {

                    XYSupplier start = way.get(lastStart);
                    if (start == null) start = new Location(way.get(lastStart).getLon(), way.get(lastStart).getLat());

                    XYSupplier end = way.get(i);
                    if (end == null) end = new Location(way.get(i).getLon(), way.get(i).getLat());


                    int startIndex = lastStart * 2;
                    int endIndex = i * 2;

                    builder.addConnection(start, end, road, startIndex, endIndex);

                    if (!road.isOneway())
                        builder.addConnection(end, start, road, endIndex, startIndex);

                    lastStart = i;
                }
            }
        });

        roadToWay.clear();
        graph = builder.build();
    }

    private void addRoad(OSMWay way, List<String> tags) {

        DrawType type = types.get(tags);
        if (type == null) return;

        // Count junctions for later graph building
        way.forEach((nd) -> {
            degreeOfNode.merge(nd, 1, Integer::sum);
        });

        String name = getValue("name", tags);
        short speedLimit = OSMHelper.getSpeedLimit(tags);
        boolean bicycleForward = OSMHelper.isCyclewayForward(tags);
        boolean bicycleBackward = OSMHelper.isCycleBackward(tags);
        boolean trafficForward = OSMHelper.isTrafficWayForward(tags);
        boolean trafficBackward = OSMHelper.isTrafficWayBackward(tags);
        boolean pedestrian = OSMHelper.isPedestrianWay(tags);
        boolean isRoundabout = OSMHelper.isRoundabout(tags);

        int vehicleBitsForward = 0;
        int vehicleBitsBackward = 0;

        if (isRoundabout) {
            vehicleBitsForward++;
            vehicleBitsBackward++;
        }

        vehicleBitsForward = vehicleBitsForward << 1;
        vehicleBitsBackward = vehicleBitsBackward << 1;


        // Walk = 1XX
        if (pedestrian) {
            vehicleBitsForward++;
            vehicleBitsBackward++;
        }
        vehicleBitsForward = vehicleBitsForward << 1;
        vehicleBitsBackward = vehicleBitsBackward << 1;

        // Bike = X1X
        if (bicycleForward) vehicleBitsForward++;
        if (bicycleBackward) vehicleBitsBackward++;

        vehicleBitsForward = vehicleBitsForward << 1;
        vehicleBitsBackward = vehicleBitsBackward << 1;

        // Car = XX1
        if (trafficForward) vehicleBitsForward++;
        if (trafficBackward) vehicleBitsBackward++;

        Road road = new Road(way.getPath(), type, name, speedLimit, vehicleBitsForward, vehicleBitsBackward);
        roadToWay.put(road, way);
    }

    private void addCoastline(OSMWay way) {
        var before = nodeToCoast.remove(way.first());
        if (before != null) {
            nodeToCoast.remove(before.first());
            nodeToCoast.remove(before.last());
        }
        var after = nodeToCoast.remove(way.last());
        if (after != null) {
            nodeToCoast.remove(after.first());
            nodeToCoast.remove(after.last());
        }
        OSMWay w = OSMWay.merge(OSMWay.merge(before, way), after);
        nodeToCoast.put(w.first(), w);
        nodeToCoast.put(w.last(), w);
    }

    /**
     * Creates an <code>InputStream</code> object of the passed file
     *
     * @param file any .osm or .osm.zip file
     * @throws InputMismatchException if file format is invalid
     */
    private InputStream toStream(File file) throws IOException {
        InputStream fileStream = null;
        if (file == null) throw new InputMismatchException("null file");

        switch (file.getName().substring(file.getName().lastIndexOf("."))) {
            case ".osm":
                fileStream = new FileInputStream(file);
                break;
            case ".zip":
                ZipFile zFile = new ZipFile(file);
                fileStream = zFile.getInputStream(zFile.entries().nextElement());
                break;
            default:
                throw new InputMismatchException("Unknown file extension");
        }

        return fileStream;
    }

    private String element() {
        if (read.getEventType() == END_DOCUMENT)
            return "";
        return read.getLocalName();
    }

    /**
     * @return "" if end of document
     */
    private String nextElement() throws XMLStreamException, EOFException {
        if (!read.hasNext()) throw new EOFException("unexpected end of file");
        while (read.next() != START_ELEMENT) {
            if (read.getEventType() == END_DOCUMENT)
                return "";
        }
        return read.getLocalName();
    }

    private String getAttribute(String attribute) {
        return read.getAttributeValue(null, attribute);
    }

    private float floatOf(String attribute) {
        return Float.parseFloat(getAttribute(attribute));
    }

    private long longOf(String attribute) {
        return Long.parseLong(getAttribute(attribute));
    }
}