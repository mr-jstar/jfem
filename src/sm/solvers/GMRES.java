package sm.solvers;

/**
 *
 * @author jstar
 */
import java.util.Arrays;
import sm.SparseMatrix;

public class GMRES implements Solver {
    
    @Override
    public double[] solve(SparseMatrix A, double[] b, double tol, int maxIter ) {
        int n = b.length;
        double[] x = new double[n];
        double[] r = vectorSubtract(b, A.multiply(x));
        double beta = norm(r);

        if (beta < tol) {
            return x;
        }

        double[][] V = new double[maxIter + 1][n];
        double[][] H = new double[maxIter + 1][maxIter];
        double[] cs = new double[maxIter];
        double[] sn = new double[maxIter];
        double[] e1 = new double[maxIter + 1];
        e1[0] = beta;

        V[0] = scalarMultiply(r, 1.0 / beta);

        for (int iter = 0; iter < maxIter; iter++) {
            double[] w = A.multiply(V[iter]);

            for (int i = 0; i <= iter; i++) {
                H[i][iter] = dot(w, V[i]);
                w = vectorSubtract(w, scalarMultiply(V[i], H[i][iter]));
            }

            H[iter + 1][iter] = norm(w);

            if (H[iter + 1][iter] != 0.0) {
                V[iter + 1] = scalarMultiply(w, 1.0 / H[iter + 1][iter]);
            }

            // Apply Givens rotations
            for (int i = 0; i < iter; i++) {
                double temp = cs[i] * H[i][iter] + sn[i] * H[i + 1][iter];
                H[i + 1][iter] = -sn[i] * H[i][iter] + cs[i] * H[i + 1][iter];
                H[i][iter] = temp;
            }

            // Compute new Givens rotation
            double rVal = Math.hypot(H[iter][iter], H[iter + 1][iter]);
            cs[iter] = H[iter][iter] / rVal;
            sn[iter] = H[iter + 1][iter] / rVal;
            H[iter][iter] = rVal;
            H[iter + 1][iter] = 0.0;

            // Update the residual
            e1[iter + 1] = -sn[iter] * e1[iter];
            e1[iter] = cs[iter] * e1[iter];

            if (Math.abs(e1[iter + 1]) < tol) {
                return updateSolution(x, V, H, e1, iter + 1);
            }
        }

        System.out.println("GMRES: in " + maxIter + " iterations error dropped to " + e1[maxIter]);
        return updateSolution(x, V, H, e1, maxIter);
    }

    private static double[] updateSolution(double[] x, double[][] V, double[][] H, double[] e1, int k) {
        double[] y = backSubstitute(H, Arrays.copyOf(e1, k), k);
        for (int i = 0; i < k; i++) {
            x = vectorAdd(x, scalarMultiply(V[i], y[i]));
        }
        return x;
    }

    private static double[] vectorAdd(double[] a, double[] b) {
        double[] r = new double[a.length];
        for (int i = 0; i < a.length; i++) r[i] = a[i] + b[i];
        return r;
    }

    private static double[] vectorSubtract(double[] a, double[] b) {
        double[] r = new double[a.length];
        for (int i = 0; i < a.length; i++) r[i] = a[i] - b[i];
        return r;
    }

    private static double[] scalarMultiply(double[] v, double alpha) {
        double[] r = new double[v.length];
        for (int i = 0; i < v.length; i++) r[i] = v[i] * alpha;
        return r;
    }

    private static double dot(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) sum += a[i] * b[i];
        return sum;
    }

    private static double norm(double[] v) {
        return Math.sqrt(dot(v, v));
    }

    private static double[] backSubstitute(double[][] H, double[] g, int k) {
        double[] y = new double[k];
        for (int i = k - 1; i >= 0; i--) {
            y[i] = g[i];
            for (int j = i + 1; j < k; j++) {
                y[i] -= H[i][j] * y[j];
            }
            y[i] /= H[i][i];
        }
        return y;
    }
}
