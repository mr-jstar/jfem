package preprocessors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import miscutils.Configuration;
import miscutils.FontFactory;

/**
 *
 * @author jstar
 */
public class PSGEditor extends JFrame {

    private DrawingPanel panel;
    private final boolean printDiag = false;

    private static final Font[] fonts = FontFactory.makeFonts("SansSerif");
    private static Font currentFont = fonts[fonts.length / 2];

    private static final String CONFIG_FILE = ".femconfig";
    private static final String LAST_DIR = "PSGEditor.last.dir";

    private final Configuration configuration = new Configuration(CONFIG_FILE);

    private String processorCommand = configuration.getValue("PSGEditor.triangle");
    private String processorSwitches = "-pAqa0.1";

    private boolean exit_on_close;

    public PSGEditor(boolean on_close) {
        exit_on_close = on_close;
        init();
    }

    public PSGEditor() {
        init();
    }

    private void init() {
        setTitle("Planar Straight Graph Editor");
        setSize(1200, 1200);

        if (exit_on_close) {
            setDefaultCloseOperation(EXIT_ON_CLOSE);
        } else {
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        }
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
        JMenuItem cmdItem = new JMenuItem("Run generator");
        cmdItem.addActionListener(e -> runPostproc());
        fileMenu.add(cmdItem);

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
                panel.repaint();
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
                panel.repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid value, y axis not changed.", "Set range of vartical axis", JOptionPane.QUESTION_MESSAGE);
            }
        });
        optMenu.add(yAxisItem);

        JMenu settings = new JMenu("Settings");
        ButtonGroup fgroup = new ButtonGroup();
        settings.add(new JMenuItem("Font size"));
        for (Font f : fonts) {
            JRadioButtonMenuItem fontOpt = new JRadioButtonMenuItem("\t\t\t" + String.valueOf(f.getSize()));
            final Font cf = f;
            fontOpt.addActionListener(e -> {
                currentFont = cf;
                setFontRecursively(this, currentFont, 0);
                UIManager.put("OptionPane.messageFont", currentFont);
                UIManager.put("OptionPane.buttonFont", currentFont);
                UIManager.put("TextField.font", currentFont);
            });
            fontOpt.setSelected(f == currentFont);
            fgroup.add(fontOpt);
            settings.add(fontOpt);
        }
        settings.addSeparator();
        JMenuItem postprocItem = new JMenuItem("Triangle command");
        postprocItem.addActionListener(e -> {
            try {
                String m = JOptionPane.showInputDialog(this, "Command", "Triangle command", JOptionPane.QUESTION_MESSAGE, null, null, processorCommand).toString();
                processorCommand = m.trim();
                configuration.saveValue("PSGEditor.triangle", processorCommand);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid value, postprocessor not changed.", "Triangle command", JOptionPane.QUESTION_MESSAGE);
            }
        });
        settings.add(postprocItem);
        JMenuItem switchesItem = new JMenuItem("Triangle switches");
        switchesItem.addActionListener(e -> {
            processorSwitches = "-pAqa" + String.format(Locale.ENGLISH, "%.6f", panel.hSquare);
            try {
                String m = JOptionPane.showInputDialog(this, "Switches", "Triangle switches", JOptionPane.QUESTION_MESSAGE, null, null, processorSwitches).toString();
                processorSwitches = m.trim();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid value, switches not changed.", "Triangle switches", JOptionPane.QUESTION_MESSAGE);
            }
        });
        settings.add(switchesItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(optMenu);
        menuBar.add(settings);
        setJMenuBar(menuBar);

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
                    saveItem.setEnabled(panel.getNoPolygons() > 0);
                    exportItem.setEnabled(panel.getNoPolygons() > 0);
                    cmdItem.setEnabled(panel.lastPolyFile != null);
                    rmItem.setText(panel.remove ? "End removing" : "Remove selected");
                }
            }
        };
        t.start();

        setFontRecursively(this, currentFont, 0);
        UIManager.put("OptionPane.messageFont", currentFont);
        UIManager.put("OptionPane.buttonFont", currentFont);
        UIManager.put("TextField.font", currentFont);
        setVisible(true);
    }

    private void runPostproc() {
        System.err.println("runPostproc");
        if (panel.lastPolyFile == null) {
            return;
        }
        try {
            JTextField dir = new JTextField(getLastUsedDirectory());
            JTextField cmd = new JTextField(processorCommand);
            JTextField cmdOpt = new JTextField(processorSwitches + " " + panel.lastPolyFile);

            Object[] message = {
                "Command:", cmd,
                "Switches:", cmdOpt,
                "Working directory", dir
            };

            int option = JOptionPane.showConfirmDialog(null, message, "Run:", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                String wrkDir = dir.getText();
                String[] cmdToExecute = Stream.concat(Stream.of(cmd.getText()), Arrays.stream(cmdOpt.getText().trim().split("\\s+"))).toArray(String[]::new);

                System.err.println(Arrays.stream(cmdToExecute).collect(Collectors.joining(" ")));
                ProcessBuilder builder = new ProcessBuilder(cmdToExecute);
                if (builder == null) {
                    throw new IllegalStateException("Can't create process builder: \"" + Arrays.stream(cmdToExecute).collect(Collectors.joining(" ")) + "\"");
                }
                builder.directory(new File(wrkDir));
                Process process = builder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                Thread out = new Thread() {
                    @Override
                    public void run() {
                        JFrame postProcOutput = new JFrame("Postprocessor output");
                        postProcOutput.setSize(600, 800);
                        postProcOutput.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                        JTextArea output = new JTextArea();
                        output.setEditable(false);
                        output.setText("");

                        JScrollPane scrollPane = new JScrollPane(output);
                        JPanel close = new JPanel();
                        JButton cbtn = new JButton("Close");
                        cbtn.addActionListener(e->postProcOutput.dispose());
                        close.add(cbtn);

                        postProcOutput.getContentPane().add(scrollPane);
                        postProcOutput.getContentPane().add(close, BorderLayout.SOUTH);
                        postProcOutput.setLocationRelativeTo(PSGEditor.this);
                        setFontRecursively(postProcOutput, currentFont, 0);
                        postProcOutput.setVisible(true);
                        String line;
                        try {
                            while ((line = reader.readLine()) != null) {
                                output.setText(output.getText() + "\n>> " + line);
                            }
                        } catch (IOException e) {

                        }
                    }
                };
                out.start();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Exception: " + e.getClass() + " " + e.getMessage());
        }
    }

    // Helper - changes fonts of all components
    private void setFontRecursively(Component comp, Font font, int level) {
        if (comp == null) {
            return;
        }
        comp.setFont(font);
        // Diagnostics
        if (printDiag) {
            for (int i = 0; i < level; i++) {
                System.err.print("\t");
            }
            System.err.println(comp.getClass().getName() + " : " + (comp instanceof Container ? ("container (" + ((Container) comp).getComponentCount() + ")") : "other"));
        }
        //
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                setFontRecursively(child, font, level + 1);
            }
        }
        // Needs specific navigation, since JMenu does not show menu components as Components
        if (comp instanceof JMenu menu) {
            for (int i = 0; i < menu.getItemCount(); i++) {
                setFontRecursively(menu.getItem(i), font, level + 1);
            }
        }
    }

    String getLastUsedDirectory() {
        String lsd = configuration.getValue(LAST_DIR);
        if (lsd == null) {
            lsd = ".";
        }
        return lsd;
    }

    // Helper - saves the last used directory
    void saveLastUsedDirectory(String directory) {
        try {
            configuration.saveValue(LAST_DIR, directory);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Write error: " + e.getClass() + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                PSGEditor instance = new PSGEditor(true);
            }
        });
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
        float hSquare;
        String lastPolyFile;

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
        int gridBrightness = 210;

        boolean remove;

        public DrawingPanel() {
            setBackground(Color.WHITE);

            setFocusable(true); // Needed for KeyBindings
            requestFocusInWindow();
            // Keys -> actions
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "sPressed");
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK), "SPressed");
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.SHIFT_DOWN_MASK), "BPressed");
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "bPressed");

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
                        repaint();
                    }
                }
            });
            getActionMap().put("bPressed", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (gridBrightness > 0) {
                        gridBrightness -= 10;
                        repaint();
                    }
                }
            });
            getActionMap().put("BPressed", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (gridBrightness < 210) {
                        gridBrightness += 10;
                        repaint();
                    }
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
            JFileChooser chooser = new JFileChooser(getLastUsedDirectory());
            int option = chooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getName().endsWith(".poly")) {
                    file = new File(file.getAbsolutePath() + ".poly");
                }
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
                    lastPolyFile = file.getAbsolutePath();
                    configuration.saveValue(LAST_DIR, file.getParent());
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Write error: " + e.getMessage());
                }
            }
        }

        public void saveToFile() {
            JFileChooser chooser = new JFileChooser(getLastUsedDirectory());
            int option = chooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(xAxis[0] + " " + xAxis[1] + " " + yAxis[0] + " " + yAxis[1] + " " + "\n");
                    writer.write(vertices.size() + "\n");
                    for (WorldPoint p : vertices) {
                        writer.write(" " + p.x + " " + p.y + "\n");
                    }
                    writer.write(polygons.size() + "\n");
                    for (List<Integer> p : polygons) {
                        writer.write(" " + p.size());
                        for (Integer i : p) {
                            writer.write(" " + i);
                        }
                        writer.newLine();
                    }
                    writer.write(holes.size() + "\n");
                    for (List<Integer> p : holes) {
                        writer.write(" " + p.size());
                        for (Integer i : p) {
                            writer.write(" " + i);
                        }
                        writer.newLine();
                    }
                    JOptionPane.showMessageDialog(this, file.getName() + " saved!");
                    configuration.saveValue(LAST_DIR, file.getParent());
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Write error: " + e.getMessage());
                }
            }
        }

        public void loadFromFile() {
            JFileChooser chooser = new JFileChooser(getLastUsedDirectory());
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
                            pol.add(scnr.nextInt());
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
                            pol.add(scnr.nextInt());
                        }
                        holes.add(pol);
                    }
                    repaint();
                    configuration.saveValue(LAST_DIR, file.getParent());
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
            int l = gridBrightness;
            float[] dashPattern = {2f, 2f}; // 2px dot, 2px white
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
            hSquare = ((xAxis[1] - xAxis[0]) / maxw) * ((xAxis[1] - xAxis[0]) / maxw);
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
}
