package fem.mesh;

import java.util.List;

/**
 *
 * @author jstar
 */
public class Mesh implements IMesh {

    private List<Vertex> v;
    private List<Elem> elems;
    private List<Elem> faces;
    private List<Elem> edges;

    public Mesh(List<Vertex> v, List<Elem> elems, List<Elem> faces, List<Elem> edges) {
        if( v == null || elems == null || faces == null || edges == null )
            throw new IllegalArgumentException("Mesh : all arguments to constructor must be not null");
        this.v = v;
        this.elems = elems;
        this.faces = faces;
        this.edges = edges;
    }

    @Override
    public int getNoVertices() {
        return v.size();
    }

    @Override
    public Vertex getVertex(int i) {
        return v.get(i);
    }

    @Override
    public int getNoElems() {
        return elems.size();
    }

    @Override
    public Elem getElem(int e) {
        return elems.get(e);
    }

    @Override
    public Elem getFace(int e) {
        return faces.get(e);
    }

    @Override
    public int getNoFaces() {
        return faces.size();
    }

    @Override
    public int getNoEdges() {
        return edges.size();
    }

    @Override
    public Elem getEdge(int e) {
        return edges.get(e);
    }
}
