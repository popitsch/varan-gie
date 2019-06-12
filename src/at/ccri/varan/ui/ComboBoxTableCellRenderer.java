package at.ccri.varan.ui;

import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * For rendering the combo boxes in a JTable
 * @author niko.popitsch
 *
 * @param <T>
 */
class ComboBoxTableCellRenderer<T> extends JComboBox<T> implements TableCellRenderer {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
	    int row, int column) {
	setSelectedItem(value);
	return this;
    }

}
