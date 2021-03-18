package dankmap.model.elements;

import dankmap.drawing.DrawType;
import dankmap.model.Bounded;
import dankmap.model.XYSupplier;
import javafx.scene.canvas.GraphicsContext;

import java.io.Serializable;

public interface MapElement extends Bounded, Serializable, XYSupplier, Comparable<MapElement> {

    void draw(GraphicsContext gfx);

    byte getDrawOrder();

    DrawType getDrawType();


    static int compareByDrawOrder(MapElement a, MapElement b) {
        return Byte.compare(a.getDrawOrder(), b.getDrawOrder());
    }

    @Override
    default int compareTo(MapElement o) {
        return compareByDrawOrder(this, o);
    }
}
