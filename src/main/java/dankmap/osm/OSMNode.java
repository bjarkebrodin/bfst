package dankmap.osm;

import dankmap.model.XYSupplier;
import dankmap.util.collections.IDSortedArrayList;

import java.io.Serializable;

public class OSMNode implements IDSortedArrayList.IDSupplier<Long>, Serializable, XYSupplier {
    private static final long serialVersionUID = -2533514826651900906L;

    private final long id;
    private final float lon, lat;

    public OSMNode(long id, float lon, float lat) {
        this.id = id;
        this.lon = lon;
        this.lat = lat;
    }

    public float getLon() {
        return lon;
    }

    public float getLat() {
        return lat;
    }

    @Override
    public Long getID() {
        return id;
    }

    @Override
    public String toString() {
        return "OSMNode{" +
                "id=" + id +
                ", lon=" + lon +
                ", lat=" + lat +
                '}';
    }

    @Override
    public float getX() {
        return lon;
    }

    @Override
    public float getY() {
        return lat;
    }

    @Override
    public float getCenterX() {
        return lon;
    }

    @Override
    public float getCenterY() {
        return lat;
    }
}
