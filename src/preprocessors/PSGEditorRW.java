package preprocessors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * @author jstar
 */
public class PSGEditorRW extends JFrame {

    private DrawingPanel panel;

    public PSGEditorRW() {
        setTitle("Planar Straight Graph Editor");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        panel = new DrawingPanel();
        add(panel);

        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem loadItem = new JMenuItem("Load");
        loadItem.addActionListener(e -> panel.loadFromFile());
        fileMenu.add(loadItem);
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> panel.saveToFile());
        fileMenu.add(saveItem);

        fileMenu.addSeparator();
        JMenuItem exportItem = new JMenuItem("Export to .poly");
        exportItem.addActionListener(e -> panel.exportToPoly());
        fileMenu.add(exportItem);

        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem clearItem = new JMenuItem("Remove all");
        clearItem.addActionListener(e -> {
            panel.clearAllPolygons();
            panel.adjustCursor();
        });
        editMenu.add(clearItem);
        JMenuItem rmItem = new JMenuItem("Remove selected");
        rmItem.addActionListener(e -> {
            panel.remove = !panel.remove;
            panel.adjustCursor();
        });
        editMenu.add(rmItem);
        Thread t = new Thread() {
            {
                this.setDaemon(true);
            }

            @Override
            public void run() {
                while (true) {
                    try {
                        sleep(200);
                    } catch (InterruptedException e) {

                    }
                    rmItem.setText(panel.remove ? "Finish remove" : "Remove selected");
                }
            }
        };
        t.start();

        JMenu optMenu = new JMenu("Options");
        JMenuItem snapEnlargeItem = new JMenuItem("Double snap size");
        snapEnlargeItem.addActionListener(e -> {
            panel.snap *= 2;
            panel.repaint();
        });
        optMenu.add(snapEnlargeItem);
        JMenuItem snapDecreaseItem = new JMenuItem("Halve snap size");
        snapDecreaseItem.addActionListener(e -> {
            panel.snap = Math.round(panel.snap / 2);
            panel.snap = panel.snap == 0 ? 1 : panel.snap;
            panel.repaint();
        });
        optMenu.add(snapDecreaseItem);
        JMenuItem xAxisItem = new JMenuItem("Scale X axis");
        xAxisItem.addActionListener(e -> {
            String m = JOptionPane.showInputDialog(this, "Range?", "Set range of horizontal axis", JOptionPane.QUESTION_MESSAGE);
            try {
                String[] w = m.trim().split("\\s+");
                float xl = Float.parseFloat(w[0]);
                float xh = Float.parseFloat(w[1]);
                if (xl >= xh) {
                    throw new IllegalArgumentException(m + "???");
                }
                panel.setXAxis(xl, xh);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid value, x axis not changed.", "Set range of horizontal axis", JOptionPane.QUESTION_MESSAGE);
            }
        });
        optMenu.add(xAxisItem);
        JMenuItem yAxisItem = new JMenuItem("Scale Y axis");
        yAxisItem.addActionListener(e -> {
            String m = JOptionPane.showInputDialog(this, "Range?", "Set range of vertical axis", JOptionPane.QUESTION_MESSAGE);
            try {
                String[] w = m.trim().split("\\s+");
                float xl = Float.parseFloat(w[0]);
                float xh = Float.parseFloat(w[1]);
                if (xl >= xh) {
                    throw new IllegalArgumentException(m + "???");
                }
                panel.setYAxis(xl, xh);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid value, y axis not changed.", "Set range of vartical axis", JOptionPane.QUESTION_MESSAGE);
            }
        });
        optMenu.add(yAxisItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(optMenu);
        setJMenuBar(menuBar);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PSGEditorRW::new);
    }
}

class DrawingPanel extends JPanel {

    private final List<WorldPoint> vertices = new ArrayList<>();
    private final List<List<Integer>> polygons = new ArrayList<>();
    private final List<List<Integer>> holes = new ArrayList<>();

    private final List<Point> vertexViews = new ArrayList<>();
    private final List<Polygon> polygonViews = new ArrayList<>();
    private final List<Point> insidePontViews = new ArrayList<>();
    private final Set<Point> holeViews = new HashSet<>();

    private final List<Point> currentPoints = new ArrayList<>();

    private final float[] xAxis = {0.0f, 1.0f};
    private final float[] yAxis = {0.0f, 1.0f};
    int xoffset, yoffset, maxw, maxh;

    private void rebuildViews() {
       // System.err.println("#V:  " + vertices.size()
       //         + " #P:  " + polygons.size()
       //         + " #H:  " + holes.size()
       // );
        vertexViews.clear();
        for (WorldPoint wp : vertices) {
            vertexViews.add(world2Panel(wp));
        }
        polygonViews.clear();
        for (List<Integer> wp : polygons) {
            Polygon pol = new Polygon();
            for (Integer v : wp) {
                Point p = vertexViews.get(v);
                pol.addPoint(p.x, p.y);
            }
            polygonViews.add(pol);
        }
        for (List<Integer> wp : holes) {
            Polygon pol = new Polygon();
            for (Integer v : wp) {
                Point p = vertexViews.get(v);
                pol.addPoint(p.x, p.y);
            }
            polygonViews.add(pol);
        }
        insidePontViews.clear();
        for (Polygon p : polygonViews) {
            insidePontViews.add(getPointIside(p));
        }
        holeViews.clear();
        for (int i = 0; i < holes.size(); i++) {
            holeViews.add(insidePontViews.get(i + polygons.size()));
        }

        //System.err.println("#Vv: " + vertexViews.size()
        //        + " #Pv: " + polygonViews.size()
        //        + " #Hv: " + holeViews.size()
        //);
    }

    int snap = 10;
    private final int iPointMarkerSize = 10;

    boolean remove;

    public DrawingPanel() {
        setBackground(Color.WHITE);

        setFocusable(true); // Needed for KeyBindings
        requestFocusInWindow();
        // Keys -> actions
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "sPressed");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK), "SPressed");

        getActionMap().put("SPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                snap *= 2;
                repaint();
            }
        });
        getActionMap().put("sPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (snap >= 5) {
                    snap /= 2;
                }
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Left - add vertex
                if (SwingUtilities.isLeftMouseButton(e)) {
                    Point p = e.getPoint();
                    //System.err.print(p + " -> ");
                    if (remove) {
                        int i = insidePointClicked(p);
                        if (i != -1) {
                            if (i < polygons.size()) {
                                polygons.remove(i);
                            } else {
                                holes.remove(i - polygons.size());
                            }
                        }
                        adjustCursor();
                    } else {
                        if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                            int i = insidePointClicked(p);
                            //System.err.println(p + " -> " + i);
                            if (i != -1) {
                                if (holeViews.contains(insidePontViews.get(i))) {
                                    int hi = i - polygons.size();
                                    List<Integer> hol = holes.get(hi);
                                    holes.remove(hi);
                                    polygons.add(hol);
                                } else {
                                    List<Integer> pol = polygons.get(i);
                                    polygons.remove(i);
                                    holes.add(pol);
                                }
                            }
                        } else {
                            int x = (int) (Math.round((p.getX() - xoffset) / (float) snap) * snap) + xoffset;
                            int y = (int) (Math.round((p.getY() - yoffset) / (float) snap) * snap) + yoffset;
                            p.setLocation(x, y);
                            //System.err.println(p);
                            currentPoints.add(p);
                        }
                    }
                    repaint();
                } // Right - add polygon
                else if (SwingUtilities.isRightMouseButton(e) && currentPoints.size() >= 3 && !remove) {
                    ArrayList<Integer> pol = new ArrayList<>();
                    for (Point point : currentPoints) {
                        int i = vertexViews.indexOf(point);
                        if (i >= 0) {
                            pol.add(i);
                        } else {
                            vertices.add(panel2World(point));
                            pol.add(vertices.size() - 1);
                        }
                    }
                    polygons.add(pol);
                    currentPoints.clear();
                    repaint();
                }
            }
        }
        );
    }

    public void setXAxis(float l, float h) {
        xAxis[0] = l;
        xAxis[1] = h;
    }

    public void setYAxis(float l, float h) {
        yAxis[0] = l;
        yAxis[1] = h;
    }

    private Point getPointIside(Polygon p) {
        if (p.npoints > 3) {
            int cx = (int) ((p.xpoints[0] * 95. + p.xpoints[2] * 5.) / 100.);
            int cy = (int) ((p.ypoints[0] * 95. + p.ypoints[2] * 5.) / 100.);
            return new Point(cx, cy);
        } else {
            int cx = (int) ((p.xpoints[0] * 90. + p.xpoints[1] * 5. + p.xpoints[2] * 5.) / 100.);
            int cy = (int) ((p.ypoints[0] * 90. + p.ypoints[1] * 5. + p.ypoints[2] * 5.) / 100.);
            return new Point(cx, cy);
        }
    }

    private int insidePointClicked(Point p) {
        for (int i = 0; i < insidePontViews.size(); i++) {
            Point ip = insidePontViews.get(i);
            if (Math.sqrt((p.x - ip.x) * (p.x - ip.x) + (p.y - ip.y) * (p.y - ip.y)) < snap) {
                return i;
            }
        }
        return -1;
    }

    public void adjustCursor() {
        if (getNoPolygons() == 0) {
            remove = false;
        }
        setCursor(Cursor.getPredefinedCursor(remove ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public void clearAllPolygons() {
        vertices.clear();
        polygons.clear();
        holes.clear();
        currentPoints.clear();
        repaint();
    }

    public int getNoPolygons() {
        return polygons.size() + holes.size();
    }

    private WorldPoint panel2World(Point p) {
        float x = xAxis[0] + ((float) (p.getX() - xoffset)) / (maxw * snap) * (xAxis[1] - xAxis[0]);
        float y = yAxis[0] + ((float) (getHeight() - p.getY() - yoffset)) / (maxh * snap) * (yAxis[1] - yAxis[0]);
        return new WorldPoint(x, y);
    }

    private Point world2Panel(WorldPoint p) {
        int x = (int) ((p.x - xAxis[0]) / (xAxis[1] - xAxis[0]) * maxw * snap) + xoffset;
        int y = getHeight() - (int) ((p.y - yAxis[0]) / (yAxis[1] - yAxis[0]) * maxh * snap) - yoffset;
        return new Point(x, y);
    }

    public void exportToPoly() {
        JFileChooser chooser = new JFileChooser();
        int option = chooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(vertices.size() + " 2 0 0");
                writer.newLine();
                for (int i = 0; i < vertices.size(); i++) {
                    writer.write(" " + i + " " + vertices.get(i).x + " " + vertices.get(i).y);
                    writer.newLine();
                }
                int nseg = 0;
                for (List<Integer> p : polygons) {
                    nseg += p.size();
                }
                for (List<Integer> p : holes) {
                    nseg += p.size();
                }
                writer.write(nseg + " 0\n");
                int seg = 0;
                for (List<Integer> p : polygons) {
                    for (int i = 1; i < p.size(); i++) {
                        writer.write(" " + seg + " " + p.get(i - 1) + " " + p.get(i) + " 0\n");
                        seg++;
                    }
                    writer.write(" " + seg + " " + p.get(p.size() - 1) + " " + p.get(0) + " 0\n");
                    seg++;
                }
                for (List<Integer> p : holes) {
                    for (int i = 1; i < p.size(); i++) {
                        writer.write(" " + seg + " " + p.get(i - 1) + " " + p.get(i) + " 0\n");
                        seg++;
                    }
                    writer.write(" " + seg + " " + p.get(p.size() - 1) + " " + p.get(0) + " 0\n");
                    seg++;
                }
                writer.write(holes.size() + "\n");
                int hi = 0;
                for (Point h : holeViews) {
                    WorldPoint c = panel2World(h);
                    writer.write(hi + " " + c.x + " " + c.y + "\n");
                    hi++;
                }
                writer.write(polygons.size() + "\n");
                for (int i = 0; i < polygons.size(); i++) {
                    Point pi = insidePontViews.get(i);
                    WorldPoint c = panel2World(pi);
                    writer.write(i + " " + c.x + " " + c.y + " " + (i + 1) + "\n");
                }
                JOptionPane.showMessageDialog(this, file.getName() + " saved!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Write error: " + e.getMessage());
            }
        }
    }

    public void saveToFile() {
        JFileChooser chooser = new JFileChooser();
        int option = chooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(xAxis[0] + " " + xAxis[1] + " " + yAxis[0] + " " + yAxis[1] + " " +"\n");
                writer.write(vertices.size() + "\n");
                for (WorldPoint p : vertices ) {
                    writer.write(" " + p.x + " " + p.y + "\n");
                }
                writer.write(polygons.size() + "\n");
                for (List<Integer> p : polygons) {
                    writer.write(" " + p.size());
                    for (Integer i : p ) {
                        writer.write(" " + i);
                    }
                    writer.newLine();
                }
                writer.write(holes.size() + "\n");
                for (List<Integer> p : holes) {
                    writer.write(" " + p.size());
                    for (Integer i : p ) {
                        writer.write(" " + i);
                    }
                    writer.newLine();
                }
                JOptionPane.showMessageDialog(this, file.getName() + " saved!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Write error: " + e.getMessage());
            }
        }
    }

    public void loadFromFile() {
        JFileChooser chooser = new JFileChooser();
        int option = chooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (Scanner scnr = new Scanner(new FileReader(file))) {
                scnr.useLocale(Locale.ENGLISH);
     
                xAxis[0] = (float) scnr.nextDouble();
                xAxis[1] = (float) scnr.nextDouble();
                yAxis[0] = (float) scnr.nextDouble();
                yAxis[1] = (float) scnr.nextDouble();
                //System.err.println("Got header");
                vertices.clear();
                int nv = scnr.nextInt();
                //System.err.println("nv=" + nv);
                for (int i = 0; i < nv; i++) {
                    float x = scnr.nextFloat();
                    float y = scnr.nextFloat();
                    vertices.add(new WorldPoint(x, y));
                }
                //System.err.println("Got " + nv + " vertices");
                int np = scnr.nextInt();
                //System.err.println("To read " + np + " polygonViews");
                polygons.clear();
                for (int i = 0; i < np; i++) {
                    nv = scnr.nextInt();
                    List<Integer> pol = new ArrayList<>();
                    for (int v = 0; v < nv; v++) {
                        pol.add( scnr.nextInt() );
                    }
                    polygons.add(pol);
                }
                np = scnr.nextInt();
                //System.err.println("To read " + np + " polygonViews");
                holes.clear();
                for (int i = 0; i < np; i++) {
                    nv = scnr.nextInt();
                    List<Integer> pol = new ArrayList<>();
                    for (int v = 0; v < nv; v++) {
                        pol.add( scnr.nextInt() );
                    }
                    holes.add(pol);
                }
                repaint();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Read error: " + e.getClass() + ": " + e.getMessage());
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Draw the snap grid
        int l = 210;
        float[] dashPattern = {1f, 2f}; // 1px dot, 2px white
        Stroke dashed = new BasicStroke(
                1f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10f,
                dashPattern,
                0f
        );
        Stroke strokeBackup = g2.getStroke();
        g2.setStroke(dashed);
        g2.setColor(new Color(l, l, l));
        int width = getWidth(), height = getHeight();
        maxw = width / snap - 1;
        xoffset = (width - maxw * snap) / 2;
        maxh = height / snap - 1;
        yoffset = (height - maxh * snap) / 2;
        for (int i = 0; i <= maxw; i++) {
            g2.drawLine(xoffset + (i * snap), yoffset, xoffset + (i * snap), height - yoffset);
        }
        for (int i = 0; i <= maxh; i++) {
            g2.drawLine(xoffset, yoffset + (i * snap), width - xoffset, yoffset + (i * snap));
        }
        g2.setStroke(strokeBackup);

        rebuildViews();

        // Draw with BLUE already created polygonViews
        g2.setColor(Color.BLUE);
        g2.setStroke(new BasicStroke(1.5f));
        for (Polygon p : polygonViews) {
            g2.drawPolygon(p);
        }
        g2.setStroke(strokeBackup);

        g2.setColor(Color.BLUE);
        for (Point p : insidePontViews) {
            g2.fillOval(p.x - iPointMarkerSize / 2, p.y - iPointMarkerSize / 2, iPointMarkerSize, iPointMarkerSize);
        }

        g2.setColor(Color.ORANGE);
        for (Point p : holeViews) {
            g2.fillOval(p.x - iPointMarkerSize / 2, p.y - iPointMarkerSize / 2, iPointMarkerSize, iPointMarkerSize);
        }

        // Draw with RED color while creating
        g2.setColor(Color.RED);
        for (int i = 0; i < currentPoints.size() - 1; i++) {
            Point p1 = currentPoints.get(i);
            Point p2 = currentPoints.get(i + 1);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    class WorldPoint {

        float x;
        float y;

        public WorldPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
