package fem.mesh;

import java.util.Iterator;

/**
 *
 * @author jstar
 */
public class Tetra implements Elem {

    private int[] v;
    private int[] markers;

    public Tetra(int[] v) {
        if (v.length != 4) {
            throw new IllegalArgumentException("Tetrahedron must have 4 nodes!");
        }
        this.v = v;
    }

    public Tetra(int[] v, int m) {
        if (v.length != 4) {
            throw new IllegalArgumentException("Tetrahedron must have 4 nodes!");
        }
        this.v = v;
        this.markers = new int[1];
        this.markers[0] = m;
    }

    public Tetra(int[] v, int[] m) {
        if (v.length != 4) {
            throw new IllegalArgumentException("Tetrahedron must have 4 nodes!");
        }
        this.v = v;
        this.markers = m;
    }

    @Override
    public int[] getVertices() {
        return v;
    }

    @Override
    public void setVertices(int[] v) {
        if (v.length != 4) {
            throw new IllegalArgumentException("Tetrahedron must have 4 nodes!");
        }
        this.v = v;
    }

    @Override
    public int[] getMarkers() {
        return markers;
    }

    @Override
    public void setMarkers(int[] m) {
        this.markers = m;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < 4;
            }

            @Override
            public Integer next() {
                return v[i++];
            }

        };
    }
}
