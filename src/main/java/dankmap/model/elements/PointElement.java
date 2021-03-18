package dankmap.model.elements;

import dankmap.drawing.DrawType;
import dankmap.model.Bounds;
import javafx.scene.canvas.GraphicsContext;

import java.io.Serializable;
import java.util.Objects;

public class PointElement implements MapElement, Serializable {
    private static final long serialVersionUID = -387344016760277192L;

    protected final float x;
    protected final float y;
    private final byte drawType;

    public PointElement(float x, float y, DrawType drawType) {
        this.x = x;
        this.y = y;
        this.drawType = drawType.getId();
    }

    public float getCenterX(){
        return x;
    }
    public float getCenterY(){
        return y;
    }
    @Override
    public void draw(GraphicsContext gfx) {
        DrawType.set(gfx, DrawType.getDrawTypes().get(drawType));
        gfx.beginPath();
        gfx.moveTo(x, y);
        gfx.lineTo(x, y);
        gfx.stroke();
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
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public String toString() {
        return "PointElement{" +
                "x=" + x +
                ", y=" + y +
                ", drawType=" + drawType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PointElement)) return false;
        PointElement that = (PointElement) o;
        return Float.compare(that.x, x) == 0 &&
                Float.compare(that.y, y) == 0 &&
                drawType == that.drawType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, drawType);
    }

    @Override
    public float getArea() {
        return getBounds().getArea();
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(x,y,x,y);
    }
}