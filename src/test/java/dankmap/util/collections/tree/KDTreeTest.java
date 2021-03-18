package dankmap.util.collections.tree;

import dankmap.drawing.DrawType;
import dankmap.model.Bounds;
import dankmap.model.Location;
import dankmap.model.elements.MapElement;
import dankmap.model.elements.PointElement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This class holds specification-tests (black box) for a KDTree implementation.
 */
public class KDTreeTest {
    static final float QUERY_MARGIN = 5e-6f;

    private static KDTree avg(){
        KDTree testTree = new KDTree();
        PointElement x1 = new PointElement(1, 5, DrawType.NONE);
        PointElement x2 = new PointElement(3,5, DrawType.NONE);
        PointElement x3 = new PointElement(5,5, DrawType.NONE);
        PointElement x4 = new PointElement(2,4, DrawType.NONE);
        PointElement x5 = new PointElement(1,3,DrawType.NONE);
        PointElement x6 = new PointElement(5,3, DrawType.NONE);
        PointElement x7 = new PointElement(2,2, DrawType.NONE);
        PointElement x8 = new PointElement(4,2, DrawType.NONE);
        PointElement x9 = new PointElement(1,1, DrawType.NONE);
        PointElement x10 = new PointElement(3,1, DrawType.NONE);
        PointElement x11 = new PointElement(5,1, DrawType.NONE);
        testTree.addElement(x1);
        testTree.addElement(x2);
        testTree.addElement(x3);
        testTree.addElement(x4);
        testTree.addElement(x5);
        testTree.addElement(x6);
        testTree.addElement(x7);
        testTree.addElement(x8);
        testTree.addElement(x9);
        testTree.addElement(x10);
        testTree.addElement(x11);
        testTree.buildTree();
        return testTree;
    }
    private static KDTree tricky(){
        KDTree testTree = new KDTree();
        PointElement x1 = new PointElement(-2, 2, DrawType.NONE);
        PointElement x2 = new PointElement(0,2, DrawType.NONE);
        PointElement x3 = new PointElement(2,2, DrawType.NONE);
        PointElement x4 = new PointElement(-1,1, DrawType.NONE);
        PointElement x5 = new PointElement(1,1,DrawType.NONE);
        PointElement x6 = new PointElement(-2,0, DrawType.NONE);
        PointElement x7 = new PointElement(0,0, DrawType.NONE);
        PointElement x8 = new PointElement(2,0, DrawType.NONE);
        PointElement x9 = new PointElement(-1,-1, DrawType.NONE);
        PointElement x10 = new PointElement(1,-1, DrawType.NONE);
        PointElement x11 = new PointElement(-2,-2, DrawType.NONE);
        PointElement x12 = new PointElement(0,-2, DrawType.NONE);
        PointElement x13 = new PointElement(2,-2, DrawType.NONE);

        testTree.addElement(x1);
        testTree.addElement(x2);
        testTree.addElement(x3);
        testTree.addElement(x4);
        testTree.addElement(x5);
        testTree.addElement(x6);
        testTree.addElement(x7);
        testTree.addElement(x8);
        testTree.addElement(x9);
        testTree.addElement(x10);
        testTree.addElement(x11);
        testTree.addElement(x12);
        testTree.addElement(x13);
        testTree.buildTree();
        return testTree;
    }
    private static KDTree empty(){
        return new KDTree();
    }


    // Average range of points, kept for reuse by multiple test classes



    public static KDTree avg = avg();
    private ArrayList<MapElement> results = new ArrayList<>();
    private KDTree tricky = tricky();
    private KDTree empty = empty();



    ////////////////////////////////
    // Range search average case  //
    ////////////////////////////////

    @Test
    public void testEmptyRangeAvg() {
        assertEquals(0, avg.rangeSearch(results,new Bounds(0, 0, 0, 0)).size());
    }

    @Test
    public void testGlobalRangeAvg() {
        assertEquals(11, avg.rangeSearch(results,new Bounds(1, 1, 5, 5)).size());
    }

    @Test
     public void testAverageRangeAvg() {
        assertEquals(3, avg.rangeSearch(results,new Bounds(2, 2, 4, 4)).size());
    }



    @Test
    public void testLowerXRangeAvg() {
        assertEquals(0, avg.rangeSearch(results,new Bounds(0, 0, 1 - QUERY_MARGIN, 5)).size());
    }

    @Test
    public void testLowerYRangeAvg() {
        assertEquals(0, avg.rangeSearch(results,new Bounds(0, 0, 5, 1 - QUERY_MARGIN)).size());
    }

    @Test
    public void testUpperXRangeAvg() {
        assertEquals(0, avg.rangeSearch(results,new Bounds(5 + QUERY_MARGIN, 0, 6, 5)).size());
    }

    @Test
    public void testUpperYRangeAvg() {
        assertEquals(0, avg.rangeSearch(results,new Bounds(0, 5 + QUERY_MARGIN, 5, 6)).size());
    }


    //////////////////////////////////
    // Nearest neighbour empty/null //
    //////////////////////////////////

    @Test
    public void testNoResultsNearest() {
        assertThrows(NullPointerException.class, () ->
                empty.nearest(new Location(1, 1)));
    }

    @Test
    public void testNullQueryNearest() {
        assertThrows(NullPointerException.class, () -> {
            empty.nearest(null);
        });
    }


    ////////////////////////////////
    // Nearest neighbour average  //
    ////////////////////////////////

    @Test
    public void testAvgResultsNearestAvg() {
        assertEquals(3, avg.nearest(new Location(3, 3)).size());
    }


    ////////////////////////////////
    // Range search tricky case   //
    ////////////////////////////////

    @Test
    public void testEmptyRangeTricky() {
        assertEquals(0, tricky.rangeSearch(results,new Bounds(0, 1, 0, 1)).size());
    }

    @Test
    public void testGlobalRangeTricky() {
        assertEquals(13, tricky.rangeSearch(results,new Bounds(-2, -2, 2, 2)).size());
    }

    @Test
    public void testAverageRangeTricky() {
        assertEquals(5, tricky.rangeSearch(results,new Bounds(-1, -1, 1, 1)).size());
    }


    @Test
    public void testLowerXTricky() {
        assertEquals(0, tricky.rangeSearch(results,new Bounds(-3, -2, -2 - QUERY_MARGIN, 2)).size());
    }

    @Test
    public void testLowerYTricky() {
        assertEquals(0, tricky.rangeSearch(results,new Bounds(-2, -3, 2, -2 - QUERY_MARGIN)).size());
    }

    @Test
    public void testUpperXTricky() {
        assertEquals(0, tricky.rangeSearch(results,new Bounds(2 + QUERY_MARGIN, -2, 3, 2)).size());
    }

    @Test
    public void testUpperYTricky() {
        assertEquals(0, tricky.rangeSearch(results,new Bounds(-2, 2 + QUERY_MARGIN, 2, 3)).size());
    }

    ////////////////////////////////
    // Nearest neighbour tricky   //
    ////////////////////////////////

    @Test
    public void testAllResultsNearestTricky() {
        assertEquals(3, tricky.nearest(new Location(0, 0)).size());
    }
}