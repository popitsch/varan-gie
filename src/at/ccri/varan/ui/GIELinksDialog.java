/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Fred Hutchinson Cancer Research Center and Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package at.ccri.varan.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.Logger;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.lists.GeneList;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.panel.IGVPopupMenu;

import at.ccri.varan.GIE;
import at.ccri.varan.GIEDatasetVersionLayer;
import at.ccri.varan.ui.ROILink.TYPE;
import at.ccri.varan.util.SpringUtilities;

/**
 * GIE links dialog.
 * 
 * @author niko.popitsch
 * 
 */
public class GIELinksDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(GIELinksDialog.class);

    /**
     * @see https://stackoverflow.com/questions/17627431/auto-resizing-the-jtable-column-widths
     * @param table
     */
    Integer[] min_widths = null;

    /**
     * table columns
     */
    final static int COLIDX_Chr1 = 0;
    final static int COLIDX_Start1 = 1;
    final static int COLIDX_End1 = 2;
    final static int COLIDX_Chr2 = 3;
    final static int COLIDX_Start2 = 4;
    final static int COLIDX_End2 = 5;
    final static int COLIDX_Del = 6;

    /**
     * Column names
     */
    Vector<String> columnNames = null;

    /**
     * Jtable
     * 
     */
    private JTable table;

    // private static Logger log = Logger.getLogger(GIEStatsDialog.class);

    public GIELinksDialog(Frame owner) {
	super(owner, "Links", true);
	init();
    }

    /**
     * @return x,y, width, height of current window
     */
    public Integer[] getCoords() {
	if (!isShowing())
	    return null;
	return new Integer[] { Math.max(0, (int) getLocationOnScreen().getX()),
		Math.max(0, (int) getLocationOnScreen().getY()), getWidth(), getHeight() };
    }

    /**
     * save coordinates
     */
    private void saveCoords() {
	Integer[] coords = getCoords();
	if (coords != null)
	    GIE.getInstance().getWindowCoordinates().put("GIELinksDialog", getCoords());
    }

    public void navigateTo(RegionOfInterest roi) {
	List<RegionOfInterest> rois = new ArrayList<RegionOfInterest>();
	rois.add(roi);
	navigateTo(rois);
    }

    /**
     * Set view
     * 
     * @param selectedRegions
     */
    public void navigateTo(List<RegionOfInterest> selectedRegions) {

	List<String> loci = new ArrayList<String>(selectedRegions.size());
	for (RegionOfInterest r : selectedRegions)
	    loci.add(r.getLocusString());
	GeneList geneList = new GeneList("Regions of Interest", loci, false);
	IGV.getInstance().setGeneList(geneList);
	IGV.getInstance().resetFrames();
    }

    public ROILink getROILink(JTable table, int row) {
	String chr1 = table.getModel().getValueAt(table.convertRowIndexToModel(row), COLIDX_Chr1).toString();
	String chr2 = table.getModel().getValueAt(table.convertRowIndexToModel(row), COLIDX_Chr2).toString();
	Integer start1 = (Integer) table.getModel().getValueAt(table.convertRowIndexToModel(row), COLIDX_Start1);
	Integer start2 = (Integer) table.getModel().getValueAt(table.convertRowIndexToModel(row), COLIDX_Start2);
	Integer end1 = (Integer) table.getModel().getValueAt(table.convertRowIndexToModel(row), COLIDX_End1);
	Integer end2 = (Integer) table.getModel().getValueAt(table.convertRowIndexToModel(row), COLIDX_End2);
	RegionOfInterest r1 = new RegionOfInterest(chr1, start1, end1, "source");
	RegionOfInterest r2 = new RegionOfInterest(chr2, start2, end2, "target");
	return new ROILink(r1, r2, TYPE.FUSION);
    }

    /**
     * 
     * @return the real index of the currently selected table rows.
     */
    private int[] getSelectedRows() {
	if (table.getSelectedRows() == null)
	    return null;
	int[] rows = new int[table.getSelectedRows().length];
	for (int i = 0; i < table.getSelectedRows().length; i++)
	    rows[i] = table.convertRowIndexToModel(table.getSelectedRows()[i]);
	return rows;
    }

    /**
     * Return the selected regions in the table view.
     *
     * @param selectedRows
     * @return
     */
    public List<ROILink> getSelectedROILink() {
	List<ROILink> selectedROILinks = new ArrayList<ROILink>();
	// convert to real indices
	int[] selectedRows = getSelectedRows();
	if (selectedRows == null)
	    return selectedROILinks;
	for (int row : selectedRows)
	    selectedROILinks.add(getROILink(table, row));
	return selectedROILinks;
    }

    /**
     * Resize table column width
     * 
     * @param table
     */
    public void resizeColumnWidth(JTable table) {
	final TableColumnModel columnModel = table.getColumnModel();
	for (int column = 0; column < table.getColumnCount(); column++) {
	    int width = min_widths[column]; // Min width
	    for (int row = 0; row < table.getRowCount(); row++) {
		TableCellRenderer renderer = table.getCellRenderer(row, column);
		Component comp = table.prepareRenderer(renderer, row, column);
		width = Math.max(comp.getPreferredSize().width + 1, width);
	    }
	    if (width > 300)
		width = 300;
	    columnModel.getColumn(column).setPreferredWidth(width);
	    columnModel.getColumn(column).setMinWidth(width);
	}
    }

    /**
     * Copy region to clipboard
     */
    public void copyRows() {
	int[] selectedRows = getSelectedRows();
	if (selectedRows.length > 0) {
	    StringBuilder sb = new StringBuilder();
	    for (int rowIndex : selectedRows) {
		Enumeration<TableColumn> cols = table.getColumnModel().getColumns();
		while (cols.hasMoreElements()) {
		    TableColumn tc = cols.nextElement();
		    switch (tc.getModelIndex()) {
		    case COLIDX_Del:
			break;
		    default:
			if (sb.length() > 0)
			    sb.append("\t");
			sb.append(table.getModel().getValueAt(rowIndex, tc.getModelIndex()));
			break;
		    }
		}
		sb.append("\n");
	    }

	    // List<RegionOfInterest> selectedRegions = getSelectedRegions(selectedRows);
	    // StringBuilder sb = new StringBuilder();
	    // for (RegionOfInterest r : selectedRegions)
	    // sb.append(r.toString() + "\n");
	    Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
	    clpbrd.setContents(new StringSelection(sb.toString()), null);
	}
    }

    /**
     * populates the main table.
     * 
     * @param model
     */
    private void reloadTable() {

	java.awt.EventQueue.invokeLater(new Runnable() {
	    public void run() {

		DefaultTableModel model = (DefaultTableModel) table.getModel();
		for (int i = model.getRowCount() - 1; i >= 0; i--) {
		    model.removeRow(i);
		}
		if (GIE.getInstance().getActiveDataset() != null) {
		    GIEDatasetVersionLayer activeLayer = GIE.getInstance().getActiveDataset().getCurrentVersion()
			    .getActiveLayer();

		    for (ROILink rl : activeLayer.getLinks()) {
			Vector<Object> vals = new Vector<>();
			vals.add(rl.getSource().getChr());
			vals.add(rl.getSource().getStart());
			vals.add(rl.getSource().getEnd());
			vals.add(rl.getTarget().getChr());
			vals.add(rl.getTarget().getStart());
			vals.add(rl.getTarget().getEnd());
			vals.add("Delete");
			model.addRow(vals.toArray());
		    }

		}
		// tableSorter.setRowFilter(filter);

		model.fireTableDataChanged();
		resizeColumnWidth(table);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		// IGV.getInstance().revalidateTrackPanels();
	    }
	});

    }

    /**
     * Initialize the dialog.
     */
    private void init() {
	setTitle("VARAN-GIE :: Links");
	setMinimumSize(new Dimension(500, 520));
	setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	// +++++++++++++++++++++++++++++++++++++++++++++++
	// set location on screen
	// +++++++++++++++++++++++++++++++++++++++++++++++
	addWindowListener(new WindowAdapter() {
	    // use windowClosed here (instead of windowClosinG!)
	    @Override
	    public void windowClosing(WindowEvent e) {
		saveCoords();
	    }
	});
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIELinksDialog");
	if (coords == null) {
	    setPreferredSize(new Dimension(500, 520));
	    setLocationRelativeTo(IGV.getMainFrame());
	} else {
	    // check compatibility with actual screen size
	    coords[0] = Math.min(coords[0], GIE.SCREEN_WIDTH - coords[2]);
	    coords[1] = Math.min(coords[1], GIE.SCREEN_HEIGHT - coords[3]);
	    setLocation(coords[0], coords[1]);
	    setPreferredSize(new Dimension(coords[2], coords[3]));
	}
	// +++++++++++++++++++++++++++++++++++++++++++++++

	JPanel formPanel = new JPanel(new SpringLayout());

	final class MyTableModel extends DefaultTableModel {
	    private static final long serialVersionUID = 1L;

	    public MyTableModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	    }

	    public boolean isCellEditable(int row, int col) {
		return col == COLIDX_Del;
	    }

	    @Override
	    public Object getValueAt(int row, int column) {
		if (row >= dataVector.size())
		    return null;
		Vector<?> rowVector = (Vector<?>) dataVector.elementAt(row);
		return rowVector.elementAt(column);
	    }

	    @Override
	    public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case COLIDX_Start1:
		case COLIDX_End1:
		case COLIDX_Start2:
		case COLIDX_End2:
		    return Integer.class;
		case COLIDX_Chr1:
		case COLIDX_Chr2:
		    return String.class;
		default:
		    return String.class;
		}
	    }

	    public void setValueAt(Object value, int row, int col) {
		super.setValueAt(value, row, col);
	    }

	}
	;

	// render the table
	final class MyTableCellRenderer extends DefaultTableCellRenderer {
	    /**
	     * 
	     */
	    private static final long serialVersionUID = 1L;

	    @Override
	    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
		    boolean hasFocus, int row, int column) {
		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		ROILink rl = getROILink(table, row);
		boolean intervalsExists = IGV.getInstance().getSession().getAllRegionsOfInterest()
			.contains(rl.getSource())
			&& IGV.getInstance().getSession().getAllRegionsOfInterest().contains(rl.getTarget());
		if (intervalsExists)
		    c.setForeground(Color.BLUE);
		else
		    c.setForeground(Color.RED);
		return c;
	    }
	}
	;

	columnNames = new Vector<>();
	for (String s : new String[] { "Chr1", "Start1", "End1", "Chr2", "Start2", "End2", "" })
	    columnNames.add(s);
	List<Integer> columnWidths = new ArrayList<>();
	for (int w : new int[] { 50, 70, 70, 50, 70, 70, 10 })
	    columnWidths.add(w);
	min_widths = (Integer[]) columnWidths.toArray(new Integer[columnWidths.size()]);

	MyTableModel model = new MyTableModel(columnNames.toArray(), 0);
	this.table = new JTable(model);
	table.setDefaultRenderer(Object.class, new MyTableCellRenderer());
	table.setDefaultRenderer(Integer.class, new MyTableCellRenderer());
	table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	table.setRowSelectionAllowed(true);
	table.setSelectionBackground(Color.lightGray);
	table.addMouseListener(new MyTablePopupHandler());
	// delete button action
	Action delete = new AbstractAction() {
	    private static final long serialVersionUID = 1L;

	    public void actionPerformed(ActionEvent e) {
		List<ROILink> selectedRegions = getSelectedROILink();
		GIEDatasetVersionLayer activeLayer = GIE.getInstance().getActiveDataset().getCurrentVersion()
			.getActiveLayer();
		activeLayer.deleteLinks(selectedRegions);
		reloadTable();
	    }
	};
	new ButtonColumn(table, delete, null, "Delete this link", UIManager.getIcon("InternalFrame.closeIcon"),
		COLIDX_Del);
	reloadTable();

	formPanel.add(new JScrollPane(table), BorderLayout.CENTER);

	SpringUtilities.makeCompactGrid(formPanel, 1, 1, // rows, cols
		6, 6, // initX, initY
		6, 6); // xPad, yPad

	JPanel buttonPanel = new JPanel();
	buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 6));
	buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

	// buttons
	JButton buttonOk = new JButton("OK");
	buttonOk.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		saveCoords();
		dispose();
	    }
	});

	buttonPanel.add(Box.createHorizontalGlue());
	buttonPanel.add(buttonOk);

	add(formPanel, BorderLayout.CENTER);
	add(buttonPanel, BorderLayout.SOUTH);

	pack();
	setVisible(true);
    }

    public void refresh() {
	if (isVisible()) {
	    pack();
	} else {
	    setVisible(true);
	    pack();
	}
	reloadTable();
    }

    /**
     * Popup handler
     */
    private class MyTablePopupHandler extends MouseAdapter {

	public void mousePressed(MouseEvent e) {
	    if (SwingUtilities.isRightMouseButton(e)) {
		Point p = e.getPoint();
		// must convert row index from view to model, in case of
		// sorting, filtering
		int[] rows = getSelectedRows();

		// int col = table.columnAtPoint(p);

		if (rows.length >= 0) {

		    GIEDatasetVersionLayer activeLayer = GIE.getInstance().getActiveDataset().getCurrentVersion()
			    .getActiveLayer();

		    List<ROILink> selectedLinks = getSelectedROILink();
		    JPopupMenu popupMenu = new IGVPopupMenu();

		    if (selectedLinks != null && selectedLinks.size() > 0) {

			JMenuItem viewItem2 = new JMenuItem("Show source");
			viewItem2.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				RegionOfInterest roi = selectedLinks.get(0).getSource();
				navigateTo(new RegionOfInterest(roi.getChr(), roi.getDisplayStart() - 1000,
					roi.getDisplayEnd() + 1000, null));
			    }
			});
			popupMenu.add(viewItem2);

			JMenuItem viewItem3 = new JMenuItem("Show target");
			viewItem3.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				RegionOfInterest roi = selectedLinks.get(0).getTarget();
				navigateTo(new RegionOfInterest(roi.getChr(), roi.getDisplayStart() - 1000,
					roi.getDisplayEnd() + 1000, null));
			    }
			});
			popupMenu.add(viewItem3);

			JMenuItem delItem = new JMenuItem("Delete Selected [CTRL-D]");
			delItem.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				int[] selectedRows = table.getSelectedRows();
				if (selectedRows.length > 0) {
				    List<ROILink> selectedRegions = getSelectedROILink();
				    GIEDatasetVersionLayer activeLayer = GIE.getInstance().getActiveDataset()
					    .getCurrentVersion().getActiveLayer();
				    activeLayer.deleteLinks(selectedRegions);
				    reloadTable();
				}
			    }
			});
			popupMenu.add(delItem);

			popupMenu.addSeparator();

			JMenuItem copyItem = new JMenuItem("Copy [CTRL-C]");
			copyItem.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				copyRows();
			    }
			});
			popupMenu.add(copyItem);

			popupMenu.show(table, p.x, p.y);
		    }
		}
	    }
	}
    }
}
