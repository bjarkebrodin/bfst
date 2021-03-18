package dankmap.osm;

import dankmap.util.collections.IDSortedArrayList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OSMRelation implements IDSortedArrayList.IDSupplier<Long>, Serializable {
    private static final long serialVersionUID = 3368422161402901797L;

    private final List<OSMNode> nodes = new ArrayList<>();
    private final List<OSMWay> ways = new ArrayList<>();
    private final List<OSMRelation> relations = new ArrayList<>();
    private final long id;
    private String[] tags;

    public OSMRelation(long id, String[] tags) {
        this.id = id;
        this.tags = tags;
    }

    public OSMRelation(long id) {
        this.id = id;
    }

    public String[] getTags() {
        return tags;
    }

    /**
     * A Relation's ways aren't always in correct order nor direction.
     * To make sure it is, we try to merge each way to every other way
     * - creating a series of correctly oriented and connected ways.
     */
    public static List<OSMWay> mergeWays(List<OSMWay> ways) {
        ArrayList<OSMWay> temp = new ArrayList<>(ways.size());
        for (OSMWay way : ways) temp.add((OSMWay) way.clone());

        List<OSMWay> newWays = new ArrayList<>();
        if (!temp.isEmpty()) {
            int i = 0;
            while (i < temp.size()) {
                OSMWay newWay = temp.get(i);
                int j = i + 1;
                while (j < temp.size()) {
                    OSMWay nextWay = temp.get(j);
                    try {
                        newWay = OSMWay.merge(newWay, nextWay);
                        temp.remove(j);
                    } catch (IllegalArgumentException e) {
                        j++;
                    }
                }
                newWays.add(newWay);
                i++;
            }
        }
        return newWays;
    }

    public static List<OSMWay> mergeRelations(List<OSMRelation> rels) {
        List<OSMWay> newWays = new ArrayList<>();
        for (OSMRelation rel : rels) {
            newWays.addAll(mergeRelation(rel));
        }
        return mergeWays(newWays);
    }

    public static List<OSMWay> mergeRelation(OSMRelation rel) {
        List<OSMWay> newWays = new ArrayList<>();
        newWays.addAll(mergeWays(rel.ways));
        newWays.addAll(mergeRelations(rel.relations));
        return mergeWays(newWays);
    }

    public List<OSMWay> getMergedWays() {
        return mergeRelation(this);
    }

    public float[][] getPaths() {
        List<OSMWay> newWays = getMergedWays();

        float[][] paths = new float[newWays.size()][];
        for (int i = 0; i < paths.length; i++) {
            paths[i] = newWays.get(i).getPath();
        }
        return paths;
    }

    public int size() {
        return nodes.size() + ways.size() + relations.size();
    }

    public boolean isEmpty() {
        for (OSMWay way : ways) {
            if (!way.isEmpty()) return false;
        }
        for (OSMRelation rel : relations) {
            if (!rel.isEmpty()) return false;
        }
        return true;
    }

    public boolean isConnected() {
        var ways = getMergedWays();
        if (ways.size() == 1) {
            return ways.get(0).first() == ways.get(0).last();
        }
        return false;
    }

    public void addNode(OSMNode node) {
        this.nodes.add(node);
    }

    public void addNodes(Collection<OSMNode> nodes) {
        this.nodes.addAll(nodes);
    }

    public void addWay(OSMWay way) {
        this.ways.add(way);
    }

    public void addWays(Collection<OSMWay> ways) {
        this.ways.addAll(ways);
    }

    public void addRelation(OSMRelation relation) {
        this.relations.add(relation);
    }

    public void addRelations(Collection<OSMRelation> relations) {
        this.relations.addAll(relations);
    }

    /**
     * @return empty string if no such key exists
     */
    public String getValue(String key) {
        for (int i = 0; i < tags.length; i += 2) {
            if (tags[i].equals(key)) return tags[i + 1];
        }
        return "";
    }

    @Override
    public Long getID() {
        return id;
    }
}