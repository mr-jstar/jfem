package sm.solvers;

import sm.SparseMatrix;

/**
 *
 * @author jstar
 */
public interface Solver {

    double[] solve(SparseMatrix A, double[] b, double tol, int maxIter);
    
}
