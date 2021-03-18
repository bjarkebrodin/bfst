package dankmap.drawing;

import javafx.util.Pair;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * This class functions as a data structure to map OSM key-value pairs to
 * a corresponding DrawType. Since mulitple DrawTypes can have the
 * same osm key, a simple HashMap wont do the job.
 */
public class DrawTypeMap extends HashMap<Pair<String, String>, DrawType> implements Serializable {

    private static final long serialVersionUID = -1948536883832151073L;

    public DrawType put(String key, String value, DrawType drawType) {
        return super.put(new Pair<>(key, value), drawType);
    }

    public DrawType get(String key, String value) {
        for (var p : keySet()) {
            var k = p.getKey();
            var v = p.getValue();
            if (k != null) {
                if (v != null) {
                    if (key.equals(k) && value.equals(v)) return get(p);
                } else {
                    if (key.equals(k)) return get(p);
                }
            }
        }
        return null;
    }

    public DrawType get(String[] tags) {
        TreeSet<DrawType> types = new TreeSet<>();
        for (int i = 0; i < tags.length; i += 2) {
            var type = get(tags[i], tags[i + 1]);
            if (type != null) types.add(type);
        }
        if (types.isEmpty()) return null;
        return types.first();
    }

    public DrawType get(List<String> tags) {
        TreeSet<DrawType> types = new TreeSet<>();
        for (int i = 0; i < tags.size(); i += 2) {
            var type = get(tags.get(i), tags.get(i + 1));
            if (type != null) types.add(type);
        }
        if (types.isEmpty()) return null;
        return types.first();
    }

    public DrawTypeMap getRange(String[] tags) {
        DrawTypeMap sub = new DrawTypeMap();
        for (int i = 0; i < tags.length; i += 2) {
            var type = get(tags[i], tags[i + 1]);
            if (type != null) sub.put(tags[i], tags[i + 1], type);
        }
        return sub;
    }

    public boolean hasTags(String[] tags) {
        for (int i = 0; i < tags.length; i += 2) {
            if (get(tags[i], tags[i + 1]) != null) return true;
        }
        return false;
    }

    public Map<Byte, DrawType> getDrawTypes() {
        Map<Byte, DrawType> drawTypes = new HashMap<>();
        for (DrawType drawType : values()) {
            drawTypes.put(drawType.getId(), drawType);
        }
        return drawTypes;
    }

    public DrawTypeMap getRange(List<String> tags) {
        DrawTypeMap sub = new DrawTypeMap();
        for (int i = 0; i < tags.size(); i += 2) {
            var type = get(tags.get(i), tags.get(i + 1));
            if (type != null) sub.put(tags.get(i), tags.get(i + 1), type);
        }
        return sub;
    }
}
