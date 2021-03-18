package dankmap.model.elements;

import dankmap.drawing.DrawType;
import dankmap.drawing.ZoomLevel;
import dankmap.model.Bounded;
import dankmap.model.Bounds;
import javafx.scene.canvas.GraphicsContext;

import java.io.Serializable;

public class IslandElement implements Serializable, MapElement {
    private static final long serialVersionUID = -8622778479750783406L;

    public static final byte drawType = (byte) 1;
    private float[] path;
    private Bounds bounds;

    public IslandElement(float[] path) {
        this.path = path;
        bounds = new Bounds(path);
    }

    @Override
    public void draw(GraphicsContext gfx) {
        var zoomLevel = ZoomLevel.getCurrent();
        if (zoomLevel.getMinArea() >= getArea()) return;
        gfx.beginPath();
        gfx.moveTo(path[0], path[1]);
        int inc = getPathIncrement();
        for (int j = inc; j < path.length; j += inc) {
            gfx.lineTo(path[j], path[j + 1]);
        }
        gfx.fill();
    }

    public float getCenterX(){
        return getBounds().getCenterX();
    }
    public float getCenterY(){
        return getBounds().getCenterY();
    }

    /**
     * IslandElements consists of a very detailed coastline.
     * In order to draw varying amount of detail, we determine,
     * from the current ZoomLevel, how many points we skip in the coastlines.
     */
    private int getPathIncrement() {
        switch (ZoomLevel.getCurrent().getId()) {
            case 0:
            case 1:
                return 128;
            case 2:
            case 3:
                return 64;
            case 4:
                return 32;
            case 5:
                return 16;
            case 6:
                return 8;
            case 7:
                return 4;
            case 8:
            default:
                return 2;
        }
    }

    public float getArea() {
        return bounds.getArea();
    }

    @Override
    public Bounds getBounds() {
        return bounds;
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
    public float getX() {
        return bounds.getCenterX();
    }

    @Override
    public float getY() {
        return bounds.getCenterY();
    }
}
