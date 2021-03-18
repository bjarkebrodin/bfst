package dankmap.navigation;

import dankmap.model.Bounded;

public interface Route extends Bounded {
    void setVehicle(Vehicle vehicle);

    float getDistance();

    float getTravelTime();

    String getDescription();

    /**
     * @return a list of arrays representing the geographical
     * path of the current planned route, in the format : <br>
     * <p>
     * [ x0, y0, x1, y1, x2, y2, ... , xN, yN ] <br>
     * <p>
     * where N is the number of points on the path. The list is
     * ordered in travel direction from start to end.
     */
    float[][] getPaths();
}