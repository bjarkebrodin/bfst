package dankmap.model.elements;

import dankmap.drawing.DrawType;
import dankmap.model.Bounded;
import dankmap.model.Bounds;
import javafx.scene.canvas.GraphicsContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class PolygonElement implements MapElement, Bounded, Serializable {
    private static final long serialVersionUID = 4001373648980993442L;

    private final boolean filled;
    protected final float[] path;
    private final byte drawType;

    public PolygonElement(float[] path, DrawType drawType) {
        this.path = path;
        this.filled = false;
        this.drawType = drawType.getId();
    }

    public PolygonElement(float[] path, boolean filled, DrawType drawType) {
        this.path = path;
        this.filled = filled;
        this.drawType = drawType.getId();
    }

    @Override
    public void draw(GraphicsContext gfx) {
        DrawType.set(gfx, DrawType.getDrawTypes().get(drawType));
        gfx.beginPath();
        gfx.moveTo(path[0], path[1]);
        for (int j = 2; j < path.length; j += 2) {
            gfx.lineTo(path[j], path[j + 1]);
        }
        if (filled) gfx.fill();
        gfx.stroke();
    }

    @Override
    public DrawType getDrawType() {
        return DrawType.getDrawTypes().get(drawType);
    }

    public float getCenterX(){
        return getBounds().getCenterX();
    }
    public float getCenterY(){
        return getBounds().getCenterY();
    }

    @Override
    public float getX() {
        int mid = path.length / 2;
        return path[((mid) % 2) + (mid)];
    }

    @Override
    public float getY() {
        int mid = path.length / 2;
        return path[((mid) % 2) + (mid) + 1];
    }

    @Override
    public byte getDrawOrder() {
        return drawType;
    }

    @Override
    public float getArea() {
        return getBounds().getArea();
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(path);
    }

    @Override
    public String toString() {
        return "PolygonElement{" +
                "filled=" + filled +
                ", path=" + Arrays.toString(path) +
                ", drawType=" + drawType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PolygonElement)) return false;
        PolygonElement that = (PolygonElement) o;
        return filled == that.filled &&
                drawType == that.drawType &&
                Arrays.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(filled, drawType);
        result = 31 * result + Arrays.hashCode(path);
        return result;
    }


}