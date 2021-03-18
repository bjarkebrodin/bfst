package dankmap.util.collections.tree;

import dankmap.drawing.ZoomLevel;
import dankmap.model.Bounds;
import dankmap.model.XYSupplier;
import dankmap.model.elements.MapElement;
import dankmap.model.elements.PointElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LayeredKDTree implements Serializable {
    List<KDTree> layers;

    //Primary constructor, creates an empty KDTree for each zoom level.
    public LayeredKDTree() {
        layers = new ArrayList<>();
        for (int i = 0; i <= ZoomLevel.ZOOM_LEVEL_MAX.getId(); i++) {
            layers.add(i, new KDTree());
        }
    }
    //adds elements to the correct KDTree by their area, unless they are Pointelements, in which they'll be added by DrawType's zoom level ID.
    public void add(MapElement element) {
        int layer = element.getDrawType().getZoomLevel();

        if(!(element instanceof PointElement)) {
            layer = Math.max(layer, ZoomLevel.getLevelByArea(element.getArea()).getId());
        }

        layers.get(layer).addElement(element);
    }
    //builds all KDTrees.
    public void build() {
        for (KDTree kdTree : layers) {
            kdTree.buildTree();
        }
    }
    //Method that calls rangeSearch for all KDTrees with zoom levels from 13 to the current zoom level.
    public List<MapElement> rangeSearch(ZoomLevel currentZoomLevel, Bounds queryRange) {
        List<MapElement> results = new ArrayList<>();

        for (int i = 0; i < currentZoomLevel.getId(); i++) {
            layers.get(i).rangeSearch(results, queryRange);
        }
        return results;
    }
    //Calls nearest in all non-empty KDTrees.
    public void nearest(ArrayList<MapElement> results, XYSupplier queryPoint) {
        for (KDTree kdTree : layers) {
            if (kdTree.getRoot() != null) {
                results.addAll(kdTree.nearest(queryPoint));
            }
        }
    }
    //returns the sum of all toAdd lists in KDTrees.
    public int size() {
        int size = 0;
        for (KDTree kdTree : layers) {
            size += kdTree.size();
        }
        return size;
    }
}
