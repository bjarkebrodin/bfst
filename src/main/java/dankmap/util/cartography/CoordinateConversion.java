package dankmap.util.cartography;

import dankmap.model.Bounds;

import java.util.function.DoubleUnaryOperator;

import static dankmap.util.cartography.MapConstants.getLatCenter;
import static dankmap.util.cartography.MapConstants.getLonCenter;
import static java.lang.Math.cos;
import static java.lang.Math.toRadians;

public enum CoordinateConversion {
    TO_LON(x -> (x / cos(toRadians(getLatCenter()))) + getLonCenter()),
    TO_LAT(y -> -y + getLatCenter()),
    TO_X(lon -> (lon - getLonCenter()) * cos(toRadians(getLatCenter()))),
    TO_Y(lat -> -(lat - getLatCenter()));

    private final DoubleUnaryOperator conversion;

    CoordinateConversion(DoubleUnaryOperator conversion) {
        this.conversion = conversion;
    }

    public float convert(double value) {
        return (float) conversion.applyAsDouble(value);
    }

    public double convertDouble(double value) {
        return conversion.applyAsDouble(value);
    }

    public static Bounds convertBounds(Bounds bounds) {
        float minX = TO_X.convert(bounds.getMinX());
        float minY = TO_Y.convert(bounds.getMaxY());
        float maxX = TO_X.convert(bounds.getMaxX());
        float maxY = TO_Y.convert(bounds.getMinY());
        return new Bounds(minX, minY, maxX, maxY);
    }

    public static Bounds inverseBounds(Bounds bounds) {
        float minX = TO_LON.convert(bounds.getMinX());
        float minY = TO_LAT.convert(bounds.getMaxY());
        float maxX = TO_LON.convert(bounds.getMaxX());
        float maxY = TO_LAT.convert(bounds.getMinY());
        return new Bounds(minX, minY, maxX, maxY);
    }
}
