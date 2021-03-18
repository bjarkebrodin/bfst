package dankmap.util;

import java.util.InputMismatchException;
import java.util.function.DoubleBinaryOperator;

import static java.lang.Math.*;

/**
 * Provides static utilities for math with vectors
 */
public class VectorMath {

    /**
     * @return  the length or magnitude of a vector
     */
    public static double length(double[] v) {
        double length = 0;
        for (double value : v) length += pow(value, 2);
        return sqrt(length);
    }

    /**
     * @return  the squared length or magnitude of a vector
     */
    public static double sqLength(double[] v) {
        double length = 0;
        for (double value : v) length += pow(value, 2);
        return length;
    }

    /**
     * @return  the squared length or magnitude of a vector
     */
    public static double sqLength(double x, double y) {
        return pow(x, 2) + pow(y, 2);
    }

    /**
     * @return  the length or magnitude of a vector
     */
    public static double length(double x, double y) {
        return sqrt( pow(x, 2) + pow(y, 2) );
    }

    /**
     * @return  the distance between any two vectors, a,b
     *          or equivalently: the magnitude/length of their difference
     */
    public static double dist(double[] a, double[] b) {
        ensureEqualDimensions(a,b);
        double length = 0;
        for (int i = 0; i < a.length; i++) {
            length += pow( a[i]-b[i], 2);
        }
        return sqrt(length);
    }

    /**
     * @return  the squared distance between any two vectors, a,b
     *          or equivalently: the magnitude/length of their difference
     */
    public static double sqDist(double[] a, double[] b) {
        ensureEqualDimensions(a,b);
        double length = 0;
        for (int i = 0; i < a.length; i++) {
            length += pow( a[i]-b[i], 2);
        }
        return length;
    }

    /**
     * @return  euclidean distance from (x0,y0) to (x1,y1)
     */
    public static double dist(double x0, double y0, double x1, double y1) {
        return sqrt( pow(x1-x0, 2) + pow(y1-y0, 2) );
    }

    /**
     * @return  the squared euclidean distance from (x0,y0) to (x1,y1)
     */
    public static double sqDist(double x0, double y0, double x1, double y1) {
        return pow(x1-x0, 2) + pow(y1-y0, 2);
    }

    /**
     * Returns the dot or scalar product of any two vectors of equal dimensions
     * @param a vector [ x, y, z, ... ]
     * @param b vector [ x, y, z, ... ]
     */
    public static double dot(double[] a, double[] b) {
        ensureEqualDimensions(a,b);

        double dot = 0;
        for (int i = 0; i < a.length; i++)
            dot += a[i]*b[i];

        return dot;
    }

    /**
     * @throws InputMismatchException if vector dimensions > 2
     */
    public static double cross(double[] a, double[] b) {
        ensureEqualDimensions(a,b);
        ensureExactDimensions(a,2);
        return a[0]*b[1] - a[1]*b[0];
    }

    /**
     * @return the normal vector for vector v, n such that n = [ -v[1] , v[0] ]
     * @throws InputMismatchException if vector dimensions > 2
     */
    public static double[] normal(double[] v) {
        return new double[]{ -v[1], v[0] };
    }

    /**
     * @return the difference vector between any two vectors of equal dimensions
     */
    public static double[] minus(double[] a, double[] b) {
        return vectorOperation(a, b, (x,y) -> x-y);
    }

    /**
     * @return the summed vector of any two vectors of equal dimensions
     */
    public static double[] plus(double[] a, double[] b) {
        return vectorOperation(a, b, Double::sum);
    }

    /**
     * @return the quotient vector of any two vectors of equal dimensions
     */
    public static double[] div( double[] a, double[] b ) {
        return vectorOperation(a, b, (x,y) -> x/y);
    }

    /**
     * @return the product vector of any two vectors of equal dimensions
     */
    public static double[] product( double[] a, double[] b ) {
        return vectorOperation(a, b, (x,y) -> x*y);
    }

    /**
     * @return the vector r such that r = [ v[0] / div , v[1] / div ]
     */
    public static double[] scalarDiv(double[] v, double div){
        return new double[] {v[0]/div,v[1]/div};
    }

    /**
     * @return the vector r such that r = [ v[0] * c, v[1] * c ]
     */
    public static double[] scalarMulti(double[] v, double c){
        return new double[] {v[0]*c,v[1]*c};
    }

    /**
     * @return  the shortest true distance from the specified point <code>m</code> to the
     *          line segment between <code>t</code> and <code>p</code>
     */
    public static double shortestDistPointToSegment(double[] m, double[] t, double[] p) {
        double dist;
        double[] tp, tm;

        /*   [ p[0] - t[0] ]
        tp = [ p[1] - t[1] ] */
        tp = minus(p, t);

        /*   [ m[0] - t[0] ]
        tm = [ m[1] - t[1] ] */
        tm = minus(m, t);

        /*
        The projection of m onto tp, in our model this corresponds to
        the projection of the mouse onto the currently inspected road segment.
        */
        double[] k = scalarMulti(tp, dot(tm, tp) / sqDist(t, p));

        /*
        let lambda = k / tp , the quotient of the projection of the point
        m onto the vector tp. If either of the coordinates of lambda
        are negative the endpoint of k must lie before the starting point of
        tp. This tells us if the true distance from m to segment tp is the
        projection of m onto tp, or a distance to one of the endpoints t or p

        case 1: " True distance is projection "

                   m
                   |
                   |  <- true dist
                   |
            t ---> k ---------> p


        case 2: " True distance is either dist to p or t "
                   ( possibility m or m' respectively )

            m                                    m'
            |                                    |
            | <- false dist        false dist -> |
            |                                    |
            k <-----  t  ------------> p ------> k'

         */
        double[] lambda = div(k, tp);

        /*
        let delta = |tp|^2 - |k|^2, then we have that :
        if lambda[0] < 0 || lambda[1] < 0  the vector k goes in
        the opposite direction of tp. If this is not the case,
        we know that the endpoint of k lies exactly ON the segment
        tp if and only if the length of k is less than the length of tp.
        */
        double delta = sqLength(tp) - sqLength(k);

        if ((lambda[0] < 0 || lambda[1] < 0) || delta < 0) {
            // Dist between closest endpoint and mouse
            dist = min(sqDist(m, p), sqDist(m, t));
        } else {
            // Projected orthogonal distance between m and k
            dist = sqLength(tm) - sqLength(k);
        }

        return dist;
    }


    /////////// Utility //////////

    private static double[] vectorOperation ( double[] left, double[] right, DoubleBinaryOperator operator ) {
        ensureEqualDimensions(left,right);

        double[] result = new double[left.length];
        for (int i = 0; i < left.length; i++)
            result[i] = operator.applyAsDouble(left[i], right[i]);

        return result;
    }

    private static void ensureEqualDimensions ( double[] a, double[] b ) {
        if ( a.length != b.length )
            throw new InputMismatchException("vectors differ in size");
    }

    private static void ensureExactDimensions ( double[] a, int D ) {
        if ( a.length != D )
            throw new InputMismatchException("vector dimensions out of bounds");
    }
}