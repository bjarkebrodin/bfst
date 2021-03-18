package dankmap.util;

import dankmap.model.XYSupplier;

public class StringUtil {

    public static String capitalizeAllFirstLetters(String input) {
        String[] inputArr = input.split(" ");
        String[] outputArr = new String[inputArr.length];
        for (int i = 0; i < inputArr.length; i++) {
            String s = inputArr[i];

            if (s.length() > 1) {
                s = s.substring(0, 1).toUpperCase() + s.substring(1);
            }

            outputArr[i] = s;
        }
        return String.join(" ", outputArr);
    }

    public static String kilometersToString(float distance) {
        if (distance >= 1) {
            return String.format("%.2f km", distance);
        } else {
            return String.format("%.2f m", distance * 1000);
        }
    }


    public static String kilometersToString(float distance, int decimals) {
        String format = "%." + decimals + "f";
        if (distance >= 1) {
            format += " km";
            return String.format(format, distance);
        } else {
            format += " m";
            return String.format(format, distance * 1000);
        }
    }

    public static String hoursToString(float hours) {
        if (hours >= 1) {
            return String.format("%.0f h %.0f min", Math.floor(hours), (hours % 1) * 60);
        } else {
            return String.format("%.0f min", Math.ceil(hours * 60));
        }
    }

    // DMS = Degrees, Minutes, Seconds
    public static String longitudeToDMS(float longitude) {
        float lon = Math.abs(longitude);
        int degrees = (int) lon;
        float r = (lon - degrees) * 3600;
        int minutes = (int) (r / 60);
        float seconds = r - (60 * minutes);
        String direction = longitude < 0 ? "W" : "E";
        return String.format("%02d°%02d'%04.1f\"%s", degrees, minutes, seconds, direction);
    }

    public static String latitudeToDMS(float latitude) {
        float lat = Math.abs(latitude);
        int degrees = (int) lat;
        float r = (lat - degrees) * 3600;
        int minutes = (int) (r / 60);
        float seconds = r - (60 * minutes);
        String direction = latitude < 0 ? "S" : "N";
        return String.format("%02d°%02d'%04.1f\"%s", degrees, minutes, seconds, direction);
    }

    public static String locationToDMS(XYSupplier location) {
        return locationToDMS(location.getX(), location.getY());
    }

    public static String locationToDMS(float longitude, float latitude) {
        return String.format("%S %S", latitudeToDMS(latitude), longitudeToDMS(longitude));
    }

}
