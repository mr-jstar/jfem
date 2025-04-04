package fem.mesh;

/**
 *
 * @author jstar
 */
public class LegacyMesh implements IMesh {  // Siatka 2D

    private int nv;  // liczba wierzchołków
    private int ne;  // liczba elementów
    private int nev; // liczba wierzchołków / element (zawsze taka sama)
    private double[][] xy; // wierzchołki
    private int[][] elems; // elementy

    /**
     * @return the nv
     */
    @Override
    public int getNoVertices() {
        return nv;
    }

    /**
     * @param nv the nv to set
     */
    public void setNv(int nv) {
        resizeXY(nv);
        this.nv = nv;
    }

    /**
     * @return the ne
     */
    @Override
    public int getNoElems() {
        return ne;
    }

    /**
     * @param ne the ne to set
     */
    public void setNe(int ne) {
        resizeElems(ne, nev);
        this.ne = ne;
    }

    /**
     * @return the nev
     */
    public int getNev() {
        return nev;
    }

    /**
     * @param nev the nev to set
     */
    public void setNev(int nev) {
        resizeElems(ne, nev);
        this.nv = nev;
    }

    public void setV(double x, double y, int i) {
        xy[i][0] = x;
        xy[i][1] = y;
    }

    @Override
    public Vertex getVertex(int v) {
        return new Vertex(xy[v]);
    }

    private void resizeXY(int newsize) {
        double nxy[][] = new double[newsize][2];
        if (nv > newsize) {
            nv = newsize;
        }
        System.arraycopy(xy, 0, nxy, 0, nv);
        xy = nxy;
    }

    private void resizeElems(int newrows, int newcols) {
        int nelems[][] = new int[newrows][newcols];
        if (ne > newrows) {
            ne = newrows;
        }
        System.arraycopy(elems, 0, nelems, 0, nv);
        elems = nelems;
    }

    public void setElem(int en, int[] elem) {
        for (int i = 0; i < nev; i++) {
            elems[en][i] = elem[i];
        }
    }

    @Override
    public Elem getElem(int e) {
        return new Triangle(elems[e]);
    }

    @Override
    public Elem getFace(int f) {
        return null;
    }

    @Override
    public int getNoFaces() {
        return 0;
    }

    @Override
    public int getNoEdges() {
        return 0;
    }

    @Override
    public Elem getEdge(int e) {
        return null;
    }

}
