/*
 * Do what you want with this file
 */
package fem.mesh;

/**
 *
 * @author jstar
 */
public interface Edge extends Iterable<Integer> {

    int[] getVertices();

    void setVertices(int[] v);

    int[] getMarkers();

    void setMarkers(int[] m);
}
