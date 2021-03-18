package dankmap.drawing;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class DrawType implements Comparable<DrawType>, Serializable {
    public static final DrawType NONE = new DrawType((byte) -1, (byte) -1, null, null, 0, false, false, null, null, null);

    private static final long serialVersionUID = -1467456478964658938L;
    private static final String FILE = "draw_types/draw_types.xml";

    private static DrawTypeMap drawTypeMap;
    private static Map<Byte, DrawType> drawTypes;

    public enum Style {
        NORMAL("Normal"),
        GRAYSCALE("Gray Scale"),
        DARK("Dark Mode"),
        RANDOM("Random"),
        COLORBLIND("Color Blind");

        private String name;

        Style(String name) {
            this.name = name;
        }

        public static Style getStyle(String name) {
            for (Style s : values()) {
                if (s.getName().equals(name)) return s;
            }
            return null;
        }

        public String getName() {
            return name;
        }
    }

    public static void loadDrawTypeMap() {
        loadDrawTypeMap(Style.NORMAL);
    }

    public static void loadDrawTypeMap(Style style) {
        try {
            DrawTypeXMLReader drawTypeXMLReader = new DrawTypeXMLReader(getFile());
            drawTypeMap = drawTypeXMLReader.load(style);
            drawTypes = drawTypeMap.getDrawTypes();
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }
    }


    public static void set(GraphicsContext gfx, DrawType drawType) {
        double strokeWidth = drawType.strokeWidth / 1e5;
        if (drawType.isDynamic()) {
            strokeWidth = Math.max(strokeWidth, (drawType.strokeWidth / gfx.getTransform().getMxx()) / 4);
        }
        gfx.setFill(drawType.fillColor);
        gfx.setStroke(drawType.strokeColor);
        gfx.setLineWidth(strokeWidth);
        var dashes = drawType.dashes;
        double[] newDashes = null;
        if (dashes != null) {
            newDashes = new double[dashes.length];
            for (int i = 0; i < dashes.length; i++) {
                newDashes[i] = dashes[i] / 1e5;
            }
        }
        gfx.setLineDashes(newDashes);
        gfx.setLineJoin(drawType.join);
        gfx.setLineCap(drawType.cap);
    }

    public static DrawTypeMap getDrawTypeMap() {
        return drawTypeMap;
    }

    public static Map<Byte, DrawType> getDrawTypes() {
        return drawTypes;
    }

    private final byte id; //Should be unique
    private final byte zoomLevel;
    private final Color fillColor;
    private final Color strokeColor;
    private final double strokeWidth;
    private final boolean dynamic;
    private final boolean outline;
    private final double[] dashes;
    private final StrokeLineCap cap; //butt, round, square
    private final StrokeLineJoin join; //miter, bevel, round

    private DrawType(byte id, byte zoomLevel, Color fillColor, Color strokeColor, double strokeWidth, boolean dynamic, boolean outline, double[] dashes, StrokeLineCap cap, StrokeLineJoin join) {
        this.id = id;
        this.zoomLevel = zoomLevel;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.dynamic = dynamic;
        this.outline = outline;
        this.dashes = dashes;
        this.cap = cap;
        this.join = join;
    }

    public byte getId() {
        return id;
    }

    public byte getZoomLevel() {
        return zoomLevel;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public double getStrokeWidth() {
        return strokeWidth;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public boolean isOutline() {
        return outline;
    }

    public double[] getDashes() {
        return dashes;
    }

    public StrokeLineCap getCap() {
        return cap;
    }

    public StrokeLineJoin getJoin() {
        return join;
    }

    @Override
    public int compareTo(DrawType o) {
        return o.id - id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DrawType drawType = (DrawType) o;
        return id == drawType.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DrawType{" +
                "id=" + id +
                ", zoomLevel=" + zoomLevel +
                ", fillColor=" + fillColor +
                ", strokeColor=" + strokeColor +
                ", strokeWidth=" + strokeWidth +
                ", dynamic=" + dynamic +
                ", outline=" + outline +
                ", dashes=" + Arrays.toString(dashes) +
                ", cap=" + cap +
                ", join=" + join +
                '}';
    }

    public static class Builder {
        private byte id;
        private byte zoomLevel;
        private Color fillColor;
        private Color strokeColor;
        private double strokeWidth;
        private boolean dynamic;
        private boolean outline;
        private double[] dashes;
        private StrokeLineCap cap; //miter, bevel, round
        private StrokeLineJoin join; //butt, round, square

        public Builder() {
            zoomLevel = 0;
            strokeWidth = 1.0;
            cap = StrokeLineCap.ROUND;
            join = StrokeLineJoin.MITER;
        }

        public Builder id(byte id) {
            this.id = id;
            return this;
        }

        public Builder zoomLevel(byte zoomLevel) {
            this.zoomLevel = zoomLevel;
            return this;
        }

        public Builder fillColor(Color fillColor) {
            this.fillColor = fillColor;
            return this;
        }

        public Builder strokeColor(Color strokeColor) {
            this.strokeColor = strokeColor;
            return this;
        }

        public Builder strokeWidth(double strokeWidth) {
            this.strokeWidth = strokeWidth;
            return this;
        }

        public Builder dynamic(boolean dynamic) {
            this.dynamic = dynamic;
            return this;
        }

        public Builder outline(boolean outline) {
            this.outline = outline;
            return this;
        }

        public Builder dashes(double[] dashes) {
            this.dashes = dashes;
            return this;
        }

        public Builder cap(StrokeLineCap cap) {
            this.cap = cap;
            return this;
        }

        public Builder join(StrokeLineJoin join) {
            this.join = join;
            return this;
        }

        public DrawType build() {
            if (this.fillColor == null) fillColor = Color.TRANSPARENT;
            if (this.strokeColor == null) strokeColor = fillColor;
            return new DrawType(id, zoomLevel, fillColor, strokeColor, strokeWidth, dynamic, outline, dashes, cap, join);
        }
    }

    private static InputStream getFile() {
        return DrawType.class.getClassLoader().getResourceAsStream(FILE);
    }

}