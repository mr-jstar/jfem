package fem;

import fem.mesh.Elem;
import fem.mesh.IMesh;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Map;
import sm.CRS;
import sm.solvers.*;

/**
 *
 * @author jstar
 */
public class FEM {

    private double tolerance = 1e-12;
    private IMesh mesh;
    private double[] mats;
    private double[] srcs;
    private double[] fld;
    private boolean symmetricMatrix= true;

    private int[] bndNodes;
    private double[] bndValues;

    private CRS H;
    private double[] rhsv;
    
        public FEM(IMesh mesh, Map<Integer,double[]> params, int[] bndNodes, double[] bndValues) {
        this.mesh = mesh;
        this.bndNodes = bndNodes;
        this.bndValues = bndValues;
        int maxSubDomNo = -1;
        for( Integer k : params.keySet() )
            if( k > maxSubDomNo )
                maxSubDomNo = k;
        this.mats = new double[maxSubDomNo+1];
        this.srcs = new double[maxSubDomNo+1];
        for( Integer k : params.keySet() ) {
            double [] kpar = params.get(k);
            mats[k] = kpar[0];
            srcs[k] = kpar[1];
        }
    }

    public FEM(IMesh mesh, double[] mats, double[] srcs, int[] bndNodes, double[] bndValues) {
        this.mesh = mesh;
        this.mats = mats;
        this.srcs = srcs;
        this.bndNodes = bndNodes;
        this.bndValues = bndValues;
    }
    
    public void buildSymmetricMatrix( boolean flag ) {
        symmetricMatrix = flag;
    }

    public void assemble(EleIntegral p) {
        H = mesh.makeCRSStructure();
        rhsv = new double[mesh.getNoVertices()];
        for (int e = 0; e < mesh.getNoElems(); e++) {
            Elem elem = mesh.getElem(e);
            int subDomain = elem.getSubdomain();
            int[] nds = elem.getVertices();
            double[][] xe = new double[nds.length][];
            for (int v = 0; v < nds.length; v++) {
                xe[v] = mesh.getVertex(nds[v]).getX();
            }
            double[][] here = p.elemat(xe, getMats()[subDomain], srcs[subDomain]);
            for (int i = 0; i < nds.length; i++) {
                for (int j = 0; j < nds.length; j++) {
                    H.addTo(nds[i], nds[j], here[i][j]);
                }
                rhsv[nds[i]] += here[i][nds.length];
            }
        }
        for (int i = 0; i < bndNodes.length; i++) {
            int v = bndNodes[i];
            double vv = H.get(v, v);
            H.clearRow(v);
            H.addTo(v, v, vv);
            rhsv[v] = vv * bndValues[i];
            if (symmetricMatrix) {
                for (int r = 0; r < H.nRows(); r++) {
                    if (r != v) {
                        rhsv[r] -= H.get(r, v) * bndValues[i];
                        H.set(r, v, 0.0);
                    }
                }
            }
        }
    }

    public void solve(Solver solver) {
        int maxit = mesh.getNoVertices();
        fld = solver.solve(H, rhsv, getTolerance(), maxit > 1000 ? maxit/10 : maxit);
    }

    /**
     * @return the tolerance
     */
    public double getTolerance() {
        return tolerance;
    }

    /**
     * @param tolerance the tolerance to set
     */
    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    /**
     * @return the mats
     */
    public double[] getMats() {
        return mats;
    }

    /**
     * @param mats the mats to set
     */
    public void setMats(double[] mats) {
        this.mats = mats;
    }

    /**
     * @return the fld
     */
    public double[] getFld() {
        return fld;
    }

    /**
     * @return the bndNodes
     */
    public int[] getBndNodes() {
        return bndNodes;
    }

    /**
     * @param bndNodes the bndNodes to set
     */
    public void setBndNodes(int[] bndNodes) {
        this.bndNodes = bndNodes;
    }

    /**
     * @return the bndValues
     */
    public double[] getBndValues() {
        return bndValues;
    }

    /**
     * @param bndValues the bndValues to set
     */
    public void setBndValues(double[] bndValues) {
        this.bndValues = bndValues;
    }

    // Basic test
    public void dump(String file) {
        try (PrintStream f = new PrintStream(new FileOutputStream(file))) {
            f.println("H=[" + H + "]");
            f.println("r=[" + vectorToString(rhsv) + "]");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String matrixToString(double[][] m) {
        String ret = new String();
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                ret += m[i][j] + " ";
            }
            ret += "\n";
        }
        return ret;
    }

    public static String vectorToString(double[] m) {
        String ret = new String();
        for (int i = 0; i < m.length; i++) {
            ret += m[i] + " ";
            ret += "\n";
        }
        return ret;
    }

    private static double[] range(double[] v) {
        double[] range = {v[0], v[0]};
        for (int i = 1; i < v.length; i++) {
            if (v[i] < range[0]) {
                range[0] = v[i];
            }
            if (v[i] > range[1]) {
                range[1] = v[i];
            }
        }
        return range;
    }

    public static void main(String[] args) {
        try {
            String meshFile = args.length > 0 ? args[0] : "data/test.1.node";

            IMesh m;
            if( meshFile.endsWith(".mesh") )
                m= Util.loadINRIAMesh(meshFile);
            else if( meshFile.endsWith(".ele") || meshFile.endsWith(".node") )
                m = Util.loadTriangleMesh(meshFile );
            else
                throw new IllegalArgumentException( "Unknown mesh format: " + meshFile );
            
            EleIntegral integral = m.getDim() == 3 ? new TetraLaplace() : new TriangleLaplace();
            
            System.out.println("Mesh");
            System.out.println("\tDimension : " + m.getDim() + "D");
            System.out.println("\tNodes : " + m.getNoVertices());
            System.out.println("\tElements : " + m.getNoElems());
            System.out.println("\tFaces: : " + m.getNoFaces());
            System.out.println("\tEdges : " + m.getNoEdges());
            System.out.println("\tSubdomains : " + m.getNoSubdomains());
            
           // for( int v = 0; v < m.getNoVertices(); v++ )
            //    System.out.println( vectorToString( m.getVertex(v).getX() ) );

            double[] srcs = new double[m.getNoSubdomains() + 1];
            double[] mats = new double[srcs.length];
            for (int i = 0; i < srcs.length; i++) {
                mats[i] = 1;
            }
            int[] bndNds = {0, m.getNoVertices() - 1};
            double[] bndVals = {0, 1.0};
            FEM fem = new FEM(m, mats, srcs, bndNds, bndVals);
            fem.symmetricMatrix = true;
            fem.assemble(integral);
            fem.dump("data/hr.m");
            fem.solve(new GMRES());
            double[] V = fem.getFld();
            double[] minmax = range(V);
            System.out.println("V in <" + minmax[0] + "," + minmax[1] + ">");
            for (int i = 0; i < V.length; i++) {
                System.out.print(V[i] + " ");
            }
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
