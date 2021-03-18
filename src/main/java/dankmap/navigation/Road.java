package dankmap.navigation;

import dankmap.drawing.DrawType;
import dankmap.model.Location;
import dankmap.model.elements.PathElement;
import javafx.scene.canvas.GraphicsContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.NoSuchElementException;

public class Road extends PathElement implements Serializable {
    private static final long serialVersionUID = 6444884680868364360L;
    private String streetName;
    private short speedLimit;

    /* A binary sequence representing the vehicles allowed
     * to traverse this road in the forward direction,
     * where forward is defined by the direction of the
     * path starting from (x0,y0).
     * The three least significant bits are interpreted as
     * pedestrian, bike, motor - thus if the least bit is
     * 1, the road can be traversed by motor vehicle in the
     * forward direction. */
    private byte vehicleForward;

    /* As vehicleForward, but opposite direction */
    private byte vehicleBackward;

    public Road(float[] path, DrawType drawType, String streetName, short speedLimit, int vehicleForward, int vehicleBackward) {
        super(path, drawType);
        this.streetName = streetName;
        this.speedLimit = speedLimit;
        this.vehicleForward = (byte) vehicleForward;
        this.vehicleBackward = (byte) vehicleBackward;

       /* System.out.println(Integer.toBinaryString(vehicleForward) +" "+ Integer.toBinaryString(this.vehicleForward));
        System.out.println(Integer.toBinaryString(vehicleBackward) +" "+ Integer.toBinaryString(this.vehicleBackward));*/
    }

    public void outline(GraphicsContext gfx) {
        super.outline(gfx);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public String getStreetName() {
        return streetName;
    }

    public short getSpeedLimit() {
        return speedLimit;
    }

    public float[] getPath() {
        return path;
    }

    public boolean isOneway() {
        return ((vehicleBackward | 0b110) & 0b111) == 0b000;
    }

    private boolean isVehicleAllowed(byte permissionBits, Vehicle vehicle) {
        switch (vehicle) {
            case MOTOR:
                return ((permissionBits | 0b110) & 0b111) == 0b111;
            case BIKE:
                return ((permissionBits | 0b101) & 0b111) == 0b111;
            case PEDESTRIAN:
                return ((permissionBits | 0b011) & 0b111) == 0b111;
        }
        throw new NoSuchElementException("No such vehicle");
    }

    /**
     * @param start starting index of sub-path X coordinate, inclusive
     * @param end   terminal index of sub-path X coordinate, inclusive
     * @return a sub-path of the path of this road,
     * represented as an array of floats, where each point
     * takes up two indices: [ x0, y0, x1, y1, ... , xn, yn ].
     * If <code>start > end</code> a reversed array will be
     * returned, affording "backwards" sub-path functionality.
     */
    float[] subPath(int start, int end) {
        if (start == end)
            throw new IllegalArgumentException("0 length subPath");

        if (start > end)
            return reverse(subPath(end, start));

        return Arrays.copyOfRange(path, start, end + 2);
    }

    float[] subPath(Location start, Location end) {
        // TODO: 09/04/2020
        return null;
    }

    public boolean isVehicleForward(Vehicle vehicle) {
        return isVehicleAllowed(vehicleForward, vehicle);
    }

    public boolean isVehicleBackward(Vehicle vehicle) {
        return isVehicleAllowed(vehicleBackward, vehicle);
    }

    public boolean isRoundabout() {
        return ((0b0111 | vehicleBackward) & 0b1111) == 0b1111;
    }

    private static float[] reverse(float[] arr) {
        float[] reversed = new float[arr.length];
        for (int c = arr.length; c > 1; c -= 2) {
            reversed[arr.length - c] = arr[c - 2];
            reversed[arr.length - c + 1] = arr[c - 1];
        }
        return reversed;
    }
}