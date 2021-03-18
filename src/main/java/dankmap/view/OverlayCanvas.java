package dankmap.view;

import dankmap.controller.Controller;
import dankmap.drawing.ZoomLevel;
import dankmap.util.StringUtil;
import dankmap.util.cartography.MapConstants;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.NonInvertibleTransformException;

public class OverlayCanvas extends BaseCanvas {

    private final int padding = 10;
    private final int gap = -15;

    private double repaintTime;
    private double updateTime;

    @Override
    public void initialize(ViewModel model) {
        super.initialize(model);
    }

    @Override
    public void repaint() {
        resetPaint();
        if (Controller.IS_DEBUG) {
            var offset = getHeight() - padding;

            gfx.fillText("Center:", padding, offset + gap * 10);
            gfx.fillText(String.format("lon: %.4f", innerBounds.getCenterX()), padding, offset + gap * 9);
            gfx.fillText(String.format("lat: %.4f", innerBounds.getCenterY()), padding, offset + gap * 8);

            var point = model.getCurrentMousePos();
            if (point != null) {
                gfx.fillText("Mouse:", 100, offset + gap * 10);
                gfx.fillText(String.format("lon: %.8f", point.getX()), 100, offset + gap * 9);
                gfx.fillText(String.format("lat: %.8f", point.getY()), 100, offset + gap * 8);
            }

            int mapCount = model.getMapElements().size();
            int roadCount = model.getRoads().size();
            gfx.fillText(ZoomLevel.getCurrent().toString(), padding, offset + gap * 6);
            gfx.fillText(String.format("Update Time: %.0fns", updateTime), padding, offset + gap * 5);
            gfx.fillText(String.format("Repaint Time: %.3fms", repaintTime), padding, offset + gap * 4);
            gfx.fillText(String.format("Time/Element: %.3fns", repaintTime / (mapCount + roadCount) * 1e6), padding, offset + gap * 3);
            gfx.fillText(String.format("MapElements: %d", mapCount), padding, offset + gap * 2);
            gfx.fillText(String.format("Roads: %d", roadCount), padding, offset + gap);
        }
        drawRuler();
        drawNearestRoad();
    }

    public void setUpdateTime(double updateTime) {
        this.updateTime = updateTime;
    }

    public void setRepaintTime(double repaintTime) {
        this.repaintTime = repaintTime;
    }

    private void resetPaint() {
        gfx.clearRect(0, 0, getWidth(), getHeight());
        gfx.setFill(darkColor);
        gfx.setStroke(darkColor);
        gfx.setFont(Font.font("Francois One", 14));
        gfx.setTextAlign(TextAlignment.LEFT);
        gfx.setLineWidth(1);
    }

    private void drawRuler() {
        double rulerWidth = ZoomLevel.getCurrent().getRulerRatio();
        gfx.setTextAlign(TextAlignment.RIGHT);
        gfx.setLineWidth(2);
        var x = getWidth() - padding;
        var y = getHeight() - padding;
        gfx.fillText(StringUtil.kilometersToString((float) rulerWidth, 0), x - 4, y - 4);

        try {
            var p = transform.inverseTransform(x, y);
            var length = rulerWidth / MapConstants.LON_DEGREE_LENGTH;

            var newX = p.getX() - length;
            var newY = p.getY();
            var newP = transform.transform(newX, newY);
            gfx.beginPath();
            gfx.moveTo(x, y);
            gfx.lineTo(newP.getX(), newP.getY());
            gfx.moveTo(newP.getX(), newP.getY());
            gfx.lineTo(newP.getX(), newP.getY() - 15);
            gfx.stroke();
        } catch (NonInvertibleTransformException e) {
            e.printStackTrace();
        }
    }

    private void drawNearestRoad() {
        var nearestRoad = model.getNearestRoad();
        if (nearestRoad == null) return;
        gfx.setFont(Font.font("Francois One", 20));
        gfx.setTextAlign(TextAlignment.CENTER);
        gfx.fillText(nearestRoad.getStreetName(), getWidth() / 2, getHeight() - padding);
    }
}
