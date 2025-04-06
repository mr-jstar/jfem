package gui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.*;
/**
 *
 * @author jstar
 */

public class MapEditorPanel extends JPanel {
    private final MapTableModel tableModel;
    private final JTable table;

    public MapEditorPanel(Map<Integer, double[]> initialData, String [] colNames ) {
        setLayout(new BorderLayout());

        tableModel = new MapTableModel(initialData, colNames);
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        JButton addButton = new JButton("Add row");
        JButton removeButton = new JButton("Remove selected");

        addButton.addActionListener(e -> tableModel.addEntry());
        removeButton.addActionListener(e -> {
            int selected = table.getSelectedRow();
            if (selected >= 0) {
                tableModel.removeEntry(selected);
            }
        });

        buttonsPanel.add(addButton);
        buttonsPanel.add(removeButton);
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    public Map<Integer, double[]> getData() {
        return tableModel.toMap();
    }

    // Model tabeli
    private static class MapTableModel extends AbstractTableModel {
        private final java.util.List<Integer> keys = new ArrayList<>();
        private final java.util.List<double[]> values = new ArrayList<>();
        private final String [] colNames;

        public MapTableModel(Map<Integer, double[]> initialData, String [] colNames) {
            this.colNames = colNames;
            for (Map.Entry<Integer, double[]> entry : initialData.entrySet()) {
                keys.add(entry.getKey());
                values.add(entry.getValue());
            }
        }

        public void addEntry() {
            int newKey = findNextFreeKey();
            keys.add(newKey);
            values.add(new double[]{0.0, 0.0});
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

        public Map<Integer, double[]> toMap() {
            Map<Integer, double[]> map = new LinkedHashMap<>();
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
            return columnIndex != 0 || true; // Klucze edytowalne jeśli chcesz
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
                            JOptionPane.showMessageDialog(null, "Klucz już istnieje.");
                        }
                    }
                    case 1 -> values.get(rowIndex)[0] = Double.parseDouble(aValue.toString());
                    case 2 -> values.get(rowIndex)[1] = Double.parseDouble(aValue.toString());
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Not a number: " + aValue);
            }
        }
    }
}
