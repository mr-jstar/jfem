/*
 * Do what you want with this file
 */
package gui;

import fem.EleIntegral;
import fem.FEM;
import fem.TetraLaplace;
import fem.TriangleLaplace;
import fem.mesh.IMesh;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import sm.solvers.GMRES;

/**
 *
 * @author jstar
 */
public class SimpleGUI extends JFrame {

    private static final Font FONT18 = new Font("SansSerif", Font.PLAIN, 18);
    private static final Font FONT24 = new Font("SansSerif", Font.PLAIN, 24);
    private static final String CONFIG_FILE = ".femconfig";

    private final int vSize = 2;

    private int vertexSelectionRadius = 20;

    private DrawingPanel drawingPanel;

    private IMesh mesh;
    private FEM model;

    private boolean meshIsVisible;
    private boolean fieldIsVisible;
    private boolean inDefBoundary = false;
    private final Set<Integer> currentVertexSelection = new TreeSet<>();

    private final ArrayList<PointPosition> xy = new ArrayList<>();
    private final TreeMap<Integer, Double> bndNodes = new TreeMap<>();

    private final JButton loadButton;
    private final JButton meshButton;
    private final JButton bndButton;
    private final JButton computeButton;
    private final JButton fieldButton;
    private final JButton clearButton;
    private final JButton saveButton;
    private final JButton exitButton;

    private final JLabel message;

    public SimpleGUI() {
        setTitle("Simple FEM GUI (Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 1200);
        setLocationRelativeTo(null);

        UIManager.put("OptionPane.messageFont", FONT18);
        UIManager.put("OptionPane.buttonFont", FONT18);

        JPanel buttonPanel = new JPanel();
        loadButton = new JButton("Load mesh");
        meshButton = new JButton("Draw mesh");
        bndButton = new JButton("Add boundary condition(s)");
        computeButton = new JButton("Compute");
        fieldButton = new JButton("Draw field");
        clearButton = new JButton("Clear");
        saveButton = new JButton("Save figure");
        exitButton = new JButton("Close");

        JButton[] buttons = {loadButton, meshButton, bndButton, computeButton, fieldButton, clearButton, saveButton, exitButton};

        for (JButton btn : buttons) {
            btn.setFont(FONT18);
        }

        loadButton.addActionListener(e -> loadFile());
        meshButton.addActionListener(e -> drawMesh());
        clearButton.addActionListener(e -> drawingPanel.clear());
        bndButton.addActionListener(e -> boundary());
        computeButton.addActionListener(e -> computeField());
        fieldButton.addActionListener(e -> drawField());
        saveButton.addActionListener(e -> drawingPanel.saveImage());
        exitButton.addActionListener(e -> System.exit(0));

        for (JButton btn : buttons) {
            buttonPanel.add(btn);
        }

        setJMenuBar(createMenuBar(buttons));

        drawingPanel = new DrawingPanel();

        message = new JLabel();
        message.setFont(FONT18);
        JPanel messagePanel = new JPanel();
        messagePanel.add(message);

        add(buttonPanel, BorderLayout.NORTH);
        add(drawingPanel, BorderLayout.CENTER);
        add(messagePanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JMenuBar createMenuBar(JButton[] btns) {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");

        for (JButton b : btns) {
            JMenuItem nextItem = new JMenuItem(b.getText());
            for (ActionListener al : b.getActionListeners()) {
                nextItem.addActionListener(al);
            }
            menu.add(nextItem);
        }

        menuBar.add(menu);
        return menuBar;
    }

    private void loadFile() {
        JFileChooser fileChooser = new JFileChooser(getLastUsedDirectory());
        setFontRecursively(fileChooser, FONT18);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File meshFile = fileChooser.getSelectedFile();
            try {
                if (meshFile.getAbsolutePath().endsWith(".mesh")) {
                    mesh = fem.Util.loadINRIAMesh(meshFile.getAbsolutePath());
                } else if (meshFile.getAbsolutePath().endsWith(".ele") || meshFile.getAbsolutePath().endsWith(".node")) {
                    mesh = fem.Util.loadTriangleMesh(meshFile.getAbsolutePath());
                } else {
                    throw new IllegalArgumentException("Unknown mesh format: " + meshFile);
                }
                saveLastUsedDirectory(meshFile.getParent());
                meshIsVisible = false;
                fieldIsVisible = false;
                inDefBoundary = false;
                xy.clear();
                bndNodes.clear();
                model = null;
                drawingPanel.repaint();
                JOptionPane.showMessageDialog(this, "Mesh loaded from: " + meshFile.getAbsolutePath() + "\n" + mesh.getNoVertices() + " vertices & " + mesh.getNoElems() + " elements");
            } catch (Exception e) {
                mesh = null;
                JOptionPane.showMessageDialog(this, "Unable to load mesh from: " + meshFile.getAbsolutePath());
            }
        }
    }

    private void drawMesh() {
        meshIsVisible = true;
        drawingPanel.repaint();
    }

    private void boundary() {
        if (inDefBoundary) {
            if (currentVertexSelection.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No nodes selected, try again.");
            } else {
                bndButton.setText("Add boundary condition(s)");
                bndButton.setForeground(Color.BLACK);
                double value;
                try {
                    String m = JOptionPane.showInputDialog("Value?");
                    if( m == null )
                        throw new NumberFormatException();
                    value = Double.parseDouble(m);
                    for (Integer v : currentVertexSelection) {
                        bndNodes.put(v, value);
                    }
                    inDefBoundary = false;
                    message.setText("");
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid value, click the button once more.");
                }
                System.err.println(bndNodes);
            }
            model = null;
            drawingPanel.repaint();
        } else {
            if (meshIsVisible) {
                JOptionPane.showMessageDialog(this, "Click on the node to select it, " +
                        "click on selected to unselect,\n"+
                        "drag mouse to select/deselect all nodes in the rectangle\n"+
                        "when done click button again to be asked for the value.");
                inDefBoundary = true;
                currentVertexSelection.clear();
                bndButton.setForeground(Color.RED);
                bndButton.setText("Click to finish selection");
            } else {
                JOptionPane.showMessageDialog(this, "Draw the mesh first!");
            }
        }
    }

    private void computeField() {
        EleIntegral integral = mesh.getElem(0).getVertices().length == 3 ? new TriangleLaplace() : new TetraLaplace();
        double[] srcs = new double[mesh.getNoSubdomains() + 1];
        double[] mats = new double[srcs.length];
        for (int i = 0; i < srcs.length; i++) {
            mats[i] = 1;
        }
        int[] bndNds = new int[bndNodes.size()];
        double[] bndVals = new double[bndNds.length];
        int i = 0;
        for (Integer v : bndNodes.keySet()) {
            bndNds[i] = v;
            bndVals[i++] = bndNodes.get(v);
        }
        model = new FEM(mesh, mats, srcs, bndNds, bndVals);
        model.buildSymmetricMatrix(true);
        Thread thread = new Thread() {
            @Override
            public void run() {
                long startTime = System.nanoTime();
                fieldButton.setEnabled(false);
                computeButton.setEnabled(false);
                loadButton.setEnabled(false);
                model.assemble(integral);
                if (mesh.getNoVertices() < 10000) {
                    model.dump("data/hr.m");
                }
                model.solve(new GMRES());
                fieldButton.setEnabled(true);
                computeButton.setEnabled(true);
                loadButton.setEnabled(true);
                long elapsedTime = System.nanoTime() - startTime;
                JOptionPane.showMessageDialog(SimpleGUI.this, "Field computed in " + elapsedTime / 1000000 + " miliseconds");
            }
        };
        thread.setDaemon(true);
        thread.start();
        //double[] V = model.getFld();
        //double[] minmax = range(V);
        //System.out.println("V in <" + minmax[0] + "," + minmax[1] + ">");
    }

    private static double[] range(double[] v) {
        double[] range = {v[0], v[0]};
        for (int i = 1; i < v.length; i++) {
            if (v[i] < range[0]) {
                range[0] = v[i];
            }
            if (v[i] > range[1]) {
                range[1] = v[i];
            }
        }
        return range;
    }

    private void drawField() {
        fieldIsVisible = true;
        drawingPanel.repaint();
    }

    private void setFontRecursively(Component comp, Font font) {
        comp.setFont(font);
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                setFontRecursively(child, font);
            }
        }
    }

    private String getLastUsedDirectory() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
                return br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ".";
    }

    private void saveLastUsedDirectory(String directory) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CONFIG_FILE))) {
            bw.write(directory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int findNearestVertex(int x, int y) {
        int nV = -1;
        int minDistance = Integer.MAX_VALUE;
        for (int v = 0; v < xy.size(); v++) {
            PointPosition p = xy.get(v);
            int distance = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y);
            if (distance < minDistance && distance < vertexSelectionRadius) {
                minDistance = distance;
                nV = v;
            }
        }
        return nV;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimpleGUI::new);
    }

    // Panel do rysowania
    class DrawingPanel extends JPanel {

        private Graphics g;
        private int prevX, prevY;
        private int currX, currY;
        private boolean pressed;

        public DrawingPanel() {
            setDoubleBuffered(false);
            setBackground(Color.WHITE);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (inDefBoundary) {
                        int nearestVertex = findNearestVertex(prevX, prevY);
                        if (nearestVertex >= 0) {
                            System.err.println(prevX + " " + prevY + " => " + nearestVertex);
                            if (currentVertexSelection.contains(nearestVertex)) {
                                currentVertexSelection.remove(nearestVertex);
                            } else {
                                currentVertexSelection.add(nearestVertex);
                            }
                            message.setText("selected nodes: " + currentVertexSelection.toString());
                        }
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    prevX = e.getX();
                    prevY = e.getY();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    currX = e.getX();
                    currY = e.getY();
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());

            if (inDefBoundary && ( currX != prevX || currY != prevY) ) {
                int xp = Math.min(currX, prevX);
                int yp = Math.min(currY, prevY);
                int width = Math.abs(currX - prevX);
                int height = Math.abs(currY - prevY);
                g.setColor(Color.ORANGE);
                g.drawRect(xp, yp, width, height);
                for (int v = 0; v < xy.size(); v++) {
                    PointPosition p = xy.get(v);
                    if (p.x >= xp && p.x <= xp + width && p.y >= yp && p.y <= yp + height) {
                        if (currentVertexSelection.contains(v)) {
                            currentVertexSelection.remove(v);
                        } else {
                            currentVertexSelection.add(v);
                        }
                    }
                }
                message.setText("selected nodes: " + currentVertexSelection.toString());
                prevX = currX;
                prevY = currY;
            }

            if (meshIsVisible) {
                if (mesh == null || mesh.getDim() != 2) {
                    return;
                }

                double[] xrange = {Double.MAX_VALUE, -Double.MAX_VALUE};
                double[] yrange = {Double.MAX_VALUE, -Double.MAX_VALUE};

                for (int v = 0; v < mesh.getNoVertices(); v++) {
                    double[] x = mesh.getVertex(v).getX();
                    if (x[0] < xrange[0]) {
                        xrange[0] = x[0];
                    }
                    if (x[0] > xrange[1]) {
                        xrange[1] = x[0];
                    }
                    if (x[1] < yrange[0]) {
                        yrange[0] = x[1];
                    }
                    if (x[1] > yrange[1]) {
                        yrange[1] = x[1];
                    }
                }
                xrange[1] -= xrange[0];
                yrange[1] -= yrange[0];

                double margin = 0.05;
                double xbase = margin * getWidth();
                double ybase = margin * getHeight();
                double width = getWidth() * (1 - 2 * margin);
                double height = getHeight() * (1 - 2 * margin);

                xy.clear();
                g.setColor(Color.BLUE);
                g.setFont(FONT18);
                for (int v = 0; v < mesh.getNoVertices(); v++) {
                    double[] x = mesh.getVertex(v).getX();
                    int ix = (int) Math.round(xbase + (x[0] - xrange[0]) / xrange[1] * width);
                    int iy = (int) Math.round(ybase + (1.0 - (x[1] - yrange[0]) / yrange[1]) * height);
                    g.fillOval(ix - vSize / 2, iy - vSize / 2, vSize, vSize);
                    if (mesh.getNoVertices() < 100) {
                        if( currentVertexSelection.contains(v)) {
                            g.setColor(Color.RED);
                            g.drawString(String.valueOf(v), ix, iy);
                            g.setColor(Color.BLUE);
                        } else {
                            g.drawString(String.valueOf(v), ix, iy);
                        }
                    }
                    xy.add(new PointPosition(ix, iy));
                }

                g.setColor(Color.GREEN);
                int[] v = new int[mesh.getElem(0).getVertices().length + 1];
                for (int e = 0; e < mesh.getNoElems(); e++) {
                    System.arraycopy(mesh.getElem(e).getVertices(), 0, v, 0, v.length - 1);
                    v[v.length - 1] = v[0];
                    for (int i = 1; i < v.length; i++) {
                        PointPosition p1 = xy.get(v[i - 1]);
                        PointPosition p2 = xy.get(v[i]);
                        g.drawLine(p1.x, p1.y, p2.x, p2.y);
                    }
                }

                g.setColor(Color.RED);
                int dh = g.getFontMetrics().getHeight();
                for (Integer b : bndNodes.keySet()) {
                    PointPosition p = xy.get(b);
                    g.drawString(String.valueOf(bndNodes.get(b)), p.x, p.y + dh);
                }
            }
            if (fieldIsVisible) {
                if (model != null) {
                    double[] fld = model.getFld();
                    if (fld != null) {
                        double[] frng = range(fld);
                        ColorMap cm = new ColorMap((float) frng[0], (float) frng[1]);
                        /*
                    g.setColor(Color.RED);
                    int dh = g.getFontMetrics().getHeight();
                    for (int v = 0; v < fld.length; v++) {
                        PointPosition p = xy.get(v);
                        g.drawString(String.format("%6.3g", fld[v]), p.x, p.y + dh);
                    }
                         */
                        double[] efld = new double[mesh.getElem(0).getVertices().length];
                        for (int e = 0; e < mesh.getNoElems(); e++) {
                            int[] ev = mesh.getElem(e).getVertices();
                            for (int v = 0; v < ev.length; v++) {
                                efld[v] = fld[ev[v]];
                            }
                            fillElem(g, ev, efld, cm);
                        }
                    }
                }
            }
            this.g = g;
        }

        public void fillElem(Graphics g, int[] v, double[] fld, ColorMap cm) {
            PointPosition[] p = new PointPosition[v.length];
            for (int i = 0; i < v.length; i++) {
                p[i] = xy.get(v[i]);
            }
            int minX = Math.min(p[0].x, Math.min(p[1].x, p[2].x));
            int maxX = Math.max(p[0].x, Math.max(p[1].x, p[2].x));
            int minY = Math.min(p[0].y, Math.min(p[1].y, p[2].y));
            int maxY = Math.max(p[0].y, Math.max(p[1].y, p[2].y));
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    if (pointInTriangle(x, y, p)) {
                        double[] bc = getBarycentricCoordinates(x, y, p);
                        double fv = 0.0;
                        for (int i = 0; i < bc.length; i++) {
                            fv += bc[i] * fld[i];
                        }
                        g.setColor(cm.getColorForValue(fv));
                        g.drawRect(x, y, 1, 1);
                    }
                }
            }
        }

        private boolean pointInTriangle(int x, int y, PointPosition[] p) {
            int x1 = p[0].x, y1 = p[0].y;
            int x2 = p[1].x, y2 = p[1].y;
            int x3 = p[2].x, y3 = p[2].y;

            double d1 = sign(x, y, x1, y1, x2, y2);
            double d2 = sign(x, y, x2, y2, x3, y3);
            double d3 = sign(x, y, x3, y3, x1, y1);

            boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
            boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);

            return !(hasNeg && hasPos);
        }

        private double sign(double x, double y, double x1, double y1, double x2, double y2) {
            return (x - x2) * (y1 - y2) - (x1 - x2) * (y - y2);
        }

        private double[] getBarycentricCoordinates(int x, int y, PointPosition[] p) {
            double x1 = p[0].x, y1 = p[0].y;
            double x2 = p[1].x, y2 = p[1].y;
            double x3 = p[2].x, y3 = p[2].y;

            double detT = (y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3);

            double l1 = ((y2 - y3) * (x - x3) + (x3 - x2) * (y - y3)) / detT;
            double l2 = ((y3 - y1) * (x - x3) + (x1 - x3) * (y - y3)) / detT;
            double l3 = 1.0 - l1 - l2;

            return new double[]{l1, l2, l3}; // barycentryczne współczynniki (α, β, γ)
        }

        public void clear() {
            meshIsVisible = false;
            fieldIsVisible = false;
            repaint();
        }

        public void saveImage() {
            try {
                File output = new File("fem_fig.png");
                BufferedImage bufferedImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = bufferedImage.createGraphics();
                paint(g2d);
                g2d.dispose();
                javax.imageio.ImageIO.write(bufferedImage, "png", output);
                JOptionPane.showMessageDialog(this, "Saved as: " + output.getAbsolutePath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    class PointPosition {

        int x;
        int y;

        public PointPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
