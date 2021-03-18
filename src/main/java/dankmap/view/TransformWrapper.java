package dankmap.view;

import dankmap.drawing.ZoomLevel;
import dankmap.model.Bounds;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to get more control over the Affine transform
 * used by the canvases. It's responsible for Transforms, ZoomLevels, PanBuffer,
 * Transform Animations, and minimizing repaints and updates.
 */
public class TransformWrapper {
    private final Affine transform;

    // Observers
    private final List<Runnable> onBoundsChanged;
    private final List<Runnable> onTransformChanged;

    // Metrics
    private Bounds innerBounds;

    private double height;
    private double width;

    public TransformWrapper(Affine transform, Bounds innerBounds, double width, double height) {
        this.transform = transform;
        this.innerBounds = innerBounds;
        this.width = width;
        this.height = height;
        this.onBoundsChanged = new ArrayList<>();
        this.onTransformChanged = new ArrayList<>();
    }

    /**
     * The width and height of the screen is used to handle
     * current innerBounds.
     */
    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
        updateInnerBounds(transform, true);
        notifyOnTransformChanged();
    }

    // PUBLIC ANIMATIONS

    public void animScaleAround(double scale, double x, double y) {
        var preTransform = transform.clone();
        scale(scale, x, y, preTransform);
        animTransform(preTransform);
    }

    public void animScaleToPoint(double scale, double x, double y) {
        var preTransform = transform.clone();
        scaleTo(scale, x, y, preTransform);
        animTransform(preTransform);
    }

    public void animScaleToBounds(double scale, Bounds bounds) {
        animScaleToPoint(scale, bounds.getCenterX(), bounds.getCenterY());
    }

    // PUBLIC TRANSFORMATIONS

    /**
     * This method scales the transform towards a pivot and notifies listeners.
     */
    public void scaleAround(double scale, double x, double y) {
        scale(scale, x, y, transform);
        notifyOnTransformChanged();
    }

    /**
     * This method scales and translates the transform to a desired
     * center point (x,y) and notifies listeners.
     */
    public void scaleToPoint(double scale, double x, double y) {
        scaleTo(scale, x, y, transform);
        notifyOnTransformChanged();
    }

    /**
     * This method scales and translates the transform to a desired
     * Bounds and notifies listeners.
     */
    public void scaleToBounds(double scale, Bounds bounds) {
        scaleToPoint(scale, bounds.getCenterX(), bounds.getCenterY());
    }

    /**
     * This method translates the transform by some (dx,dy), then
     * updates innerBounds and notifies listeners.
     */
    public void translate(double dx, double dy) {
        setTransform(transform.getTx() + dx, transform.getTy() + dy);
        updateInnerBounds(transform, false);
        notifyOnTransformChanged();
    }

    /**
     * This method translates the transform to a desired center point (x,y),
     * then updates innerBounds and notifies listeners.
     */
    public void translateTo(double x, double y) {
        try {
            setTransform(x * transform.getMxx(), y * transform.getMyy());

            var p = transform.inverseTransform(width / 2.0, height / 2.0);
            setTransform(p.getX() * transform.getMxx(), p.getY() * transform.getMyy());

            updateInnerBounds(transform, true);
            notifyOnTransformChanged();
        } catch (NonInvertibleTransformException e) {
            e.printStackTrace();
        }
    }

    // PRIVATE TRANSFORMATIONS

    /**
     * This method scales a transform towards a pivot (x,y)
     * then updates innerBounds
     */
    private void scale(double scale, double x, double y, Affine transform) {
        var scaleRatio = scale / transform.getMxx();
        var dx = x * transform.getMxx() + transform.getTx();
        var dy = y * transform.getMxx() + transform.getTy();

        var tx = (((transform.getTx() - dx) * scaleRatio) + dx);
        var ty = (((transform.getTy() - dy) * scaleRatio) + dy);
        setTransform(scale, tx, ty, transform);

        updateInnerBounds(transform, true);
    }

    /**
     * This method scales a transform to a center point (x,y)
     * then updates innerBounds
     */
    private void scaleTo(double scale, double x, double y, Affine transform) {
        try {
            setTransform(scale, x * scale, y * scale, transform);

            var p = transform.inverseTransform(width / 2.0, height / 2.0);

            setTransform(p.getX() * scale, p.getY() * scale, transform);

            updateInnerBounds(transform, true);
        } catch (NonInvertibleTransformException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is used to animate the transform
     * to another desired transform
     */
    private void animTransform(Affine to) {
        var mxx = transform.getMxx();
        var myy = transform.getMyy();
        var tx = transform.getTx();
        var ty = transform.getTy();
        var dMxx = to.getMxx() - mxx;
        var dMyy = to.getMyy() - myy;
        var dTx = to.getTx() - tx;
        var dTy = to.getTy() - ty;
        final Animation animation = new Transition() {
            {
                setCycleDuration(Duration.millis(300));
                setInterpolator(Interpolator.EASE_OUT);
            }

            @Override
            protected void interpolate(double frac) {
                setTransform(mxx + (dMxx * frac), myy + (dMyy * frac), tx + (dTx * frac), ty + (dTy * frac));
                notifyOnTransformChanged();
            }
        };
        animation.play();

    }

    /**
     * This method is used to update the current innerBounds.
     * This is done by creating a Bounds of the transformed
     * screen coordinates (0,0) and (width,height).
     */
    private void updateInnerBounds(Affine transform, boolean overrideBuffer) {
        try {
            var p1 = transform.inverseTransform(0, 0);
            var dx = p1.getX() - innerBounds.getMinX();
            var dy = p1.getY() - innerBounds.getMinY();
            var panBuffer = ZoomLevel.getCurrent().getPanBuffer();
            if (overrideBuffer || dx >= 2 * panBuffer || dx < 0 || dy >= 2 * panBuffer || dy < 0) {
                var p2 = transform.inverseTransform(width, height);
                innerBounds = new Bounds(p1.getX() - panBuffer, p1.getY() - panBuffer, p2.getX() + panBuffer, p2.getY() + panBuffer);
                notifyOnBoundsChanged();
            }
        } catch (NonInvertibleTransformException e) {
            e.printStackTrace();
        }
    }


    // Transform Setters
    private void setTransform(double tx, double ty) {
        setTransform(transform.getMxx(), tx, ty);
    }

    private void setTransform(double tx, double ty, Affine transform) {
        setTransform(transform.getMxx(), tx, ty, transform);
    }

    private void setTransform(double scale, double tx, double ty) {
        setTransform(scale, scale, tx, ty);
    }

    private void setTransform(double scale, double tx, double ty, Affine transform) {
        setTransform(scale, scale, tx, ty, transform);
    }

    private void setTransform(double mxx, double myy, double tx, double ty) {
        setTransform(mxx, myy, tx, ty, transform);
    }

    /**
     * These setters are used inorder to minimize set calls on the transform.
     * It is more efficient to set all variables once, than doing:
     * transform.setMxx(mxx);
     * transform.set ....
     */
    private void setTransform(double mxx, double myy, double tx, double ty, Affine transform) {
        transform.setToTransform(mxx, transform.getMxy(), tx, transform.getMyx(), myy, ty);
    }

    ////////// Getters ///////////
    public Bounds getInnerBounds() {
        return innerBounds;
    }

    ////////// Observers ////////

    public void addOnBoundsChangedListener(Runnable listener) {
        onBoundsChanged.add(listener);
    }

    public void addOnTransformChangedListener(Runnable listener) {
        onTransformChanged.add(listener);
    }

    private void notifyOnBoundsChanged() {
        onBoundsChanged.forEach(Runnable::run);
    }

    public void notifyOnTransformChanged() {
        onTransformChanged.forEach(Runnable::run);
    }
}
