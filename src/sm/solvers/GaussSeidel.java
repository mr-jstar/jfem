package sm.solvers;

import sm.*;

/**
 *
 * @author jstar
 */
public class GaussSeidel implements Solver {

    private static final java.util.Random rg = new java.util.Random();

    public double[] solve(SparseMatrix A, double[] b, double tol, int maxIter) {  // some unnecessary allocations
        int n = b.length;
        double[] xp = new double[n]; // Rozwiązanie początkowe x = 0
        double[] x = new double[n];
        double err = 1.0;

        for (int k = 0; k < maxIter; k++) {
            for( int r= 0; r < n; r++ ) {
                double s= b[r];
                for( int c= 0; c < r; c++ )
                    s -= A.get(r, c) * x[c];
                for( int c= r+1; c < n; c++ )
                    s -= A.get(r,c) * xp[c];
                x[r] = s / A.get(r, r);
            }
            vectorSubtract(x ,xp, xp);
            err = norm(xp);
            if (err < tol) {
                //System.out.println("GaussSeidel: in " + k + " iterations error dropped to " + err);
                break; // Kryterium zakończenia
            }

            System.arraycopy( x, 0, xp, 0, n);
        }
        System.out.println("GaussSeidel: in " + maxIter + " iterations error dropped to " + err);
        return x;
    }

    private static double norm(double[] a) {
        double result = 0;
        for (int i = 0; i < a.length; i++) {
            result += a[i] * a[i];
        }
        return Math.sqrt(result);
    }

    private static void vectorSubtract(double[] a, double[] b, double [] result) {
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] - b[i];
        }
    }
}
