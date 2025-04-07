package miscutils;

import java.util.Map;

/**
 *
 * @author jstar
 */
public class MiscUtils {

    public static <T> String arrayToString(T[][] array) {
        StringBuilder b = new StringBuilder("[\n");
        for (T[] row : array) {
            for (T el : row) {
                b.append(" ").append(el.toString());
            }
            b.append("\n");
        }
        b.append("]");
        return b.toString();
    }

    public static String makeString(Object o) {
        if( o == null )
            return "";
        if (o.getClass().isArray()) {
            System.err.println( o.getClass().getCanonicalName() );
            StringBuilder s = new StringBuilder("[");
            Object [] t = (Object [])o;
            for( Object e : t )
                s.append( ",").append(makeString(e));
            s.append(" ]");
            return s.toString();
        } else {
            return o.toString();
        }
    }

    public static <K, V> String mapToString(Map<K, V> map) {
        StringBuilder b = new StringBuilder("{\n");
        for (K k : map.keySet()) {
            b.append(" ").append(k).append("->").append(makeString(map.get(k)));
        }
        b.append("\n}");
        return b.toString();
    }

    public static String arrayToString(double[][] array) {
        StringBuilder b = new StringBuilder("[\n");
        for (double[] row : array) {
            for (double el : row) {
                b.append(" ").append(el);
            }
            b.append("\n");
        }
        b.append("]");
        return b.toString();
    }

}
