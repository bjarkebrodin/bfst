package dankmap.util;

import org.junit.Test;
import static dankmap.util.cartography.MapConstants.*;
import static org.junit.Assert.assertEquals;

public class MapConstantsTest {
    @Test
    public void testHaversine() {
        //Tests built with help from http://edwilliams.org/gccalc.htm todo find better and more numbers
        float[][] cases = new float[][]{
                new float[]{0,0,10,10,1567},
                new float[]{5,5,5.01f,5.01f,1.566105841953617f},
                new float[]{12.582628f,55.67673f,12.582982f,55.67684f,0.025416234743957346f}
        };
        for(var c : cases) assertEquals(c[4],haversine(c[0],c[1],c[2],c[3]),0.005*c[4]);
    }
}