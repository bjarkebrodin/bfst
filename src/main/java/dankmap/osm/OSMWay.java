package dankmap.osm;

import dankmap.util.collections.IDSortedArrayList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class OSMWay extends ArrayList<OSMNode> implements IDSortedArrayList.IDSupplier<Long>, Serializable {
    private static final long serialVersionUID = -2959420991581337037L;
    private final long id;

    public OSMWay(long id) {
        this.id = id;
    }

    public OSMWay(List<OSMNode> wayNodes, long id) {
        this.id = id;
        addAll(wayNodes);
    }

    public OSMNode first() {
        if (!isEmpty()) return get(0);
        return null;
    }

    public OSMNode last() {
        if (!isEmpty()) return get(size() - 1);
        return null;
    }

    public boolean isConnected() {
        return first() == last();
    }

    public float[] getPath() {
        float[] path = new float[size() * 2];
        for (int i = 0; i < size(); i++) {
            path[i * 2] = get(i).getLon();
            path[i * 2 + 1] = get(i).getLat();
        }
        return path;
    }

    /**
     * This method merges 2 connected OSMWays by trying all
     * 4 combinations of direction.
     * The new OSMWay is created with a random id.
     * If the 2 OSMWays aren't connected an exception is thrown.
     */
    public static OSMWay merge(OSMWay way1, OSMWay way2) {
        if (way1 == way2) return way1;
        if (way1 == null || way1.isEmpty()) return way2;
        if (way2 == null || way2.isEmpty()) return way1;
        Random r = new Random();
        var res = new OSMWay(r.nextInt());
        if (way1.first() == way2.first()) {
            res.addAll(way1);
            Collections.reverse(res);
            res.addAll(way2.subList(1, way2.size()));
        } else if (way1.first() == way2.last()) {
            res.addAll(way2);
            res.addAll(way1.subList(1, way1.size()));
        } else if (way1.last() == way2.first()) {
            res.addAll(way1);
            res.addAll(way2.subList(1, way2.size()));
        } else if (way1.last() == way2.last()) {
            var tmp = new ArrayList<>(way2);
            Collections.reverse(tmp);
            res.addAll(way1);
            res.addAll(tmp.subList(1, tmp.size()));

        } else {
            throw new IllegalArgumentException("Cannot merge unconnected OSMWays");
        }
        return res;
    }


    @Override
    public Long getID() {
        return id;
    }
}
