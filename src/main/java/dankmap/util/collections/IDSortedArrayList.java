package dankmap.util.collections;

import java.util.ArrayList;
import java.util.Comparator;

public class IDSortedArrayList<T extends Number & Comparable<T>, U extends IDSortedArrayList.IDSupplier<T>> extends ArrayList<U> {
    private boolean isSorted;

    public IDSortedArrayList() {
        isSorted = false;
    }

    public boolean add(U t) {
        if (!isSorted) return super.add(t);
        super.add(t);
        isSorted = false;
        return true;
    }

    public U get(T id) {
        if (!isSorted) sort();
        int index = binarySearch(id);
        if (index >= 0 && index < size()) return get(index);
        else return null;
    }

    private void sort() {
        sort(Comparator.comparing(U::getID));
        isSorted = true;
    }

    private int binarySearch(T id) {
        int mid;
        int lo = 0;
        int hi = size() - 1;
        while (lo <= hi) {
            mid = (lo + hi) / 2;
            var compare = val(mid).compareTo(id);
            if (compare < 0) lo = mid + 1;
            else if (compare > 0) hi = mid - 1;
            else return mid;
        }
        return -1;
    }

    private T val(int index) {
        return super.get(index).getID();
    }

    public static interface IDSupplier<T extends Number> {
        T getID();
    }
}