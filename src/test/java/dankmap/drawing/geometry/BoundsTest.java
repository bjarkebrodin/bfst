package dankmap.drawing.geometry;

import dankmap.model.Bounds;
import dankmap.model.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundsTest {
    Bounds test = new Bounds(0, 0, 10, 10);

    @Test
    void testGetArea() {
        assertEquals(100, test.getArea(), 1e-11);
    }

    @Test
    void intersects() {
        assertTrue(test.intersects(new Bounds(-1, -1, 0, 0)));
        assertFalse(test.intersects(new Bounds(-1, -1, -1e-11, -1e-11)));
    }

    @Test
    void intersection() {
        Bounds inter = test.intersection(new Bounds(2, 2, 7, 7));
        assertEquals(2, inter.getMinX(), 1e-11);
        assertEquals(2, inter.getMinY(), 1e-11);
        assertEquals(7, inter.getMaxX(), 1e-11);
        assertEquals(7, inter.getMaxY(), 1e-11);
    }

    @Test
    void inclusion() {
        Bounds incl = test.inclusion(new Bounds(11, 11, 12, 12));
        assertEquals(0, incl.getMinX(), 1e-11);
        assertEquals(0, incl.getMinY(), 1e-11);
        assertEquals(12, incl.getMaxX(), 1e-11);
        assertEquals(12, incl.getMaxY(), 1e-11);
    }

    @Test
    void testContains() {
        assertTrue(test.contains(new Location(0, 0)));
        assertFalse(test.contains(new Bounds(-2, -2, -1e-11, -1e-11)));
    }

    @Test
    void expandToFit() {
        test.expandToFit(new Bounds(-1, -1, 12, 12));
        assertEquals(-1, test.getMinX(), 1e-11);
        assertEquals(-1, test.getMinY(), 1e-11);
        assertEquals(12, test.getMaxX(), 1e-11);
        assertEquals(12, test.getMaxY(), 1e-11);
    }

    @Test
    void hasNegativeArea() {
        assertTrue(new Bounds(10, 10, 0, 0).hasNegativeArea());
    }

    @Test
    void testEqualsReflexive() {
        assertEquals(test, test);
    }

    @Test
    void testEqualsSymmetric() {
        Bounds a = new Bounds(0, 0, 10, 10);
        assertEquals(a, test);
        assertEquals(test, a);

        Bounds b = new Bounds(0, 0, 5, 5);
        assertNotEquals(b, test);
        assertNotEquals(test, b);
    }

    @Test
    void testEqualsConsistent() {
        for (int i = 0; i < 10; i++) {
            testEqualsReflexive();
            testEqualsSymmetric();
        }
    }

    @Test
    void getCenterX() {
        assertEquals(5, test.getCenterX(), 1e-11);
    }

    @Test
    void getCenterY() {
        assertEquals(5, test.getCenterY(), 1e-11);
    }

    @Test
    void getWidth() {
        assertEquals(10, test.getWidth(), 1e-11);
    }

    @Test
    void getHeight() {
        assertEquals(10, test.getHeight(), 1e-11);
    }
}