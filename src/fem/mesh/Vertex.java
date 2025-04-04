package fem.mesh;

/**
 *
 * @author jstar
 */
public class Vertex {

    private int dim;
    private double[] x;
    private double[] attributes;
    private int[] markers;

    public Vertex(double[] x) {
        dim = x.length;
        this.x = x;
    }

    public Vertex(double[] x, double[] attributes) {
        dim = x.length;
        this.x = x;
        this.attributes = attributes;
    }

    public Vertex(double[] x, int m, double[] attributes) {
        dim = x.length;
        this.x = x;
        this.markers = new int[1];
        this.markers[0] = m;
        this.attributes = attributes;
    }

    public Vertex(double[] x, int[] m) {
        dim = x.length;
        this.x = x;
        this.markers = m;
    }

    public Vertex(double[] x, int[] m, double[] attributes) {
        dim = x.length;
        this.x = x;
        this.markers = m;
        this.attributes = attributes;
    }

    public int getDim() {
        return dim;
    }

    public void setDim(int dim) {
        this.dim = dim;
    }

    public double[] getX() {
        return x;
    }

    public void setX(double[] x) {
        this.x = x;
    }

    public int[] getMarkers() {
        return markers;
    }

    public void setMarkers(int[] markers) {
        this.markers = markers;
    }

    /**
     * @return the attributes
     */
    public double[] getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(double[] attributes) {
        this.attributes = attributes;
    }
}
