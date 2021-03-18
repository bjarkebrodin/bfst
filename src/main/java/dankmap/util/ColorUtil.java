package dankmap.util;

import javafx.scene.paint.Color;

import java.util.Random;

public class ColorUtil {

    public static Color hexToColor(String hex) {
        hex = hex.replace("#", "");
        hex = hex.replace("0x", "");
        switch (hex.length()) {
            case 6:
                return Color.rgb(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16));
            case 8:
                return Color.rgb(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16),
                        (Integer.valueOf(hex.substring(6, 8), 16) / 256f));
        }
        throw new IllegalArgumentException();
    }

    public static Color randomRGB() {
        Random obj = new Random();
        int rand_num = obj.nextInt(0xffffff + 1);
        String colorCode = String.format("#%06x", rand_num);
        return hexToColor(colorCode);
    }

    public static Color saturatedColor(Color color) {
        return color.saturate().saturate().saturate().saturate().saturate();
    }

    public static Color saturatedColor(String hex) {
        return saturatedColor(hexToColor(hex));
    }

    public static Color grayScale(Color color) {
        return color.saturate().grayscale();
    }

    public static Color grayScale(String hex) {
        return grayScale(hexToColor(hex));
    }

    public static Color darkMode(Color color) {
        return color.desaturate().invert().saturate().brighter();
    }

    public static Color darkMode(String hex) {
        return darkMode(hexToColor(hex));
    }

}
