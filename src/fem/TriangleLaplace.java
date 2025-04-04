package fem;

/**
 *
 * @author jstar
 */
public class TriangleLaplace implements EleIntegral {

    @Override
    public double[][] elemat(double[][] x, double eps, double rho ) {
        if (x.length != 3 || x[0].length != 2) {
            throw new IllegalArgumentException("Expected 3 nodes with 2 coordinates each.");
        }

        // Wierzchołki
        double x1 = x[0][0], y1 = x[0][1];
        double x2 = x[1][0], y2 = x[1][1];
        double x3 = x[2][0], y3 = x[2][1];

        // Obszar trójkąta (z wyznacznika Jacobiego)
        double detJ = (x2 - x1)*(y3 - y1) - (x3 - x1)*(y2 - y1);
        double area = Math.abs(detJ) / 2.0;

        // Pochodne funkcji kształtu (stałe gradienty)
        double[] b = { y2 - y3, y3 - y1, y1 - y2 };
        double[] c = { x3 - x2, x1 - x3, x2 - x1 };

        // Macierz sztywności: (1/(4A)) * (b_i * b_j + c_i * c_j)
        double[][] K = new double[3][4];  // 4 kolumna to wektor prawych stron
        double factor = eps / (4.0 * area);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                K[i][j] = factor * (b[i] * b[j] + c[i] * c[j]);
            }
            K[i][3] = 1.0/3.0*rho*area;
        }

        return K;
    }
}

