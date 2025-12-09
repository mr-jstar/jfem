package fem.mesh;

import java.util.Arrays;
import java.util.Iterator;

/**
 *
 * @author jstar
 */
public class StraightEdge implements Edge {

    private int[] v;
    private int[] markers;
    
    public StraightEdge(int v1, int v2) {
        v = new int[2];
        v[0] = v1; 
        v[1] = v2;
        reorderV();
    }

    public StraightEdge(int[] v) {
        if (v.length != 2) {
            throw new IllegalArgumentException("StraigthEdge must have 2 nodes!");
        }
        this.v = v;
        reorderV();
    }

    public StraightEdge(int[] v, int m) {
        if (v.length != 2) {
            throw new IllegalArgumentException("StraigthEdge must have 2 nodes!");
        }
        this.v = v;
        reorderV();
        this.markers = new int[1];
        this.markers[0] = m;
    }

    public StraightEdge(int[] v, int[] m) {
        if (v.length != 2) {
            throw new IllegalArgumentException("StraigthEdge must have 2 nodes!");
        }
        this.v = v;
        reorderV();
        this.markers = m;
        if( this.v[0] != v[0]) {
            int tmp = this.markers[0];
            this.markers[0] = this.markers[1];
            this.markers[1] = tmp;
        }
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
                return i < 3;
            }

            @Override
            public Integer next() {
                return v[i++];
            }

        };
    }

    @Override
    public String toString() {
        return v[0] + "_" + v[1];
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof StraightEdge e) {
            return v[0] == e.v[0] && v[1] == e.v[1];
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.v);
    }

    private int minV() {
        return v[0] > v[1] ? v[1] : v[0];
    }

    private int maxV() {
        return v[0] < v[1] ? v[1] : v[0];
    }
    
    private void reorderV() {
        int min = minV();
        int max = maxV();
        v[0] = min;
        v[1] = max;
    }

}
