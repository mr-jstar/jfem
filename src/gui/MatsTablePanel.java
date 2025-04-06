package gui;


import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jstar
 */
public class MatsTablePanel extends JPanel {
    private final MatsTableModel tableModel;
    private final JTable table;

    public MatsTablePanel(double[][] initialData, String [] colNames ) {
        setLayout(new BorderLayout());

        tableModel = new MatsTableModel(initialData, colNames);
        table = new JTable(tableModel);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        JButton addButton = new JButton("Dodaj wiersz");
        JButton removeButton = new JButton("Usuń zaznaczony");

        addButton.addActionListener(e -> tableModel.addRow(0.0, 0.0));
        removeButton.addActionListener(e -> {
            int selected = table.getSelectedRow();
            if (selected >= 0) {
                tableModel.removeRow(selected);
            }
        });

        buttonsPanel.add(addButton);
        buttonsPanel.add(removeButton);
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    public double[][] getMatsArray() {
        return tableModel.getArray();
    }

    private static class MatsTableModel extends AbstractTableModel {
        private final List<double[]> data = new ArrayList<>();
        private final String [] colNames;

        public MatsTableModel(double[][] initialData, String [] colNames) {
            for (double[] row : initialData) {
                if (row.length == 2) {
                    data.add(new double[]{row[0], row[1]});
                } else {
                    throw new IllegalArgumentException("Each row must have 2 elements.");
                }
            }
            this.colNames = colNames;
        }

        public void addRow(double x, double y) {
            data.add(new double[]{x, y});
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        public void removeRow(int rowIndex) {
            if (rowIndex >= 0 && rowIndex < data.size()) {
                data.remove(rowIndex);
                fireTableRowsDeleted(rowIndex, rowIndex);
            }
        }

        public double[][] getArray() {
            double[][] result = new double[data.size()][2];
            for (int i = 0; i < data.size(); i++) {
                result[i] = data.get(i);
            }
            return result;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return colNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data.get(rowIndex)[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            try {
                double value = Double.parseDouble(aValue.toString());
                data.get(rowIndex)[columnIndex] = value;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Nieprawidłowa liczba: " + aValue);
            }
        }
    }
}

