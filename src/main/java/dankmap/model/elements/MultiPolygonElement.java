package dankmap.model.elements;

import dankmap.drawing.DrawType;
import dankmap.model.Bounded;
import dankmap.model.Bounds;
import javafx.scene.canvas.GraphicsContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class MultiPolygonElement implements MapElement, Serializable {
    private static final long serialVersionUID = 6954394668948598973L;

    protected final float[][] paths;
    private final byte drawType;

    public MultiPolygonElement(float[][] paths, DrawType drawType) {
        if (paths.length == 0 || (paths[0].length == 0 && paths.length == 1))
            throw new IllegalArgumentException("Empty path");
        this.paths = paths;
        this.drawType = drawType.getId();
    }

    @Override
    public void draw(GraphicsContext gfx) {
        DrawType.set(gfx, DrawType.getDrawTypes().get(drawType));
        gfx.beginPath();
        for (float[] path : paths) {
            gfx.moveTo(path[0], path[1]);
            for (int j = 2; j < path.length; j += 2) {
                gfx.lineTo(path[j], path[j + 1]);
            }
        }
        gfx.fill();
        gfx.stroke();
    }

    public float getCenterX(){
        return getBounds().getCenterX();
    }
    public float getCenterY(){
        return getBounds().getCenterY();
    }
    @Override
    public float getX() {
        int mid = paths[0].length / 2;
        return paths[0][((mid) % 2) + (mid)];
    }

    @Override
    public float getY() {
        int mid = paths[0].length / 2;
        return paths[0][((mid) % 2) + (mid) + 1];
    }

    @Override
    public byte getDrawOrder() {
        return drawType;
    }

    @Override
    public DrawType getDrawType() {
        return DrawType.getDrawTypes().get(drawType);
    }

    @Override
    public float getArea() {
        return getBounds().getArea();
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(paths);
    }

    @Override
    public String toString() {
        return "MultiPolygonElement{" +
                "paths=" + Arrays.toString(paths) +
                ", drawType=" + drawType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MultiPolygonElement)) return false;
        MultiPolygonElement that = (MultiPolygonElement) o;
        return drawType == that.drawType &&
                Arrays.equals(paths, that.paths);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(drawType);
        result = 31 * result + Arrays.hashCode(paths);
        return result;
    }
}