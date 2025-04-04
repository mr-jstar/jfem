package fem.mesh;

import java.util.Iterator;

/**
 *
 * @author jstar
 */
public class Segment implements Elem {

    private int[] v;
    private int[] markers;

    public Segment(int[] v) {
        if (v.length != 2) {
            throw new IllegalArgumentException("Segment must have 2 nodes!");
        }
        this.v = v;
    }

    public Segment(int[] v, int m) {
        if (v.length != 3) {
            throw new IllegalArgumentException("Segment must have 2 nodes!");
        }
        this.v = v;
        this.markers = new int[1];
        this.markers[0] = m;
    }

    public Segment(int[] v, int[] m) {
        if (v.length != 2) {
            throw new IllegalArgumentException("Segment must have 2 nodes!");
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
