/*
 * Do what you want with this file
 */
package fem.mesh;

import fem.EleIntegral;

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
        int [] markers = getMarkers();
        return markers == null ? 0 : markers[0];
    }
}
