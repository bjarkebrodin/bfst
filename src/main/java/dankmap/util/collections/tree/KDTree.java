package dankmap.util.collections.tree;


import dankmap.model.Bounds;
import dankmap.model.XYSupplier;
import dankmap.model.elements.MapElement;
import dankmap.util.VectorMath;

import java.io.Serializable;
import java.util.*;

import static java.lang.Float.*;


public class KDTree implements Serializable {

    public List<MapElement> toAdd;
    // The universe of our KD-tree
    public float minX, minY, maxX, maxY;
    //Root node of the tree.
    private Node root;

    public Node getRoot() {
        return root;
    }

    //primary constructor of KDTree.
    public KDTree() {
        toAdd = new ArrayList<>();
        initializeBounds();
    }


    private void initializeBounds() {
        minX = NEGATIVE_INFINITY;
        minY = NEGATIVE_INFINITY;
        maxX = POSITIVE_INFINITY;
        maxY = POSITIVE_INFINITY;
    }
    //builds the tree and clears temporary list of mapElements toAdd.
    public void buildTree() {
        root = buildKDTree(toAdd, true);
        toAdd.clear();
    }

    /**
     * @param mapElements List of MapElements, in each recursive call, this is sorted by split-coordinate and split into two list, split at the lowest index median value.
     * @param splitVertical  toggles on each call, to discern between splitting vertically or horizontally, is called with value=true.
     * @return a Node, holding pointers to left and right nodes, if these exist. if the node is a leaf, it has two null references instead.
     */
    private Node buildKDTree(List<MapElement> mapElements, boolean splitVertical) {
        Node node;
        if (mapElements.isEmpty()) {
            return null;
        } else if (mapElements.size() == 1) {
            return new Node(mapElements.get(0), null, null);
        } else {
            //sorts by either x or y coordinate 
            if (splitVertical) {
                mapElements.sort(Comparator.comparing(MapElement::getCenterX));
            } else {
                mapElements.sort(Comparator.comparing(MapElement::getCenterY));
            }

            int median = median(mapElements, splitVertical);

            List<MapElement> leftList = mapElements.subList(0, median);
            List<MapElement> rightList = mapElements.subList(median + 1, mapElements.size());

            //@Todo find out which way of writing this we see most fitting.
            node = new Node();
            node.element = mapElements.get(median);
            node.left = buildKDTree(leftList, !splitVertical);
            node.right = buildKDTree(rightList, !splitVertical);
        }
        return node;
    }

    public List<MapElement> rangeSearch(List<MapElement> results, Bounds queryRange) {
        Bounds universe = new Bounds(minX, minY, maxX, maxY);
        search(results, root, universe, queryRange, true);
        return results;
    }



    private void search(List<MapElement> results, Node node, Bounds region, Bounds queryRange, boolean splitVertical) {
        if (node == null) {
            return;
        }

        // Bounds for each segment of the subtrees
        Bounds regionLeft = regionLeft(region, node, splitVertical);
        Bounds regionRight = regionRight(region, node, splitVertical);

        // If the region in the call is fully contained in the query range the subtree is reported...
        if (queryRange.contains(region) && !node.isLeaf()) {
            reportSubTree(results, node);
            return;

            //else we check if the query range intersects the two regions divided by the splitting value of the current node.
        } else {
            if (queryRange.intersects(regionLeft) && node.hasLeft()) {
                search(results, node.left, regionLeft, queryRange, !splitVertical);
            }
            if (queryRange.intersects(regionRight) && node.hasRight()) {
                search(results, node.right, regionRight, queryRange, !splitVertical);
            }
        }
        // if the current nodes' elements' bounds intersects with the query range the MapElement is reported
        if (queryRange.intersects(node.element.getBounds())) {
            results.add(node.element);
        }
    }

    // Reports every nodes' MapElement in a given subtree starting at the root of the subtree
    private void reportSubTree(List<MapElement> results, Node node) {

        if (node == null) {
            return;
        }

        results.add(node.element);

        if (node.hasLeft()) {
            reportSubTree(results, node.left);
        }
        if (node.hasRight()) {
            reportSubTree(results, node.right);
        }
    }


    private Bounds regionLeft(Bounds region, Node node, boolean splitVertical) {
        Bounds regionLeft = new Bounds(region.getMinX(), region.getMinY(), region.getMaxX(), region.getMaxY());

        if (splitVertical) {
            regionLeft.setMaxX(node.getX());
        } else {
            regionLeft.setMaxY(node.getY());
        }
        return regionLeft;
    }

    private Bounds regionRight(Bounds region, Node node, boolean splitVertical) {
        Bounds regionRight = new Bounds(region.getMinX(), region.getMinY(), region.getMaxX(), region.getMaxY());
        if (splitVertical) {
            regionRight.setMinX(node.getX());
        } else {
            regionRight.setMinY(node.getY());
        }
        return regionRight;
    }

    //Returns an ArrayList of possible NN going by the elements Center coordinates.
    public ArrayList<MapElement> nearest( XYSupplier queryPoint) {
        //The universe of the kd Tree
        ArrayList<MapElement> results = new ArrayList<>();
        Bounds universe = new Bounds(minX, minY, maxX, maxY);
        nearest(queryPoint, root, POSITIVE_INFINITY, universe, true, results);
        return results;
    }


    public void nearest(XYSupplier queryPoint, Node currentNode, double currentDist, Bounds region, boolean splitVertical, ArrayList<MapElement> results) {
        //checks if the best distance found so far is greater than distance between querypoint and current node.
        double distToCompare = VectorMath.sqDist(queryPoint.getX(), queryPoint.getY(), currentNode.getX(), currentNode.getY());

        if (currentDist > distToCompare) {
            currentDist = distToCompare;
            results.add(currentNode.element);
        }

        Bounds regionLeft = regionLeft(region, currentNode, splitVertical);
        Bounds regionRight = regionRight(region, currentNode, splitVertical);

        //checks if nearest should traverse left or right in the tree.
        if (splitCoord(queryPoint, splitVertical) < splitCoord(currentNode.element, splitVertical) && currentNode.hasLeft()) {
            nearest(queryPoint, currentNode.left, currentDist, regionLeft, !splitVertical, results);

            if(shouldCheckOther(queryPoint, currentNode, regionRight, currentDist, splitVertical)) {
                nearest(queryPoint, currentNode.right, currentDist, regionRight, !splitVertical, results);
            }

        } else if (splitCoord(queryPoint, splitVertical) >= splitCoord(currentNode.element, splitVertical) && currentNode.hasRight()) {
            nearest(queryPoint, currentNode.right, currentDist, regionRight, !splitVertical, results);
            if(shouldCheckOther(queryPoint, currentNode, regionLeft, currentDist, splitVertical)) {
                nearest(queryPoint, currentNode.right, currentDist, regionLeft, !splitVertical, results);
            }
        }
    }
    //helper
    public float splitCoord(XYSupplier element, boolean splitVertical) {
        if (splitVertical) {
            return element.getCenterX();
        } else {
            return element.getCenterY();
        }
    }

    public void addElement(MapElement element) {
        toAdd.add(element);
    }


    //                    UTILITIES                 //

    //returns the median as an index of a List, ensures that we choose the lowest index with the median value.
    public int median(List<MapElement> elements, boolean splitVertical) {
        int median;

        median = (elements.size() - 1) / 2; //chooses the floor value

        while (median != 0 && splitCoord(elements.get(median), splitVertical) == splitCoord(elements.get(median - 1), splitVertical)) {
            median--;
        }


        return median;
    }

    //Checks the orthogonal distance from the queryPoint to the splitting line.
    public double orthogonalDistance(XYSupplier queryPoint, Node splittingPoint, boolean splitVertical) {

        double orthogonalDistance;
        if (splitVertical) {
            orthogonalDistance = VectorMath.sqDist(queryPoint.getX(), queryPoint.getY(), splittingPoint.getX(), queryPoint.getY());
        } else {
            orthogonalDistance = VectorMath.sqDist(queryPoint.getX(), queryPoint.getY(), queryPoint.getX(), splittingPoint.getY());
        }
        return orthogonalDistance;
    }

    //if the queryPoints is not in the interval of a splitting line we must find the nearest point to the region
    private double minDistToRegion(XYSupplier queryPoint, Bounds region) {
        float x = queryPoint.getX();
        float y = queryPoint.getY();
        double distToTopLeft = VectorMath.sqDist(x, y, region.getMinX(),region.getMinY());
        double distToTopRight = VectorMath.sqDist(x, y, region.getMaxX(), region.getMinY());
        double distToBottomLeft = VectorMath.sqDist(x, y, region.getMinX(), region.getMaxY());
        double distToBottomRight = VectorMath.sqDist(x, y, region.getMaxX(), region.getMaxY());

        double dist = distToTopLeft;
        if(dist > distToTopRight) {
            dist = distToTopRight;
        }
        if(dist > distToBottomLeft){
            dist = distToBottomLeft;
        }
        if(dist > distToBottomRight){
            dist = distToBottomRight;
        }
            return dist;
    }

    //Computes whether there could exist a point in another region, that is closer to the query point in NN.
    public boolean shouldCheckOther(XYSupplier queryPoint, Node splittingPoint, Bounds region, double dist, boolean splitVertical) {
        if(splitVertical) {
            if(queryPoint.getY() >= region.getMinY() && queryPoint.getY() <= region.getMaxY()) {
                if (orthogonalDistance(queryPoint, splittingPoint, splitVertical) < dist) {
                    return true;
                }
            }
            else {
                if(minDistToRegion(queryPoint, region) < dist) {
                    return true;
                }
            }
        }
        else {
            if(queryPoint.getX() >= region.getMinX() && queryPoint.getX() <= region.getMaxX()) {
                if(orthogonalDistance(queryPoint, splittingPoint, splitVertical) < dist) {
                    return true;
                }
            }
            else {
                if(minDistToRegion(queryPoint, region) < dist) {
                    return true;
                }
            }
        }
        return false;
    }

    public int size() {
        return toAdd.size();
    }


    /**
     * Nodes in our KD-tree. holds a reference to a left and right child and a reference to a mapElement.
     */
    private static class Node implements Serializable{
        private Node left;
        private Node right;
        private MapElement element;

        public Node() {
        }

        public Node(MapElement element, Node left, Node right) {
            this.element = element;
            this.left = left;
            this.right = right;
        }

        private boolean hasLeft() {
            return this.left != null;
        }

        private boolean hasRight() {
            return this.right != null;
        }

        public float getX() {
            return this.element.getCenterX();
        }

        public float getY() {
            return this.element.getCenterY();
        }

        public boolean isLeaf() {
            return this.left == null && this.right == null;
        }

    }
}

