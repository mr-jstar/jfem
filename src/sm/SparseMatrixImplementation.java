package sm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jstar
 */
public class SparseMatrixImplementation implements SparseMatrix {

    private final Map<Integer, Map<Integer, Double>> matrix;
    private final int rows;
    private final int cols;

    public SparseMatrixImplementation(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.matrix = new HashMap<>();
    }

    public void addTo(int row, int col, double a) {
        // m[row,col] += a
        if (!matrix.containsKey(row)) {
            matrix.put(row, new HashMap<>());
        }
        Map<Integer, Double> rowentry = matrix.get(row);
        if (rowentry.containsKey(col)) {
            rowentry.put(col, rowentry.get(col) + a);
        } else {
            rowentry.put(col, a);
        }
    }

    @Override
    public int nRows() {
        return rows;
    }

    @Override
    public int nCols() {
        return cols;
    }
    
    @Override
    public int nNonZeroes() {
        int result = 0;
        for (Map.Entry<Integer, Map<Integer, Double>> rowEntry : matrix.entrySet()) {
            result += rowEntry.getValue().size();
        }
        return result;
    }

    @Override
    public double get(int row, int col) {
        if (matrix.containsKey(row)) {
            Map<Integer, Double> rowEntry = matrix.get(row);
            if (rowEntry.containsKey(col)) {
                return rowEntry.get(col);
            } else {
                return 0.0;
            }
        } else {
            return 0.0;
        }
    }

    public void set(int row, int col, double value) {
        matrix.computeIfAbsent(row, k -> new HashMap<>()).put(col, value);
    }

    @Override
    public double[] multiply(double[] x) {
        double[] result = new double[rows];
        for (Map.Entry<Integer, Map<Integer, Double>> rowEntry : matrix.entrySet()) {
            int i = rowEntry.getKey();
            for (Map.Entry<Integer, Double> colEntry : rowEntry.getValue().entrySet()) {
                int j = colEntry.getKey();
                result[i] += colEntry.getValue() * x[j];
            }
        }
        return result;
    }
    
    
    @Override
    public void multiply(double[] x, double[] result) {
        for (Map.Entry<Integer, Map<Integer, Double>> rowEntry : matrix.entrySet()) {
            int i = rowEntry.getKey();
            result[i] = 0.0;
            for (Map.Entry<Integer, Double> colEntry : rowEntry.getValue().entrySet()) {
                int j = colEntry.getKey();
                result[i] += colEntry.getValue() * x[j];
            }
        }
    }

    @Override
    public SparseMatrix transpose() {
        SparseMatrixImplementation transposed = new SparseMatrixImplementation(cols, rows);
        for (Map.Entry<Integer, Map<Integer, Double>> rowEntry : matrix.entrySet()) {
            int i = rowEntry.getKey();
            for (Map.Entry<Integer, Double> colEntry : rowEntry.getValue().entrySet()) {
                int j = colEntry.getKey();
                transposed.set(j, i, colEntry.getValue());
            }
        }
        return transposed;
    }

    @Override
    public String toString() {
        int n = 10;
        String ret = new String();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                ret += get(i, j) + " ";
            }
            ret += "\n";
        }
        return ret;
    }

    public CRS toCRS() {
        int[] ia = new int[rows + 1];
        for (Map.Entry<Integer, Map<Integer, Double>> rowEntry : matrix.entrySet()) {
            int i = rowEntry.getKey();
            ia[i + 1] = rowEntry.getValue().size();
        }
        for (int i = 2; i <= rows; i++) {
            ia[i] += ia[i - 1];
        }
        int nz = ia[rows];
        int[] ja = new int[nz];
        double[] a = new double[nz];
        for (Map.Entry<Integer, Map<Integer, Double>> rowEntry : matrix.entrySet()) {
            int i = rowEntry.getKey();
            List<Map.Entry<Integer, Double>> row = new ArrayList<>(rowEntry.getValue().entrySet());

            Collections.sort(row, new Comparator<Map.Entry<Integer, Double>>() {
                @Override
                public int compare(Map.Entry<Integer, Double> e1, Map.Entry<Integer, Double> e2) {
                    return e1.getKey() - e2.getKey();
                }
            });
            int j = ia[i];
            for (Map.Entry<Integer, Double> e : row) {
                ja[j] = e.getKey();
                a[j] = e.getValue();
                j++;
            }
        }

        return new CRS(ia, ja, a);
    }

    public static void main(String[] args) {
        int n = 10;
        SparseMatrixImplementation m = new SparseMatrixImplementation(n, n);
        java.util.Random r = new java.util.Random();
        int nz = r.nextInt(30) + 1;
        for (int k = 0; k < n; k++) {
            m.set(k, k, 2.0);
        }
        for (int k = 0; k < nz - n; k++) {
            m.set(r.nextInt(n), r.nextInt(n), 1.0);
        }
        System.out.println(m);

        for (int i = 0; i < n; i++) {
            m.addTo(i, i, 5.0);
        }
        System.out.println("-----");
        System.out.println(m);

    }
}
