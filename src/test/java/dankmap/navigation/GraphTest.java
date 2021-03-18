package dankmap.navigation;

import dankmap.drawing.DrawType;
import dankmap.model.Location;
import dankmap.model.XYSupplier;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GraphTest {
    private DrawType dt = DrawType.NONE;

    @Test
    public void dumbVehicleTest() {
        Graph.GraphBuilder g = new Graph.GraphBuilder();

        Location a, b, c;
        a = new Location(0, 0);
        b = new Location(1, 1);
        c = new Location(2, 2);

        float[] ap, bp;
        ap = new float[]{a.getX(), a.getY(), b.getX(), b.getY()};
        bp = new float[]{b.getX(), b.getY(), c.getX(), c.getY()};


        byte bb = 0b111;
        byte ab = 0b111;

        Road A, B;
        A = new Road(ap, dt, "Vej A", (short) 100, ab, ab);
        B = new Road(bp, dt, "Vej B", (short) 100, bb, bb);

        g.addConnection(a, b, A, 0, 2);
        g.addConnection(b, a, A, 2, 0);
        g.addConnection(b, c, B, 0, 2);
        g.addConnection(c, b, B, 2, 0);

        Route route = g.build().getRoute(Vehicle.MOTOR, a, c, false);
        assertNotNull(route);
    }


    @Test
    public void threePointVehicleTest() {
        Graph.GraphBuilder g = new Graph.GraphBuilder();

        Location a, b, c, d;
        a = new Location(0, 0);
        b = new Location(1, 1);
        c = new Location(2, 2);
        d = new Location(4, 4);


        float[] ap, bp;
        ap = new float[]{a.getX(), a.getY(), b.getX(), b.getY()};
        bp = new float[]{b.getX(), b.getY(), c.getX(), c.getY(), d.getX(), d.getY()};

        byte ab = 0b001;
        byte bb = 0b001;

        Road A, B;
        A = new Road(ap, dt, "Vej A", (short) 100, ab, ab);
        B = new Road(bp, dt, "Vej B", (short) 100, bb, bb);
        g.addConnection(a, b, A, 0, 2);
        g.addConnection(b, a, A, 2, 0);
        g.addConnection(b, c, B, 0, 2);
        g.addConnection(c, b, B, 2, 0);
        g.addConnection(c, d, B, 2, 4);
        g.addConnection(d, c, B, 4, 2);


        Route route = g.build().getRoute(Vehicle.MOTOR, a, d, false);
        assertNotNull(route);
    }

    @Test
    public void pedestrianTest() {
        Graph.GraphBuilder g = new Graph.GraphBuilder();

        Location a, b, c;
        a = new Location(0, 0);
        b = new Location(1, 1);
        c = new Location(2, 2);

        float[] ap, bp;
        ap = new float[]{a.getX(), a.getY(), b.getX(), b.getY()};
        bp = new float[]{b.getX(), b.getY(), c.getX(), c.getY()};

        byte ab = 0b100;
        byte bb = 0b100;

        Road A, B;
        A = new Road(ap, dt, "Vej A", (short) 100, ab, ab);
        B = new Road(bp, dt, "Vej B", (short) 100, bb, bb);

        g.addConnection(a, b, A, 0, 2);
        g.addConnection(b, a, A, 2, 0);
        g.addConnection(b, c, B, 0, 2);
        g.addConnection(c, b, B, 2, 0);

        Route route = g.build().getRoute(Vehicle.PEDESTRIAN, a, c, false);
        assertNotNull(route);
    }

    @Test //should show no possible road for walking
    public void pedestrianFailTest() {
        Graph.GraphBuilder g = new Graph.GraphBuilder();

        Location a, b, c;
        a = new Location(0, 0);
        b = new Location(1, 1);
        c = new Location(2, 2);

        float[] ap, bp;
        ap = new float[]{a.getX(), a.getY(), b.getX(), b.getY()};
        bp = new float[]{b.getX(), b.getY(), c.getX(), c.getY()};

        byte ab = 0b011;
        byte bb = 0b011;

        Road A, B;
        A = new Road(ap, dt, "Vej A", (short) 100, ab, ab);
        B = new Road(bp, dt, "Vej B", (short) 100, bb, bb);

        g.addConnection(a, b, A, 0, 2);
        g.addConnection(b, a, A, 2, 0);
        g.addConnection(b, c, B, 0, 2);
        g.addConnection(c, b, B, 2, 0);

        Route route = g.build().getRoute(Vehicle.PEDESTRIAN, a, c, false);
        assertNull(route);
    }

    @Test //should show no possible road for biking.
    public void bikeFailTest() {
        Graph.GraphBuilder g = new Graph.GraphBuilder();

        XYSupplier a, b, c;
        a = new Location(0,0);


        b = new Location(0,0);

        c = new Location(0,0);

        float[] ap, bp;
        ap = new float[]{a.getX(), a.getY(), b.getX(), b.getY()};
        bp = new float[]{b.getX(), b.getY(), c.getX(), c.getY()};

        byte ab = 0b101;
        byte bb = 0b101;

        Road A, B;
        A = new Road(ap, dt, "Vej A", (short) 100, ab, ab);
        B = new Road(bp, dt, "Vej B", (short) 100, bb, bb);

        g.addConnection(a, b, A, 0, 2);
        g.addConnection(b, a, A, 2, 0);
        g.addConnection(b, c, B, 0, 2);
        g.addConnection(c, b, B, 2, 0);

        Route route = g.build().getRoute(Vehicle.BIKE, a, c, false);
        assertNull(route);
    }

    @Test //should show possible route for biking.
    public void bikeTest() {
        Graph.GraphBuilder g = new Graph.GraphBuilder();

        Location a, b, c;
        a = new Location(0, 0);
        b = new Location(1, 1);
        c = new Location(2, 2);

        float[] ap, bp;
        ap = new float[]{a.getX(), a.getY(), b.getX(), b.getY()};
        bp = new float[]{b.getX(), b.getY(), c.getX(), c.getY()};

        byte ab = 0b010;
        byte bb = 0b010;

        Road A, B;
        A = new Road(ap, dt, "Vej A", (short) 100, ab, ab);
        B = new Road(bp, dt, "Vej B", (short) 100, bb, bb);

        g.addConnection(a, b, A, 0, 2);
        g.addConnection(b, a, A, 2, 0);
        g.addConnection(b, c, B, 0, 2);
        g.addConnection(c, b, B, 2, 0);

        Route route = g.build().getRoute(Vehicle.BIKE, a, c, false);
        assertNotNull(route);
    }

    @Test
    public void bikePedestTest() {
        Graph.GraphBuilder g = new Graph.GraphBuilder();

        Location a, b, c;
        a = new Location(0, 0);
        b = new Location(1, 1);
        c = new Location(2, 2);

        float[] ap, bp;
        ap = new float[]{a.getX(), a.getY(), b.getX(), b.getY()};
        bp = new float[]{b.getX(), b.getY(), c.getX(), c.getY()};

        byte ab = 0b110;
        byte bb = 0b110;

        Road A, B;
        A = new Road(ap, dt, "Vej A", (short) 100, ab, ab);
        B = new Road(bp, dt, "Vej B", (short) 100, bb, bb);

        g.addConnection(a, b, A, 0, 2);
        g.addConnection(b, a, A, 2, 0);
        g.addConnection(b, c, B, 0, 2);
        g.addConnection(c, b, B, 2, 0);

        Route route = g.build().getRoute(Vehicle.BIKE, a, c, false);
        assertNotNull(route);
    }

    // more advanced graphs
    @Test
    public void longGoodRoadTest() {
        Graph.GraphBuilder G = new Graph.GraphBuilder();

        Location a, b, c, d, e, f, g;
        a = new Location(0, 0);
        b = new Location(1, 1);
        c = new Location(2, 2);
        d = new Location(5, 5);
        e = new Location(7, 7);
        f = new Location(3, 3);
        g = new Location(10, 10);


        float[] ap, bp, cp;
        ap = new float[]{a.getX(), a.getY(), b.getX(), b.getY()};
        bp = new float[]{b.getX(), b.getY(), c.getX(), c.getY(), d.getX(), d.getY()};
        cp = new float[]{d.getX(), d.getY(), e.getX(), e.getY(), f.getX(), f.getY(), g.getX(), g.getY()};

        byte ab = 0b001;
        byte bb = 0b001;
        byte cb = 0b111;
        byte db = 0b101;

        byte eb = 0b000;
        byte fb = 0b111;

        Road A, B, C, D, E, F;
        A = new Road(ap, dt, "Vej A", (short) 100, ab, ab);
        B = new Road(bp, dt, "Vej B", (short) 100, bb, bb);
        C = new Road(ap, dt, "Vej C", (short) 100, cb, cb);
        D = new Road(bp, dt, "Vej D", (short) 100, db, db);
        //impossible to walk, drive or bike on road E
        E = new Road(cp, dt, "Vej E", (short) 100, eb, eb);

        F = new Road(bp, dt, "Vej F", (short) 100, fb, fb);


        G.addConnection(a, b, A, 0, 2);
        G.addConnection(b, a, A, 2, 0);
        G.addConnection(b, c, B, 0, 2);
        G.addConnection(c, b, B, 2, 0);
        G.addConnection(c, d, B, 2, 4);
        G.addConnection(d, c, B, 4, 2);
        G.addConnection(d, f, C, 0, 4);
        G.addConnection(f, d, C, 4, 0);
        G.addConnection(f, e, F, 4, 2);
        G.addConnection(e, f, F, 2, 4);


        Route route = G.build().getRoute(Vehicle.MOTOR, a, f, false);
        assertNotNull(route);
    }
    //returns no route
    @Test
    public void longFailRoadTest() {
        Graph.GraphBuilder G = new Graph.GraphBuilder();

        Location a, b, c, d, e, f, g;
        a = new Location(0, 0);
        b = new Location(1, 1);
        c = new Location(2, 2);
        d = new Location(5, 5);
        e = new Location(7, 7);
        f = new Location(3, 3);
        g = new Location(10, 10);

        float[] ap, bp, cp;
        ap = new float[]{a.getX(), a.getY(), b.getX(), b.getY()};
        bp = new float[]{b.getX(), b.getY(), c.getX(), c.getY(), d.getX(), d.getY()};
        cp = new float[]{d.getX(), d.getY(), e.getX(), e.getY(), f.getX(), f.getY(), g.getX(), g.getY()};

        byte ab = 0b001;
        byte bb = 0b001;
        byte cb = 0b111;
        byte db = 0b101;

        byte eb = 0b000;
        byte fb = 0b111;

        Road A, B, C, D, E, F;
        A = new Road(ap, dt, "Vej A", (short) 100, ab, ab);
        B = new Road(bp, dt, "Vej B", (short) 100, bb, bb);
        C = new Road(ap, dt, "Vej C", (short) 100, cb, cb);
        D = new Road(bp, dt, "Vej D", (short) 100, db, db);
        //impossible to walk, drive or bike on road E
        E = new Road(cp, dt, "Vej E", (short) 100, eb, eb);

        F = new Road(bp, dt, "Vej F", (short) 100, fb, fb);


        G.addConnection(a, b, A, 0, 2);
        G.addConnection(b, a, A, 2, 0);
        G.addConnection(b, c, B, 0, 2);
        G.addConnection(c, b, B, 2, 0);
        G.addConnection(c, d, B, 2, 4);
        G.addConnection(d, c, B, 4, 2);
        G.addConnection(d, f, E, 0, 4);
        G.addConnection(f, d, E, 4, 0);
        G.addConnection(f, e, F, 4, 2);
        G.addConnection(e, f, F, 2, 4);


        Route route = G.build().getRoute(Vehicle.MOTOR, a, f, false);
        assertNotNull(route);
    }
}