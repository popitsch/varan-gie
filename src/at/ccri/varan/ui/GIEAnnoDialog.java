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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.broad.igv.track.Track;
import org.broad.igv.ui.IGV;
import org.broad.igv.util.ResourceLocator;

import at.ccri.varan.GIE;
import at.ccri.varan.GIEAnnotationTrack;

/**
 * GIE annotation track dialog.
 * 
 * @author niko.popitsch
 * 
 */
public class GIEAnnoDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    // private static Logger log = Logger.getLogger(GIEAnnoDialog.class);

    /**
     * Singleton instance
     */
    public static GIEAnnoDialog instance;

    /**
     * @see https://stackoverflow.com/questions/17627431/auto-resizing-the-jtable-column-widths
     * @param table
     */
    final int[] min_widths = new int[] { 30, 100, 50, 80, 50 };

    /**
     * Jtable
     * 
     */
    private JTable table;

    /**
     * Return the active GIEMainDialog. null if none
     *
     * @return
     */
    public static GIEAnnoDialog getInstance(Frame owner) {
	if (instance == null) {
	    instance = new GIEAnnoDialog(owner);
	}
	return instance;
    }

    /**
     * Return the active GIEMainDialog. null if none
     *
     * @return
     */
    public static GIEAnnoDialog getInstance() {
	return instance;
    }

    /**
     * dispose the active instance and get rid of the pointer. Return whether or
     * not there was an active instance
     */
    public static boolean destroyInstance() {
	if (instance == null)
	    return false;
	instance.dispose();
	instance = null;
	return true;
    }

    private GIEAnnoDialog(Frame owner) {
	super(owner);
	init();
    }

    private GIEAnnoDialog(Dialog owner) {
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

    /**
     * populates the main table.
     * 
     * @param model
     */
    private void reloadTable() {
	DefaultTableModel model = (DefaultTableModel) table.getModel();
	for (int i = model.getRowCount() - 1; i >= 0; i--) {
	    model.removeRow(i);
	}

	// load from regions of interest
	for (GIEAnnotationTrack r : GIE.getInstance().getAnnotationTracks()) {
	    Object[] d = new Object[] { r.getName(), r.getDescription(), "", r.getDataFile().getAbsolutePath(),
		    r.getInfo() };
	    model.addRow(d);
	}

	model.fireTableDataChanged();
	resizeColumnWidth(table);
	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	IGV.getInstance().revalidateTrackPanels();
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

    public GIEAnnotationTrack getSelectedTrack(int row) {
	return GIE.getInstance().getAnnotationTracks().get(row);
    }

    public JTable getTable() {
	return table;
    }

    /**
     * table columns
     */
    final static int COLIDX_NAME = 0;
    final static int COLIDX_DESCR = 1;
    final static int COLIDX_LOAD = 2;
    final static int COLIDX_PATH = 3;
    final static int COLIDX_INFO = 4;

    /**
     * @return x, y, with, height of current window
     */
    public Integer[] getCoords() {
	if (!isShowing())
	    return null;
	return new Integer[] { Math.max(0, (int) getLocationOnScreen().getX()),  Math.max(0, (int) getLocationOnScreen().getY()), getWidth(),
		getHeight() };
    }

    /**
     * Initialize the dialog.
     */
    private void init() {
	// ======== this ========
	setTitle("VARAN-GIE :: Manage Annotation Tracks");
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
		    GIE.getInstance().getWindowCoordinates().put("GIEAnnoDialog", getCoords());
	    }
	});
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIEAnnoDialog");
	if (coords == null) {
	    setPreferredSize(new Dimension(700, 500));
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
	    JLabel lab = new JLabel("Preconfigured Annotation tracks");
	    panel.add(lab);
	}

	Object columnNames[] = { "Name", "Description", "", "Path", "Info" };
	final class MyTableModel extends DefaultTableModel {
	    private static final long serialVersionUID = 1L;

	    public MyTableModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	    }

	    public boolean isCellEditable(int row, int col) {
		// do not allow editing of info field
		return col != COLIDX_INFO & col != COLIDX_PATH;
	    }

	    public void setValueAt(Object value, int row, int col) {
		GIEAnnotationTrack r = getSelectedTrack(row);
		String v = (String) value;
		if (r == null)
		    return;
		switch (col) {
		case COLIDX_NAME:
		    r.setName(v);
		    break;
		case COLIDX_PATH:
		    r.setDataFile(new File(v));
		    break;
		case COLIDX_DESCR:
		    r.setDescription(v);
		    break;
		}
		super.setValueAt(value, row, col);
		fireTableCellUpdated(row, col);
		repaint();
		revalidate();
	    }
	}
	;

	/**
	 * SET UP TABLE
	 */
	MyTableModel model = new MyTableModel(columnNames, 0);
	this.table = new JTable(model);
	table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	table.setRowSelectionAllowed(true);
	table.setSelectionBackground(Color.red);
	contentPane.add(new JScrollPane(table), BorderLayout.CENTER);

	// load button action
	Action load = new AbstractAction() {
	    private static final long serialVersionUID = 1L;

	    public void actionPerformed(ActionEvent e) {
		GIEAnnotationTrack t = getSelectedTrack(table.getSelectedRow());
		ResourceLocator locator = new ResourceLocator(t.getDataFile().toString());
		List<ResourceLocator> l = new ArrayList<ResourceLocator>();
		l.add(locator);
		List<Track> tracks = IGV.getInstance().loadResources(l);
		for (Track track : tracks) {
		    if (!track.getName().equals(t.getName()))
			track.setName(t.getName() + " :: " + track.getName());
		}
		IGV.getInstance().doRefresh();
	    }

	};
	new ButtonColumn(table, load, "Load", "Load this annotation file", null, COLIDX_LOAD);

	/**
	 * buttons
	 */
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
	getContentPane().add(buttonPane, BorderLayout.SOUTH);
	{
	    JButton but = new JButton("Add Track...");
	    but.setHorizontalAlignment(SwingConstants.LEFT);
	    buttonPane.add(but);
	    but.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    new GIEAddAnnoDialog(IGV.getMainFrame());
		}
	    });
	}

	buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
	getContentPane().add(buttonPane, BorderLayout.SOUTH);
	{
	    JButton but = new JButton("Delete selected track");
	    but.setHorizontalAlignment(SwingConstants.LEFT);
	    buttonPane.add(but);
	    but.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    int reply = JOptionPane.showConfirmDialog(null,
			    "Do you really want to delete this annotation track?", "Confirmation Dialog",
			    JOptionPane.YES_NO_OPTION);
		    if (reply == JOptionPane.YES_OPTION) {
			GIE.getInstance().delAnnotationTrack(getSelectedTrack(table.getSelectedRow()));
			refresh();
		    }
		}

	    });
	}

	refresh();
	pack();
	setVisible(true);
    }

}
