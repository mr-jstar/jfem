package gui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.*;
import javax.swing.event.TableModelListener;
/**
 *
 * @author jstar
 */

public class MapEditorPanel extends JPanel {
    private final MapTableModel tableModel;
    private final JTable table;

    public MapEditorPanel(Map<Integer, Double[]> initialData, String [] colNames, Font headerFont ) {
        setLayout(new BorderLayout());

        tableModel = new MapTableModel(initialData, colNames);
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(headerFont);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());
        
        JButton addButton = new JButton("Add row");
        JButton removeButton = new JButton("Remove selected");
        JButton closeButton = new JButton("Close");

        addButton.addActionListener(e -> tableModel.addEntry());
        removeButton.addActionListener(e -> {
            int selected = table.getSelectedRow();
            if (selected >= 0) {
                tableModel.removeEntry(selected);
            }
        });
        
        closeButton.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispose(); // zamyka JFrame
            }
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBackground(Color.WHITE);
        buttonsPanel.add(addButton);
        buttonsPanel.add(removeButton);
        controlPanel.add(buttonsPanel, BorderLayout.NORTH);
        JPanel closePanel = new JPanel();
        closePanel.add(closeButton);
        controlPanel.add(closePanel);
        add(controlPanel, BorderLayout.SOUTH);
    }

    public Map<Integer, Double[]> getData() {
        return tableModel.toMap();
    }
    
    public void addModelListener( TableModelListener listener ) {
        tableModel.addTableModelListener(listener);
    }

    // Model tabeli
    private static class MapTableModel extends AbstractTableModel {
        private final java.util.List<Integer> keys = new ArrayList<>();
        private final java.util.List<Double[]> values = new ArrayList<>();
        private final String [] colNames;

        public MapTableModel(Map<Integer, Double[]> initialData, String [] colNames) {
            this.colNames = colNames;
            for (Map.Entry<Integer, Double[]> entry : initialData.entrySet()) {
                keys.add(entry.getKey());
                values.add(entry.getValue());
            }
        }

        public void addEntry() {
            int newKey = findNextFreeKey();
            keys.add(newKey);
            values.add(new Double[]{0.0, 0.0});
            fireTableRowsInserted(keys.size() - 1, keys.size() - 1);
        }

        public void removeEntry(int rowIndex) {
            keys.remove(rowIndex);
            values.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }

        private int findNextFreeKey() {
            int i = 0;
            while (keys.contains(i)) i++;
            return i;
        }

        public Map<Integer, Double[]> toMap() {
            Map<Integer, Double[]> map = new LinkedHashMap<>();
            for (int i = 0; i < keys.size(); i++) {
                map.put(keys.get(i), values.get(i));
            }
            return map;
        }

        @Override
        public int getRowCount() {
            return keys.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            return colNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return switch (columnIndex) {
                case 0 -> keys.get(rowIndex);
                case 1 -> values.get(rowIndex)[0];
                case 2 -> values.get(rowIndex)[1];
                default -> null;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true; // Klucze są edytowalne
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            try {
                switch (columnIndex) {
                    case 0 -> {
                        int newKey = Integer.parseInt(aValue.toString());
                        if (!keys.contains(newKey) || newKey == keys.get(rowIndex)) {
                            keys.set(rowIndex, newKey);
                        } else {
                            JOptionPane.showMessageDialog(null, "Key already exists.");
                        }
                    }
                    case 1 -> values.get(rowIndex)[0] = Double.parseDouble(aValue.toString());
                    case 2 -> values.get(rowIndex)[1] = Double.parseDouble(aValue.toString());
                }
                fireTableCellUpdated(rowIndex, columnIndex);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Not a number: " + aValue);
            }
        }
    }
}
