package dankmap.util.collections.tree;

import dankmap.model.Bounds;
import dankmap.model.XYSupplier;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static dankmap.util.collections.tree.KDTreeUtilities.*;
import static java.lang.Float.*;
import static java.util.Arrays.copyOfRange;

/**
 * Maintains a fully balanced KD-tree by lazily
 * reconstructing the tree. This is highly inefficient
 * for frequent insertions/deletions and even more so
 * the larger the tree becomes.
 */
public class KDTreeMap<K extends XYSupplier, V> implements Map<K, V>, Serializable {
    private static final long serialVersionUID = 4210652501620949128L;

    /**
     * A node of the tree
     */
    private class Node implements Serializable {
        private static final long serialVersionUID = 3114187454544539263L;

        K key;
        V value;
        Node left, right;

        Node(K key, V value, Node left, Node right) {
            this.key = key;
            this.value = value;
            this.left = left;
            this.right = right;

            minX = min(minX, key.getX());
            minY = min(minY, key.getY());
            maxX = max(maxX, key.getX());
            maxY = max(maxY, key.getY());
        }

        private boolean isLeaf() {
            return left == null && right == null;
        }
    }

    // Root of this tree
    private Node root;

    // Size of this tree
    private int size;

    // Boundaries of the universe of elements, that is :
    // for any key k with (x,y) in this tree :
    //  minX <= x <= maxX
    //  minY <= y <= maxY
    private float minX, minY, maxX, maxY;

    // If the tree is balanced
    private boolean isBalanced;

    // Contains recently added elements
    private Map<K, V> recentlyAdded = new HashMap<>();


    /**
     * Default constructor
     */
    public KDTreeMap() {
        minX = POSITIVE_INFINITY;
        minY = POSITIVE_INFINITY;
        maxX = NEGATIVE_INFINITY;
        maxY = NEGATIVE_INFINITY;
        recentlyAdded = new HashMap<>();
        size = 0;
    }

    /**
     * Constructs a balanced tree of the entries in the
     * specified map
     */
    public KDTreeMap(Map<K, V> map) {
        minX = POSITIVE_INFINITY;
        minY = POSITIVE_INFINITY;
        maxX = NEGATIVE_INFINITY;
        maxY = NEGATIVE_INFINITY;
        recentlyAdded.putAll(map);
        reconstruct();
    }

    /**
     * @inheritDoc
     */
    @Override
    public V get(Object o) {
        checkBalance();

        if (root == null) return null;

        @SuppressWarnings("unchecked") K k = (K) o;

        Node found = find(k, root, true);
        if (found == null) return null;

        return found.value;
    }

    /**
     * returns the key equal to the specified element
     * or null if no such key exists
     */
    public K getKey(XYSupplier k) {
        checkBalance();

        if (root == null) return null;

        Node found = find(k, root, true);
        if (found == null) return null;

        return found.key;
    }

    private void update(K k, V v) {
        Node node = find(k, root, true);

        if (node == null) {
            throw new NoSuchElementException(k.toString());
        }

        node.value = v;
    }

    /**
     * @inheritDoc
     */
    @Override
    public V put(K k, V v) {
        if (v == null || k == null) {
            throw new NullPointerException();
        }

        if (root == null) {
            V replaced = recentlyAdded.put(k, v);
            if (replaced == null) {
                isBalanced = false;
                size++;
            }
            return replaced;
        }

        Node node = find(k, root, true);

        if (node == null) {
            isBalanced = false;
            size++;
            return recentlyAdded.put(k, v);
        }

        V temp = node.value;
        node.value = v;
        return temp;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        checkBalance();
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        checkBalance();

        @SuppressWarnings("unchecked") K k = (K) key;
        Node found = find(k, root, true);
        if (found == null) return null;

        V val = found.value;
        found.value = null;

        return val;
    }

    @Override
    public void clear() {
        recentlyAdded.clear();
        root = null;
        size = 0;
        minX = POSITIVE_INFINITY;
        minY = POSITIVE_INFINITY;
        maxX = NEGATIVE_INFINITY;
        maxY = NEGATIVE_INFINITY;
    }

    @Override
    public boolean containsKey(Object key) {
        checkBalance();
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        checkBalance();
        for (V v : values()) {
            if (v.equals(value)) return true;
        }
        return false;
    }

    @Override
    public Set<K> keySet() {
        checkBalance();
        Collection<Node> results = new LinkedList<>();
        return keys(collectTree(root, results));
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        checkBalance();
        Collection<Node> results = new ArrayList<>();
        collectTree(root, results);
        return results.stream()
                .map((nd) -> new Entry<K, V>() {
                    K k = nd.key;
                    V v = nd.value;

                    @Override
                    public K getKey() {
                        return k;
                    }

                    @Override
                    public V getValue() {
                        return v;
                    }

                    @Override
                    public V setValue(V value) {
                        V old = v;
                        v = value;
                        return old;
                    }
                })
                .collect(Collectors.toSet());
    }




    @Override
    public Collection<V> values() {
        checkBalance();
        Collection<Node> results = new LinkedList<>();
        return values(collectTree(root, results));
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }



    public Collection<K> nearestKeys(XYSupplier query, int limit) {
        checkBalance();
        return keys(nearestNodes(query, limit));
    }


    private Collection<Node> collectTree(Node node, Collection<Node> results) {
        results.add(node);
        Node left = node.left, right = node.right;
        if (left != null) collectTree(left, results);
        if (right != null) collectTree(right, results);
        return results;
    }

    private Collection<Node> nearestNodes(XYSupplier query, int limit) {
        if (query == null) {
            throw new IllegalArgumentException("null query");
        }
        if (root == null) {
            throw new NoSuchElementException();
        }

        Comparator<Node> ascending = (a, b) -> {
            if (sqDist(query, a.key) > sqDist(query, b.key)) return 1;
            return -1; /* IMPORTANT: breaks if a,b are ever equal */
        };
        TreeSet<Node> results = new TreeSet<>(ascending);

        return nearest(query, root, results, limit, true);
    }

    private TreeSet<Node> nearest(XYSupplier query, Node node, TreeSet<Node> results, int limit, boolean vertical) {
        K key = node.key;

        if (results.isEmpty()) {
            results.add(node);
        } else if ((results.size() < limit) || sqDist(query, key) < sqDist(query, results.last().key)) {
            results.add(node);
        }

        if (results.size() > limit) {
            results.pollLast();
        }

        Node left = node.left;
        Node right = node.right;
        boolean hasLeft = left != null;
        boolean hasRight = right != null;

        Function<Node, Boolean> shouldCheckOther = (k) -> {
            boolean couldHaveShorter = sqDistPerpendicular(query, k.key, vertical) < sqDist(query, results.last().key);
            return results.size() < limit || couldHaveShorter;
        };

        if (lt(key, query, vertical)) {
            if (hasRight) {
                nearest(query, right, results, limit, !vertical);
            }
            if (hasLeft && shouldCheckOther.apply(node)) {
                nearest(query, left, results, limit, !vertical);
            }
        } else {
            if (hasLeft) {
                nearest(query, left, results, limit, !vertical);
            }
            if (hasRight && shouldCheckOther.apply(node)) {
                nearest(query, right, results, limit, !vertical);
            }
        }

        return results;
    }

    private Collection<Node> searchTree(Node node, Bounds region, Bounds query, Collection<Node> results, boolean vertical) {
        K key = node.key;

        if (query.contains(key.getX(), key.getY())) {
            results.add(node);
        }

        Node left = node.left;
        Node right = node.right;
        boolean hasLeft = left != null;
        boolean hasRight = right != null;

        if (node.isLeaf()) {
            return results;
        }

        Bounds regionLeft = null, regionRight = null;

        if (hasLeft) {
            regionLeft = intersection(region, key, vertical, false);
            if (query.contains(regionLeft)) {
                collectTree(left, results);
            } else if (query.intersects(regionLeft)) {
                searchTree(left, regionLeft, query, results, !vertical);
            }
        }

        if (hasRight) {
            regionRight = intersection(region, key, vertical, true);
            if (query.contains(regionRight)) {
                collectTree(right, results);
            } else if (query.intersects(regionRight)) {
                searchTree(right, regionRight, query, results, !vertical);
            }
        }

        return results;
    }

    private Node find(XYSupplier k, Node node, boolean vertical) {
        K key = node.key;

        Node left = node.left;
        Node right = node.right;
        boolean hasLeft = left != null;
        boolean hasRight = right != null;

        if (key.getX() == k.getX() && key.getY() == k.getY()) {
            return node;
        }
        if (le(k, key, vertical)) {
            if (hasLeft) {
                return find(k, left, !vertical);
            } else {
                return null;
            }
        } else {
            if (hasRight) {
                return find(k, right, !vertical);
            } else {
                return null;
            }
        }
    }

    private void checkBalance() {
        if (!isBalanced) {
            reconstruct();
            isBalanced = true;
        }
    }

    private void reconstruct() {
        if (root != null) {
            Collection<Node> results = new LinkedList<>();
            collectTree(root, results);
            for (Node nd : results) {
                if (nd.value != null) {
                    recentlyAdded.put(nd.key, nd.value);
                }
            }
        }
        root = buildTree(toArray(recentlyAdded.keySet()), true);
        size = recentlyAdded.size();
        for (Entry<K, V> entry : recentlyAdded.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            update(key, value);
        }
        recentlyAdded.clear();
        isBalanced = true;
    }

    private Node buildTree(K[] keys, boolean vertical) {
        int n = keys.length;

        if (n == 0) {
            return null;
        }

        Node left = null, right = null;

        int median = median(keys, 0, keys.length - 1, vertical);
        K key = keys[median];

        if (median > 0) {
            left = buildTree(copyOfRange(keys, 0, median), !vertical);
        }
        if (median < n - 1) {
            right = buildTree(copyOfRange(keys, median + 1, n), !vertical);
        }

        return new Node(key, null, left, right);
    }

    private Collection<V> values(Collection<Node> nodes) {
        return nodes.stream()
                .map((Node nd) -> nd.value)
                .collect(Collectors.toList());
    }

    private Set<K> keys(Collection<Node> nodes) {
        return nodes.stream()
                .map((Node nd) -> nd.key)
                .collect(Collectors.toSet());
    }
}
