package dankmap.util.cartography;

import dankmap.model.XYSupplier;

import java.io.Serializable;

import static dankmap.util.cartography.CoordinateConversion.TO_LAT;
import static dankmap.util.cartography.CoordinateConversion.TO_LON;
import static java.lang.Math.*;

public class MapConstants implements Serializable {
    private static final long serialVersionUID = -4022284441451044867L;

    public static double lonCenter = 0;
    public static double latCenter = 0;

    public static final double RADIUS_EQUATORIAL = 6378.137; // km (https://nssdc.gsfc.nasa.gov/planetary/factsheet/earthfact.html)
    public static final double CIRCUMFERENCE_EQUATORIAL = RADIUS_EQUATORIAL * 2 * PI; //km (https://en.wikipedia.org/wiki/Decimal_degrees)
    public static final double LON_DEGREE_LENGTH = CIRCUMFERENCE_EQUATORIAL / 360.0;

    public static void setLatCenter(double latCenter) {
        MapConstants.latCenter = latCenter;
    }

    public static void setLonCenter(double lonCenter) {
        MapConstants.lonCenter = lonCenter;
    }

    public static double getLatCenter() {
        return latCenter;
    }

    public static double getLonCenter() {
        return lonCenter;
    }

    /**
     * Applying the haversine formula as described at :
     * https://en.wikipedia.org/wiki/Haversine_formula //todo source check
     *
     * @return the approximate distance from (lon0,lat0) to (lon1,lat1)
     */
    public static double haversine(double lon0, double lat0, double lon1, double lat1) {
        double delta_lambda = toRadians(lon1 - lon0);
        double delta_phi = toRadians(lat1 - lat0);
        double phi_0 = toRadians(lat0);
        double phi_1 = toRadians(lat1);
        double h = (1 - cos(delta_phi)) / 2 + (cos(phi_0) * cos(phi_1) * ((1 - cos(delta_lambda)) / 2));
        return 2 * RADIUS_EQUATORIAL * asin(sqrt(h));
    }

    public static double sphericalDistance(double x0, double y0, double x1, double y1) {
        return haversine(TO_LON.convertDouble(x0), TO_LAT.convertDouble(y0), TO_LON.convertDouble(x1), TO_LAT.convertDouble(y1));
    }

    public static float sphericalDistance(float lon0, float lat0, float lon1, float lat1) {
        return (float) haversine(lon0, lat0, lon1, lat1);
    }

    public static <T extends XYSupplier> float sphericalDistance(T from, T to) {
        return (float) haversine(from.getX(), from.getY(), to.getX(), to.getY());
    }

}
