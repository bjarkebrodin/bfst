package dankmap.osm;

import java.util.List;
import java.util.regex.Pattern;

class OSMHelper {
    private static final Pattern ROAD = Pattern.compile("(motorway.*)|(trunk.*)|(primary.*)|(secondary.*)|(tertiary.*)|(unclassified.*)|(residential)|(living_street)|(pedestrian)|(track)|(road)|(cycleway)|(service)|(footway)|(steps)|(path)|(turning_loop*)");
    private static final Pattern CYCLE_WAY = Pattern.compile("(cycleway)|(residential)|(living_street)|(service)|(unclassified)|(primary)|(secondary)|(tertiary)|(track)");
    private static final Pattern TRAFFIC_WAY = Pattern.compile("(motorway.*)|(trunk.*)|(primary.*)|(secondary.*)|(tertiary.*)|(unclassified.*)|(residential)|(living_street)|(service)|(turning_loop*)");
    private static final Pattern PEDESTRIAN_WAY = Pattern.compile("(living_street)|(residential)|(pedestrian)|(track)|(service)|(unclassified)|(primary)|(secondary)|(tertiary)|(footway)|(steps)|(path)|(foot)");

    // Reusable objects
    private static final StringBuilder addressBldr = new StringBuilder();

    /**
     * Roads, streams, railway lines...
     * Non-shared first and last node.
     */
    static boolean isPath(List<String> tags, List<OSMNode> wayNodes) {
        boolean definitePath = isPath(tags);
        boolean isConnected = wayNodes.get(0).equals(wayNodes.get(wayNodes.size()-1));

        if (definitePath) return true;
        else return !isConnected;
    }

    static boolean isPath(List<String> tags) {
        if (!getValue("railway", tags).isEmpty() && !getValue("railway", tags).equals("platform")) return true;
        if (getValue("waterway", tags).equals("stream")) return true;
        return !getValue("route", tags).isEmpty();
    }

    static boolean isPoint(String k, String v) {
        boolean isHighway = v.equals("turning_circle");
        boolean isTree = v.equals("tree");
        return isTree || isHighway;
    }

    /**
     * highway=* Closed ways are used to define roundabouts and circular walks
     * junction=roundabout
     * highway=turning_circle
     * barrier=* Closed ways are used to define barriers, such as hedges and walls,
     * that go completely round a property.
     * Must not have 'area=yes'.
     * Shared first and last node.
     */
    static boolean isPolygon(List<String> tags) {
        if (!getValue("area", tags).isEmpty()) return false;
        if (getValue("highway", tags).equals("raceway")) return true;
        if (getValue("leisure", tags).equals("track")) return true;
        if (getValue("junction", tags).equals("roundabout")) return true;
        return !getValue("barrier", tags).isEmpty();
    }

    static boolean isPolygon(List<String> tags, List<OSMNode> wayNodes){
        boolean isConnected = wayNodes.get(0).equals(wayNodes.get(wayNodes.size()-1));
        if ( !isConnected ) return false;
        return isPolygon(tags);
    }

    /**
     * members > 1
     * type=multipoligon
     */
    static boolean isMultiPolygon(List<String> tags, OSMRelation rel) {
        if (getValue("type", tags).equals("multipolygon")) return true;
        return !(rel.size() == 1);
    }


    static boolean isRoad(List<String> tags) {
        String highwayTag = getValue("highway", tags);
        if (highwayTag.isEmpty() && !getValue("route",tags).equals("ferry")) return false;
        return ROAD.matcher(highwayTag).matches() ||
                getValue("route",tags).equals("ferry");
    }

    static boolean isCoastline(List<String> tags) {
        return getValue("natural", tags).equals("coastline");
    }


    static short getSpeedLimit(List<String> tags) {
        String maxSpeed = getValue("maxspeed", tags);
        int speedLimit = 0;
        try {
            speedLimit = Integer.parseInt(maxSpeed);
        } catch (NumberFormatException e) {
            switch (maxSpeed) {
                case "DK:urban":
                    speedLimit = 50;
                    break;
                case "DK:rural":
                    speedLimit = 80;
                    break;
                case "DK:motorway":
                    speedLimit = 130;
                    break;
                default:
                    switch (getValue("highway", tags)){
                        case "motorway":
                            speedLimit = 130;
                            break;
                        case "motorway_link":
                            speedLimit = 100;
                            break;
                        case "trunk":
                        case "trunk_link":
                        case "primary":
                        case "primary_link":
                        case "secondary":
                        case "secondary_link":
                        case "tertiary":
                        case "tertiary_link":
                            speedLimit = 80;
                            break;
                        default:
                            speedLimit = 50;
                            break;
                    }
                    break;
            }
        }

        if(getValue("route",tags).equals("ferry")){
            speedLimit = 30;
        }


        return (short) speedLimit;
    }

    static String getAddress(List<String> tags) {
        String street = getValue("addr:street", tags);
        if (street.isEmpty()) return "";

        String house = getValue("addr:housenumber", tags);
        String zip = getValue("addr:postcode", tags);
        String city = getValue("addr:city", tags);

        addressBldr.delete(0, addressBldr.length());
        addressBldr.append(street.toLowerCase());
        addressBldr.append(" ");
        addressBldr.append(house.toLowerCase());

        if (!zip.isEmpty()) addressBldr.append(", ").append(zip.toLowerCase());
        // if (!city.isEmpty()) match.append(" ").append(city.toLowerCase());

        return addressBldr.toString();
    }


    static boolean isTrafficWay(List<String> tags) {
        String highwayTag = getValue("highway", tags);
        return TRAFFIC_WAY.matcher(highwayTag).matches() ||
                getValue("motor_vehicle",tags).equals("yes");
    }

    static boolean isTrafficWayForward(List<String> tags) {
        return isTrafficWay(tags);
    }

    static boolean isTrafficWayBackward(List<String> tags) {
        return isTrafficWay(tags) &&
                !( getValue("oneway", tags).equals("yes")
                        || getValue("junction", tags).equals("roundabout"));
    }


    static boolean isCycleway(List<String> tags) {
        String highwayTag = getValue("highway", tags);

        String bicycleTag = getValue("bicycle", tags);
        if(!bicycleTag.isEmpty() && bicycleTag.equals("no")){
            return false;
        }


        return CYCLE_WAY.matcher(highwayTag).matches() ||
                getValue("source:maxspeed", tags).equals("DK:urban") ||
                getValue("maxspeed", tags).equals("50") ||
                getValue("bicycle", tags).equals("yes") ||
                getValue("cycleway",tags).equals("track") ||
                getValue("bicycle",tags).equals("designated");
    }

    static boolean isCyclewayForward(List<String> tags) {

        return isCycleway(tags);
    }

    static boolean isCycleBackward(List<String> tags) {
        if (getValue("junction", tags).equals("roundabout") || !isCycleway(tags))
            return false;

        if ( !getValue("oneway", tags).equals("yes"))
            return true;

        return getValue("oneway:bicycle", tags).equals("no") ||
                getValue("cycleway", tags).equals("opposite") ||
                getValue("cycleway", tags).equals("opposite_share_busway");
    }


    static boolean isPedestrianWay(List<String> tags) {
        String highwayTag = getValue("highway",tags);

        String sidewalkTag = getValue("sidewalk", tags);

        String footTag = getValue("foot",tags);
        if(!footTag.isEmpty() && footTag.equals("no")){
            return false;
        }

        return (PEDESTRIAN_WAY.matcher(highwayTag).matches() ||
                getValue("cycleway",tags).equals("track") ||
                getValue("bicycle",tags).equals("designated")||
                footTag.equals("yes") ||
                getValue("bicycle",tags).equals("yes"));
                //&& !(sidewalkTag.equals("no") || sidewalkTag.equals("none"));
        // Dansk lovgivning siger at vi må gå på vejen hvis der ikke er noget fortov eller nogen anden sti
    }



    static String getValue(String key, List<String> tags) {
        for (int i = 0; i < tags.size(); i += 2) {
            if (tags.get(i).equals(key)) return tags.get(i + 1);
        }
        return "";
    }

    public static boolean isRoundabout(List<String> tags) {
        return getValue("junction",tags).equals("roundabout");
    }
}