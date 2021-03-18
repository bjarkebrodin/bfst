package dankmap.osm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OSMRelationTest {

    final OSMNode node1 = new OSMNode(1, 1f, 1f);
    final OSMNode node2 = new OSMNode(2, 2f, 2f);
    final OSMNode node3 = new OSMNode(3, 3f, 3f);
    final OSMNode node4 = new OSMNode(4, 4f, 4f);

    final OSMWay way1 = new OSMWay(1);
    final OSMWay way2 = new OSMWay(2);
    final OSMWay way3 = new OSMWay(3);
    final OSMWay way4 = new OSMWay(4);

    final OSMRelation rel1 = new OSMRelation(1, new String[0]);
    final OSMRelation rel2 = new OSMRelation(2, new String[0]);
    final OSMRelation rel3 = new OSMRelation(3, new String[0]);
    final OSMRelation rel4 = new OSMRelation(4, new String[0]);

    // isEmpty

    @Test
    public void testIsEmptyNoElements() {
        OSMRelation rel = new OSMRelation(1, new String[0]);
        assertTrue(rel.isEmpty());
    }

    @Test
    public void testIsEmptyOnlyNodes() {
        OSMRelation rel = new OSMRelation(1, new String[0]);
        rel.addNode(node1);
        assertTrue(rel.isEmpty());
    }

    @Test
    public void testIsEmptyOnlyEmptyWays() {
        OSMRelation rel = new OSMRelation(1, new String[0]);
        rel.addWay(way1);
        rel.addWay(way2);
        assertTrue(rel.isEmpty());
    }

    @Test
    public void testIsEmptyOnlyWays() {
        OSMRelation rel = new OSMRelation(1, new String[0]);
        way1.add(node1);
        way1.add(node2);
        rel.addWay(way1);
        way2.add(node3);
        way2.add(node4);
        rel.addWay(way2);
        assertFalse(rel.isEmpty());
    }

    @Test
    public void testIsEmptyOnlyEmptyRelations() {
        OSMRelation rel = new OSMRelation(1, new String[0]);
        rel.addRelation(rel1);
        rel.addRelation(rel2);
        assertTrue(rel.isEmpty());
    }

    @Test
    public void testIsEmptyOnlyRelationsWithEmptyWays() {
        OSMRelation rel = new OSMRelation(1, new String[0]);
        rel1.addWay(way1);
        rel1.addWay(way2);
        rel2.addWay(way3);
        rel2.addWay(way4);
        rel.addRelation(rel1);
        rel.addRelation(rel2);
        assertTrue(rel.isEmpty());
    }

    @Test
    public void testIsEmptyOnlyRelationsWithWays() {
        OSMRelation rel = new OSMRelation(1, new String[0]);
        way1.add(node1);
        way1.add(node2);
        way2.add(node3);
        way2.add(node4);

        rel1.addWay(way1);
        rel2.addWay(way2);

        rel.addRelation(rel1);
        rel.addRelation(rel2);
        assertFalse(rel.isEmpty());
    }

    @Test
    public void testIsEmptyWaysAndRelationsBothEmpty() {
        OSMRelation rel = new OSMRelation(1, new String[0]);
        rel.addWay(way1);
        rel.addWay(way2);
        rel.addRelation(rel1);
        rel.addRelation(rel2);

        assertTrue(rel.isEmpty());
    }

    @Test
    public void testIsEmptyWaysAndEmptyRelations() {
        OSMRelation rel = new OSMRelation(1, new String[0]);
        way1.add(node1);
        way1.add(node2);
        way2.add(node3);
        way2.add(node4);

        rel.addWay(way1);
        rel.addWay(way2);
        rel.addRelation(rel1);
        rel.addRelation(rel2);

        assertFalse(rel.isEmpty());
    }

    @Test
    public void testIsEmptyRelationsAndEmptyWays() {
        OSMRelation rel = new OSMRelation(1, new String[0]);
        way1.add(node1);
        way1.add(node2);
        rel1.addWay(way1);
        rel.addWay(way2);
        rel.addRelation(rel1);

        assertFalse(rel.isEmpty());
    }

    @Test
    public void testIsEmptyWaysAndRelations() {
        OSMRelation rel = new OSMRelation(1, new String[0]);
        way1.add(node1);
        way1.add(node2);
        rel1.addWay(way1);
        rel.addRelation(rel1);

        way2.add(node3);
        way2.add(node4);
        rel.addWay(way2);

        assertFalse(rel.isEmpty());
    }

    @Test
    public void testIsEmptySuperRelationEmpty() {
        OSMRelation rel = new OSMRelation(1, new String[0]);

        rel4.addRelation(rel1);
        rel4.addRelation(rel2);

        rel.addRelation(rel4);
        rel.addRelation(rel3);
        assertTrue(rel.isEmpty());
    }

    @Test
    public void testIsEmptySuperRelation() {
        OSMRelation rel = new OSMRelation(1, new String[0]);
        way1.add(node1);
        way1.add(node2);
        rel1.addWay(way1);

        rel4.addRelation(rel1);
        rel4.addRelation(rel2);

        rel.addRelation(rel4);
        rel.addRelation(rel3);
        assertFalse(rel.isEmpty());
    }
}
