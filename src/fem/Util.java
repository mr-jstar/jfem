package fem;

import fem.mesh.Elem;
import fem.mesh.IMesh;
import fem.mesh.Mesh;
import fem.mesh.Segment;
import fem.mesh.Tetra;
import fem.mesh.Triangle;
import fem.mesh.Vertex;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.ArrayList;
import sm.CRS;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author jstar
 */
public class Util {

    public static CRS symbForm(IMesh m) {
        Map<Integer, Set<Integer>> a = new HashMap<>();
        for (int e = 0; e < m.getNoElems(); e++) {
            int[] ev = m.getElem(e).getVertices();
            for (int i = 0; i < ev.length; i++) {
                for (int j = 0; j < ev.length; j++) {
                    if (!a.containsKey(i)) {
                        a.put(i, new TreeSet<>());
                    }
                    a.get(i).add(j);
                }
            }
        }
        int[] ia = new int[a.size() + 1];
        ia[0] = 0;
        for (int i = 0; i < a.size(); i++) {
            ia[i + 1] = ia[i] + a.get(i).size();
        }
        int[] ja = new int[ia[a.size()]];
        double[] v = new double[ja.length];
        for (int i = 0; i < a.size(); i++) {
            Set<Integer> s = a.get(i);
            int j = ia[i];
            for (Integer k : s) {
                ja[j++] = k;
            }
        }
        return new CRS(ia, ja, v);
    }

    public static int nextLabel(BufferedReader r, String label, String path) throws Exception {
        String line = r.readLine();
        if (line == null || !line.startsWith(label)) {
            throw new IllegalArgumentException("File " + path + " does not contain INRIA Mesh");
        }
        line = r.readLine();
        int value;
        try {
            value = Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("File " + path + " does not contain INRIA Mesh (" + line + ")");
        }
        return value;
    }

    public static IMesh loadINRIAMesh(String path) throws Exception {
        String line = null;
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            line = r.readLine();
            if (line == null || !line.startsWith(" MeshVersionFormatted")) {
                throw new IllegalArgumentException("File " + path + " does not contain INRIA Mesh");
            }
            int dim = nextLabel(r, " Dimension", path);
            if (dim != 2 && dim != 3) {
                throw new IllegalArgumentException("File " + path + " does not contain INRIA Mesh");
            }
            int nVerts = nextLabel(r, " Vertices", path);
            List<Vertex> vl = new ArrayList<>(nVerts);
            for (int v = 0; v < nVerts; v++) {
                line = r.readLine();
                String[] w = line.trim().split("\\s+");
                double[] x = new double[dim];
                for (int d = 0; d < dim; d++) {
                    x[d] = Double.parseDouble(w[d]);
                }
                if (w.length > dim) {
                    int[] m = new int[w.length - dim];
                    for (int i = 0; i < m.length; i++) {
                        m[i] = Integer.parseInt(w[dim + i]);
                    }
                    vl.add(new Vertex(x, m));
                } else {
                    vl.add(new Vertex(x));
                }
            }
            int nEdges = nextLabel(r, " Edges", path);
            List<Elem> edgl = new ArrayList<>(nEdges);
            for (int e = 0; e < nEdges; e++) {
                line = r.readLine();
                String[] w = line.trim().split("\\s+");
                int[] n = new int[2];
                n[0] = Integer.parseInt(w[0]) - 1;
                n[1] = Integer.parseInt(w[1]) - 1;
                if (w.length > 2) {
                    int[] m = new int[w.length - 2];
                    for (int i = 0; i < m.length; i++) {
                        m[i] = Integer.parseInt(w[2 + i]);
                    }
                    edgl.add(new Segment(n, m));
                } else {
                    edgl.add(new Segment(n));
                }
            }

            int nTriangles = nextLabel(r, " Triangles", path);
            List<Elem> trl = new ArrayList<>(nTriangles);
            for (int e = 0; e < nTriangles; e++) {
                line = r.readLine();
                String[] w = line.trim().split("\\s+");
                int[] n = new int[3];
                n[0] = Integer.parseInt(w[0]) - 1;
                n[1] = Integer.parseInt(w[1]) - 1;
                n[2] = Integer.parseInt(w[2]) - 1;
                if (w.length > 3) {
                    int[] m = new int[w.length - 3];
                    for (int i = 0; i < m.length; i++) {
                        m[i] = Integer.parseInt(w[3 + i]);
                    }
                    trl.add(new Triangle(n, m));
                } else {
                    trl.add(new Triangle(n));
                }
            }

            int nTetra = nextLabel(r, " Tetrahedra", path);
            List<Elem> tetl = new ArrayList<>(nTetra);
            for (int e = 0; e < nTetra; e++) {
                line = r.readLine();
                String[] w = line.trim().split("\\s+");
                int[] n = new int[4];
                n[0] = Integer.parseInt(w[0]) - 1;
                n[1] = Integer.parseInt(w[1]) - 1;
                n[2] = Integer.parseInt(w[2]) - 1;
                n[3] = Integer.parseInt(w[3]) - 1;
                if (w.length > 4) {
                    int[] m = new int[w.length - 4];
                    for (int i = 0; i < m.length; i++) {
                        m[i] = Integer.parseInt(w[4 + i]);
                    }
                    tetl.add(new Tetra(n, m));
                } else {
                    tetl.add(new Tetra(n));
                }
            }
            return new Mesh(vl, tetl, trl, edgl);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("File " + path + " does not contain INRIA Mesh (" + line + " => " + e.getMessage() + ")");
        }
    }

    public static String nextLine(BufferedReader r) throws Exception {
        String line;
        do {
            if ((line = r.readLine()) == null) {
                return null;
            }
            line = line.trim();
        } while (line.length() == 0 || line.startsWith("#"));
        return line;
    }

    public static IMesh loadTriangleMesh(String path) throws Exception {
        String nodeFile = path.replaceFirst("\\.ele$", ".node");
        String eleFile = path.replaceFirst("\\.node$", ".ele");
        String edgeFile = eleFile.replaceFirst("\\.ele", ".edge");
        System.out.println(nodeFile);
        System.out.println(eleFile);
        String line = null;
        List<Vertex> vl;
        List<Elem> trl;
        int firstNodeNo = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(nodeFile))) {
            line = nextLine(r);
            String[] w = line.split("\\s+");
            int nVerts = Integer.parseInt(w[0]);
            int dim = Integer.parseInt(w[1]);
            int nAttributes = Integer.parseInt(w[2]);
            int nMarkers = Integer.parseInt(w[3]);
            vl = new ArrayList<>(nVerts);
            line = nextLine(r);
            w = line.split("\\s+");
            firstNodeNo = Integer.parseInt(w[0]);
            for (int v = 0; v < nVerts; v++) {
                w = line.split("\\s+");
                double[] x = new double[2];
                x[0] = Double.parseDouble(w[1]);
                x[1] = Double.parseDouble(w[2]);
                double[] attrib = null;
                if (nAttributes > 0) {
                    attrib = new double[nAttributes];
                    for (int i = 0; i < nAttributes; i++) {
                        attrib[i] = Double.parseDouble(w[i + 3]);
                    }
                }
                int mark;
                if (nMarkers > 0) {
                    mark = Integer.parseInt(w[3 + nAttributes]);
                    vl.add(new Vertex(x, mark, attrib));
                } else {
                    vl.add(new Vertex(x, attrib));
                }
                line = nextLine(r);
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("File " + nodeFile + " does not contain nodes of a triangle mesh (" + line + " => " + e.getClass() + "=>" + e.getMessage() + ")");
        }
        List<Elem> edgl = new ArrayList<>();
        if (Files.isReadable((new File(edgeFile)).toPath())) {
            try (BufferedReader r = new BufferedReader(new FileReader(edgeFile))) {
                line = nextLine(r);
                String[] w = line.split("\\s+");
                int nEdges = Integer.parseInt(w[0]);
                int nBndMarks = Integer.parseInt(w[1]);
                for (int e = 0; e < nEdges; e++) {
                    int[] nds = new int[2];
                    line = nextLine(r);
                    w = line.split("\\s+");
                    nds[0] = Integer.parseInt(w[1]) - firstNodeNo;
                    nds[1] = Integer.parseInt(w[2]) - firstNodeNo;
                    if (nBndMarks > 0) {
                        int bMark = Integer.parseInt(w[2]);
                        edgl.add(new Segment(nds, bMark));
                    } else {
                        edgl.add(new Segment(nds));
                    }
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("File " + edgeFile + " does not contain edges of a triangle mesh (" + line + " => " + e.getMessage() + ")");
            }
        }
        try (BufferedReader r = new BufferedReader(new FileReader(eleFile))) {
            line = nextLine(r);
            String[] w = line.split("\\s+");
            int nTrngls = Integer.parseInt(w[0]);
            int nNodes = Integer.parseInt(w[1]);
            int nAttributes = Integer.parseInt(w[2]);
            trl = new ArrayList<>(nTrngls);
            for (int e = 0; e < nTrngls; e++) {
                int[] nds = new int[nNodes];
                line = nextLine(r);
                w = line.split("\\s+");
                for (int i = 0; i < nNodes; i++) {
                    nds[i] = Integer.parseInt(w[i + 1]) - firstNodeNo;
                }
                int[] attrib = null;
                if (nAttributes > 0) {
                    attrib = new int[nAttributes];
                    for (int i = 0; i < nAttributes; i++) {
                        attrib[i] = (int) Double.parseDouble(w[i + 1 + nNodes]);
                    }
                    trl.add(new Triangle(nds, attrib));
                } else {
                    trl.add(new Triangle(nds));
                }
            }
            return new Mesh(vl, trl, new ArrayList<Elem>(), edgl);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("File " + eleFile + " does not contain triangles of a triangle mesh (" + line + " => " + e.getMessage() + ")");
        }
    }

}
