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
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.event.GenomeChangeEvent;
import org.broad.igv.event.GenomeResetEvent;
import org.broad.igv.event.IGVEventBus;
import org.broad.igv.event.IGVEventObserver;
import org.broad.igv.event.ViewChange;
import org.broad.igv.feature.Chromosome;
import org.broad.igv.feature.Cytoband;
import org.broad.igv.feature.Range;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.lists.GeneList;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.panel.IGVPopupMenu;
import org.broad.igv.util.Interval;

import at.ccri.varan.GIE;
import at.ccri.varan.GIEDatasetVersion;
import at.ccri.varan.GIEDatasetVersionLayer;
import at.ccri.varan.ui.ROILink.TYPE;
import at.ccri.varan.util.CanonicalChromsomeComparator;

/**
 * @author niko.popitsch
 * 
 *         GIE data dialog
 * 
 */
public class GIEDataDialog extends JDialog implements Observer, IGVEventObserver {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(GIEDataDialog.class);

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
     * Maps column names to column indices.
     */
    Map<String, Integer> colNameMap = new HashMap<>();

    /**
     * Jtable
     * 
     */
    private JTable table;

    /**
     * Table sorter.
     */
    private TableRowSorter<TableModel> tableSorter;

    /**
     * Apply filter checkbox
     */
    private JCheckBox useFilter;

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
     * Rename layer menu
     */
    private JMenuItem renLayerM;
    /**
     * Delete layer button.
     */
    private JMenuItem delLayerM;

    /**
     * Color chooser (stores recent colors)
     */
    private JColorChooser colorChooser = new JColorChooser();

    /**
     * For chr sorting
     */
    private CanonicalChromsomeComparator chrComp = new CanonicalChromsomeComparator();

    /**
     * For creating ROI links.
     */
    private RegionOfInterest linkSourceROI = null;

    /**
     * cytobands
     */
    private RegionOfInterestTree cytobands;
    private boolean firstCytobandLoad = false;

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
	instance.saveCoords();
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

	java.awt.EventQueue.invokeLater(new Runnable() {
	    public void run() {

		// *******************
		setUpComboColumn(table, table.getColumnModel().getColumn(COLIDX_Chr),
			IGV.getInstance().getChromNamesArray(), null);

		DefaultTableModel model = (DefaultTableModel) table.getModel();
		for (int i = model.getRowCount() - 1; i >= 0; i--) {
		    model.removeRow(i);
		}
		if (GIE.getInstance().getActiveDataset() != null) {
		    GIEDatasetVersionLayer activeLayer = GIE.getInstance().getActiveDataset().getCurrentVersion()
			    .getActiveLayer();

		    // // remove filter
		    // javax.swing.RowFilter<? super TableModel, ? super Integer> filter = tableSorter.getRowFilter();
		    // tableSorter.setRowFilter(null);

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
				defGenome ? CanonicalChromsomeComparator.getCanonicalMappingHuman(r.getChr())
					: r.getChr(),
				r.getStart(), r.getEnd(), getIntervalWidth(r.getEnd() - r.getStart()), getChromBand(r),
				"View", "Delete", (r.getDescription() == null ? "-" : r.getDescription()),
				(r.getScore() == null ? 0d : r.getScore()),
				(r.getStrand() == null ? "0" : r.getStrand()),
				(r.getColor() == null ? "-" : r.getColor()),
				(activeLayer.getLinkedROIs().contains(r) ? "1" : "0") })
			    d.add(o);

			for (String s : activeLayer.getAnnotations()) {
			    String v = r.getAnnotation(s);
			    d.add(v == null ? "" : v);
			}
			model.addRow(d.toArray());
		    }
		    // tableSorter.setRowFilter(filter);

		    testActionListenerActive = false;
		    layerCombo.removeAllItems();
		    Iterator<String> lns = GIE.getInstance().getActiveDataset().getCurrentVersion().getLayers().keySet()
			    .iterator();
		    String al = activeLayer.getLayerName();
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
			renLayerM.setEnabled(false);
			delLayerM.setEnabled(false);
		    } else {
			renLayerM.setEnabled(true);
			delLayerM.setEnabled(true);
		    }
		    testActionListenerActive = true;
		}

		model.fireTableDataChanged();
		resizeColumnWidth(table);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		// IGV.getInstance().revalidateTrackPanels();
	    }
	});

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

    /**
     * Return the ChromBand or null if none found.
     * 
     * @param i
     * @return
     */
    private String getChromBand(RegionOfInterest reg) {
	if (cytobands == null && !firstCytobandLoad) {
	    System.out.println("Loading cytobands");
	    loadCytobands();
	    firstCytobandLoad = true;
	}
	if (cytobands == null)
	    return "-";
	List<Interval<RegionOfInterest>> hits = cytobands.queryOverlapping(reg);
	if (hits.size() == 0)
	    return "-";
	if (hits.size() == 1)
	    return hits.get(0).getValue().getDescription();
	else {
	    Collections.sort(hits);
	    return hits.get(0).getValue().getDescription() + "-"
		    + hits.get(hits.size() - 1).getValue().getDescription();
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
    Integer parseIntervalWidth(String s) throws ParseException {
	s = s.trim().toLowerCase();
	double mult = 1;
	if (s.endsWith("bp")) {
	    s = s.substring(0, s.length() - 2);
	} else if (s.endsWith("kb")) {
	    s = s.substring(0, s.length() - 2);
	    mult = 1000;
	} else if (s.endsWith("mb")) {
	    s = s.substring(0, s.length() - 2);
	    mult = 1000000;
	} else if (s.endsWith("gb")) {
	    s = s.substring(0, s.length() - 2);
	    mult = 1000000000;
	}
	Number n = NumberFormat.getInstance(Locale.US).parse(s);
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

    /**
     * Return the selected regions in the table view.
     *
     * @param selectedRows
     * @return
     */
    public List<RegionOfInterest> getSelectedRegions() {
	List<RegionOfInterest> selectedRegions = new ArrayList<RegionOfInterest>();
	// convert to real indices
	int[] selectedRows = getSelectedRows();
	if (selectedRows == null)
	    return selectedRegions;
	List<RegionOfInterest> regions = (List<RegionOfInterest>) IGV.getInstance().getSession()
		.getAllRegionsOfInterest();
	for (int selectedRowIndex : selectedRows) {
	    try {
		selectedRegions.add(regions.get(selectedRowIndex));
	    } catch (Exception e) {
		log.error("Error selecting region # " + table.convertRowIndexToModel(selectedRowIndex));
	    }
	}
	return selectedRegions;
    }

    /**
     * Return the region corresponding to the passed row index (MUST be normalized via table.convertRowIndexToModel()).
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
	int vidx = table.convertRowIndexToView(idx);
	table.setRowSelectionInterval(vidx, vidx);
	table.repaint();
    }

    /**
     * Select a roi in the table
     * 
     * @param roi
     */
    public void selectVisibleRegions() {
	List<RegionOfInterest> regions = (List<RegionOfInterest>) IGV.getInstance().getSession()
		.getAllRegionsOfInterest();
	// get visible regions
	table.clearSelection();
	Range visible = IGV.getInstance().getSession().getReferenceFrame().getCurrentRange();
	int idx = 0;
	for (RegionOfInterest r : regions) {
	    if (visible.overlaps(r.getRange())) {
		int vidx = table.convertRowIndexToView(idx);
		if (vidx < 0) { // don#t select as not currently shown (filtered)
		    idx++;
		    continue;
		}
		table.addRowSelectionInterval(vidx, vidx);
	    }
	    idx++;
	}
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

	    private static final long serialVersionUID = 1L;
	    List<String> tooltips = new ArrayList<>();

	    @Override
	    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
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
	comboBox.registerKeyboardAction(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		copyRows();
	    }
	}, "Copy", KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false), JComponent.WHEN_FOCUSED);
	col.setCellEditor(new DefaultCellEditor(comboBox));
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
		    case COLIDX_View:
		    case COLIDX_Del:
			break;
		    default:
			sb.append(table.getModel().getValueAt(rowIndex, tc.getModelIndex()) + "\t");
			break;
		    }
		}
		sb.append("\n");
	    }
	    // for (int i : selectedRows) {
	    // sb.append(getSelectedRegion(i).toFullString() + "\n");
	    // }

	    // List<RegionOfInterest> selectedRegions = getSelectedRegions(selectedRows);
	    // StringBuilder sb = new StringBuilder();
	    // for (RegionOfInterest r : selectedRegions)
	    // sb.append(r.toString() + "\n");
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
    final static int COLIDX_ChrBand = 4;
    final static int COLIDX_View = 5;
    final static int COLIDX_Del = 6;
    final static int COLIDX_Name = 7;
    final static int COLIDX_Score = 8;
    final static int COLIDX_Strand = 9;
    final static int COLIDX_COLOR = 10;
    final static int COLIDX_LINKED = 11;

    /**
     * @return x,y, width, height of current window
     */
    public Integer[] getCoords() {
	if (!isShowing())
	    return null;
	return new Integer[] { (int) getLocationOnScreen().getX(), (int) getLocationOnScreen().getY(), getWidth(),
		getHeight() };
	// return new Integer[] { Math.max(0, (int) getLocationOnScreen().getX()),
	// Math.max(0, (int) getLocationOnScreen().getY()), getWidth(), getHeight() };
    }

    public void saveCoords() {
	Integer[] coords = getCoords();
	if (coords != null) {
	    // check compatibility with actual screen size
	    coords[0] = Math.min(coords[0], GIE.SCREEN_WIDTH - coords[2]);
	    coords[1] = Math.min(coords[1], GIE.SCREEN_HEIGHT - coords[3]);
	    setLocation(coords[0], coords[1]);
	    setPreferredSize(new Dimension(coords[2], coords[3]));
	    GIE.getInstance().getWindowCoordinates().put("GIEDataDialog", coords);
	}
    }

    /**
     * Initialize the dialog.
     */
    private void init() {
	// ======== this ========
	setTitle("VARAN-GIE :: Genomic Intervals");
	setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	setMinimumSize(new Dimension(640, 300));
	Container contentPane = getContentPane();
	getContentPane().setLayout(new BorderLayout());
	// +++++++++++++++++++++++++++++++++++++++++++++++
	// set location on screen
	// +++++++++++++++++++++++++++++++++++++++++++++++
	addWindowListener(new WindowAdapter() {
	    @Override
	    public void windowClosing(WindowEvent e) {
		saveCoords();
	    }
	});
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIEDataDialog");
	if (coords == null) {
	    setPreferredSize(new Dimension(640, 300));
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
	 * Menu
	 * 
	 */
	Color MENU_COL = GIEMainDialog.COL_EVEN_ROWS;
	JColorMenuBar menuBar = new JColorMenuBar();
	menuBar.setColor(MENU_COL);
	setJMenuBar(menuBar);
	// ------------------------------- LAYERS ------------------------------------------------
	JMenu layerM = new JMenu("Layers");
	layerM.setOpaque(false);
	layerM.setBackground(MENU_COL);

	JMenuItem addLayerM = new JMenuItem("Add Layer");
	addLayerM.setToolTipText("Add a new interval layer");
	addLayerM.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		String lname = JOptionPane.showInputDialog(null, "Layer name: ", "");
		if (lname != null && !lname.trim().equals("")) {
		    try {
			GIE.getInstance().getActiveDataset().getCurrentVersion().addLayer(lname);

			// ensure that new table is added to IGV session
			GIEDatasetVersionLayer layer = GIE.getInstance().getActiveDataset().getCurrentVersion()
				.getActiveLayer();
			layer.updateAndSave();

			GIE.getInstance().reloadActiveDataset();

			refresh();

		    } catch (IOException e1) {
			e1.printStackTrace();
		    }
		}
	    }
	});
	layerM.add(addLayerM);

	delLayerM = new JMenuItem("Delete Layer");
	delLayerM.setToolTipText("Delete this layer. The 'main' layer cannot be deleted.");
	delLayerM.addActionListener(new ActionListener() {
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
			e1.printStackTrace();
		    }
		}
	    }

	});
	delLayerM.setEnabled(false);
	layerM.add(delLayerM);

	renLayerM = new JMenuItem("Rename Layer");
	renLayerM.setToolTipText(
		"Rename current layer. Layer names have to unique within a dataset, the 'main' layer cannot be renamed.");
	renLayerM.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		String oldlname = getSelectedLayerName();
		String lname = JOptionPane.showInputDialog(null, "Layer name: ", oldlname);
		if (lname != null && !lname.trim().equals("") && !lname.trim().equalsIgnoreCase("main")) {
		    try {
			if (!GIE.getInstance().getActiveDataset().getCurrentVersion().renameLayer(oldlname, lname)) {
			    log.error("Could not rename layer " + oldlname);
			}
		    } catch (IOException e1) {
			log.error("Could not rename layer " + oldlname);
			e1.printStackTrace();
		    }
		    refresh();
		}
	    }
	});
	renLayerM.setEnabled(false);
	layerM.add(renLayerM);
	menuBar.add(layerM);
	// ------------------------------- LAYERS ------------------------------------------------
	JMenu impExpM = new JMenu("Import/Export");
	impExpM.setOpaque(false);
	impExpM.setBackground(MENU_COL);

	JMenuItem addIntM = new JMenuItem("Add/Import Intervals");
	addIntM.setToolTipText("Add intervals by manual editing, copy/paste or import from a BED file.");
	addIntM.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		new GIEAddIntervalDialog(IGV.getMainFrame());
	    }
	});
	impExpM.add(addIntM);
	JMenuItem expIntM = new JMenuItem("Export Intervals");
	expIntM.setToolTipText("Export intervals from selected layers as BED or TSV file.");
	expIntM.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer().updateAndSave();
		new GIEExportDialog(IGV.getMainFrame());
	    }
	});
	impExpM.add(expIntM);
	menuBar.add(impExpM);

	// ------------------------------- ANNOTATIONS ------------------------------------------------
	JMenu annoM = new JMenu("Annotations");
	annoM.setOpaque(false);
	annoM.setBackground(MENU_COL);

	JMenuItem editAnnoM = new JMenuItem("Edit annotations");
	editAnnoM.setToolTipText("Add/remove custom annotation fields.");
	editAnnoM.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		// show edit annotations dialog.
		new GIEEditAnnotationsDialog(IGV.getMainFrame());
		// save to BED and reload
		GIE.getInstance().reloadActiveDataset();
	    }
	});
	annoM.add(editAnnoM);
	menuBar.add(annoM);

	// ------------------------------- STATS ------------------------------------------------
	JMenu statsM = new JMenu("Statistics");
	statsM.setOpaque(false);
	statsM.setBackground(MENU_COL);

	JMenuItem calcStatsM = new JMenuItem("Interval Statistics");
	calcStatsM.setToolTipText("Show basic statistics about the intervals in the selected layer");
	calcStatsM.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		new GIEStatsDialog(IGV.getMainFrame());
	    }
	});
	statsM.add(calcStatsM);
	menuBar.add(statsM);

	// ------------------------------- LINKS ------------------------------------------------
	JMenu linksM = new JMenu("Links");
	linksM.setOpaque(false);
	linksM.setBackground(MENU_COL);

	JMenuItem showLinksM = new JMenuItem("Show Links");
	showLinksM.setToolTipText("Show configured links in the selected layer");
	showLinksM.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		new GIELinksDialog(IGV.getMainFrame());
	    }
	});
	linksM.add(showLinksM);
	menuBar.add(linksM);

	// ------------------------------- KARY ------------------------------------------------
	JMenu karyM = new JMenu("Visualize");
	karyM.setOpaque(false);
	karyM.setBackground(MENU_COL);

	JMenuItem calcKaryM = new JMenuItem("Whole Genome View");
	calcKaryM.setToolTipText("Show whole genome view");
	calcKaryM.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		new GIEGenomeViewDialog(IGV.getMainFrame());
	    }
	});
	karyM.add(calcKaryM);
	menuBar.add(karyM);

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

	    JButton layDesc = new JButton("Layer Description");
	    if (GIE.getInstance().getActiveDataset() != null
		    && GIE.getInstance().getActiveDataset().getCurrentVersion() != null
		    && GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer() != null)
		layDesc.setToolTipText("<html><body>" + abbrv(
			GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer().getDescription(), 100)
			+ "</body></html>");
	    layDesc.setHorizontalAlignment(SwingConstants.LEFT);
	    panel.add(layDesc);
	    layDesc.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    new GIEEditDescriptionDialog(IGV.getMainFrame());
		    refresh();
		}
	    });

	    panel.add(Box.createHorizontalStrut(50));

	    useFilter = new JCheckBox("Filter Intervals");
	    useFilter.setToolTipText("<html><body>Toggle filter</body></html>");
	    useFilter.setHorizontalAlignment(SwingConstants.RIGHT);
	    panel.add(useFilter);
	    useFilter.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    GIE.getInstance().getRowFilter().updateCurrentlyVisible();
		    GIE.getInstance().setUseFilter(useFilter.isSelected());
		    if (useFilter.isSelected())
			tableSorter.setRowFilter(GIE.getInstance().getRowFilter());
		    else
			tableSorter.setRowFilter(null);
		}
	    });

	    JButton filterBut = new JButton("Filter");
	    filterBut.setToolTipText("<html><body>Configure filter</body></html>");
	    filterBut.setHorizontalAlignment(SwingConstants.RIGHT);
	    panel.add(filterBut);
	    filterBut.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    new GIEFilterDialog(IGV.getMainFrame());
		}
	    });

	}

	columnNames = new ArrayList<>();
	for (String s : new String[] { "Chr", "Start", "End", "Width", "ChrBand", "", "", "Name", "Score", "Strand",
		"Color", "Linked" })
	    columnNames.add(s);
	List<Integer> columnWidths = new ArrayList<>();
	for (int w : new int[] { 50, 70, 70, 50, 50, 50, 25, 100, 50, 30, 50, 10 })
	    columnWidths.add(w);
	if (GIE.getInstance().getActiveDataset() != null) {
	    for (String s : GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer()
		    .getAnnotations()) {
		columnNames.add(s);
		columnWidths.add(50);
	    }
	}
	min_widths = (Integer[]) columnWidths.toArray(new Integer[columnWidths.size()]);

	// columNames/colindex map
	for (int i = 0; i < columnNames.size(); i++) {
	    if (columnNames.get(i).equals(""))
		continue;
	    colNameMap.put(columnNames.get(i), i);
	}

	final class MyTableModel extends DefaultTableModel {
	    private static final long serialVersionUID = 1L;

	    public MyTableModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	    }

	    public boolean isCellEditable(int row, int col) {
		return col != COLIDX_COLOR && col != COLIDX_LINKED && col != COLIDX_ChrBand;
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
		case COLIDX_Start:
		case COLIDX_End:
		    return String.class;
		case COLIDX_LINKED:
		    return Integer.class;
		case COLIDX_Score:
		    return Double.class;
		default:
		    return String.class;
		}
	    }

	    public void setValueAt(Object value, int row, int col) {
		RegionOfInterest r = getSelectedRegion(row);
		String v = value.toString();
		if (r == null)
		    return;

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
			// close region
			RegionOfInterest mod = r.deepClone();
			mod.setEnd(r.getStart() + Math.abs(w));
			IGV.getInstance().updateROI(r, mod);
			super.setValueAt(mod.getEnd(), row, COLIDX_End);
			super.setValueAt(getChromBand(mod), row, COLIDX_ChrBand);
			super.setValueAt(getIntervalWidth(mod.getEnd() - mod.getStart()), row, COLIDX_Width);
		    } catch (ParseException ex) {
			JOptionPane.showMessageDialog(IGV.getMainFrame(), "Parsing Error " + ex.getMessage(), "Error",
				JOptionPane.ERROR_MESSAGE);
		    }
		    break;
		case COLIDX_Start:
		    RegionOfInterest mod = r.deepClone();
		    mod.setStart(CanonicalChromsomeComparator.parseCoordinate(r.getChr(), r.getStart(), v));
		    IGV.getInstance().updateROI(r, mod);
		    super.setValueAt(getIntervalWidth(mod.getEnd() - mod.getStart()), row, COLIDX_Width);
		    super.setValueAt(getChromBand(mod), row, COLIDX_ChrBand);
		    super.setValueAt(v, row, col);
		    break;
		case COLIDX_End:
		    mod = r.deepClone();
		    mod.setEnd(CanonicalChromsomeComparator.parseCoordinate(r.getChr(), r.getEnd(), v));
		    IGV.getInstance().updateROI(r, mod);
		    super.setValueAt(getIntervalWidth(mod.getEnd() - mod.getStart()), row, COLIDX_Width);
		    super.setValueAt(getChromBand(mod), row, COLIDX_ChrBand);
		    super.setValueAt(v, row, col);
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

		List<RegionOfInterest> now = (List<RegionOfInterest>) IGV.getInstance().getSession()
			.getAllRegionsOfInterest();
		UndoHandler.getInstance().addUndoStep(now);

		fireTableCellUpdated(row, col);
		repaint();
		revalidate();
		IGV.getInstance().revalidateTrackPanels();
		resizeColumnWidth(table);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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
		boolean isLinked = table.getModel().getValueAt(table.convertRowIndexToModel(row), COLIDX_LINKED)
			.toString().equals("1");
		if (isLinked)
		    c.setForeground(Color.BLUE);
		else
		    c.setForeground(Color.BLACK);
		return c;
	    }
	}

	/**
	 * SET UP TABLE
	 */
	MyTableModel model = new MyTableModel(columnNames.toArray(), 0);
	this.table = new JTable(model);
	this.tableSorter = new TableRowSorter<TableModel>(model);
	this.tableSorter.setRowFilter(null);
	this.table.setRowSorter(tableSorter);
	this.tableSorter.setComparator(COLIDX_Chr, new ChrComparator());
	this.tableSorter.setComparator(COLIDX_Start, new IntComparator());
	this.tableSorter.setComparator(COLIDX_End, new IntComparator());
	this.tableSorter.setComparator(COLIDX_Score, new DoubleComparator());
	this.tableSorter.setComparator(COLIDX_Width, new WidthComparator());
	this.tableSorter.setComparator(COLIDX_ChrBand, new ChrComparator());
	this.table.setDefaultRenderer(Object.class, new MyTableCellRenderer());

	table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	table.setRowSelectionAllowed(true);
	table.setSelectionBackground(Color.red);
	table.addMouseListener(new MyTablePopupHandler());
	table.registerKeyboardAction(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		copyRows();
	    }
	}, "Copy", KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false), JComponent.WHEN_FOCUSED);

	// activate checkbox?
	if (GIE.getInstance().isUseFilter()) {
	    GIE.getInstance().getRowFilter().updateCurrentlyVisible();
	    useFilter.setSelected(true);
	    tableSorter.setRowFilter(GIE.getInstance().getRowFilter());
	}

	// configure strand input
	setUpComboColumn(table, table.getColumnModel().getColumn(COLIDX_Strand), new String[] { "0", "+", "-" },
		new String[] { "No/unknown strand", "Plus strand", "Minus strand" });
	reloadTable();

	// load button action
	Action view = new AbstractAction() {
	    private static final long serialVersionUID = 1L;

	    public void actionPerformed(ActionEvent e) {
		navigateTo(getSelectedRegions());
	    }

	};
	new ButtonColumn(table, view, "View", "View this interval", null, COLIDX_View);

	// delete button action
	Action delete = new AbstractAction() {
	    private static final long serialVersionUID = 1L;

	    public void actionPerformed(ActionEvent e) {
		List<RegionOfInterest> selectedRegions = getSelectedRegions();
		IGV.getInstance().getSession().removeROI(selectedRegions);
		reloadTable();
	    }
	};
	new ButtonColumn(table, delete, null, "Delete this interval", UIManager.getIcon("InternalFrame.closeIcon"),
		COLIDX_Del);

	contentPane.add(new JScrollPane(table), BorderLayout.CENTER);

	/**
	 * Add button row
	 */
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));

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
	// register for view changes
	IGVEventBus.getInstance().subscribe(ViewChange.class, this);
	// register for genome changes
	IGVEventBus.getInstance().subscribe(GenomeChangeEvent.class, this);
	IGVEventBus.getInstance().subscribe(GenomeResetEvent.class, this);
    }

    private String abbrv(String txt, int max) {
	if (txt == null)
	    return "";
	txt = txt.replaceAll("\n", "<br/>");
	if (txt.length() < max)
	    return txt;
	return txt.substring(0, max) + "...";
    }

    private void loadCytobands() {
	cytobands = new RegionOfInterestTree();
	Genome g = GenomeManager.getInstance().getCurrentGenome();
	if (g != null) {
	    List<RegionOfInterest> rois = new ArrayList<>();
	    for (String c : g.getAllChromosomeNames()) {
		Chromosome chr = g.getChromosome(c);
		List<Cytoband> bands = chr.getCytobands();
		if (bands != null) {
		    for (Cytoband cyto : bands) {
			RegionOfInterest r = new RegionOfInterest(cyto.getChr(), cyto.getStart(), cyto.getEnd(),
				cyto.getLongName());
			rois.add(r);
		    }
		}
	    }
	    cytobands = new RegionOfInterestTree(rois);
	}
    }

    @Override
    public void receiveEvent(Object event) {
	if (event instanceof ViewChange) {
	    GIERowFilter filter = GIE.getInstance().getRowFilter();
	    filter.setViewChanged();
	    filterUpdate();
	} else if (event instanceof GenomeChangeEvent || event instanceof GenomeResetEvent) {
	    loadCytobands();
	}
    }

    public void filterUpdate() {
	// set row filter
	GIERowFilter filter = GIE.getInstance().getRowFilter();
	if (useFilter.isSelected()) {
	    tableSorter.setRowFilter(filter);
	}
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
	String color = null, strand = null;
	Double score = null;
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
		score = (score + r.getScore()) / 2d; // mean score

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
		int row = table.convertRowIndexToModel(table.rowAtPoint(p));
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
		int[] rows = getSelectedRows();

		// int col = table.columnAtPoint(p);

		if (rows.length >= 0) {

		    RegionOfInterest selectedRegion = getSelectedMergedRegion(rows);
		    GIEDatasetVersionLayer activeLayer = GIE.getInstance().getActiveDataset().getCurrentVersion()
			    .getActiveLayer();

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

			// linked?
			for (ROILink rl : activeLayer.getLinks()) {
			    if (rl.getSource().equals(selectedRegion)) {
				JMenuItem viewItem4 = new JMenuItem(
					"Show linked target [" + rl.getTarget().toString() + "]");
				viewItem4.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {

					navigateTo(new RegionOfInterest(rl.getTarget().getChr(),
						rl.getTarget().getDisplayStart() - 1000,
						rl.getTarget().getDisplayEnd() + 1000, null));
				    }
				});
				popupMenu.add(viewItem4);

			    } else if (rl.getTarget().equals(selectedRegion)) {
				JMenuItem viewItem4 = new JMenuItem(
					"Show linked source [" + rl.getSource().toString() + "]");
				viewItem4.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent e) {
					navigateTo(new RegionOfInterest(rl.getSource().getChr(),
						rl.getSource().getDisplayStart() - 1000,
						rl.getSource().getDisplayEnd() + 1000, null));
				    }
				});
				popupMenu.add(viewItem4);
			    }
			}

			popupMenu.addSeparator();
		    }

		    JMenuItem mergeItem = new JMenuItem("Merge Selected [CTRL-M]");
		    mergeItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    int[] selectedRows = table.getSelectedRows();
			    if (selectedRows.length > 0) {
				List<RegionOfInterest> selectedRegions = getSelectedRegions();
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
				List<RegionOfInterest> selectedRegions = getSelectedRegions();
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

		    JMenuItem addUpBPItem = new JMenuItem("Add/subtract X bp upstream");
		    addUpBPItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    int[] selectedRows = table.getSelectedRows();
			    if (selectedRows.length > 0) {
				List<RegionOfInterest> selectedRegions = getSelectedRegions();
				String tmp = JOptionPane.showInputDialog(null,
					"<html><body>" + "BP to add to min coordinate</br>"
						+ "(use negative integer to extend interval upstream):</body></html>",
					"0");
				if (tmp != null) {
				    int inc = Integer.parseInt(tmp);
				    for (RegionOfInterest r : selectedRegions) {
					r.setStart(Math.max(0, r.getStart() + inc));
				    }
				    Iterator<RegionOfInterest> delit = IGV.getInstance().getSession()
					    .getAllRegionsOfInterest().iterator();
				    while (delit.hasNext()) {
					RegionOfInterest r = delit.next();
					if (r.getRange().getLength() <= 0)
					    delit.remove();
				    }
				    List<RegionOfInterest> now = (List<RegionOfInterest>) IGV.getInstance().getSession()
					    .getAllRegionsOfInterest();
				    UndoHandler.getInstance().addUndoStep(now);
				    reloadTable();
				    IGV.getInstance().getSession().informListeners();
				    IGV.getInstance().repaint();
				}
			    }
			}
		    });
		    popupMenu.add(addUpBPItem);

		    JMenuItem addDownBPItem = new JMenuItem("Add/subtract X bp downstream");
		    addDownBPItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    int[] selectedRows = table.getSelectedRows();
			    if (selectedRows.length > 0) {
				List<RegionOfInterest> selectedRegions = getSelectedRegions();
				String tmp = JOptionPane.showInputDialog(null,
					"<html><body>" + "BP to add to max coordinate</br>"
						+ "(use positive integer to extend interval downstream):</body></html>",
					"0");
				if (tmp != null) {
				    int inc = Integer.parseInt(tmp);
				    for (RegionOfInterest r : selectedRegions)
					r.setEnd(Math.max(0, r.getEnd() + inc));
				    Iterator<RegionOfInterest> delit = IGV.getInstance().getSession()
					    .getAllRegionsOfInterest().iterator();
				    while (delit.hasNext()) {
					RegionOfInterest r = delit.next();
					if (r.getRange().getLength() <= 0)
					    delit.remove();
				    }
				    List<RegionOfInterest> now = (List<RegionOfInterest>) IGV.getInstance().getSession()
					    .getAllRegionsOfInterest();
				    UndoHandler.getInstance().addUndoStep(now);
				    reloadTable();
				    IGV.getInstance().getSession().informListeners();
				    IGV.getInstance().repaint();
				}
			    }
			}
		    });
		    popupMenu.add(addDownBPItem);

		    JMenuItem setScoreItem = new JMenuItem("Set Score of Selected (use '-' for null score)");
		    setScoreItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    int[] selectedRows = table.getSelectedRows();
			    if (selectedRows.length > 0) {
				List<RegionOfInterest> selectedRegions = getSelectedRegions();
				Double prev = selectedRegions.size() == 1 ? selectedRegions.get(0).getScore() : 1000d;
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
				List<RegionOfInterest> selectedRegions = getSelectedRegions();
				Color prev = selectedRegions.size() == 1
					? RegionOfInterest.getAWTColor(selectedRegions.get(0).getColor())
					: null;
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
				List<RegionOfInterest> selectedRegions = getSelectedRegions();
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
				List<RegionOfInterest> selectedRegions = getSelectedRegions();

				List<String> ll = new ArrayList<String>();
				for (String l : GIE.getInstance().getActiveDataset().getCurrentVersion().getLayers()
					.keySet())
				    if (!l.equals(activeLayer.getLayerName()))
					ll.add(l);
				if (ll.size() == 0)
				    return;
				String[] options = { "OK", "Cancel" };
				final JComboBox<String> combo = new JComboBox<>(ll.toArray(new String[ll.size()]));
				int selection = JOptionPane.showOptionDialog(null, combo,
					selectedRegions.size() == 1 ? "Copy interval to layer"
						: "Copy " + selectedRegions.size() + " intervals to layer",
					JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options,
					options[0]);
				if (selection == 0) {
				    String tl = (String) combo.getSelectedItem();
				    GIEDatasetVersionLayer targetLayer = GIE.getInstance().getActiveDataset()
					    .getCurrentVersion().getLayers().get(tl);
				    targetLayer.addRegions(selectedRegions);
				    targetLayer.save();
				    GIE.getInstance().reloadActiveDataset();
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
				List<RegionOfInterest> selectedRegions = getSelectedRegions();

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
				    targetLayer.save();
				    GIE.getInstance().reloadActiveDataset();
				}
			    }
			}
		    });
		    popupMenu.add(moveToLayerItem);

		    popupMenu.addSeparator();

		    for (String s : activeLayer.getAnnotations()) {
			JMenuItem setAnnoItem = new JMenuItem("Set " + s + " of Selected");
			setAnnoItem.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				int[] selectedRows = table.getSelectedRows();
				if (selectedRows.length > 0) {
				    List<RegionOfInterest> selectedRegions = getSelectedRegions();
				    String prev = selectedRegions.size() == 1 ? selectedRegions.get(0).getAnnotation(s)
					    : "";
				    String value = JOptionPane.showInputDialog(null, s + ": ", prev);
				    if (value != null) {
					if (value.startsWith("ATTR ")) {
					    String attrname = value.substring("ATTR ".length());
					    if (!activeLayer.hasAnnotation(attrname)) {
						log.error("Cannot find custom attribute " + attrname);
					    } else {
						for (RegionOfInterest r : selectedRegions) {
						    r.addAnnotation(s, r.getAnnotation(attrname));
						}
					    }
					} else if (value.startsWith("PRE ")) {
					    String prefix = value.substring("PRE ".length());
					    for (RegionOfInterest r : selectedRegions) {
						r.addAnnotation(s, prefix + r.getAnnotation(s));
					    }

					} else if (value.startsWith("POST ")) {
					    String postfix = value.substring("POST ".length());
					    for (RegionOfInterest r : selectedRegions) {
						r.addAnnotation(s, r.getAnnotation(s) + postfix);
					    }

					} else {
					    for (RegionOfInterest r : selectedRegions) {
						r.addAnnotation(s, value);
					    }
					}
				    }
				    reloadTable();
				}
			    }
			});
			popupMenu.add(setAnnoItem);
		    }

		    int[] selectedRows = table.getSelectedRows();

		    if (selectedRows.length == 1) {
			int idx = table.convertRowIndexToModel(table.getSelectedRow());
			RegionOfInterest roi = getSelectedRegion(idx);
			JMenuItem createLinkItem = null;
			if (linkSourceROI == null) {
			    createLinkItem = new JMenuItem("Set Link Source...");
			    createLinkItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    linkSourceROI = roi;
				}
			    });
			} else {
			    createLinkItem = new JMenuItem("Link with " + linkSourceROI + "...");
			    createLinkItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    ROILink rl = new ROILink(linkSourceROI, roi, TYPE.FUSION);
				    activeLayer.addLink(rl);
				    refresh();
				    linkSourceROI = null;
				}
			    });

			}
			popupMenu.addSeparator();
			popupMenu.add(createLinkItem);

			if (GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer().getLinkedROIs()
				.contains(roi)) {
			    // TODO: allow deletion of single links.
			    JMenuItem deleteLinkItem = new JMenuItem("Delete links...");
			    deleteLinkItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    activeLayer.deleteLinks(roi);
				    refresh();
				    linkSourceROI = null;
				}
			    });
			    popupMenu.add(deleteLinkItem);
			}
		    }

		    popupMenu.show(table, p.x, p.y);
		}
	    }
	}

    }

    public Integer colName2Index(String name) {
	if (!colNameMap.containsKey(name))
	    System.err.println("Unknown column " + name);
	return colNameMap.get(name);
    }

    public String colIndex2Name(Integer idx) {
	return columnNames.get(idx);
    }

    public Set<String> getColumnNames() {
	return colNameMap.keySet();
    }

    /**
     * For col sorting
     */
    private class ChrComparator implements Comparator<String> {

	public int compare(String o1, String o2) {
	    return chrComp.compare(o1, o2);
	}

    }

    /**
     * For col sorting
     */
    private class IntComparator implements Comparator<Integer> {

	public int compare(Integer o1, Integer o2) {
	    return o1.compareTo(o2);
	}

    }

    /**
     * For col sorting
     */
    private class DoubleComparator implements Comparator<Double> {

	public int compare(Double o1, Double o2) {
	    return o1.compareTo(o2);
	}

    }

    /**
     * For col sorting
     */
    private class WidthComparator implements Comparator<String> {
	public int compare(String s1, String s2) {
	    Integer w1, w2;
	    try {
		w1 = new Integer(parseIntervalWidth(s1));
		w2 = new Integer(parseIntervalWidth(s2));
	    } catch (ParseException e) {
		e.printStackTrace();
		return 0;
	    }
	    return w1.compareTo(w2);
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
