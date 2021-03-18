package dankmap.model.elements;

import dankmap.drawing.DrawType;
import dankmap.model.Bounded;
import dankmap.model.Bounds;
import dankmap.util.VectorMath;
import javafx.scene.canvas.GraphicsContext;

import java.io.Serializable;
import java.util.Arrays;

public class PathElement implements MapElement, Serializable {
    private static final long serialVersionUID = 5043162928750707189L;
    protected final float[] path;
    private final byte drawType;

    public PathElement(float[] path, DrawType drawType) {
        if (path.length < 2) throw new IllegalArgumentException("Empty path");
        this.path = path;
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
        gfx.stroke();
    }

    /**
     * When drawing roads, we wanna be able to outline each road's stroke. Since
     * no such method exists in javafx, the solution is to draw all roads
     * twice with different lineWidths. This also creates the illusion of the
     * roads 'sticking' together.
     */
    protected void outline(GraphicsContext gfx) {
        var dt = DrawType.getDrawTypes().get(drawType);
        if (dt.isOutline()) {
            DrawType.set(gfx, dt);
            gfx.setStroke(dt.getStrokeColor().darker());

            gfx.setLineWidth(gfx.getLineWidth() / 4 * 5);
            gfx.beginPath();
            gfx.moveTo(path[0], path[1]);
            for (int j = 2; j < path.length; j += 2) {
                gfx.lineTo(path[j], path[j + 1]);
            }
            gfx.stroke();
        }
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
    public DrawType getDrawType() {
        return DrawType.getDrawTypes().get(drawType);
    }

    @Override
    public float getArea() {
        Bounds bounds = getBounds();
        // We only want to hide small roads/paths.
        // Constant is found by trial and error.
        float minAllowedArea = 6e-7f;

        float area = (float) VectorMath.sqDist(bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY());
        if (area > minAllowedArea) {
            // An area of 1 should be large enough to always be drawn
            // at the ZoomLevel given by the DrawType.
            // We could have used Float.POSITIVE_INFINITY to be sure.
            return 1;
        }

        // We multiple the area by a constant found by trial and error
        // to get the best results.
        return area * 0.30f;
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(path);
    }

    @Override
    public String toString() {
        return "PathElement{" +
                "path=" + Arrays.toString(path) +
                ", drawType=" + drawType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathElement other = (PathElement) o;
        return drawType == other.drawType && Arrays.equals(path, other.path);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(path) + drawType;
    }
}