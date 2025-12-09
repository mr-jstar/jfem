package fem.mesh;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import sm.CRS;

/**
 *
 * @author jstar
 */
public interface IMesh {

    public int getNoVertices();

    public Vertex getVertex(int v);

    public int getNoElems();

    public int getNoFaces();

    public int getNoEdges();

    public Elem getElem(int e);

    public Elem getFace(int f);

    public Elem getEdge(int e);
    
    default int getDim() {
        return getVertex(0).getDim();
    }
    
    default double distance( int v1, int v2 ) {
        if( v1 < 0 || v1 >= getNoVertices() || v2 < 0 || v2 >= getNoVertices() )
            throw new IllegalArgumentException("IMesh: illegal vertex number");
        double [] x1 = getVertex(v1).getX();
        double [] x2 = getVertex(v2).getX();
        double d = 0;
        for( int i= 0; i < getDim(); i++ )
            d += (x1[i]-x2[i])*(x1[i]-x2[i]);
        return Math.sqrt(d);
    }
    
    default public int getNoSubdomains() {
        TreeSet<Integer> subdomains = new TreeSet<>();
        for( int e= 0; e < getNoElems(); e++ )
            subdomains.add( getElem(e).getSubdomain());
        return subdomains.size();
    }

    default CRS makeCRSStructure() {
        @SuppressWarnings("unchecked")
        TreeSet<Integer>[] c = new TreeSet[getNoVertices()];
        for (int i = 0; i < c.length; i++) {
            c[i] = new TreeSet<>();
        }
        for (int e = 0; e < getNoElems(); e++) {
            int[] n = getElem(e).getVertices();
            for (int i = 0; i < n.length; i++) {
                for (int j = 0; j < n.length; j++) {
                    ((Set<Integer>) c[n[i]]).add(n[j]);
                    ((Set<Integer>) c[n[j]]).add(n[i]);
                }
            }
        }
        int [] ia = new int[getNoVertices()+1];
        for( int v= 0; v < getNoVertices(); v++ )
            ia[v+1] = ia[v] + ((Set)c[v]).size();
        int [] ja = new int[ia[getNoVertices()]];
        double [] a = new double[ja.length];
        for( int v= 0; v < getNoVertices(); v++ ) {
            int j= ia[v];
            for( Integer k : ((Set<Integer>)c[v]))
                ja[j++] = k;
        }
        return new CRS(ia, ja, a);
    }
    
    default CRS buildCRSStructure() {
        List<TreeSet<Integer>> c = new ArrayList<>(getNoVertices());
        for (int i = 0; i < getNoVertices(); i++) {
            c.set(i, new TreeSet<>());
        }
        for (int e = 0; e < getNoElems(); e++) {
            int[] n = getElem(e).getVertices();
            for (int i = 0; i < n.length; i++) {
                for (int j = 0; j < n.length; j++) {
                    c.get(n[i]).add(n[j]);
                    c.get(n[j]).add(n[i]);
                }
            }
        }
        int [] ia = new int[getNoVertices()+1];
        for( int v= 0; v < getNoVertices(); v++ )
            ia[v+1] = ia[v] + c.get(v).size();
        int [] ja = new int[ia[getNoVertices()]];
        double [] a = new double[ja.length];
        for( int v= 0; v < getNoVertices(); v++ ) {
            int j= ia[v];
            for( Integer k : c.get(v))
                ja[j++] = k;
        }
        return new CRS(ia, ja, a);
    }
}
