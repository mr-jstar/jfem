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
public class PSGEditor extends JFrame {

    private DrawingPanel panel;

    public PSGEditor() {
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
        SwingUtilities.invokeLater(PSGEditor::new);
    }
}

class DrawingPanel extends JPanel {

    private final List<Polygon> polygons = new ArrayList<>();
    private final List<Point> insidePonts = new ArrayList<>();
    private final Set<Point> holes = new HashSet<>();

    private final List<Point> currentPoints = new ArrayList<>();

    private final float[] xAxis = {0.0f, 1.0f};
    private final float[] yAxis = {0.0f, 1.0f};
    int xoffset, yoffset, maxw, maxh;

    int snap = 10;
    private final int iPointMarkerSie = 10;

    boolean remove;

    public void setXAxis(float l, float h) {
        xAxis[0] = l;
        xAxis[1] = h;
    }

    public void setYAxis(float l, float h) {
        yAxis[0] = l;
        yAxis[1] = h;
    }

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
                snap /= 2;
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
                            if (holes.contains(insidePonts.get(i))) {
                                holes.remove(insidePonts.get(i));
                            }
                            insidePonts.remove(i);
                            polygons.remove(i);
                        }
                        adjustCursor();
                    } else {
                        if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                            int i = insidePointClicked(p);
                            if (i != -1) {
                                if (holes.contains(insidePonts.get(i))) {
                                    holes.remove(insidePonts.get(i));
                                } else {
                                    holes.add(insidePonts.get(i));
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
                    Polygon p = new Polygon();
                    for (Point point : currentPoints) {
                        p.addPoint(point.x, point.y);
                    }
                    polygons.add(p);
                    insidePonts.add(getPointIside(p));
                    currentPoints.clear();
                    repaint();
                }
            }
        }
        );
    }

    private Point getPointIside(Polygon p) {
        int cx = (int) ((p.xpoints[0] * 95. + p.xpoints[2] * 5.) / 100.);
        int cy = (int) ((p.ypoints[0] * 95. + p.ypoints[2] * 5.) / 100.);
        return new Point(cx, cy);
    }

    private int insidePointClicked(Point p) {
        for (int i = 0; i < insidePonts.size(); i++) {
            Point ip = insidePonts.get(i);
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
        polygons.clear();
        insidePonts.clear();
        holes.clear();
        currentPoints.clear();
        repaint();
    }

    public int getNoPolygons() {
        return polygons.size();
    }

    private WorldPoint panel2World(Point p) {
        float x = xAxis[0] + ((float) (p.getX() - xoffset)) / (maxw * snap) * (xAxis[1] - xAxis[0]);
        float y = yAxis[0] + ((float) (getHeight() - p.getY() - yoffset)) / (maxh * snap) * (yAxis[1] - yAxis[0]);
        return new WorldPoint(x, y);
    }

    public void exportToPoly() {
        JFileChooser chooser = new JFileChooser();
        int option = chooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                Set<Point> v = new HashSet<>();
                int nseg = 0;
                for (Polygon p : polygons) {
                    nseg += p.npoints;
                    for (int i = 0; i < p.npoints; i++) {
                        v.add(new Point(p.xpoints[i], p.ypoints[i]));
                    }
                }
                ArrayList<Point> l = new ArrayList<>(v);
                writer.write(l.size() + " 2 0 0");
                writer.newLine();
                for (int i = 0; i < l.size(); i++) {
                    WorldPoint p = panel2World(l.get(i));
                    writer.write(" " + i + " " + p.x + " " + p.y);
                    writer.newLine();
                }
                writer.write(nseg + " 0\n");
                int seg = 0;
                for (Polygon p : polygons) {
                    Point f = new Point(p.xpoints[0], p.ypoints[0]);
                    int fi = l.indexOf(f);
                    for (int i = 1; i < p.npoints; i++) {
                        Point e = new Point(p.xpoints[i], p.ypoints[i]);
                        int ei = l.indexOf(e);
                        writer.write(" " + seg + " " + fi + " " + ei + " 0\n");
                        fi = ei;
                        seg++;
                    }
                    writer.write(" " + seg + " " + fi + " " + l.indexOf(new Point(p.xpoints[0], p.ypoints[0])) + " 0\n");
                    seg++;
                }
                writer.write(holes.size() + "\n");
                int hi = 0;
                for (Point h : holes) {
                    WorldPoint c = panel2World(h);
                    writer.write(hi + " " + c.x + " " + c.y + "\n");
                    hi++;
                }
                writer.write((polygons.size()-holes.size()) + "\n");
                hi= 0;
                for (int i = 0; i < polygons.size(); i++) {
                    Point pi = insidePonts.get(i);
                    if (!holes.contains(pi)) {
                        WorldPoint c = panel2World(pi);
                        writer.write(hi + " " + c.x + " " + c.y + " " + (hi + 1) + "\n");
                        hi++;
                    }
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
                Set<Point> v = new HashSet<>();
                for (Polygon p : polygons) {
                    for (int i = 0; i < p.npoints; i++) {
                        v.add(new Point(p.xpoints[i], p.ypoints[i]));
                    }
                }
                ArrayList<Point> l = new ArrayList<>(v);
                writer.write(l.size() + " " + xAxis[0] + " " + xAxis[1] + " " + yAxis[0] + " " + yAxis[1] + "\n");
                for (int i = 0; i < l.size(); i++) {
                    Point p = l.get(i);
                    writer.write(" " + p.x + " " + p.y + "\n");
                }
                writer.write(polygons.size() + "\n");
                for (Polygon p : polygons) {
                    writer.write(" " + p.npoints);
                    for (int i = 0; i < p.npoints; i++) {
                        writer.write(" " + l.indexOf(new Point(p.xpoints[i], p.ypoints[i])));
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
                int nv = scnr.nextInt();
                //System.err.println("nv=" + nv);
                xAxis[0] = (float) scnr.nextDouble();
                xAxis[1] = (float) scnr.nextDouble();
                yAxis[0] = (float) scnr.nextDouble();
                yAxis[1] = (float) scnr.nextDouble();
                //System.err.println("Got header");
                ArrayList<Point> l = new ArrayList<>(nv);
                for (int i = 0; i < nv; i++) {
                    int x = scnr.nextInt();
                    int y = scnr.nextInt();
                    l.add(new Point(x, y));
                }
                //System.err.println("Got " + nv + " vertices");
                int np = scnr.nextInt();
                //System.err.println("To read " + np + " polygons");
                ArrayList<Polygon> polys = new ArrayList<>(np);
                for (int i = 0; i < np; i++) {
                    nv = scnr.nextInt();
                    Polygon pol = new Polygon();
                    for (int v = 0; v < nv; v++) {
                        Point p = l.get(scnr.nextInt());
                        pol.addPoint(p.x, p.y);
                    }
                    polys.add(pol);
                }
                polygons.clear();
                polygons.addAll(polys);
                insidePonts.clear();
                for (Polygon p : polygons) {
                    insidePonts.add(getPointIside(p));
                }
                //System.err.println("Got " + polygons.size() + " polygons");
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

        // Draw with BLUE already created polygons
        g2.setColor(Color.BLUE);
        g2.setStroke(new BasicStroke(1.5f));
        for (Polygon p : polygons) {
            g2.drawPolygon(p);
        }
        g2.setStroke(strokeBackup);

        g2.setColor(Color.BLUE);
        for (Point p : insidePonts) {
            g2.fillOval(p.x - iPointMarkerSie / 2, p.y - iPointMarkerSie / 2, iPointMarkerSie, iPointMarkerSie);
        }

        g2.setColor(Color.ORANGE);
        for (Point p : holes) {
            g2.fillOval(p.x - iPointMarkerSie / 2, p.y - iPointMarkerSie / 2, iPointMarkerSie, iPointMarkerSie);
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
