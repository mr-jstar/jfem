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
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import miscutils.Configuration;
import miscutils.FontFactory;
import miscutils.MiscUtils;
import sm.solvers.GMRES;

/**
 *
 * A simple GUI for FEM demo package Allows reading of meshes, draws 2D meshes
 * and allows to set-up 2D FEM elliptic model with Dirichlet BC
 *
 * @author jstar
 */
public class SimpleGUI extends JFrame {

    private final boolean VERTICAL_LAYOUT = true;

    private static final int[] sizes = {12, 18, 24};
    private static final Font[] fonts = FontFactory.makeFonts("SansSerif", Font.PLAIN, sizes);
    private static Font currentFont = fonts[fonts.length / 2];

    private static final String CONFIG_FILE = ".femconfig";

    private final Configuration configuration = new Configuration(CONFIG_FILE);
    private final String LAST_DIR = "SimpleGUI.last.dir";

    private final int vSize = 4;   // radius of mesh vertex symbol

    private final int vertexSelectionRadius = 100;  // precision for vertex selection

    private DrawingPanel drawingPanel;

    private IMesh mesh;
    private FEM model;

    private final Map<String, Boolean> options = new HashMap<>();   // diffrent FEM related options
    private final Set<Integer> currentSelection = new TreeSet<>();  // current selection of vertices of elements
    private final ArrayList<PointPosition> xy = new ArrayList<>();  // integer coordinates of vertices (in the drawingPanel space)
    private final TreeMap<Integer, Double> bndNodes = new TreeMap<>();  // Dirichlet boundary nodes: Vertex# -> value at

    private final String DEFAULT_BND_TEXT = "Add DBC(s)";
    private final String DEFAULT_SUB_TEXT = "Edit subdmn(s)";

    private final JButton loadButton = new JButton("Load mesh");
    private final JButton psgButton = new JButton("Run PSG editor");
    private final JButton meshButton = new JButton("Draw mesh");
    private final JButton bndButton = new JButton(DEFAULT_BND_TEXT);
    private final JButton rmBndButton = new JButton("Clear DBC(s)");
    private final JButton subDomButton = new JButton(DEFAULT_SUB_TEXT);
    private final JButton matsButton = new JButton("Edit materials");
    private final JButton computeButton = new JButton("Compute");
    private final JButton fieldButton = new JButton("Draw field");
    private final JButton clearButton = new JButton("Clear");
    private final JButton saveButton = new JButton("Save figure");
    private final JButton exitButton = new JButton("Close");

    private final JButton[] buttons = {
        loadButton, meshButton, bndButton, rmBndButton,
        subDomButton, matsButton, computeButton, fieldButton,
        clearButton, saveButton, psgButton, exitButton
    };

    private final JLabel message;  // Used to show status messages

    private final double[] xrange = {Double.MAX_VALUE, -Double.MAX_VALUE};  // 2D mesh vertices coordinates x-range
    private final double[] yrange = {Double.MAX_VALUE, -Double.MAX_VALUE};  // and y-range

    private final Map<Integer, Color> subDomColors = new HashMap<>(); // colors to show sub-domains

    private Map<Integer, Double[]> subDomParameters = new HashMap<>(); // [0] - materials, [1] - sources

    private boolean printDiag = false;  // if true, prints some info to System.err

    public SimpleGUI() {
        setTitle("Simple Vertical FEM GUI (Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 1200);
        setLocationRelativeTo(null);

        UIManager.put("OptionPane.messageFont", currentFont);
        UIManager.put("OptionPane.buttonFont", currentFont);
        UIManager.put("OptionPane.messageFont", currentFont);

        psgButton.addActionListener(e -> runPSG());
        loadButton.addActionListener(e -> loadFile());
        meshButton.addActionListener(e -> drawMesh());
        clearButton.addActionListener(e -> drawingPanel.clear());
        bndButton.addActionListener(e -> modifyBoundary());
        rmBndButton.addActionListener(e -> clrBoundary());
        subDomButton.addActionListener(e -> modifySubdomains());
        matsButton.addActionListener(e -> subDomainsParameters());
        computeButton.addActionListener(e -> computeField());
        fieldButton.addActionListener(e -> drawField());
        saveButton.addActionListener(e -> drawingPanel.saveImage());
        exitButton.addActionListener(e -> System.exit(0));

        JPanel buttonPanel = new JPanel();

        for (JButton btn : buttons) {
            buttonPanel.add(btn);
        }

        JMenuBar menuBar = createMenuBar(buttons, currentFont);

        options.put("showMesh", false);
        options.put("showField", false);
        options.put("showVertexNo", true);
        options.put("showSubDomains", true);
        options.put("inDefBoundary", false);
        options.put("inDefSubdomain", false);

        JMenu opts = options(options, currentFont);
        (new Thread() {
            {
                this.setDaemon(true);
            }

            @Override
            public void run() {
                while (true) {
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {

                    }
                    for (int i = 0; i < opts.getItemCount(); i++) {
                        JMenuItem mi = opts.getItem(i);
                        if (mi instanceof JCheckBoxMenuItem cb && options.containsKey(cb.getText())) {
                            cb.setSelected(options.get(cb.getText()));
                        }
                    }
                }
            }
        }).start();
        menuBar.add(opts);

        JMenu guiOpts = new JMenu("GUI options");
        ButtonGroup fgroup = new ButtonGroup();
        guiOpts.add(new JMenuItem("Font size"));
        for (Font f : fonts) {
            JRadioButtonMenuItem fontOpt = new JRadioButtonMenuItem("\t\t\t" + String.valueOf(f.getSize()));
            final Font cf = f;
            fontOpt.addActionListener(e -> {
                currentFont = cf;
                setFontRecursively(this, currentFont, 0);
                UIManager.put("OptionPane.messageFont", currentFont);
                UIManager.put("OptionPane.buttonFont", currentFont);
                UIManager.put("OptionPane.messageFont", currentFont);
            });
            fontOpt.setSelected(f == currentFont);
            fgroup.add(fontOpt);
            guiOpts.add(fontOpt);
        }
        guiOpts.addSeparator();
        JCheckBoxMenuItem barOpt = new JCheckBoxMenuItem("Show buttons");
        barOpt.setState(true);
        barOpt.addActionListener(e -> {
            buttonPanel.setVisible(barOpt.getState());
        });
        guiOpts.add(barOpt);
        guiOpts.addSeparator();
        JCheckBoxMenuItem diagOpt = new JCheckBoxMenuItem("Print some info to System.err");
        diagOpt.setState(false);
        diagOpt.addActionListener(e -> {
            printDiag = diagOpt.getState();
        });
        guiOpts.add(diagOpt);

        setFontRecursively(guiOpts, currentFont, 0);
        menuBar.add(guiOpts);
        setJMenuBar(menuBar);

        drawingPanel = new DrawingPanel();

        message = new JLabel("OK");
        message.setFont(currentFont);
        JPanel messagePanel = new JPanel();
        messagePanel.add(message);

        if (VERTICAL_LAYOUT) {
            buttonPanel.setLayout(new GridLayout(buttons.length + 5, 1));
            add(buttonPanel, BorderLayout.WEST);
        } else {
            add(buttonPanel, BorderLayout.NORTH);
        }

        add(drawingPanel, BorderLayout.CENTER);
        add(messagePanel, BorderLayout.SOUTH);

        switchAllButtons(false);
        psgButton.setEnabled(true);
        loadButton.setEnabled(true);
        exitButton.setEnabled(true);

        setFontRecursively(this, currentFont, 0);
        setVisible(true);
    }

    // Activates/deactivates all buttons  ( ANALOG FOR MENU TO BE DONE!!! )
    private void switchAllButtons(boolean flag) {
        for (JButton btn : buttons) {
            btn.setEnabled(flag);
        }
    }

    // Menu to set/un-set FEM-related options
    private JMenu options(Map<String, Boolean> optsMap, Font font) {
        JMenu opts = new JMenu("Options");
        opts.setFont(font);

        for (String s : optsMap.keySet()) {
            JCheckBoxMenuItem newOpt = new JCheckBoxMenuItem(s, optsMap.get(s));
            newOpt.setFont(font);

            final String key = s;

            newOpt.addActionListener(e -> {
                optsMap.put(key, newOpt.getState());
            });

            opts.add(newOpt);
        }

        return opts;
    }

    // Menu bar out of defined buttons
    private JMenuBar createMenuBar(JButton[] btns, Font font) {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");
        menu.setFont(font);

        for (JButton b : btns) {
            JMenuItem newItem = new JMenuItem(b.getText());
            newItem.setFont(font);
            for (ActionListener al : b.getActionListeners()) {
                newItem.addActionListener(al);
            }
            menu.add(newItem);
        }

        menuBar.add(menu);

        return menuBar;
    }

    // Action for Load mesh button/menu item
    private void loadFile() {
        JFileChooser fileChooser = new JFileChooser(getLastUsedDirectory());
        setFontRecursively(fileChooser, currentFont, 0);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File meshFile = fileChooser.getSelectedFile();
            try {
                if (meshFile.getAbsolutePath().endsWith(".mesh")) {
                    mesh = fem.Util.loadINRIAMesh(meshFile.getAbsolutePath());
                } else if (meshFile.getAbsolutePath().endsWith(".poly") || meshFile.getAbsolutePath().endsWith(".ele") || meshFile.getAbsolutePath().endsWith(".node")) {
                    mesh = fem.Util.loadTriangleMesh(meshFile.getAbsolutePath());
                } else {
                    throw new IllegalArgumentException("Unknown mesh format: " + meshFile);
                }
                saveLastUsedDirectory(meshFile.getParent());
                computeMeshRange();
                computeInitialSubdomains();
                options.put("showMesh", false);
                if (mesh.getNoElems() > 100) {
                    options.put("showVertexNo", false);
                }
                options.put("showField", false);
                options.put("inDefBoundary", false);
                options.put("inDefSubdomains", false);
                currentSelection.clear();
                xy.clear();
                bndNodes.clear();
                model = null;
                drawingPanel.repaint();
                switchAllButtons(true);
                fieldButton.setEnabled(false);
                message.setText("Mesh loaded from: " + meshFile.getAbsolutePath() + "\n" + mesh.getNoVertices() + " vertices & " + mesh.getNoElems() + " elements");
            } catch (Exception e) {
                mesh = null;
                JOptionPane.showMessageDialog(this, "Unable to load mesh from: " + meshFile.getAbsolutePath());
            }
        }
    }

    // Action for Draw mesh button/menu item
    @SuppressWarnings("unchecked")
    private void runPSG() {
        try {
            Class c = Class.forName("preprocessors.PSGEditor");
            c.getConstructor().newInstance();
            message.setText("OK");
        } catch (Exception e) {
            message.setText("Can't run: " + e.getLocalizedMessage());
        }

    }

    // Action for Draw mesh button/menu item
    private void drawMesh() {
        options.put("showMesh", true);
        drawingPanel.repaint();
    }

    // Action for AD DBC(s) button/menu item
    private void modifyBoundary() {
        if (options.get("inDefSubdomain")) {
            return;
        }
        if (options.get("inDefBoundary")) {
            if (currentSelection.isEmpty()) {
                if (JOptionPane.showConfirmDialog(this, "No nodes selected, want to try again?",
                        DEFAULT_BND_TEXT, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    options.put("inDefBoundary", false);
                    switchAllButtons(true);
                    bndButton.setText(DEFAULT_BND_TEXT);
                    bndButton.setForeground(Color.BLACK);
                    message.setText("OK");
                    return;
                }
            } else {
                bndButton.setText(DEFAULT_BND_TEXT);
                bndButton.setForeground(Color.BLACK);
                double value;
                try {
                    String m = JOptionPane.showInputDialog(this, "Value?", DEFAULT_BND_TEXT, JOptionPane.QUESTION_MESSAGE);
                    if (m == null) {
                        throw new NumberFormatException();
                    }
                    value = Double.parseDouble(m);
                    for (Integer v : currentSelection) {
                        bndNodes.put(v, value);
                    }
                    options.put("inDefBoundary", false);
                    switchAllButtons(true);
                    message.setText("OK");
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid value, click the button once more.", DEFAULT_BND_TEXT, JOptionPane.QUESTION_MESSAGE);
                }
                if (printDiag) {
                    System.err.println(bndNodes);
                }
            }
            model = null;
            fieldButton.setEnabled(false);
            drawingPanel.repaint();
        } else {
            if (options.get("showMesh")) {
                JOptionPane.showMessageDialog(this, """
                                                    Click on the node to select it, click on selected to unselect,
                                                    drag mouse to select/deselect all nodes in the rectangle
                                                    when done click button again to be asked for the value.""", DEFAULT_BND_TEXT, JOptionPane.QUESTION_MESSAGE);
                options.put("inDefBoundary", true);
                currentSelection.clear();
                switchAllButtons(false);
                bndButton.setEnabled(true);
                bndButton.setForeground(Color.RED);
                bndButton.setText("Click to finish selection");
            } else {
                JOptionPane.showMessageDialog(this, "Draw the mesh first!", DEFAULT_BND_TEXT, JOptionPane.QUESTION_MESSAGE);
            }
        }
    }

    // Action for Clear DBC(s) button/menu item
    private void clrBoundary() {
        model = null;
        bndNodes.clear();
        currentSelection.clear();
        options.put("inDefBoundary", false);
        options.put("showField", false);
        fieldButton.setEnabled(false);
        bndButton.setText(DEFAULT_BND_TEXT);
        bndButton.setForeground(Color.BLACK);
        drawingPanel.repaint();
    }

    // Action for Modify subdomain(s) button/menu item
    private void modifySubdomains() {
        if (options.get("inDefBoundary")) {
            return;
        }
        if (options.get("inDefSubdomain")) {
            if (currentSelection.isEmpty()) {
                if (JOptionPane.showConfirmDialog(this, "No elements selected, want to try again?",
                        DEFAULT_SUB_TEXT, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    options.put("inDefSubdomain", false);
                    switchAllButtons(true);
                    subDomButton.setText(DEFAULT_SUB_TEXT);
                    subDomButton.setForeground(Color.BLACK);
                    message.setText("OK");
                    return;
                }
            } else {
                subDomButton.setText(DEFAULT_SUB_TEXT);
                subDomButton.setForeground(Color.BLACK);
                int sbd;
                try {
                    String m = JOptionPane.showInputDialog(this, "Subdomain #?", DEFAULT_SUB_TEXT, JOptionPane.QUESTION_MESSAGE);
                    if (m == null) {
                        throw new NumberFormatException();
                    }
                    sbd = Integer.parseInt(m);
                    if (!subDomParameters.containsKey(sbd)) {
                        Double[] npar = {1.0, 0.0};
                        subDomParameters.put(sbd, npar);
                    }
                    for (Integer v : currentSelection) {
                        mesh.getElem(v).setSubdomain(sbd);
                    }
                    rebuildSubDomainColors();
                    if (printDiag) {
                        System.err.println(MiscUtils.mapToString(subDomParameters));
                    }
                    switchAllButtons(true);
                    options.put("inDefSubdomain", false);
                    message.setText("OK");
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid value, click the button once more.", DEFAULT_SUB_TEXT, JOptionPane.QUESTION_MESSAGE);
                }
            }
            model = null;
            options.put("showField", false);
            fieldButton.setEnabled(false);
            drawingPanel.repaint();
        } else {
            if (options.get("showMesh")) {
                JOptionPane.showMessageDialog(this, """
                                                    Click on the element to select it, click on selected to unselect,
                                                    drag mouse to select/deselect all elements in the rectangle
                                                    when done click button again to be asked for the subdomain.""", DEFAULT_SUB_TEXT, JOptionPane.QUESTION_MESSAGE);
                options.put("inDefSubdomain", true);
                currentSelection.clear();
                switchAllButtons(false);
                subDomButton.setEnabled(true);
                subDomButton.setForeground(Color.RED);
                subDomButton.setText("Click to finish selection");
            } else {
                JOptionPane.showMessageDialog(this, "Draw the mesh first!", DEFAULT_SUB_TEXT, JOptionPane.QUESTION_MESSAGE);
            }
        }
    }

    // Action for Edit materials button/menu item
    private void subDomainsParameters() {
        if (mesh == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Subdomain(s) parameters");
            frame.setSize(400, 500);
            frame.setLocationRelativeTo(SimpleGUI.this);

            String[] colN = {"SubDom", "Materials", "Sources"};

            MapEditorPanel panel = new MapEditorPanel(subDomParameters, colN, currentFont);

            panel.addModelListener(e -> {
                model = null;
                options.put("showField", false);
                fieldButton.setEnabled(false);
                drawingPanel.repaint();
            });
            frame.add(panel);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    subDomParameters = panel.getData();
                    if (printDiag) {
                        System.err.println(MiscUtils.mapToString(subDomParameters));
                    }
                }
            });
            setFontRecursively(frame, currentFont, 0);
            frame.setVisible(true);
        });
    }

    // Action for Compute button/menu item
    private void computeField() {
        EleIntegral integral = mesh.getElem(0).getVertices().length == 3 ? new TriangleLaplace() : new TetraLaplace();

        int[] bndNds = new int[bndNodes.size()];
        double[] bndVals = new double[bndNds.length];
        int i = 0;
        for (Integer v : bndNodes.keySet()) {
            bndNds[i] = v;
            bndVals[i++] = bndNodes.get(v);
        }
        model = new FEM(mesh, subDomParameters, bndNds, bndVals);
        model.buildSymmetricMatrix(true);
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
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
                    message.setText("Field computed in " + elapsedTime / 1000000 + " miliseconds");
                } catch (Exception e) {
                    message.setText("Field NOT computed, reason: " + e.getLocalizedMessage());
                    model = null;
                    fieldButton.setEnabled(false);
                    options.put("showField", false);
                }

            }
        };
        thread.setDaemon(true);
        thread.start();
        //double[] V = model.getFld();
        //double[] minmax = range(V);
        //System.out.println("V in <" + minmax[0] + "," + minmax[1] + ">");
    }

    // Helper - get mesh vertices coordinates range
    private void computeMeshRange() {
        xrange[0] = yrange[0] = Double.MAX_VALUE;
        xrange[1] = yrange[1] = -Double.MAX_VALUE;
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
    }

    // Helper - build initial material(s) parameters
    private void computeInitialSubdomains() {
        subDomParameters.clear();
        for (int e = 0; e < mesh.getNoElems(); e++) {
            int s = mesh.getElem(e).getSubdomain();
            if (!subDomParameters.containsKey(s)) {
                Double[] p = {1.0, 0.0};
                subDomParameters.put(s, p);
            }
        }
        rebuildSubDomainColors();
    }

    // Helper - rebuild color map used to show sub-domains
    private void rebuildSubDomainColors() {
        subDomColors.clear();
        int c = 240, dc = 20;
        for (Integer k : subDomParameters.keySet()) {
            subDomColors.put(k, new Color(c % 256, c % 256, c % 256));
            c -= dc;
        }
    }

    // Helper - get range values in a double vector
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

    // Action for Draw field button/menu item
    private void drawField() {
        options.put("showField", true);
        drawingPanel.repaint();
    }

    // Helper - changes fonts of all components
    private void setFontRecursively(Component comp, Font font, int d) {
        if (comp == null) {
            return;
        }
        comp.setFont(font);
        // Diagnostics
        if (printDiag) {
            for (int i = 0; i < d; i++) {
                System.err.print("\t");
            }
            System.err.println(comp.getClass().getName() + " : " + (comp instanceof Container ? ("container (" + ((Container) comp).getComponentCount() + ")") : "other"));
        }
        //
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                setFontRecursively(child, font, d + 1);
            }
        }
        // Needs specific navigation, since JMenu does not show menu components as Components
        if (comp instanceof JMenu menu) {
            for (int i = 0; i < menu.getItemCount(); i++) {
                setFontRecursively(menu.getItem(i), font, d + 1);
            }
        }
    }

    // Helper - retrieves the last used directory from the config file
    private String getLastUsedDirectory() {
        String lsd = configuration.getValue(LAST_DIR);
        if (lsd == null) {
            lsd = ".";
        }
        return lsd;
    }

    // Helper - saves the last used directory
    private void saveLastUsedDirectory(String directory) {
        try {
            configuration.saveValue(LAST_DIR, directory);
        } catch (IOException e) {
            message.setText(e.getLocalizedMessage());
        }
    }

    // Helper -finds vertex nearest to (x,y) - clicked by the mouse
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

    // Starts the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimpleGUI::new);
    }

    // All drawing implemented here
    class DrawingPanel extends JPanel {

        private int prevX, prevY;
        private int currX, currY;

        public DrawingPanel() {
            setDoubleBuffered(false);
            setBackground(Color.WHITE);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (options.get("inDefBoundary")) {
                        int nearestVertex = findNearestVertex(prevX, prevY);
                        if (nearestVertex >= 0) {
                            if (printDiag) {
                                System.err.println(prevX + " " + prevY + " => " + nearestVertex);
                            }
                            if (currentSelection.contains(nearestVertex)) {
                                currentSelection.remove(nearestVertex);
                            } else {
                                currentSelection.add(nearestVertex);
                            }
                            message.setText("selected nodes: " + currentSelection.toString());
                        }
                    }
                    if (options.get("inDefSubdomain")) {
                        int element = findClickedElement(prevX, prevY);
                        if (element >= 0) {
                            if (printDiag) {
                                System.err.println(prevX + " " + prevY + " => Element" + element);
                            }
                            if (currentSelection.contains(element)) {
                                currentSelection.remove(element);
                            } else {
                                currentSelection.add(element);
                            }
                            message.setText("selected elements: " + currentSelection.toString());
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

                @Override
                public void mouseDragged(MouseEvent e) {
                    currX = e.getX();
                    currY = e.getY();
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (mesh == null) {
                return;
            } else {
                double margin = 0.05;
                double xbase = margin * getWidth();
                double ybase = margin * getHeight();
                double width = getWidth() * (1 - 2 * margin);
                double height = getHeight() * (1 - 2 * margin);

                xy.clear();
                for (int v = 0; v < mesh.getNoVertices(); v++) {
                    double[] x = mesh.getVertex(v).getX();
                    int ix = (int) Math.round(xbase + (x[0] - xrange[0]) / xrange[1] * width);
                    int iy = (int) Math.round(ybase + (1.0 - (x[1] - yrange[0]) / yrange[1]) * height);
                    xy.add(new PointPosition(ix, iy));
                }
            }

            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());

            if (options.get("inDefBoundary") && (currX != prevX || currY != prevY)) {
                int xp = Math.min(currX, prevX);
                int yp = Math.min(currY, prevY);
                int width = Math.abs(currX - prevX);
                int height = Math.abs(currY - prevY);
                g.setColor(Color.ORANGE);
                g.drawRect(xp, yp, width, height);
                for (int v = 0; v < xy.size(); v++) {
                    PointPosition p = xy.get(v);
                    if (p.x >= xp && p.x <= xp + width && p.y >= yp && p.y <= yp + height) {
                        if (currentSelection.contains(v)) {
                            currentSelection.remove(v);
                        } else {
                            currentSelection.add(v);
                        }
                    }
                }
                message.setText("selected nodes: " + currentSelection.toString());
                prevX = currX;
                prevY = currY;
            }

            if (options.get("inDefSubdomain") && (currX != prevX || currY != prevY)) {
                int xp = Math.min(currX, prevX);
                int yp = Math.min(currY, prevY);
                int width = Math.abs(currX - prevX);
                int height = Math.abs(currY - prevY);
                g.setColor(Color.ORANGE);
                g.drawRect(xp, yp, width, height);
                for (int e = 0; e < mesh.getNoElems(); e++) {
                    int[] ev = mesh.getElem(e).getVertices();
                    int nin = 0;
                    for (int v : ev) {
                        PointPosition p = xy.get(v);
                        if (p.x >= xp && p.x <= xp + width && p.y >= yp && p.y <= yp + height) {
                            nin++;
                        }
                    }
                    if (nin == ev.length) {
                        if (currentSelection.contains(e)) {
                            currentSelection.remove(e);
                        } else {
                            currentSelection.add(e);
                        }
                    }
                }
                message.setText("selected elements: " + currentSelection.toString());
                prevX = currX;
                prevY = currY;
            }

            if (options.get("showMesh")) {
                if (mesh == null || mesh.getDim() != 2) {
                    return;
                }

                int[] ev = new int[mesh.getElem(0).getVertices().length + 1];
                int[] evX = new int[mesh.getElem(0).getVertices().length];
                int[] evY = new int[evX.length];
                if (options.get("showSubDomains")) {
                    for (int e = 0; e < mesh.getNoElems(); e++) {
                        System.arraycopy(mesh.getElem(e).getVertices(), 0, ev, 0, ev.length - 1);
                        if (options.get("inDefSubdomain") && currentSelection.contains(e)) {
                            g2.setColor(Color.WHITE);
                        } else {
                            g2.setColor(subDomColors.get(mesh.getElem(e).getSubdomain()));
                        }
                        for (int i = 0; i < ev.length - 1; i++) {
                            PointPosition p = xy.get(ev[i]);
                            evX[i] = p.x;
                            evY[i] = p.y;
                        }
                        g2.fillPolygon(evX, evY, evX.length);
                    }
                    // The legend
                    int lwidth = (int) (0.7 * getWidth());
                    int lheight = 20;
                    int bottomMargin = 20;
                    int pxl = (int) (0.15 * getWidth());
                    int pyl = bottomMargin;
                    int pos = 0;
                    for (Integer i : subDomParameters.keySet()) {
                        g2.setColor(subDomColors.get(i));
                        g2.fillRect(pxl + pos, pyl, 2 * lheight, lheight);
                        g2.setColor(Color.BLACK);
                        g.drawString(String.valueOf(i), pxl + pos + lheight / 2 + 3, pyl + lheight - 3);
                        pos += lwidth / mesh.getNoSubdomains();
                    }

                }
                for (int e = 0; e < mesh.getNoElems(); e++) {
                    System.arraycopy(mesh.getElem(e).getVertices(), 0, ev, 0, ev.length - 1);
                    ev[ev.length - 1] = ev[0];
                    g.setColor(Color.GREEN);
                    for (int i = 1; i < ev.length; i++) {
                        PointPosition p1 = xy.get(ev[i - 1]);
                        PointPosition p2 = xy.get(ev[i]);
                        g.drawLine(p1.x, p1.y, p2.x, p2.y);
                    }
                }

                g.setColor(Color.BLUE);
                g.setFont(currentFont);
                for (int v = 0; v < mesh.getNoVertices(); v++) {
                    PointPosition p = xy.get(v);
                    if (options.get("inDefBoundary") && currentSelection.contains(v)) {
                        g.setColor(Color.RED);
                        g.fillOval(p.x - vSize / 2, p.y - vSize / 2, vSize, vSize);
                        g.setColor(Color.BLUE);
                    } else {
                        g.fillOval(p.x - vSize / 2, p.y - vSize / 2, vSize, vSize);
                    }
                    if (options.get("showVertexNo")) {
                        if (options.get("inDefBoundary") && currentSelection.contains(v)) {
                            g.setColor(Color.RED);
                            g.drawString(String.valueOf(v), p.x, p.y);
                            g.setColor(Color.BLUE);
                        } else {
                            g.drawString(String.valueOf(v), p.x, p.y);
                        }
                    }
                }

                g.setColor(Color.RED);
                int dh = g.getFontMetrics().getHeight();
                for (Integer b : bndNodes.keySet()) {
                    PointPosition p = xy.get(b);
                    g.drawString(String.valueOf(bndNodes.get(b)), p.x, p.y + dh);
                }
            }
            if (options.get("showField")) {
                if (model != null) {
                    double[] fld = model.getFld();
                    if (fld != null) {
                        double[] frng = range(fld);
                        ColorMap cm = new ColorMap((float) frng[0], (float) frng[1]);
                        double[] efld = new double[mesh.getElem(0).getVertices().length];
                        for (int e = 0; e < mesh.getNoElems(); e++) {
                            int[] ev = mesh.getElem(e).getVertices();
                            for (int v = 0; v < ev.length; v++) {
                                efld[v] = fld[ev[v]];
                            }
                            fillElem(g, ev, efld, cm);
                        }
                        // The legend
                        int lwidth = (int) (0.7 * getWidth());
                        int lheight = 20;
                        int bottomMargin = 20;
                        int xmargin = 3;
                        int pxl = (int) (0.15 * getWidth());
                        int pyl = getHeight() - lheight - bottomMargin;
                        Image legend = cm.createColorScaleImage(lwidth, lheight, ColorMap.Menu.HORIZONTAL);

                        g.drawImage(legend, pxl, pyl, this);
                        g.setColor(Color.BLACK);
                        String low = String.format("%.3g", frng[0]);

                        g.drawString(low, pxl - getFontMetrics(currentFont).stringWidth(low) - xmargin, pyl + 3 * lheight / 4);
                        String high = String.format("%.3g", frng[1]);
                        g.drawString(high, pxl + lwidth + xmargin, pyl + 3 * lheight / 4);
                    }
                }
            }
        }

        // Get int coordites of all vertices in v
        private PointPosition[] intCoordinatesForElem(int[] v) {
            PointPosition[] p = new PointPosition[v.length];
            for (int i = 0; i < v.length; i++) {
                p[i] = xy.get(v[i]);
            }
            return p;
        }

        // Shows field fld over the triangle v, uses cm to represent fld value
        public void fillElem(Graphics g, int[] v, double[] fld, ColorMap cm) {
            PointPosition[] p = intCoordinatesForElem(v);
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

        // Find triangle on which the mouse button was pressed
        private int findClickedElement(int x, int y) {
            for (int e = 0; e < mesh.getNoElems(); e++) {
                if (pointInTriangle(x, y, intCoordinatesForElem(mesh.getElem(e).getVertices()))) {
                    return e;
                }
            }
            return -1;
        }

        // Checks if (x,y) is in triangle p
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

        // Side of (x,y) relative to (x1,y1)-(x2,y2)
        private double sign(double x, double y, double x1, double y1, double x2, double y2) {
            return (x - x2) * (y1 - y2) - (x1 - x2) * (y - y2);
        }

        // Barycentric coordinates of (x,y) in triangle p
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

        // Action for Clear button/menu item
        public void clear() {
            options.put("showMesh", false);
            options.put("showField", false);
            repaint();
        }

        // Action for Save image button/menu item
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

    // Helper holding integer (relative to drawing window) coordinates of a Vertex
    class PointPosition {

        int x;
        int y;

        public PointPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
