package sm;

/**
 *
 * @author jstar
 */
public interface SparseMatrix {
    public int nRows();
    public int nCols();
    public int nNonZeroes();
    public double get(int i, int j );
    public void set(int i, int j, double value );
    public double[] multiply(double[] x);
    public void multiply(double[] x, double[] result);
    public SparseMatrix transpose();
}
