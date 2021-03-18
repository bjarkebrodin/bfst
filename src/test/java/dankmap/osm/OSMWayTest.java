package dankmap.osm;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OSMWayTest {

    final OSMNode node1 = new OSMNode(1, 1f, 1f);
    final OSMNode node2 = new OSMNode(2, 2f, 2f);
    final OSMNode node3 = new OSMNode(3, 3f, 3f);
    final OSMNode node4 = new OSMNode(4, 4f, 4f);

    @Test
    public void testMergeEquals() {
        OSMWay way = new OSMWay(1);
        assertEquals(OSMWay.merge(way, way), way);
    }

    @Test
    public void testMergeWay1IsNull() {
        OSMWay way1 = new OSMWay(1);
        way1.add(node1);
        assertEquals(OSMWay.merge(way1, null), way1);
    }

    @Test
    public void testMergeWay2IsNull() {
        OSMWay way2 = new OSMWay(1);
        way2.add(node1);
        assertEquals(OSMWay.merge(null, way2), way2);
    }

    @Test
    public void testMergeWay1IsEmpty() {
        OSMWay way1 = new OSMWay(1);
        OSMWay way2 = new OSMWay(2);
        way2.add(node1);
        assertEquals(OSMWay.merge(way1, way2), way2);
    }

    @Test
    public void testMergeWay2IsEmpty() {
        OSMWay way1 = new OSMWay(1);
        way1.add(node1);
        OSMWay way2 = new OSMWay(2);
        assertEquals(OSMWay.merge(way1, way2), way1);
    }

    @Test
    public void testMergeWay1FirstEqualsWay2First() {
        //way1 should get flipped
        OSMWay way1 = new OSMWay(1);
        way1.add(node1);
        way1.add(node2);

        OSMWay way2 = new OSMWay(2);
        way2.add(node1);
        way2.add(node3);

        var mergedWay = OSMWay.merge(way1, way2);

        assertEquals(mergedWay.get(0), node2);
        assertEquals(mergedWay.get(1), node1);
        assertEquals(mergedWay.get(2), node3);
    }

    @Test
    public void testMergeWay1FirstEqualsWay2Last() {
        OSMWay way1 = new OSMWay(1);
        way1.add(node1);
        way1.add(node2);

        OSMWay way2 = new OSMWay(2);
        way2.add(node3);
        way2.add(node1);

        var mergedWay = OSMWay.merge(way1, way2);

        assertEquals(mergedWay.get(0), node3);
        assertEquals(mergedWay.get(1), node1);
        assertEquals(mergedWay.get(2), node2);
    }

    @Test
    public void testMergeWay1LastEqualsWay2First() {
        OSMWay way1 = new OSMWay(1);
        way1.add(node1);
        way1.add(node2);

        OSMWay way2 = new OSMWay(2);
        way2.add(node2);
        way2.add(node3);

        var mergedWay = OSMWay.merge(way1, way2);

        assertEquals(mergedWay.get(0), node1);
        assertEquals(mergedWay.get(1), node2);
        assertEquals(mergedWay.get(2), node3);
    }

    @Test
    public void testMergeWay1LastEqualsWay2Last() {
        OSMWay way1 = new OSMWay(1);
        way1.add(node1);
        way1.add(node2);

        OSMWay way2 = new OSMWay(2);
        way2.add(node3);
        way2.add(node2);

        var mergedWay = OSMWay.merge(way1, way2);

        assertEquals(mergedWay.get(0), node1);
        assertEquals(mergedWay.get(1), node2);
        assertEquals(mergedWay.get(2), node3);
    }

    @Test
    public void testMergeUnconnectedWays() {
        OSMWay way1 = new OSMWay(1);
        way1.add(node1);
        way1.add(node2);
        OSMWay way2 = new OSMWay(2);
        way2.add(node3);
        way2.add(node4);
        assertThrows(IllegalArgumentException.class, () -> OSMWay.merge(way1, way2));
    }

}
