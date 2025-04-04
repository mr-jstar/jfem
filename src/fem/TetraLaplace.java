package fem;

/**
 *
 * @author jstar
 */
public class TetraLaplace implements EleIntegral {
    
        
    public static String toString( double [][] m ) {
        String ret = new String();
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                ret += m[i][j] + " ";
            }
            ret += "\n";
        }
        return ret;
    }

    @Override
    public double[][] elemat(double[][] x, double eps, double rho ) {
        if (x.length != 4 || x[0].length != 3) {
            throw new IllegalArgumentException("Expected 4 nodes with 3 coordinates each.");
        }

        double[][] grads = new double[4][3]; // gradienty funkcji kształtu
        double detJ = computeGradientsAndVolume(x, grads); // oblicza gradienty d objętość (6*volume)
       

        double volume = Math.abs(detJ) / 6.0;
        double[][] K = new double[4][5];  // piąta kolumna to wektor prawych stron

        // Obliczenie macierzy sztywności: K_ij = volume * (gradN_i • gradN_j)
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                double dot = 0.0;
                for (int k = 0; k < 3; k++) {
                    dot += grads[i][k] * grads[j][k];
                }
                K[i][j] = volume * dot * eps;
            }
            K[i][4] = volume * rho / 6;
        }

        return K;
    }

    // Oblicza gradienty funkcji kształtu d zwraca 6*objętość (czyli detJ)
    private static double computeGradientsAndVolume(double[][] x, double[][] grads) {
        //.out.println( toString(x));
        // Macierz Jacobiego (3x3) dla przekształcenia współrzędnych
        double[][] J = {  { x[1][0]-x[0][0], x[2][0]-x[0][0], x[3][0]-x[0][0] },
                          { x[1][1]-x[0][1], x[2][1]-x[0][1], x[3][1]-x[0][1] },
                          { x[1][2]-x[0][2], x[2][2]-x[0][2], x[3][2]-x[0][2] },
        };
        
                
        //System.out.println( toString(J));

        double detJ = J[0][0] * (J[1][1]*J[2][2] - J[1][2]*J[2][1])
             - J[0][1] * (J[1][0]*J[2][2] - J[1][2]*J[2][0])
             + J[0][2] * (J[1][0]*J[2][1] - J[1][1]*J[2][0]);

        // Macierz odwrotna do transponowanej J 
        double[][] invJT = inverseTranspose3x3(J, detJ);

        // Dla elementu liniowego w układzie naturalnym:
        double[][] dN = {
            {-1, -1, -1},
            { 1,  0,  0},
            { 0,  1,  0},
            { 0,  0,  1}
        };

        // Gradienty funkcji kształtu w układzie globalnym
        for (int a = 0; a < 4; a++) {
            for (int i = 0; i < 3; i++) {
                grads[a][i] = 0;
                for (int j = 0; j < 3; j++) {
                    grads[a][i] += invJT[i][j] * dN[a][j];
                }
            }
        }

        return detJ;
    }

    private static double[][] inverseTranspose3x3(double[][] J, double det) {
        double[][] invT = new double[3][3];

        invT[0][0] =  (J[1][1]*J[2][2] - J[1][2]*J[2][1]) / det;
        invT[1][0] = -(J[1][0]*J[2][2] - J[1][2]*J[2][0]) / det;
        invT[2][0] =  (J[1][0]*J[2][1] - J[1][1]*J[2][0]) / det;

        invT[0][1] = -(J[0][1]*J[2][2] - J[0][2]*J[2][1]) / det;
        invT[1][1] =  (J[0][0]*J[2][2] - J[0][2]*J[2][0]) / det;
        invT[2][1] = -(J[0][0]*J[2][1] - J[0][1]*J[2][0]) / det;

        invT[0][2] =  (J[0][1]*J[1][2] - J[0][2]*J[1][1]) / det;
        invT[1][2] = -(J[0][0]*J[1][2] - J[0][2]*J[1][0]) / det;
        invT[2][2] =  (J[0][0]*J[1][1] - J[0][1]*J[1][0]) / det;

        return invT;
    }
}

