package dankmap.view;

import dankmap.model.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

public abstract class BaseCanvas extends Canvas {
    protected ViewModel model;
    protected Bounds outerBounds;
    protected Bounds innerBounds;

    protected Affine transform;
    protected TransformWrapper transformWrapper;

    protected GraphicsContext gfx;

    protected final Color darkColor = Color.rgb(29, 29, 29);

    public void initialize(ViewModel model) {
        setManaged(false);
        this.model = model;
        outerBounds = model.getOuterBounds();
        innerBounds = model.getTransformWrapper().getInnerBounds();
        transform = model.getTransform();
        transformWrapper = model.getTransformWrapper();
        gfx = getGraphicsContext2D();
        setManaged(false);
    }

    public abstract void repaint();

    public void update() {
        setSize();
        innerBounds = model.getTransformWrapper().getInnerBounds();
    }

    protected void setSize() {
        setWidth(model.getWidth());
        setHeight(model.getHeight());
    }

}
