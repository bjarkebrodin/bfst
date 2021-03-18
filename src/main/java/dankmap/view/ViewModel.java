package dankmap.view;

import dankmap.drawing.ZoomLevel;
import dankmap.model.*;
import dankmap.model.elements.IslandElement;
import dankmap.model.elements.MapElement;
import dankmap.navigation.Road;
import dankmap.navigation.Route;
import javafx.geometry.Point2D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class ViewModel {
    private final DataModel dataModel;

    // Observers
    private final List<Runnable> onMapUpdate;
    private final List<Runnable> onHighlightUpdate;
    private final List<Runnable> onInputUpdate;

    // Transform
    private TransformWrapper transformWrapper;
    private Affine transform;

    // Temporary data
    private Collection<MapElement> roads;
    private Collection<IslandElement> islands;
    private Collection<MapElement> mapElements;
    private Route currentRoute;
    private Road nearestRoad;
    private Address nearestAddress;
    private List<PointOfInterest> pointsOfInterest;

    // View metrics
    private double width, height;
    private Bounds outerBounds;
    private Bounds selectedBounds;

    // Mouse positional information
    private Point2D primaryReleasedPos;
    private Point2D secondaryReleasedPos;
    private Point2D currentClickedPos;
    private Point2D currentMousePos;

    // Measure position information
    private Point2D measureFromPos;
    private Point2D measureToPos;


    public ViewModel(DataModel dataModel, double width, double height) {
        this.dataModel = dataModel;
        this.width = width;
        this.height = height;

        onMapUpdate = new ArrayList<>();
        onHighlightUpdate = new ArrayList<>();
        onInputUpdate = new ArrayList<>();
        pointsOfInterest = new ArrayList<>();

        initialize();
    }

    public void initialize() {
        dataModel.addOnDataUpdateListener(this::onDataModelUpdate);
        outerBounds = dataModel.getBounds();
        islands = dataModel.getIslands();

        transform = new Affine();
        transformWrapper = new TransformWrapper(transform, outerBounds, width, height);
        transformWrapper.addOnBoundsChangedListener(this::onTransformWrapperChanged);
        transformWrapper.addOnTransformChangedListener(this::onTransformChanged);
        fitToBounds(outerBounds, false);
    }

    public void updateElements() {
        roads = dataModel.getRoads(transformWrapper.getInnerBounds());
        mapElements = dataModel.getMapElements(transformWrapper.getInnerBounds());
    }

    //////////// Route and address? ///////////

    public void jumpToPoint(XYSupplier point) {
        if (point == null) return;
        primaryReleasedPos = new Point2D(point.getX(), point.getY());
        transformWrapper.translateTo(primaryReleasedPos.getX(), primaryReleasedPos.getY());
    }

    public void animToPoint(XYSupplier point) {
        if (point == null) return;

        if (ZoomLevel.getCurrent().getId() < 7)
            ZoomLevel.setCurrent(ZoomLevel.ZOOM_LEVEL_7);
        transformWrapper.animScaleToPoint(ZoomLevel.getCurrent().getScale(), point.getX(), point.getY());
    }

    public void setCurrentRoute(Route route) {
        currentRoute = route;
        if (route != null) {
            fitToBounds(currentRoute.getBounds(), true);
        }
    }


    //////////// Transform ////////////

    private void zoom(Point2D pivot) {
        try {
            Point2D point = transform.inverseTransform(pivot);
            transformWrapper.animScaleAround(ZoomLevel.getCurrent().getScale(), point.getX(), point.getY());
        } catch (NonInvertibleTransformException e) {
            e.printStackTrace();
        }
    }

    public void zoomIn(Point2D pivot) {
        ZoomLevel.zoomIn();
        zoom(pivot);
    }

    public void zoomOut(Point2D pivot) {
        ZoomLevel.zoomOut();
        zoom(pivot);
    }

    public void pan(double dx, double dy) {
        transformWrapper.translate(dx, dy);
    }

    public void fitToBounds(Bounds bounds, boolean animate) {
        double scale = Math.min(width / bounds.getWidth(), height / bounds.getHeight());
        ZoomLevel.setToNearestByScale(scale);
        if (animate) {
            transformWrapper.animScaleToBounds(ZoomLevel.getCurrent().getScale(), bounds);
        } else {
            transformWrapper.scaleToBounds(ZoomLevel.getCurrent().getScale(), bounds);
        }
    }

    public void fitToSelectedBounds() {
        fitToBounds(selectedBounds, true);
    }

    ///////////// Measure ////////////

    public void setMeasureFrom() {
        measureFromPos = secondaryReleasedPos;
        notifyOnHighlightUpdate();
    }

    public void setMeasureTo() {
        measureToPos = secondaryReleasedPos;
        notifyOnHighlightUpdate();
    }

    public void clearMeasure() {
        measureFromPos = null;
        measureToPos = null;
        notifyOnHighlightUpdate();
    }


    ////////// Setters /////////////

    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
        transformWrapper.setSize(width, height);
    }

    public void setCurrentMovedPos(double x, double y) {
        try {
            currentMousePos = transform.inverseTransform(x, y);
            if (ZoomLevel.getCurrent().getId() >= 9) {
                nearestRoad = dataModel.getNearestRoad(new Location(currentMousePos.getX(), currentMousePos.getY()));
                nearestAddress = dataModel.getNearestAddress(new Location(currentMousePos.getX(), currentMousePos.getY()));
            } else {
                nearestRoad = null;
                nearestAddress = null;
            }

            notifyOnHighlightUpdate();
        } catch (NonInvertibleTransformException e) {
            e.printStackTrace();
        }
    }

    public void setPrimaryReleasedPos(double x, double y) {
        try {
            setPrimaryReleasedPos(transform.inverseTransform(x, y));
        } catch (NonInvertibleTransformException e) {
            e.printStackTrace();
        }
    }

    public void setPrimaryReleasedPos(Point2D point) {
        primaryReleasedPos = point;
        notifyOnHighlightUpdate();
    }

    public void clearPrimaryReleasePos() {
        primaryReleasedPos = null;
        notifyOnHighlightUpdate();
    }

    public void setSecondaryReleasedPos(double x, double y) {
        try {
            secondaryReleasedPos = transform.inverseTransform(x, y);
            notifyOnHighlightUpdate();
        } catch (NonInvertibleTransformException e) {
            e.printStackTrace();
        }
    }

    public void setCurrentClickedPos(double x, double y) {
        try {
            currentClickedPos = transform.inverseTransform(x, y);
        } catch (NonInvertibleTransformException e) {
            e.printStackTrace();
        }
    }

    public void setCurrentDragPos(double x, double y) {
        try {
            var currentDragPos = transform.inverseTransform(x, y);
            var minX = min(currentClickedPos.getX(), currentDragPos.getX());
            var minY = min(currentClickedPos.getY(), currentDragPos.getY());
            var maxX = max(currentDragPos.getX(), currentClickedPos.getX());
            var maxY = max(currentDragPos.getY(), currentClickedPos.getY());
            selectedBounds = new Bounds(minX, minY, maxX, maxY);
            notifyOnHighlightUpdate();
        } catch (NonInvertibleTransformException e) {
            e.printStackTrace();
        }

    }

    public void clearCurrentDragPos() {
        selectedBounds = null;
        notifyOnHighlightUpdate();
    }


    ////////// Getters ////////////

    public Point2D getPrimaryReleasedPos() {
        return primaryReleasedPos;
    }

    public Point2D getSecondaryReleasedPos() {
        return secondaryReleasedPos;
    }

    public Point2D getCurrentMousePos() {
        return currentMousePos;
    }

    public Affine getTransform() {
        return transform;
    }

    public TransformWrapper getTransformWrapper() {
        return transformWrapper;
    }

    public Point2D getMeasureFromPos() {
        return measureFromPos;
    }

    public Point2D getMeasureToPos() {
        return measureToPos;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Bounds getOuterBounds() {
        return outerBounds;
    }

    public Collection<MapElement> getRoads() {
        return roads;
    }

    public Collection<IslandElement> getIslands() {
        return islands;
    }

    public Collection<MapElement> getMapElements() {
        return mapElements;
    }

    public Bounds getSelectedBounds() {
        return selectedBounds;
    }

    public Route getCurrentRoute() {
        return currentRoute;
    }

    public List<PointOfInterest> getPointsOfInterest() {
        return pointsOfInterest;
    }

    public Road getNearestRoad() {
        return nearestRoad;
    }

    public Address getNearestAddress() {
        return nearestAddress;
    }


    ////////// Listeners /////////

    public void addOnMapUpdateListener(Runnable listener) {
        onMapUpdate.add(listener);
    }

    public void addOnHighlightUpdateListener(Runnable listener) {
        onHighlightUpdate.add(listener);
    }

    public void addOnInputUpdateListener(Runnable listener) {
        onInputUpdate.add(listener);
    }

    private void notifyOnMapUpdate() {
        onMapUpdate.forEach(Runnable::run);
    }

    private void notifyOnHighlightUpdate() {
        onHighlightUpdate.forEach(Runnable::run);
    }

    private void notifyOnInputUpdate() {
        onInputUpdate.forEach(Runnable::run);
    }

    ////////// Observers ////////

    private void onDataModelUpdate() {
        pointsOfInterest = dataModel.getPointsOfInterest();
        notifyOnHighlightUpdate();
    }

    private void onTransformChanged() {
        notifyOnMapUpdate();
    }

    private void onTransformWrapperChanged() {
        updateElements();
    }

}