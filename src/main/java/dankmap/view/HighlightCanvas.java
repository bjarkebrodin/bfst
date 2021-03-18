package dankmap.view;

import dankmap.controller.Controller;
import dankmap.drawing.ZoomLevel;
import dankmap.model.Address;
import dankmap.model.Location;
import dankmap.navigation.Route;
import dankmap.util.StringUtil;
import dankmap.util.cartography.MapConstants;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.atan2;
import static java.lang.Math.toDegrees;

public class HighlightCanvas extends BaseCanvas {

    private Image marker;
    private Image mapPin;
    private Point2D markerPoint;
    private Point2D measureFrom;
    private Point2D measureTo;

    private Bounds selectedBounds;

    private List<Point2D> currentRoutePoints;
    private Route currentRoute;

    private Point2D[] pointsOfInterest;

    private Point2D[] nearestRoad;
    private Address nearestAddress;

    public void initialize(ViewModel model) {
        super.initialize(model);
        marker = new Image(getClass().getClassLoader().getResourceAsStream("media/icons/pin.png"));
        mapPin = new Image(getClass().getClassLoader().getResourceAsStream("media/icons/map_pin.png"));
    }

    private void resetPaint() {
        gfx.clearRect(0, 0, getWidth(), getHeight());
        gfx.setFill(darkColor);
        gfx.setStroke(darkColor);
        gfx.setLineJoin(StrokeLineJoin.ROUND);
        gfx.setLineCap(StrokeLineCap.ROUND);
        gfx.setFont(Font.font("Francois One", 14));
        gfx.setTextAlign(TextAlignment.CENTER);
        gfx.setLineWidth(1);
    }

    @Override
    public void repaint() {
        resetPaint();
        drawRoute();
        if (ZoomLevel.getCurrent().getId() >= 5) {
            drawPointsOfInterest();
        }
        drawMeasure();
        drawMarker();
        if (Controller.IS_DEBUG) {
            drawNearestRoad();
            drawNearestAddress();
        }
        drawSelectedBounds();
    }

    private void drawNearestRoad() {
        if (nearestRoad == null || nearestRoad.length < 2) return;

        gfx.beginPath();
        var radius = 5;
        gfx.fillOval(nearestRoad[0].getX() - radius, nearestRoad[0].getY() - radius, radius * 2, radius * 2);
        gfx.moveTo(nearestRoad[0].getX(), nearestRoad[0].getY());
        for (int i = 1; i < nearestRoad.length; i++) {
            gfx.fillOval(nearestRoad[i].getX() - radius, nearestRoad[i].getY() - radius, radius * 2, radius * 2);
            gfx.lineTo(nearestRoad[i].getX(), nearestRoad[i].getY());
        }

        gfx.setLineWidth(radius);
        gfx.setStroke(Color.DARKMAGENTA);
        gfx.stroke();

    }

    private void drawNearestAddress() {
        if (nearestAddress == null) return;

        var point = nearestAddress;
        var radius = 5;
        gfx.setFill(Color.PURPLE);
        gfx.fillOval(point.getX() - radius, point.getY() - radius, radius * 2, radius * 2);
        gfx.setFill(Color.BLACK);
        gfx.fillText(nearestAddress.getFormattedAddress(), point.getX() - radius, point.getY() - radius - 4);

    }

    private void drawMarker() {
        if (markerPoint == null) return;
        var radius = 25;
        var widthRatio = 0.647;
        gfx.drawImage(marker, markerPoint.getX() - (radius * widthRatio / 2), markerPoint.getY() - radius, radius * widthRatio, radius);

    }

    private void drawPointsOfInterest() {
        for (var point : pointsOfInterest) {
            var width = 18;
            var height = width * 1.6f;
            gfx.drawImage(mapPin, point.getX() - width / 2f, point.getY() - height, width, height);
        }
    }

    private void drawRoute() {
        if (currentRoute == null || currentRoutePoints.isEmpty()) return;

        gfx.beginPath();
        gfx.moveTo(currentRoutePoints.get(0).getX(), currentRoutePoints.get(0).getY());

        for (int i = 1; i < currentRoutePoints.size(); i++) {
            var point = currentRoutePoints.get(i);
            gfx.lineTo(point.getX(), point.getY());
        }
        gfx.setLineWidth(5);
        gfx.setStroke(darkColor);
        gfx.stroke();

        //// Endpoints

        gfx.setLineWidth(3);

        Point2D startPoint = currentRoutePoints.get(0);
        Point2D endPoint = currentRoutePoints.get(currentRoutePoints.size() - 1);

        var startRadius = 5;
        gfx.setFill(Color.WHITE);
        gfx.fillOval(startPoint.getX() - startRadius, startPoint.getY() - startRadius, startRadius * 2, startRadius * 2);
        gfx.strokeOval(startPoint.getX() - startRadius, startPoint.getY() - startRadius, startRadius * 2, startRadius * 2);

        var endRadius = 8;

        gfx.fillOval(endPoint.getX() - endRadius, endPoint.getY() - endRadius, endRadius * 2, endRadius * 2);
        gfx.strokeOval(endPoint.getX() - endRadius, endPoint.getY() - endRadius, endRadius * 2, endRadius * 2);
        gfx.setFill(darkColor);
        endRadius = 3;
        gfx.fillOval(endPoint.getX() - endRadius, endPoint.getY() - endRadius, endRadius * 2, endRadius * 2);

        //// Info Box
        var boxHeight = 30;
        var boxWidth = 80;
        var leftOffset = 10;
        var bottomOffset = 10;
        gfx.setFill(Color.rgb(255, 255, 255, 0.8));
        gfx.setStroke(darkColor);
        gfx.setTextAlign(TextAlignment.CENTER);
        gfx.setLineWidth(1);
        gfx.fillRect(endPoint.getX() + leftOffset, endPoint.getY() - boxHeight - bottomOffset, boxWidth, boxHeight);
        gfx.strokeRect(endPoint.getX() + leftOffset, endPoint.getY() - boxHeight - bottomOffset, boxWidth, boxHeight);

        var cx = endPoint.getX() + leftOffset + (boxWidth / 2);
        var cy = endPoint.getY() - boxHeight - bottomOffset + (boxHeight / 2);
        gfx.setFill(darkColor);
        gfx.fillText(StringUtil.kilometersToString(currentRoute.getDistance()), cx, cy - 2);
        gfx.fillText(StringUtil.hoursToString(currentRoute.getTravelTime()), cx, cy + 11);
    }

    private void drawSelectedBounds() {
        if (selectedBounds == null) return;
        gfx.setStroke(darkColor);
        gfx.setLineWidth(1);
        gfx.setFill(darkColor.deriveColor(1, 1, 1, 0.2));
        gfx.fillRect(selectedBounds.getMinX(), selectedBounds.getMinY(), selectedBounds.getWidth(), selectedBounds.getHeight());
        gfx.strokeRect(selectedBounds.getMinX(), selectedBounds.getMinY(), selectedBounds.getWidth(), selectedBounds.getHeight());

    }

    private void drawMeasure() {
        var radius = 5;
        gfx.setLineWidth(2);
        gfx.setStroke(darkColor);
        gfx.setFill(Color.WHITE);
        if (measureTo != null && measureFrom != null) {
            gfx.beginPath();
            gfx.moveTo(measureTo.getX(), measureTo.getY());
            gfx.lineTo(measureFrom.getX(), measureFrom.getY());
            gfx.stroke();

            drawDistance();
        }

        if (measureFrom != null) {
            gfx.fillOval(measureFrom.getX() - radius, measureFrom.getY() - radius, radius * 2, radius * 2);
            gfx.strokeOval(measureFrom.getX() - radius, measureFrom.getY() - radius, radius * 2, radius * 2);
        }
        if (measureTo != null) {
            gfx.fillOval(measureTo.getX() - radius, measureTo.getY() - radius, radius * 2, radius * 2);
            gfx.strokeOval(measureTo.getX() - radius, measureTo.getY() - radius, radius * 2, radius * 2);
        }
    }

    private void drawDistance() {
        var distance = MapConstants.sphericalDistance(model.getMeasureFromPos().getX(), model.getMeasureFromPos().getY(), model.getMeasureToPos().getX(), model.getMeasureToPos().getY());
        var cx = (measureFrom.getX() + measureTo.getX()) / 2;
        var cy = (measureFrom.getY() + measureTo.getY()) / 2;
        var angle = (getAngle(measureFrom, measureTo) + 180) % 180;
        angle = angle > 90 ? angle + 180 : angle;
        Rotate r = new Rotate(angle, cx, cy);
        gfx.save();
        gfx.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(), r.getTx(), r.getTy());
        gfx.setTextAlign(TextAlignment.CENTER);
        gfx.setFill(darkColor);
        gfx.fillText(StringUtil.kilometersToString((float) distance), cx, cy - 4);
        gfx.restore();
    }

    private double getAngle(Point2D from, Point2D to) {
        return toDegrees(atan2(to.getY() - from.getY(), to.getX() - from.getX()));
    }

    @Override
    public void update() {
        super.update();
        getMarkerPosition();
        getMeasurePositions();
        getSelectedBounds();
        getRoute();
        getPointsOfInterest();
        getNearestRoad();
        getNearestAddress();
    }

    private void getSelectedBounds() {
        var bounds = model.getSelectedBounds();
        if (bounds != null) {
            var p1 = transform.transform(new Point2D(bounds.getMinX(), bounds.getMinY()));
            var p2 = transform.transform(new Point2D(bounds.getMaxX(), bounds.getMaxY()));
            selectedBounds = new BoundingBox(p1.getX(), p1.getY(), p2.getX() - p1.getX(), p2.getY() - p1.getY());
        } else {
            selectedBounds = null;
        }
    }

    private void getRoute() {
        currentRoute = model.getCurrentRoute();
        if (currentRoute != null) {
            currentRoutePoints = new ArrayList<>();
            float[][] paths = currentRoute.getPaths();

            float[] currentPath;
            for (int i = 0; i < paths.length; i++) {
                currentPath = paths[i];
                for (int j = 0; j < currentPath.length; j += 2) {
                    currentRoutePoints.add(transform.transform(currentPath[j], currentPath[j + 1]));
                }
            }
        }
    }

    private void getMarkerPosition() {
        var pos = model.getPrimaryReleasedPos();
        if (pos != null) {
            markerPoint = transform.transform(pos);
        } else {
            markerPoint = null;
        }
    }


    public void getMeasurePositions() {
        var from = model.getMeasureFromPos();
        if (from != null) {
            measureFrom = transform.transform(from);
        } else {
            measureFrom = null;
        }
        var to = model.getMeasureToPos();
        if (to != null) {
            measureTo = transform.transform(to);
        } else {
            measureTo = null;
        }
    }

    public void getPointsOfInterest() {
        var pois = model.getPointsOfInterest();
        pointsOfInterest = new Point2D[pois.size()];
        Location currentLocation = null;
        for (int i = 0; i < pois.size(); i++) {
            currentLocation = pois.get(i);
            if (currentLocation == null) continue;
            pointsOfInterest[i] = transform.transform(currentLocation.getX(), currentLocation.getY());
        }
    }

    public void getNearestRoad() {
        var road = model.getNearestRoad();
        if (road != null) {
            float[] path = road.getPath();
            nearestRoad = new Point2D[path.length / 2];
            for (int i = 0, j = 0; i < path.length; i += 2, j++) {
                nearestRoad[j] = transform.transform(path[i], path[i + 1]);
            }
        } else {
            nearestRoad = null;
        }
    }

    public void getNearestAddress() {
        Address address = model.getNearestAddress();
        if (address != null) {
            Point2D pos = transform.transform(address.getX(), address.getY());
            nearestAddress = new Address(pos.getX(), pos.getY(), address.getAddress());
        } else {
            nearestAddress = null;
        }
    }
}