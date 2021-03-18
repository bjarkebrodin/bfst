package dankmap.model;

import dankmap.util.StringUtil;

import java.io.Serializable;

import static dankmap.util.cartography.CoordinateConversion.*;

public class Location implements XYSupplier, Serializable {
    private static final long serialVersionUID = -6518385824125537311L;

    final float x, y;

    public Location(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Location(double x, double y) {
        this.x = (float) x;
        this.y = (float) y;
    }

    public Location(XYSupplier point) {
        this.x = point.getX();
        this.y = point.getY();
    }

    public String toDMS() {
        return StringUtil.locationToDMS(x, y);
    }

    public Location toGeo() {
        return new Location(TO_LON.convert(x), TO_LAT.convert(y));
    }

    public Location toXY() {
        return new Location(TO_X.convert(x), TO_Y.convert(y));
    }


    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public float getCenterX() {
        return x;
    }

    @Override
    public float getCenterY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;
        Location location = (Location) o;
        return Float.compare(location.x, x) == 0 &&
                Float.compare(location.y, y) == 0;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = result * 31 + Float.hashCode(x);
        result = result * 31 + Float.hashCode(y);
        return result;
    }

    @Override
    public String toString() {
        return "Location{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }


}