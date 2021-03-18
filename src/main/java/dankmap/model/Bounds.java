package dankmap.model;

import java.io.Serializable;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Bounds implements Serializable {
    private static final long serialVersionUID = -3513078004958776371L;
    private float minX, minY, maxX, maxY;

    public Bounds(double minX, double minY, double maxX, double maxY) {
        this((float) minX, (float) minY, (float) maxX, (float) maxY);
    }

    public Bounds(float minX, float minY, float maxX, float maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public Bounds(float[] path) {
        if (path.length < 1) throw new IllegalArgumentException();
        float minX = path[0];
        float minY = path[1];
        float maxX = path[0];
        float maxY = path[1];

        for (int j = 2; j < path.length; j += 2) {
            minX = min(path[j], minX);
            minY = min(path[j + 1], minY);
            maxX = max(path[j], maxX);
            maxY = max(path[j + 1], maxY);
        }

        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public Bounds(float[][] paths) {
        if (paths.length < 1 || paths[0].length < 2) throw new IllegalArgumentException();
        float minX = paths[0][0];
        float minY = paths[0][1];
        float maxX = minX;
        float maxY = minY;

        for (float[] path : paths) {
            for (int j = 0; j < path.length; j += 2) {
                minX = min(path[j], minX);
                minY = min(path[j + 1], minY);
                maxX = max(path[j], maxX);
                maxY = max(path[j + 1], maxY);
            }
        }

        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public float getMinX() {
        return minX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMaxY() {
        return maxY;
    }

    public float getArea() {
        return (maxY - minY) * (maxX - minX);
    }

    public float getWidth() {
        return maxX - minX;
    }

    public float getHeight() {
        return maxY - minY;
    }

    public float getCenterX() {
        return (minX + maxX) / 2;
    }

    public float getCenterY() {
        return (minY + maxY) / 2;
    }

    public void setMinX(float minX) {
        this.minX = minX;
    }

    public void setMinY(float minY) {
        this.minY = minY;
    }

    public void setMaxX(float maxX) {
        this.maxX = maxX;
    }

    public void setMaxY(float maxY) {
        this.maxY = maxY;
    }

    public boolean intersects(Bounds other) {
        return other.maxX >= minX &&
                other.minX <= maxX &&
                other.maxY >= minY &&
                other.minY <= maxY;
    }

    public boolean contains(XYSupplier p) {
        return contains(p.getX(), p.getY());
    }

    public boolean contains(float x, float y) {
        return x >= minX &&
                x <= maxX &&
                y >= minY &&
                y <= maxY;
    }

    public boolean contains(float minX, float minY, float maxX, float maxY) {
        return minY >= this.minY
                && maxY <= this.maxY
                && minX >= this.minX
                && maxX <= this.maxX;
    }

    public boolean contains(Bounds other) {
        return contains(other.minX, other.minY, other.maxX, other.maxY);
    }


    public boolean hasNegativeArea() {
        return maxX < minX || maxY < minY;
    }


    public Bounds intersection(Bounds other) {
        return new Bounds(
                max(minX, other.getMinX()),
                max(minY, other.getMinY()),
                min(maxX, other.getMaxX()),
                min(maxY, other.getMaxY())
        );
    }

    public boolean expandToFit(Bounds other) {
        boolean hasChanged = false;

        if (minX > other.getMinX()) {
            minX = other.getMinX();
            hasChanged = true;
        }
        if (minY > other.getMinY()) {
            minY = other.getMinY();
            hasChanged = true;
        }
        if (maxX < other.getMaxX()) {
            maxX = other.getMaxX();
            hasChanged = true;
        }
        if (maxY < other.getMaxY()) {
            maxY = other.getMaxY();
            hasChanged = true;
        }

        return hasChanged;
    }

    /**
     * @return the minimal expansion of this element
     * such that it contains the specified element
     */
    public Bounds inclusion(Bounds other) {
        return new Bounds(
                min(minX, other.getMinX()),
                min(minY, other.getMinY()),
                max(maxX, other.getMaxX()),
                max(maxY, other.getMaxY())
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Bounds bounds = (Bounds) o;

        return bounds.minX == minX &&
                bounds.minY == minY &&
                bounds.maxX == maxX &&
                bounds.maxY == maxY;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = result * 31 + Float.hashCode(minX);
        result = result * 31 + Float.hashCode(minY);
        result = result * 31 + Float.hashCode(maxX);
        result = result * 31 + Float.hashCode(maxY);
        return result;
    }

    @Override
    public String toString() {
        return "Bounds{" +
                "minX=" + minX +
                ", minY=" + minY +
                ", maxX=" + maxX +
                ", maxY=" + maxY +
                '}';
    }
}