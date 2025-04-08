package preprocessors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author jstar
 */
public class PolyEditor extends JFrame {

    private DrawingPanel panel;

    public PolyEditor() {
        setTitle("Poly File Editor");
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
            //((JMenuItem) e.getSource()).setText(panel.remove ? "Finish remove" : "Remove selected");
        });
        editMenu.add(rmItem);

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
        SwingUtilities.invokeLater(PolyEditor::new);
    }
}

class DrawingPanel extends JPanel {

    private final List<Polygon> polygons = new ArrayList<>();
    private final List<Point> currentPoints = new ArrayList<>();

    private float[] xAxis = {0.0f, 1.0f};
    private float[] yAxis = {0.0f, 1.0f};
    int xoffset, yoffset, maxw, maxh;

    int snap = 10;

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

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Left - add vertex
                if (SwingUtilities.isLeftMouseButton(e)) {
                    Point p = e.getPoint();
                    //System.err.print(p + " -> ");
                    if (remove) {
                        polygons.removeIf(pol -> pol.contains(p));
                        adjustCursor();
                    } else {
                        int x = (int) (Math.round(p.getX() / (float) snap) * snap) + xoffset;
                        int y = (int) (Math.round(p.getY() / (float) snap) * snap) + yoffset;
                        p.setLocation(x, y);
                        //System.err.println(p);
                        currentPoints.add(p);
                    }
                    repaint();
                } // Right - add polygon
                else if (SwingUtilities.isRightMouseButton(e) && currentPoints.size() >= 3 && !remove) {
                    Polygon p = new Polygon();
                    for (Point point : currentPoints) {
                        p.addPoint(point.x, point.y);
                    }
                    polygons.add(p);
                    currentPoints.clear();
                    repaint();
                }
            }
        });
    }

    public void adjustCursor() {
        if (getNoPolygons() == 0) {
            remove = false;
        }
        setCursor(Cursor.getPredefinedCursor(remove ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public void clearAllPolygons() {
        polygons.clear();
        currentPoints.clear();
        repaint();
    }

    public int getNoPolygons() {
        return polygons.size();
    }

    public void saveToFile() {
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
                    Point p = l.get(i);
                    float x = xAxis[0] + ((float) (p.getX() - xoffset)) / (maxw * snap) * (xAxis[1] - xAxis[0]);
                    float y = yAxis[0] + ((float) (getHeight() - p.getY() - yoffset)) / (maxh * snap) * (yAxis[1] - yAxis[0]);
                    writer.write(" " + i + " " + x + " " + y);
                    writer.newLine();
                }
                writer.write(nseg + " 0\n");
                int seg = 0;
                for (Polygon p : polygons) {
                    StringBuilder sb = new StringBuilder();
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
                }
                writer.write("0\n");
                writer.write(polygons.size() + "\n");
                for (int i = 0; i < polygons.size(); i++) {
                    Rectangle r = polygons.get(i).getBounds();
                    writer.write(i + " " + r.getCenterX() + " " + (getHeight() - r.getCenterY()) + " " + (i + 1) + "\n");
                }
                JOptionPane.showMessageDialog(this, "Saved!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Write error: " + e.getMessage());
            }
        }
    }

    public void loadFromFile() {
        JFileChooser chooser = new JFileChooser();
        int option = chooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println(line);
                }
                JOptionPane.showMessageDialog(this, "Saved!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Write error: " + e.getMessage());
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Draw the snap grid
        g2.setColor(Color.LIGHT_GRAY);
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

        // Draw with BLUE already created
        g2.setColor(Color.BLUE);
        for (Polygon p : polygons) {
            g2.drawPolygon(p);
        }

        // Draw with RED color while crating
        g2.setColor(Color.RED);
        for (int i = 0; i < currentPoints.size() - 1; i++) {
            Point p1 = currentPoints.get(i);
            Point p2 = currentPoints.get(i + 1);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }
}
