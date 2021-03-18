package dankmap.util.collections.tree;

import dankmap.model.Bounds;
import dankmap.model.XYSupplier;

import java.util.Collection;
import java.util.Random;

import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.Float.compare;
import static java.lang.Math.*;

abstract class KDTreeUtilities {
    static <T extends XYSupplier> Bounds intersection(Bounds b, T node, boolean vertical, boolean right) {
        if (right && vertical) {
            return new Bounds(max(node.getX(), b.getMinX()), b.getMinY(), b.getMaxX(), b.getMaxY());
        } else if (right) {
            return new Bounds(b.getMinX(), max(node.getY(), b.getMinY()), b.getMaxX(), b.getMaxY());
        } else if (vertical) {
            return new Bounds(b.getMinX(), b.getMinY(), min(b.getMaxX(), node.getX()), b.getMaxY());
        } else {
            return new Bounds(b.getMinX(), b.getMinY(), b.getMaxX(), min(b.getMaxY(), node.getY()));
        }
    }

    /**
     * Partitions elements such that all elements indexed lower than the returned integer M,
     * are less than or equal to the value indexed by M, and all greater indexed elements
     * are strictly greater than. The ordering of elements respects only the specified axis.
     *
     * @param vertical if vertical x values are compared, otherwise y
     * @return the greatest index of elements with the median value on the specified axis
     */
    static <T extends XYSupplier> int median(T[] keys, int lo, int hi, boolean vertical) {
        int left = lo, right = hi, N = hi - lo, middle = lo + N / 2;

        while (right > left) {
            int split = partition(keys, left, right, vertical);
            if (split == middle) break;
            else if (split > middle) right = split - 1;
            else if (split < middle) left = split + 1;
        }

        int offset = 0;

        while (middle + offset < hi) {
            if (eq(keys[middle + offset + 1], keys[middle], vertical)) {
                offset++;
            } else {
                break;
            }
        }

        return middle + offset;
    }

    static <T extends XYSupplier> boolean eq(T self, T other, boolean vertical) {
        if (vertical)
            return compare(self.getX(), other.getX()) == 0;
        else
            return compare(self.getY(), other.getY()) == 0;
    }

    static <T extends XYSupplier> int partition(T[] keys, int lo, int hi, boolean vertical) {
        if (lo == hi) return lo;

        var rand = new Random();
        int left = lo, right = hi + 1;
        swap(keys, lo, lo + rand.nextInt(hi - lo));

        while (true) {
            while (le(keys[++left], keys[lo], vertical)) if (left == hi) break;
            while (lt(keys[lo], keys[--right], vertical)) if (right == lo) break;
            if (left >= right) break;
            swap(keys, left, right);
        }

        swap(keys, lo, right);
        return right;
    }

    static <T extends XYSupplier> boolean le(T self, T other, boolean vertical) {
        if (vertical)
            return compare(self.getX(), other.getX()) <= 0;
        else
            return compare(self.getY(), other.getY()) <= 0;
    }


    static <T extends XYSupplier> double dist(XYSupplier point, T node) {
        if (node == null) {
            return Double.POSITIVE_INFINITY;
        }
        return sqrt(pow(node.getX() - point.getX(), 2) + pow(node.getY() - point.getY(), 2));
    }

    static <T extends XYSupplier> double sqDist(XYSupplier point, T node) {
        if (node == null) {
            return Double.POSITIVE_INFINITY;
        }
        return pow(node.getX() - point.getX(), 2) + pow(node.getY() - point.getY(), 2);
    }

    static <T extends XYSupplier> double sqDistPerpendicular(XYSupplier point, T node, boolean vertical) {
        if (node == null) {
            return Double.POSITIVE_INFINITY;
        }
        if (vertical) {
            return pow(point.getX() - node.getX(), 2);
        } else {
            return pow(point.getY() - node.getY(), 2);
        }
    }

    static <T extends XYSupplier> double distPerpendicular(XYSupplier point, T node, boolean vertical) {
        if (node == null) {
            return Double.POSITIVE_INFINITY;
        }
        if (vertical) {
            return abs(point.getX() - node.getX());
        } else {
            return abs(point.getY() - node.getY());
        }
    }

    static <T extends XYSupplier> void swap(T[] a, int from, int to) {
        T temp = a[from];
        a[from] = a[to];
        a[to] = temp;
    }

    @SuppressWarnings("unchecked")
    static <T extends XYSupplier> T[] toArray(Collection<T> elements) {
        return (T[]) elements.toArray(XYSupplier[]::new);
    }

    static <T extends XYSupplier> boolean lt(T self, T other, boolean vertical) {
        if (vertical)
            return compare(self.getX(), other.getX()) < 0;
        else
            return compare(self.getY(), other.getY()) < 0;
    }
}

