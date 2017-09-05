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
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.broad.igv.Globals;
import org.broad.igv.event.IGVEventObserver;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.lists.GeneList;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.panel.IGVPopupMenu;

import at.ccri.varan.GIE;
import at.ccri.varan.GIEDatasetVersion;
import at.ccri.varan.GIEDatasetVersionLayer;

/**
 * @author niko.popitsch
 * 
 *         GIE data dialog
 * 
 */
public class GIEDataDialog extends JDialog implements Observer, IGVEventObserver {

    private static final long serialVersionUID = 1L;

    /**
     * Singleton instance
     */
    public static GIEDataDialog instance;

    /**
     * @see https://stackoverflow.com/questions/17627431/auto-resizing-the-jtable-column-widths
     * @param table
     */
    Integer[] min_widths = null;

    /**
     * Column names
     */
    List<String> columnNames = null;

    /**
     * Jtable
     * 
     */
    private JTable table;

    /**
     * Layers combo box
     */
    private JComboBox<String> layerCombo = new JComboBox<>();

    /* internal. Flag whether combobox listener actions should be executed */
    private boolean testActionListenerActive;

    private JMenuItem copyToLayerItem;
    private JMenuItem moveToLayerItem;

    /**
     * If set, reload will not update the JTable
     */
    public static boolean blockReload = false;

    /**
     * Rename layer button
     */
    private JButton renLay;
    /**
     * Delete layer button.
     */
    private JButton delLay;

    /**
     * Color chooser (stores recent colors)
     */
    private JColorChooser colorChooser = new JColorChooser();

    /**
     * Return the active GIEMainDialog. null if none
     *
     * @return
     */
    public static GIEDataDialog getInstance(Frame owner) {
	if (instance == null) {
	    instance = new GIEDataDialog(owner);
	}
	return instance;
    }

    /**
     * Return the active GIEMainDialog. null if none
     *
     * @return
     */
    public static GIEDataDialog getInstance() {
	return instance;
    }

    /**
     * dispose the active instance and get rid of the pointer. Return whether or
     * not there was an active instance
     */
    public static boolean destroyInstance() {
	if (instance == null)
	    return false;
	GIE.getInstance().getWindowCoordinates().put("GIEDataDialog", instance.getCoords());
	instance.dispose();
	instance = null;
	return true;
    }

    private GIEDataDialog(Frame owner) {
	super(owner);
	init();
    }

    private GIEDataDialog(Dialog owner) {
	super(owner);
	init();
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

    public void navigateTo(RegionOfInterest roi) {
	List<RegionOfInterest> rois = new ArrayList<RegionOfInterest>();
	rois.add(roi);
	navigateTo(rois);
    }

    public void navigateTo(List<RegionOfInterest> selectedRegions) {

	List<String> loci = new ArrayList<String>(selectedRegions.size());
	for (RegionOfInterest r : selectedRegions)
	    loci.add(r.getLocusString());
	GeneList geneList = new GeneList("Regions of Interest", loci, false);
	IGV.getInstance().setGeneList(geneList);
	IGV.getInstance().resetFrames();
    }

    /**
     * populates the main table.
     * 
     * @param model
     */
    private void reloadTable() {
	if (blockReload)
	    return;

	// *******************
	setUpComboColumn(table, table.getColumnModel().getColumn(COLIDX_Chr), IGV.getInstance().getChromNamesArray(),
		null);

	DefaultTableModel model = (DefaultTableModel) table.getModel();
	for (int i = model.getRowCount() - 1; i >= 0; i--) {
	    model.removeRow(i);
	}
	if (GIE.getInstance().getActiveDataset() != null) {

	    List<RegionOfInterest> now = (List<RegionOfInterest>) IGV.getInstance().getSession()
		    .getAllRegionsOfInterest();
	    UndoHandler.getInstance().addUndoStep(now);

	    // load from regions of interest
	    for (RegionOfInterest r : now) {
		List<Object> d = new ArrayList<>();
		// NOTE: display a normalized version of the chr string for hg19
		boolean defGenome = GenomeManager.getInstance().getCurrentGenome().getId()
			.equals(Globals.DEFAULT_GENOME);
		for (Object o : new Object[] {
			defGenome ? CanonicalChromsomeComparator.getCanonicalMappingHuman(r.getChr()) : r.getChr(),
			r.getStart(), r.getEnd(), getIntervalWidth(r.getEnd() - r.getStart()), "View", "Delete",
			(r.getDescription() == null ? "-" : r.getDescription()),
			(r.getScore() == null ? "-" : r.getScore()), (r.getStrand() == null ? "0" : r.getStrand()),
			(r.getColor() == null ? "-" : r.getColor()) })
		    d.add(o);

		for (String s : GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer()
			.getAnnotations()) {
		    String v = r.getAnnotation(s);
		    d.add(v == null ? "" : v);
		}
		model.addRow(d.toArray());
	    }
	}

	// update layer combobox
	if (GIE.getInstance().getActiveDataset() != null) {
	    testActionListenerActive = false;
	    layerCombo.removeAllItems();
	    Iterator<String> lns = GIE.getInstance().getActiveDataset().getCurrentVersion().getLayers().keySet()
		    .iterator();
	    String al = GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer().getLayerName();
	    String aln = null;
	    int i = 1;
	    while (lns.hasNext()) {
		String ln = lns.next();
		String visibleLn = i + ": " + ln;
		layerCombo.addItem(visibleLn);
		if (ln.equals(al))
		    aln = visibleLn;
		i++;
	    }
	    if (aln != null) {
		layerCombo.setSelectedItem(aln);
	    }
	    if (al != null && al.equals(GIEDatasetVersion.defaultLayerName)) {
		renLay.setEnabled(false);
		delLay.setEnabled(false);
	    } else {
		renLay.setEnabled(true);
		delLay.setEnabled(true);
	    }
	    testActionListenerActive = true;
	}

	model.fireTableDataChanged();
	resizeColumnWidth(table);
	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	// IGV.getInstance().revalidateTrackPanels();
    }

    /**
     * Return interval length in human readable form
     * 
     * @param i
     * @return
     */
    private String getIntervalWidth(int v) {
	v = Math.abs(v);
	if (v == 0)
	    return String.format(Locale.US, "0 %s", "bp");
	int unit = 1000;
	int exp = (int) (Math.log(v) / Math.log(unit));
	switch (exp) {
	case 0:
	    return String.format(Locale.US, "%.1f %s", v / Math.pow(unit, exp), "bp");
	case 1:
	    return String.format(Locale.US, "%.1f %s", v / Math.pow(unit, exp), "kb");
	case 2:
	    return String.format(Locale.US, "%.1f %s", v / Math.pow(unit, exp), "Mb");
	case 3:
	    return String.format(Locale.US, "%.1f %s", v / Math.pow(unit, exp), "Gb");
	default:
	    return String.format(Locale.US, "%.1f %s", v / Math.pow(unit, exp), "bp");
	}
    }

    private String getSelectedLayerName() {
	if (layerCombo.getSelectedItem() == null)
	    return null;
	// remove i+": "
	String lname = ((String) layerCombo.getSelectedItem());
	lname = lname.substring(lname.indexOf(":") + 1).trim();
	return lname;
    }

    /**
     * parse a width given in human readable format
     * 
     * @param s
     * @return
     * @throws ParseException
     */
    private Integer parseIntervalWidth(String s) throws ParseException {
	String[] t = s.split(" ", -1);
	double mult = 1;
	if (t.length > 1) {
	    switch (t[1].toLowerCase()) {
	    case "bp":
		mult = 1;
		break;
	    case "kb":
		mult = 1000;
		break;
	    case "mb":
		mult = 1000000;
		break;
	    case "gb":
		mult = 1000000000;
		break;
	    default:
		throw new ParseException("Unknown unit symbol " + t[1], 0);
	    }
	}
	Number n = NumberFormat.getInstance(Locale.US).parse(t[0]);
	Double d = n.doubleValue() * mult;
	return d.intValue();
    }

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

    public List<RegionOfInterest> getSelectedRegions() {
	return getSelectedRegions(table.getSelectedRows());
    }

    /**
     * Return the selected regions in the table view.
     *
     * @param selectedRows
     * @return
     */
    private List<RegionOfInterest> getSelectedRegions(int[] selectedRows) {
	List<RegionOfInterest> selectedRegions = new ArrayList<RegionOfInterest>();
	if (selectedRows == null)
	    return selectedRegions;
	List<RegionOfInterest> regions = (List<RegionOfInterest>) IGV.getInstance().getSession()
		.getAllRegionsOfInterest();
	for (int selectedRowIndex : selectedRows) {
	    selectedRegions.add(regions.get(selectedRowIndex));
	}
	return selectedRegions;
    }

    /**
     * Return the region corresponding to the passed row index.
     * 
     * @param selectedRows
     * @return
     */
    private RegionOfInterest getSelectedRegion(int selectedRow) {
	List<RegionOfInterest> regions = (List<RegionOfInterest>) IGV.getInstance().getSession()
		.getAllRegionsOfInterest();
	if (selectedRow < 0 || selectedRow >= regions.size())
	    return null;
	return regions.get(selectedRow);
    }

    /**
     * Return the region corresponding to the passed row indices.
     * 
     * @param selectedRows
     * @return
     */
    private RegionOfInterest getSelectedMergedRegion(int[] selectedRows) {
	if (selectedRows.length == 0)
	    return null;
	List<RegionOfInterest> regions = (List<RegionOfInterest>) IGV.getInstance().getSession()
		.getAllRegionsOfInterest();
	int min = Integer.MAX_VALUE;
	int max = Integer.MIN_VALUE;
	for (int s : selectedRows) {
	    min = Math.min(s, min);
	    max = Math.max(s, max);
	}

	if (min < 0 || min >= regions.size() || max < 0 || max >= regions.size())
	    return null;
	RegionOfInterest minreg = regions.get(min);
	if (min == max)
	    return minreg;
	int maxcoord = minreg.getEnd();
	for (int i = min + 1; i <= max; i++) {
	    RegionOfInterest r = regions.get(i);
	    if (r.getChr().equals(minreg.getChr()))
		maxcoord = r.getEnd();
	}

	return new RegionOfInterest(minreg.getChr(), minreg.getStart(), maxcoord, minreg.getDescription() + "...");
    }

    /**
     * Select a roi in the table
     * 
     * @param roi
     */
    public void selectRegion(RegionOfInterest roi) {
	List<RegionOfInterest> regions = (List<RegionOfInterest>) IGV.getInstance().getSession()
		.getAllRegionsOfInterest();
	int idx = 0;
	for (RegionOfInterest r : regions) {
	    if (r.equals(roi))
		break;
	    idx++;
	}
	table.clearSelection();
	table.setRowSelectionInterval(idx, idx);
	table.repaint();
    }

    /**
     * Create a combobox column.
     * 
     * @param table
     * @param col
     * @param choices
     */
    public void setUpComboColumn(JTable table, TableColumn col, String[] choices, String[] tooltips) {
	// renderer
	class ComboboxToolTipRenderer extends DefaultListCellRenderer {
	    List<String> tooltips = new ArrayList<>();

	    @Override
	    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
		    boolean cellHasFocus) {

		JComponent comp = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected,
			cellHasFocus);

		if (-1 < index && null != value && null != tooltips) {
		    list.setToolTipText(tooltips.get(index));
		}
		return comp;
	    }

	    public void addToolTip(String tt) {
		this.tooltips.add(tt);
	    }
	}
	// Set up combobox
	JComboBox<String> comboBox = new JComboBox<>();
	ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
	comboBox.setRenderer(renderer);
	DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
	if (tooltips == null)
	    tooltips = choices;
	for (int i = 0; i < choices.length; i++) {
	    model.addElement(choices[i]);
	    renderer.addToolTip(tooltips[i]);
	}
	comboBox.setModel(model);
	col.setCellEditor(new DefaultCellEditor(comboBox));
    }

    /**
     * Copy region to clipboard
     */
    public void copyRows() {
	int[] selectedRows = table.getSelectedRows();
	if (selectedRows.length > 0) {
	    List<RegionOfInterest> selectedRegions = getSelectedRegions(selectedRows);
	    StringBuilder sb = new StringBuilder();
	    for (RegionOfInterest r : selectedRegions)
		sb.append(r.toString() + "\n");
	    Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
	    clpbrd.setContents(new StringSelection(sb.toString()), null);
	}
    }

    public JTable getTable() {
	return table;
    }

    /**
     * table columns
     */
    final static int COLIDX_Chr = 0;
    final static int COLIDX_Start = 1;
    final static int COLIDX_End = 2;
    final static int COLIDX_Width = 3;
    final static int COLIDX_View = 4;
    final static int COLIDX_Del = 5;
    final static int COLIDX_Name = 6;
    final static int COLIDX_Score = 7;
    final static int COLIDX_Strand = 8;
    final static int COLIDX_COLOR = 9;

    /**
     * @return x, y, with, height of current window
     */
    public Integer[] getCoords() {
	if (!isShowing())
	    return null;
	return new Integer[] { (int) getLocationOnScreen().getX(), (int) getLocationOnScreen().getY(), getWidth(),
		getHeight() };
    }

    /**
     * Initialize the dialog.
     */
    private void init() {
	// ======== this ========
	setTitle("VARAN-GIE :: Genomic Intervals");
	setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	Container contentPane = getContentPane();
	getContentPane().setLayout(new BorderLayout());
	// +++++++++++++++++++++++++++++++++++++++++++++++
	// set location on screen
	// +++++++++++++++++++++++++++++++++++++++++++++++
	addWindowListener(new WindowAdapter() {
	    @Override
	    public void windowClosing(WindowEvent e) {
		Integer[] coords = getCoords();
		if (coords != null)
		    GIE.getInstance().getWindowCoordinates().put("GIEDataDialog", getCoords());
	    }
	});
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIEDataDialog");
	if (coords == null) {
	    setPreferredSize(new Dimension(620, 500));
	    setLocationRelativeTo(IGV.getMainFrame());
	} else {
	    // check compatibility with actual screen size
	    coords[0] = Math.min(coords[0], GIE.SCREEN_WIDTH - coords[2]);
	    coords[1] = Math.min(coords[1], GIE.SCREEN_HEIGHT - coords[3]);
	    setLocation(coords[0], coords[1]);
	    setPreferredSize(new Dimension(coords[2], coords[3]));
	}
	// +++++++++++++++++++++++++++++++++++++++++++++++

	/**
	 * header label
	 */
	JPanel panel = new JPanel();
	FlowLayout flowLayout = (FlowLayout) panel.getLayout();
	flowLayout.setAlignment(FlowLayout.LEFT);
	getContentPane().add(panel, BorderLayout.NORTH);
	{
	    JLabel lab = new JLabel("Genomic Intervals in Layer");
	    panel.add(lab);

	    layerCombo.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if (testActionListenerActive) {
			String lname = getSelectedLayerName();
			System.out.println("Select layer: " + lname);

			String current = GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer()
				.getLayerName();
			if (current != lname)
			    GIE.getInstance().getActiveDataset().getCurrentVersion().setActiveLayer(lname);
			IGV.getInstance().repaintNamePanels();

			refresh();
		    }
		}

	    });
	    panel.add(layerCombo);
	    testActionListenerActive = true;

	    JButton addLay = new JButton("Add Layer");
	    addLay.setToolTipText("Add a new interval layer");
	    addLay.setHorizontalAlignment(SwingConstants.LEFT);
	    panel.add(addLay);
	    addLay.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    String lname = JOptionPane.showInputDialog(null, "Layer name: ", "");
		    if (lname != null && !lname.trim().equals("")) {
			try {
			    GIE.getInstance().getActiveDataset().getCurrentVersion().addLayer(lname);

			    // ensure that new table is added to IGV session
			    GIEDatasetVersionLayer layer = GIE.getInstance().getActiveDataset().getCurrentVersion()
				    .getActiveLayer();
			    layer.save();

			    GIE.getInstance().reloadActiveDataset();

			    refresh();

			} catch (IOException e1) {
			    e1.printStackTrace();
			}
		    }
		}

	    });
	    delLay = new JButton("Delete Layer");
	    delLay.setToolTipText("Delete this layer. The 'main' layer cannot be deleted.");
	    delLay.setHorizontalAlignment(SwingConstants.LEFT);
	    panel.add(delLay);
	    delLay.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    String lname = getSelectedLayerName();
		    int reply = JOptionPane.showConfirmDialog(null,
			    "Are you sure you want to delete this layer (cannot be undone)?", "Delete Layer",
			    JOptionPane.YES_NO_OPTION);
		    if (reply == JOptionPane.YES_OPTION) {
			try {
			    // delete layer
			    GIE.getInstance().getActiveDataset().getCurrentVersion().delLayer(lname);
			    refresh();
			} catch (IOException e1) {
			    // TODO Auto-generated catch block
			    e1.printStackTrace();
			}
		    }
		}

	    });
	    delLay.setEnabled(false);

	    renLay = new JButton("Rename Layer");
	    renLay.setToolTipText("Rename this layer. Layer names have to unique within a dataset.");
	    renLay.setHorizontalAlignment(SwingConstants.LEFT);
	    panel.add(renLay);
	    renLay.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    String oldlname = getSelectedLayerName();
		    String lname = JOptionPane.showInputDialog(null, "Layer name: ", oldlname);
		    if (lname != null && !lname.trim().equals("")) {
			try {
			    if (!GIE.getInstance().getActiveDataset().getCurrentVersion().renameLayer(oldlname,
				    lname)) {
				System.err.println("Could nor rename layer " + oldlname);
			    }
			} catch (IOException e1) {
			    System.err.println("Could nor rename layer " + oldlname);
			    e1.printStackTrace();
			}
			refresh();
		    }
		}
	    });
	    renLay.setEnabled(false);
	}

	columnNames = new ArrayList<>();
	for (String s : new String[] { "Chr", "Start", "End", "Width", "", "", "Name", "Score", "Strand", "Color" })
	    columnNames.add(s);
	List<Integer> columnWidths = new ArrayList<>();
	for (int w : new int[] { 50, 70, 70, 50, 50, 25, 100, 50, 30, 50 })
	    columnWidths.add(w);
	if (GIE.getInstance().getActiveDataset() != null) {
	    for (String s : GIE.getInstance().getActiveDataset().getCurrentVersion().getDefaultLayer()
		    .getAnnotations()) {
		columnNames.add(s);
		columnWidths.add(50);
	    }
	}
	System.out.println("COLNAMES" + columnNames);
	min_widths = (Integer[]) columnWidths.toArray(new Integer[columnWidths.size()]);

	final class MyTableModel extends DefaultTableModel {
	    private static final long serialVersionUID = 1L;

	    public MyTableModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	    }

	    public boolean isCellEditable(int row, int col) {
		return col != COLIDX_COLOR;
	    }

	    @Override
	    public Object getValueAt(int row, int column) {
		if (row >= dataVector.size())
		    return null;
		Vector rowVector = (Vector) dataVector.elementAt(row);
		return rowVector.elementAt(column);
	    }

	    public void setValueAt(Object value, int row, int col) {
		RegionOfInterest r = getSelectedRegion(row);
		String v = (String) value;
		if (r == null)
		    return;

		List<RegionOfInterest> now = (List<RegionOfInterest>) IGV.getInstance().getSession()
			.getAllRegionsOfInterest();
		UndoHandler.getInstance().addUndoStep(now);

		switch (col) {
		case COLIDX_Chr:
		    r.setChr(v);
		    boolean defGenome = GenomeManager.getInstance().getCurrentGenome().getId()
			    .equals(Globals.DEFAULT_GENOME);
		    super.setValueAt(defGenome ? CanonicalChromsomeComparator.getCanonicalMappingHuman(v) : v, row,
			    col);
		    break;
		case COLIDX_Width:
		    try {
			Integer w = parseIntervalWidth(v);
			r.setEnd(r.getStart() + Math.abs(w));
			super.setValueAt(r.getEnd(), row, COLIDX_End);
			super.setValueAt(getIntervalWidth(r.getEnd() - r.getStart()), row, COLIDX_Width);
		    } catch (ParseException ex) {
			JOptionPane.showMessageDialog(IGV.getMainFrame(), "Parsing Error " + ex.getMessage(), "Error",
				JOptionPane.ERROR_MESSAGE);
		    }
		    break;
		case COLIDX_Start:
		    r.setStart(Math.abs(Integer.parseInt(v)));
		    super.setValueAt(getIntervalWidth(r.getEnd() - r.getStart()), row, COLIDX_Width);
		    super.setValueAt(value, row, col);
		    break;
		case COLIDX_End:
		    r.setEnd(Math.abs(Integer.parseInt(v)));
		    super.setValueAt(getIntervalWidth(r.getEnd() - r.getStart()), row, COLIDX_Width);
		    super.setValueAt(value, row, col);
		    break;
		case COLIDX_Name:
		    r.setDescription(v);
		    super.setValueAt(value, row, col);
		    break;
		case COLIDX_Score:
		    r.setScore(v);
		    super.setValueAt(value, row, col);
		    break;
		case COLIDX_Strand:
		    r.setStrand(v);
		    super.setValueAt(value, row, col);
		    break;
		case COLIDX_COLOR:
		    r.setColor(v);
		    super.setValueAt(value, row, col);
		    break;
		default:
		    r.addAnnotation(columnNames.get(col), v);
		    super.setValueAt(value, row, col);
		}

		fireTableCellUpdated(row, col);
		repaint();
		revalidate();
		IGV.getInstance().revalidateTrackPanels();
		resizeColumnWidth(table);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	    }
	}
	;

	/**
	 * SET UP TABLE
	 */
	MyTableModel model = new MyTableModel(columnNames.toArray(), 0);
	this.table = new JTable(model);
	table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	table.setRowSelectionAllowed(true);
	table.setSelectionBackground(Color.red);
	table.addMouseListener(new MyTablePopupHandler());

	// configure strand input
	setUpComboColumn(table, table.getColumnModel().getColumn(COLIDX_Strand), new String[] { "0", "+", "-" },
		new String[] { "No/unknown strand", "Plus strand", "Minus strand" });
	reloadTable();

	// load button action
	Action view = new AbstractAction() {
	    private static final long serialVersionUID = 1L;

	    public void actionPerformed(ActionEvent e) {
		JTable table = (JTable) e.getSource();
		int[] selectedRows = table.getSelectedRows();
		navigateTo(getSelectedRegions(selectedRows));
	    }

	};
	new ButtonColumn(table, view, "View", "View this interval", null, COLIDX_View);

	// delete button action
	Action delete = new AbstractAction() {
	    private static final long serialVersionUID = 1L;

	    public void actionPerformed(ActionEvent e) {
		JTable table = (JTable) e.getSource();
		int[] selectedRows = table.getSelectedRows();
		List<RegionOfInterest> selectedRegions = getSelectedRegions(selectedRows);
		IGV.getInstance().getSession().removeROI(selectedRegions);
		reloadTable();
	    }
	};
	new ButtonColumn(table, delete, null, "Delete this interval", UIManager.getIcon("InternalFrame.closeIcon"),
		COLIDX_Del);

	contentPane.add(new JScrollPane(table), BorderLayout.CENTER);

	/**
	 * Add interval button
	 */
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
	getContentPane().add(buttonPane, BorderLayout.SOUTH);
	{
	    JButton but = new JButton("Add/Import Intervals");
	    but.setToolTipText("Add intervals by manual editing, copy/paste or import from a BED file.");
	    but.setHorizontalAlignment(SwingConstants.LEFT);
	    buttonPane.add(but);
	    but.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    new GIEAddIntervalDialog(IGV.getMainFrame());
		}

	    });
	}

	/**
	 * Export button
	 */
	getContentPane().add(buttonPane, BorderLayout.SOUTH);
	{
	    JButton but = new JButton("Export Intervals");
	    but.setToolTipText("Export intervals from selected layers");
	    but.setHorizontalAlignment(SwingConstants.LEFT);
	    buttonPane.add(but);
	    but.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    new GIEExportDialog(IGV.getMainFrame());
		}

	    });
	}

	/**
	 * Stats button
	 */
	getContentPane().add(buttonPane, BorderLayout.SOUTH);
	{
	    JButton but = new JButton("Statistics");
	    but.setToolTipText("Show some basic statistics about the intervals in the selected layer.");
	    but.setHorizontalAlignment(SwingConstants.LEFT);
	    buttonPane.add(but);
	    but.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    new GIEStatsDialog(IGV.getMainFrame());
		}

	    });
	}

	/**
	 * Edit annotations button
	 */
	getContentPane().add(buttonPane, BorderLayout.SOUTH);
	{
	    JButton but = new JButton("Edit annotations");
	    but.setToolTipText("Add/remove additional annotation fields");
	    but.setHorizontalAlignment(SwingConstants.LEFT);
	    buttonPane.add(but);
	    but.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    // show edit annotations dialog.
		    new GIEEditAnnotationsDialog(IGV.getMainFrame());
		    // save to BED and reload
		    GIE.getInstance().reloadActiveDataset();
		}

	    });
	}

	/**
	 * Undo button
	 */
	getContentPane().add(buttonPane, BorderLayout.SOUTH);
	{
	    JButton undoButton = new JButton("UNDO");
	    undoButton.setHorizontalAlignment(SwingConstants.LEFT);
	    undoButton.setToolTipText("Undo last action [CTRL-Z]");
	    buttonPane.add(undoButton);
	    undoButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    IGV.getInstance().getSession().replaceRegionsOfInterest(UndoHandler.getInstance().undo());
		    reloadTable();
		}

	    });
	}

	/**
	 * Save track button
	 */
	getContentPane().add(buttonPane, BorderLayout.SOUTH);
	{
	    JButton but = new JButton("Save Track");
	    but.setToolTipText("Save current state and update BED tracks in IGV");
	    but.setHorizontalAlignment(SwingConstants.LEFT);
	    buttonPane.add(but);
	    but.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    // save to BED and reload
		    GIE.getInstance().reloadActiveDataset();
		}

	    });
	}

	pack();
	setVisible(true);

	// register for ROI updates
	IGV.getInstance().getSession().getRegionsOfInterestObservable().addObserver(this);

    }

    @Override
    public void receiveEvent(Object event) {
    }

    @Override
    public void update(Observable o, Object arg) {
	reloadTable();
    }

    public void deleteRegions(List<RegionOfInterest> selectedRegions) {
	if (selectedRegions == null || selectedRegions.size() == 0)
	    return;
	IGV.getInstance().getSession().removeROI(selectedRegions);
	reloadTable();
    }

    /**
     * Merge a (sorted) list of rois
     * 
     * @param selectedRegions
     */
    public void mergeRegions(RegionOfInterest A, RegionOfInterest B) {
	List<RegionOfInterest> rois = new ArrayList<RegionOfInterest>();
	rois.add(A);
	rois.add(B);
	mergeRegions(rois);
    }

    /**
     * Merge a (sorted) list of rois
     * 
     * @param selectedRegions
     */
    public void mergeRegions(List<RegionOfInterest> selectedRegions) {
	if (selectedRegions == null || selectedRegions.size() == 0)
	    return;
	String chr = null, label = null;
	Integer start = null, end = null;
	String color = null, strand = null, score = null;
	Map<String, String> mergedAnnotations = new HashMap<>();
	for (RegionOfInterest r : selectedRegions) {
	    if (chr == null)
		chr = r.getChr();
	    else if (!chr.equals(r.getChr())) {
		JOptionPane.showMessageDialog(IGV.getMainFrame(), "Cannot merge regions from different chromosomes.",
			"Error", JOptionPane.INFORMATION_MESSAGE);
		return;
	    }
	    if (start == null)
		start = r.getStart();
	    else
		start = Math.min(start, r.getStart());
	    if (end == null)
		end = r.getEnd();
	    else
		end = Math.max(end, r.getEnd());
	    if (label == null)
		label = r.getDescription();
	    else if (r.getDescription() != null && !label.equals(r.getDescription()) && !label.endsWith("..."))
		label = label + "...";

	    if (score == null)
		score = r.getScore();
	    else if (r.getScore() != null && !score.equals(r.getScore()))
		score = "-";

	    if (color == null)
		color = r.getColor();
	    else if (r.getColor() != null && !color.equals(r.getColor()))
		color = "-";

	    if (strand == null)
		strand = r.getStrand();
	    else if (r.getStrand() != null && !strand.equals(r.getStrand()))
		strand = "0";

	    // merge custom annotations
	    for (String k : r.getAnnotations().keySet()) {
		String a = mergedAnnotations.get(k);
		String b = r.getAnnotation(k);
		String m = "";
		if (a != null && !a.trim().equals(""))
		    m += a.trim();
		if (b != null && !b.trim().equals("")) {
		    if (!m.equals(""))
			m += ", ";
		    m += b;
		}
		mergedAnnotations.put(k, m);
	    }
	}
	RegionOfInterest merged = new RegionOfInterest(chr, start, end, label);
	merged.setScore(score);
	merged.setColor(color);
	merged.setStrand(strand);
	merged.setAnnotations(mergedAnnotations);

	IGV.getInstance().getSession().addROI(merged, false, true);
	IGV.getInstance().getSession().removeROI(selectedRegions);
    }

    /**
     * Popup handler
     */
    private class MyTablePopupHandler extends MouseAdapter {

	public void mousePressed(MouseEvent e) {
	    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
		Point p = e.getPoint();
		// must convert row index from view to model, in case of
		// sorting, filtering
		int row = table.rowAtPoint(p);
		int col = table.columnAtPoint(p);
		if (col == COLIDX_COLOR) {
		    RegionOfInterest r = getSelectedRegion(row);
		    colorChooser.setColor(r.getAWTColor());
		    JDialog dialog = JColorChooser.createDialog(null, "Choose color (close dialog to clear selection)",
			    true, colorChooser, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    Color color = colorChooser.getColor();
				    r.setColor(color.getRed() + "," + color.getGreen() + "," + color.getBlue());
				    reloadTable();
				}
			    }, null);
		    dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
			    r.setColor(null);
			    reloadTable();
			}
		    });
		    dialog.setVisible(true);

		}
	    } else if (SwingUtilities.isRightMouseButton(e)) {
		Point p = e.getPoint();
		// must convert row index from view to model, in case of
		// sorting, filtering
		int[] rows = table.getSelectedRows();
		// int col = table.columnAtPoint(p);

		if (rows.length >= 0) {

		    RegionOfInterest selectedRegion = getSelectedMergedRegion(rows);

		    JPopupMenu popupMenu = new IGVPopupMenu();

		    if (selectedRegion != null) {
			JMenuItem viewItem = new JMenuItem("Show all [+/- 100bp]");
			viewItem.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				navigateTo(new RegionOfInterest(selectedRegion.getChr(),
					selectedRegion.getDisplayStart() - 100, selectedRegion.getDisplayEnd() + 100,
					null));
			    }
			});
			popupMenu.add(viewItem);

			JMenuItem viewItem2 = new JMenuItem("Show start");
			viewItem2.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				navigateTo(new RegionOfInterest(selectedRegion.getChr(),
					selectedRegion.getDisplayStart() - 100, selectedRegion.getDisplayStart() + 100,
					null));
			    }
			});
			popupMenu.add(viewItem2);

			JMenuItem viewItem3 = new JMenuItem("Show end");
			viewItem3.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				navigateTo(new RegionOfInterest(selectedRegion.getChr(),
					selectedRegion.getDisplayEnd() - 100, selectedRegion.getDisplayEnd() + 100,
					null));
			    }
			});
			popupMenu.add(viewItem3);

			popupMenu.addSeparator();
		    }

		    JMenuItem mergeItem = new JMenuItem("Merge Selected [CTRL-M]");
		    mergeItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    int[] selectedRows = table.getSelectedRows();
			    if (selectedRows.length > 0) {
				List<RegionOfInterest> selectedRegions = getSelectedRegions(selectedRows);
				mergeRegions(selectedRegions);
				reloadTable();
			    }
			}
		    });
		    popupMenu.add(mergeItem);

		    JMenuItem delItem = new JMenuItem("Delete Selected [CTRL-D]");
		    delItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    int[] selectedRows = table.getSelectedRows();
			    if (selectedRows.length > 0) {
				List<RegionOfInterest> selectedRegions = getSelectedRegions(selectedRows);
				IGV.getInstance().getSession().removeROI(selectedRegions);
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

		    popupMenu.addSeparator();

		    JMenuItem setScoreItem = new JMenuItem("Set Score of Selected (use '-' for null score)");
		    setScoreItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    int[] selectedRows = table.getSelectedRows();
			    if (selectedRows.length > 0) {
				List<RegionOfInterest> selectedRegions = getSelectedRegions(selectedRows);
				String prev = selectedRegions.size() == 1 ? selectedRegions.get(0).getScore() : "1000";
				String score = JOptionPane.showInputDialog(null, "Score to set: ", prev);
				if (score != null) {
				    for (RegionOfInterest r : selectedRegions)
					r.setScore(score);
				}
				reloadTable();
			    }
			}
		    });
		    popupMenu.add(setScoreItem);

		    JMenuItem setColorItem = new JMenuItem("Set Color of Selected (use '-' for default)");
		    setColorItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    int[] selectedRows = table.getSelectedRows();
			    if (selectedRows.length > 0) {
				List<RegionOfInterest> selectedRegions = getSelectedRegions(selectedRows);
				Color prev = selectedRegions.size() == 1
					? RegionOfInterest.getAWTColor(selectedRegions.get(0).getColor()) : null;
				colorChooser.setColor(prev);
				JDialog dialog = JColorChooser.createDialog(null,
					"Choose color (close dialog to clear selection)", true, colorChooser,
					new ActionListener() {
					    public void actionPerformed(ActionEvent e) {
						Color color = colorChooser.getColor();
						for (RegionOfInterest r : selectedRegions)
						    r.setColor(color.getRed() + "," + color.getGreen() + ","
							    + color.getBlue());
						reloadTable();
					    }
					}, null);
				dialog.addWindowListener(new WindowAdapter() {
				    public void windowClosing(WindowEvent evt) {
					for (RegionOfInterest r : selectedRegions)
					    r.setColor(null);
					reloadTable();
				    }
				});
				dialog.setVisible(true);
			    }
			}
		    });
		    popupMenu.add(setColorItem);

		    JMenuItem setNameItem = new JMenuItem("Set Name of Selected");
		    setNameItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    int[] selectedRows = table.getSelectedRows();
			    if (selectedRows.length > 0) {
				List<RegionOfInterest> selectedRegions = getSelectedRegions(selectedRows);
				String prev = selectedRegions.size() == 1 ? selectedRegions.get(0).getDescription()
					: "";
				String name = JOptionPane.showInputDialog(null, "Name: ", prev);
				if (name != null) {
				    for (RegionOfInterest r : selectedRegions)
					r.setDescription(name);
				}
				reloadTable();
			    }
			}
		    });
		    popupMenu.add(setNameItem);

		    popupMenu.addSeparator();

		    copyToLayerItem = new JMenuItem("Copy to Layer");
		    copyToLayerItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    int[] selectedRows = table.getSelectedRows();
			    if (selectedRows.length > 0) {
				List<RegionOfInterest> selectedRegions = getSelectedRegions(selectedRows);

				GIEDatasetVersionLayer activeLayer = GIE.getInstance().getActiveDataset()
					.getCurrentVersion().getActiveLayer();
				List<String> ll = new ArrayList<String>();
				for (String l : GIE.getInstance().getActiveDataset().getCurrentVersion().getLayers()
					.keySet())
				    if (!l.equals(activeLayer.getLayerName()))
					ll.add(l);
				if (ll.size() == 0)
				    return;
				String[] options = { "OK", "Cancel" };
				final JComboBox<String> combo = new JComboBox<>(ll.toArray(new String[ll.size()]));
				int selection = JOptionPane.showOptionDialog(null, combo, "Copy intervals to layer",
					JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options,
					options[0]);
				if (selection == 0) {
				    String tl = (String) combo.getSelectedItem();
				    GIEDatasetVersionLayer targetLayer = GIE.getInstance().getActiveDataset()
					    .getCurrentVersion().getLayers().get(tl);
				    targetLayer.addRegions(selectedRegions);
				    reloadTable();
				}
			    }
			}
		    });
		    popupMenu.add(copyToLayerItem);

		    moveToLayerItem = new JMenuItem("Move to Layer");
		    moveToLayerItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    int[] selectedRows = table.getSelectedRows();
			    if (selectedRows.length > 0) {
				List<RegionOfInterest> selectedRegions = getSelectedRegions(selectedRows);

				GIEDatasetVersionLayer activeLayer = GIE.getInstance().getActiveDataset()
					.getCurrentVersion().getActiveLayer();
				List<String> ll = new ArrayList<String>();
				for (String l : GIE.getInstance().getActiveDataset().getCurrentVersion().getLayers()
					.keySet())
				    if (!l.equals(activeLayer.getLayerName()))
					ll.add(l);
				if (ll.size() == 0)
				    return;
				String[] options = { "OK", "Cancel" };
				final JComboBox<String> combo = new JComboBox<>(ll.toArray(new String[ll.size()]));
				int selection = JOptionPane.showOptionDialog(null, combo, "Move intervals to layer",
					JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options,
					options[0]);
				if (selection == 0) {
				    String tl = (String) combo.getSelectedItem();
				    GIEDatasetVersionLayer targetLayer = GIE.getInstance().getActiveDataset()
					    .getCurrentVersion().getLayers().get(tl);
				    targetLayer.addRegions(selectedRegions);
				    activeLayer.removeRegions(selectedRegions);
				    reloadTable();
				}
			    }
			}
		    });
		    popupMenu.add(moveToLayerItem);

		    popupMenu.addSeparator();

		    for (String s : GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer()
			    .getAnnotations()) {
			JMenuItem setAnnoItem = new JMenuItem("Set " + s + " of Selected");
			setAnnoItem.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				int[] selectedRows = table.getSelectedRows();
				if (selectedRows.length > 0) {
				    List<RegionOfInterest> selectedRegions = getSelectedRegions(selectedRows);
				    String prev = selectedRegions.size() == 1 ? selectedRegions.get(0).getAnnotation(s)
					    : "";
				    String value = JOptionPane.showInputDialog(null, s + ": ", prev);
				    if (value != null) {
					for (RegionOfInterest r : selectedRegions)
					    r.addAnnotation(s, value);
				    }
				    reloadTable();
				}
			    }
			});
			popupMenu.add(setAnnoItem);
		    }

		    popupMenu.show(table, p.x, p.y);
		}
	    }
	}

    }

    // /**
    // * Launch the application.
    // */
    // public static void main(String[] args) {
    // try {
    // new GIEDataDialog(new Frame());
    // WindowListener exitListener = new WindowAdapter() {
    // @Override
    // public void windowClosing(WindowEvent e) {
    // System.exit(0);
    // }
    // };
    // GIE.getInstance().close();
    // System.out.println("Done.");
    // System.exit(0);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }

}
