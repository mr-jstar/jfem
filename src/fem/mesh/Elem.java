/*
 * Do what you want with this file
 */
package fem.mesh;

/**
 *
 * @author jstar
 */
public interface Elem extends Iterable<Integer> {

    int[] getVertices();

    void setVertices(int[] v);

    int[] getMarkers();

    void setMarkers(int[] m);

    default int getSubdomain() {
        int[] markers = getMarkers();
        return markers == null ? 0 : markers[0];
    }

    default void setSubdomain(int s) {
        int[] markers = getMarkers();
        if (markers == null) {
            markers = new int[1];
        }
        markers[0] = s;
        setMarkers(markers);
    }
}
