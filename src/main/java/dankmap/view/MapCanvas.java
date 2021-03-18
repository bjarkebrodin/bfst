package dankmap.view;

import dankmap.drawing.DrawType;
import dankmap.drawing.ZoomLevel;
import dankmap.model.elements.IslandElement;
import dankmap.model.elements.MapElement;
import dankmap.navigation.Road;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

import java.util.Collection;

public class MapCanvas extends BaseCanvas {
    private final Affine reset = new Affine();

    @Override
    public void initialize(ViewModel viewModel) {
        super.initialize(viewModel);
        gfx.setFillRule(FillRule.EVEN_ODD);
    }

    public void repaint() {
        drawBackground();
        resetPaint();
        drawIslands();
        drawMapElements();
        drawRoads();
        requestFocus();
    }

    private void drawIslands() {
        DrawType.set(gfx, DrawType.getDrawTypes().get(IslandElement.drawType));
        for (var isle : model.getIslands()) {
            if (isle.getBounds().intersects(innerBounds)) {
                isle.draw(gfx);
            }
        }
    }

    private void drawBackground() {
        gfx.setTransform(reset);
        gfx.setFill(DrawType.getDrawTypes().get((byte) 0).getFillColor());
        gfx.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawRoads() {
        boolean shouldOutline = ZoomLevel.getCurrent().getId() >= 10;
        Collection<MapElement> roads = model.getRoads();
        if (shouldOutline) {
            for (MapElement road : roads) {
                ((Road) road).outline(gfx);
            }
        }
        for (MapElement road : roads) {
            ((Road) road).draw(gfx);
        }
    }

    private void drawMapElements() {
        model.getMapElements()
                .stream()
                .sorted(MapElement::compareByDrawOrder)
                .forEach(element -> element.draw(gfx));
    }

    private void resetPaint() {
        gfx.setTransform(transform);
        gfx.setLineWidth(1);
        gfx.setFill(Color.BLACK);
        gfx.setStroke(Color.BLACK);
        gfx.setFont(Font.font("Arial", 12));
        gfx.setTextAlign(TextAlignment.LEFT);
        gfx.setLineCap(StrokeLineCap.ROUND);
        gfx.setLineJoin(StrokeLineJoin.ROUND);
    }
}