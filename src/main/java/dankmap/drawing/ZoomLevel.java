package dankmap.drawing;

public enum ZoomLevel {
    // smaller scale = more zoomed out
    ZOOM_LEVEL_0(0, 100, 200.0, 5.0, 9e-4f),
    ZOOM_LEVEL_1(1, 150, 100.0, 3.0, 7e-4f),
    ZOOM_LEVEL_2(2, 400, 50.0, 1.2, 5e-4f),
    ZOOM_LEVEL_3(3, 700, 20.0, 0.7, 1e-4f),
    ZOOM_LEVEL_4(4, 1500, 10.0, 0.3, 5e-5f),
    ZOOM_LEVEL_5(5, 3000, 5.0, 0.15, 2e-5f),
    ZOOM_LEVEL_6(6, 6500, 2.0, 0.06, 2e-6f),
    ZOOM_LEVEL_7(7, 15000, 1.0, 0.03, 1e-7f),
    ZOOM_LEVEL_8(8, 35000, 0.5, 0.008, 1.5e-8f),
    ZOOM_LEVEL_9(9, 65000, 0.2, 0.008, 5e-9f),
    ZOOM_LEVEL_10(10, 120000, 0.1, 0.008, 1e-11f),
    ZOOM_LEVEL_11(11, 240000, 0.05, 0.008, 1e-12f),
    ZOOM_LEVEL_12(12, 360000, 0.02, 0.008, 1e-12f);


    public final static ZoomLevel ZOOM_LEVEL_MIN = ZOOM_LEVEL_0;
    public final static ZoomLevel ZOOM_LEVEL_MAX = ZOOM_LEVEL_12;

    private static ZoomLevel current = ZOOM_LEVEL_MIN;

    private int id;
    private double scale;
    private double rulerRatio;
    private double panBuffer;
    private float minArea;

    ZoomLevel(int id, double scale, double rulerRatio, double panBuffer, float minArea) {
        this.id = id;
        this.scale = scale;
        this.rulerRatio = rulerRatio;
        this.panBuffer = panBuffer;
        this.minArea = minArea;
    }

    public static ZoomLevel getCurrent() {
        return current;
    }

    public static void setCurrent(ZoomLevel zoomLevel) {
        current = zoomLevel;
    }

    public static void setByLevel(int id) {
        for (ZoomLevel zoomLevel : values()) {
            if (zoomLevel.id == id) {
                current = zoomLevel;
                return;
            }
        }
        current = ZOOM_LEVEL_MAX;
    }

    public static void setToNearestByScale(double scale) {
        for (int i = 1; i < values().length; i++) {
            if (scale < values()[i].scale) {
                current = values()[i - 1];
                return;
            }
        }
        current = ZOOM_LEVEL_MAX;
    }

    public static ZoomLevel getLevelByArea(double area) {
        for (int i = 0; i < values().length; i++) {
            var zoomLevel = values()[i];
            if (area > zoomLevel.minArea) return zoomLevel;
        }
        return ZOOM_LEVEL_MAX;
    }

    public static void zoomIn() {
        if (current.id == ZOOM_LEVEL_MAX.id) return;
        setByLevel(current.id + 1);
    }

    public static void zoomOut() {
        if (current.id == ZOOM_LEVEL_MIN.id) return;
        setByLevel(current.id - 1);
    }

    public int getId() {
        return id;
    }

    public double getScale() {
        return scale;
    }

    public double getRulerRatio() {
        return rulerRatio;
    }

    public double getPanBuffer() {
        return panBuffer;
    }

    public float getMinArea() {
        return minArea;
    }

    @Override
    public String toString() {
        return "ZoomLevel{" +
                "id=" + id +
                ", scale=" + scale +
                ", rulerRatio=" + rulerRatio +
                ", panBuffer=" + panBuffer +
                ", minArea=" + minArea +
                '}';
    }
}
