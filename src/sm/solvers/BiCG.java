package sm.solvers;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import sm.*;

/**
 *
 * @author jstar
 */
public class BiCG implements Solver {

    private static final java.util.Random rg = new java.util.Random();

    public double[] solve(SparseMatrix A, double[] b, double tol, int maxIter) {  // some unnecessary allocations
        int n = b.length;
        double[] x = new double[n]; // Rozwiązanie początkowe x = 0
        double[] r = Arrays.copyOf(b, n); // r_0 = b - Ax (Ax = 0 na początku)
        double[] rTilde = Arrays.copyOf(r, n); // Kopia wektora resztowego
        double[] p = Arrays.copyOf(r, n);
        double[] pTilde = Arrays.copyOf(rTilde, n);

        double rho = dotProduct(rTilde, r);

        for (int k = 0; k < maxIter; k++) {
            double[] Ap = A.multiply(p);
            double[] ATpTilde = A.transpose().multiply(pTilde);

            double alpha = rho / dotProduct(pTilde, Ap);

            x = vectorAdd(x, scalarMultiply(alpha, p));
            r = vectorSubtract(r, scalarMultiply(alpha, Ap));
            rTilde = vectorSubtract(rTilde, scalarMultiply(alpha, ATpTilde));

            double rhoNew = dotProduct(rTilde, r);
            if (Math.sqrt(rhoNew) < tol) {
                //System.out.println("BiCG: in " + k + " iterations error dropped to " + Math.sqrt(rhoNew));
                break; // Kryterium zakończenia
            }

            double beta = rhoNew / rho;
            p = vectorAdd(r, scalarMultiply(beta, p));
            pTilde = vectorAdd(rTilde, scalarMultiply(beta, pTilde));

            rho = rhoNew;
        }
        System.out.println("BiCG: in " + maxIter + " iterations error dropped to " + Math.sqrt(rho));
        return x;
    }

    private static double dotProduct(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private static double[] vectorAdd(double[] a, double[] b) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] + b[i];
        }
        return result;
    }

    private static double[] vectorSubtract(double[] a, double[] b) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] - b[i];
        }
        return result;
    }

    private static double norm(double[] a) {
        double result = 0;
        for (int i = 0; i < a.length; i++) {
            result += a[i] * a[i];
        }
        return Math.sqrt(result);
    }

    private static double[] scalarMultiply(double scalar, double[] vector) {
        double[] result = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = scalar * vector[i];
        }
        return result;
    }

    public static HashSM makeHashSM(int n, double sparsity) {
        HashSM m = new HashSM(n, n);

        int nz = rg.nextInt((int) (n * (n * sparsity))) + 1;

        for (int k = 0; k < nz - n; k++) {
            m.set(rg.nextInt(n), rg.nextInt(n), rg.nextDouble());
        }

        for (int k = 0; k < n; k++) {
            m.set(k, k, 1.05 * n);
        }
        return m;
    }

    public static CRS makeCRS(int n, double sparsity) {
        int[] ia = new int[n + 1];
        List<Integer> jaList = new ArrayList<>();
        int maxRow = (int) (2 * sparsity * n);
        List<Integer> cols = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            cols.add(i);
        }
        for (int i = 1; i <= n; i++) {
            Collections.shuffle(cols);
            int rnz = rg.nextInt(1, maxRow);
            boolean diag = false;
            for (int j = 0; j < rnz; j++) {
                int k = cols.get(j);
                if (k == i - 1) {
                    diag = true;
                }
                jaList.add(cols.get(j));
            }
            if (!diag) {
                jaList.set(jaList.size() - 1, i - 1);
            }
            ia[i] = ia[i - 1] + rnz;
        }

        //System.out.println(ia[n]);
        int[] ja = new int[jaList.size()];
        double[] a = new double[jaList.size()];
        for (int i = 0; i < jaList.size(); i++) {
            ja[i] = jaList.get(i);
            a[i] = rg.nextDouble();
        }

        for (int i = 0; i < n; i++) {
            for (int j = ia[i]; j < ia[i + 1]; j++) {
                if (ja[j] == i) {
                    a[j] += n;
                }
            }
        }
        return new CRS(ia, ja, a);
    }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 100000;
        double minSpars = 0.05;
        double maxSpars = 0.1;

        System.out.println("n^2=" + n * n);

        int nRep = 10;

        long startTime = System.nanoTime();
        for (int tr = 0; tr < nRep; tr++) {
            double sparsity = rg.nextDouble(minSpars, maxSpars);
            CRS crs;
            if (args.length > 1 && args[1].equals("Hash")) {
                HashSM m = makeHashSM(n, sparsity);

                if (n <= 20) {
                    System.out.println(m);
                }

                crs = m.toCRS();
            } else {
                crs = makeCRS(n, sparsity);
            }

            for (int i = 0; i < n; i++) {
                crs.addTo(i, i, 1.0);
            }

            if (n <= 20) {
                System.out.println(crs);
            }

            double[] x = new double[n];
            for (int i = 1; i < n + 1; i++) {
                x[i - 1] = i;
            }
            double[] b = crs.multiply(x);

            Solver bicg = new BiCG();

            double[] solution = bicg.solve(crs, b, 1e-12, n);

            if (n <= 20) {
                System.out.println("Rozwiązanie: " + Arrays.toString(solution));
            }

            double[] ax = crs.multiply(solution);
            double resN = norm(vectorSubtract(ax, b));
            System.out.print(tr + ": sparsity: " + sparsity);
            System.out.print(", actual non-zeroes: " + crs.nNonZeroes() + ", actual sparsity: " + (double) crs.nNonZeroes() / n / n);
            System.out.println(" -> ||Residuum|| = " + resN);
            if (resN > 1e-3) {
                try (FileWriter wr = new FileWriter("test.txt")) {
                    wr.write(crs.toString());
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
        long elapsedTime = System.nanoTime() - startTime;

        System.out.println("Total execution time to solve " + nRep + " [" + n + "x" + n + "] sparse LinEqS in millis: "
                + elapsedTime / 1000000);
    }
}
