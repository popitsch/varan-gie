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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.Logger;
import org.broad.igv.event.IGVEventObserver;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.ui.IGV;

import at.ccri.varan.GIE;
import at.ccri.varan.GIEDataset;
import at.ccri.varan.GIEDatasetVersion;

/**
 * GIE main dialog
 * 
 * @author niko.popitsch
 */
public class GIEMainDialog extends JDialog implements Observer, IGVEventObserver {

    
    private static Logger log = Logger.getLogger(GIEMainDialog.class);
    
    private static final long serialVersionUID = 1L;

    public static final String FILTER_SHOW_ALL = "Show All";

    public static Color COL_ODD_ROWS = Color.lightGray;
    public static Color COL_EVEN_ROWS = new Color(179, 191, 250);

    /**
     * Singleton instance
     */
    public static GIEMainDialog instance;

    /**
     * @see https://stackoverflow.com/questions/17627431/auto-resizing-the-jtable-column-widths
     * @param table
     */
    final int[] min_widths = new int[] { 30, 220, 120, 50, 70, 10, 10 };
    final Integer[] max_widths = new Integer[] { null, null, null, null, 70, 25, 25 };

    /**
     * JTable
     */
    private JTable table;

    /**
     * add dataset version button.
     */
    private JButton addVersionButton;

    /**
     * Dataset description
     */
    private JTextArea descr;

    /**
     * Layers combo box
     */
    private JComboBox<String> catCombo = new JComboBox<>();
    /* internal. Flag whether combobox listener actions should be executed */
    private boolean testActionListenerActive;

    /**
     * Return the active GIEMainDialog. null if none
     *
     * @return
     */
    public static GIEMainDialog getInstance(Frame owner) {
	if (instance == null) {
	    instance = new GIEMainDialog(owner);
	}
	return instance;
    }

    /**
     * Return the active GIEMainDialog. null if none
     *
     * @return
     */
    public static GIEMainDialog getInstance() {
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

    private GIEMainDialog(Frame owner) {
	super(owner);
	init();
    }

    private GIEMainDialog(Dialog owner) {
	super(owner);
	init();
    }

    /**
     * populates the main table.
     * 
     * @param model
     */
    private void reloadTable() {

	DefaultTableModel model = (DefaultTableModel) table.getModel();
	GIE gie = GIE.getInstance();

	String selected = (String) catCombo.getSelectedItem();
	for (int i = model.getRowCount() - 1; i >= 0; i--) {
	    model.removeRow(i);
	}
	int dx = 0;
	for (GIEDataset d : gie.getDatasets(selected)) {
	    int i = 0;
	    for (GIEDatasetVersion v : d.getVersions().values()) {
		List<String> entry = new ArrayList<String>();
		entry.add(i == 0 ? d.getCategory() : "");
		entry.add(i == 0 ? d.getName() : "");
		entry.add(v.getVersionName());
		entry.add(v.getActiveLayer().getLastModified());
		entry.add("Load");
		entry.add("Delete");
		entry.add("Download");
		entry.add(d.getName());
		entry.add(dx % 2 == 0 ? "0" : "1");
		model.addRow(entry.toArray());
		i++;
	    }
	    dx++;
	}
	model.fireTableDataChanged();
	table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	// table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
	resizeColumnWidth(table);
	// table.addMouseListener(new MyTablePopupHandler());

	if (addVersionButton != null) {

	    if (gie.getActiveDataset() != null)
		addVersionButton.setEnabled(true);
	    else
		addVersionButton.setEnabled(false);
	}
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
	    columnModel.getColumn(column).setPreferredWidth(width);
	    columnModel.getColumn(column).setMinWidth(width);
	    if (max_widths[column] != null)
		columnModel.getColumn(column).setMaxWidth(max_widths[column]);
	}
    }

    public String getCurrentDescription() {
	return descr.getText();
    }

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
	setTitle("VARAN-GIE :: Datasets");
	setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	setMinimumSize(new Dimension(640, 300));
	// +++++++++++++++++++++++++++++++++++++++++++++++
	// set location on screen
	// +++++++++++++++++++++++++++++++++++++++++++++++
	addWindowListener(new WindowAdapter() {
	    @Override
	    public void windowClosing(WindowEvent e) {
		Integer[] coords = getCoords();
		if (coords != null)
		    GIE.getInstance().getWindowCoordinates().put("GIEMainDialog", getCoords());
	    }
	});
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIEMainDialog");
	if (coords == null) {
	    setPreferredSize(new Dimension(640, 400));
	    setLocationRelativeTo(IGV.getMainFrame());
	} else {
	    // check compatibility with actual screen size
	    coords[0] = Math.min(coords[0], GIE.SCREEN_WIDTH - coords[2]);
	    coords[1] = Math.min(coords[1], GIE.SCREEN_HEIGHT - coords[3]);
	    setLocation(coords[0], coords[1]);
	    setPreferredSize(new Dimension(coords[2], coords[3]));
	}
	// +++++++++++++++++++++++++++++++++++++++++++++++

	getContentPane().setLayout(new BorderLayout());
	JPanel contentPanel = new JPanel();
	contentPanel.setLayout(new BorderLayout());
	contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
	addWindowListener(new WindowAdapter() {
	    @Override
	    public void windowClosed(WindowEvent e) {
		GIE.getInstance().save();
	    }
	});
	getContentPane().add(contentPanel);

	// cat selection panel
	JPanel catPanel = new JPanel();
	catPanel.setLayout(new BorderLayout());
	catPanel.add(new JLabel("Filter Category"), BorderLayout.NORTH);
	DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>();
	comboModel.addElement(FILTER_SHOW_ALL);
	for (String cat : GIE.getInstance().getCategories())
	    comboModel.addElement(cat);
	catCombo.setModel(comboModel);
	catCombo.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		if (testActionListenerActive) {
		    // System.out.println("Selected " + catCombo.getSelectedItem());
		    reloadTable();
		}
	    }

	});
	catPanel.add(catCombo, BorderLayout.CENTER);
	testActionListenerActive = true;

	// dataset description panel
	JPanel descPanel = new JPanel();
	descPanel.setLayout(new BorderLayout());
	this.descr = new JTextArea(3, 50);
	descr.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true),
		"Description"));
	descPanel.add(this.descr, BorderLayout.CENTER);
	descr.addFocusListener(new FocusListener() {

	    @Override
	    public void focusLost(FocusEvent e) {
		if (GIE.getInstance() != null && GIE.getInstance().getActiveDataset() != null
			&& GIE.getInstance().getActiveDataset().getCurrentVersion() != null)
		    GIE.getInstance().getActiveDataset().getCurrentVersion().setDescription(descr.getText());
	    }

	    @Override
	    public void focusGained(FocusEvent e) {
	    }
	});

	// TOP PANEL: descr + cat selection
	JPanel topPanel = new JPanel(new FlowLayout());
	topPanel.add(descPanel);
	topPanel.add(catPanel);
	contentPanel.add(topPanel, BorderLayout.PAGE_START);

	/**
	 * table
	 */
	final int COLIDX_Cat = 0;
	final int COLIDX_Name = 1;
	final int COLIDX_Ver = 2;
//	final int COLIDX_Date = 3;
	final int COLIDX_Load = 4;
	final int COLIDX_Del = 5;
	final int COLIDX_Download = 6;
	final int COLIDX_Name2 = 7;
	final int COLIDX_COLOR = 8;

	Object columnNames[] = { "Category", "Name", "Version", "Last Update", "", "", "", "", "" };
	final class MyTableModel extends DefaultTableModel {
	    /**
	     * 
	     */
	    private static final long serialVersionUID = 1L;

	    public MyTableModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	    }

	    public boolean isCellEditable(int row, int col) {

		boolean isDsRow = !(super.getValueAt(row, COLIDX_Name).toString().equals(""));

		return (isDsRow && (col == COLIDX_Name || col == COLIDX_Cat)) || col == COLIDX_Load || col == COLIDX_Del
			|| col == COLIDX_Download || col == COLIDX_Ver;
	    }

	    public void setValueAt(Object newname, int row, int col) {
		if (col == COLIDX_Name) {
		    String oldname = (String) getValueAt(row, col);
		    try {
			if (GIE.getInstance().renameDataset(oldname, (String) newname)) {
			    super.setValueAt(newname, row, COLIDX_Name);
			    for (int r = 0; r < super.getRowCount(); r++) {
				if (super.getValueAt(r, COLIDX_Name2).equals(oldname)) {
				    super.setValueAt(newname, r, COLIDX_Name2);
				}
			    }
			    fireTableDataChanged();
			    repaint();
			    revalidate();
			} else {
			    log.error("Cannot rename " + oldname + " to " + newname);
			}
		    } catch (IOException e) {
			log.error("Cannot rename " + oldname + " to " + newname);
			e.printStackTrace();
		    }
		} else if (col == COLIDX_Ver) {
		    String k = (String) table.getModel().getValueAt(row, COLIDX_Name2);
		    String v = (String) table.getModel().getValueAt(row, COLIDX_Ver);
		    GIEDataset ds = GIE.getInstance().getDatasets().get(k);
		    if (ds.renameVersion(v, (String) newname)) {
			super.setValueAt(newname, row, COLIDX_Ver);
			fireTableDataChanged();
			repaint();
			revalidate();
		    } else {
			log.error("Cannot rename " + v + " to " + newname);
		    }
		} else if (col == COLIDX_Cat) {
		    String k = (String) table.getModel().getValueAt(row, COLIDX_Name2);
		    GIEDataset ds = GIE.getInstance().getDatasets().get(k);
		    ds.setCategory((String) newname);
		    super.setValueAt(newname, row, COLIDX_Cat);
		    fireTableDataChanged();

		    // update combobox
		    testActionListenerActive = false;
		    Object selected = comboModel.getSelectedItem();
		    comboModel.removeAllElements();
		    comboModel.addElement(FILTER_SHOW_ALL);
		    for (String cat : GIE.getInstance().getCategories())
			comboModel.addElement(cat);
		    if (selected != null) {
			comboModel.setSelectedItem(selected);
		    }
		    testActionListenerActive = true;

		    repaint();
		    revalidate();

		}
		// rowData[row][col] = value;
		fireTableCellUpdated(row, col);
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
		String k = GIE.getInstance().getActiveDatasetName();
		String v = GIE.getInstance().getActiveDatasetVersion();
		boolean isOdd = table.getModel().getValueAt(row, COLIDX_COLOR).toString().equals("1");

		c.setForeground(Color.BLACK);
		if (isOdd)
		    c.setBackground(COL_ODD_ROWS);
		else
		    c.setBackground(COL_EVEN_ROWS);

		if (k != null && v != null) {
		    if (k != null && table.getModel().getValueAt(row, COLIDX_Name2).equals(k)
			    && table.getModel().getValueAt(row, COLIDX_Ver).equals(v)) {
			c.setBackground(Color.red);
		    }
		}
		return c;
	    }
	}
	MyTableModel model = new MyTableModel(columnNames, 0);
	table = new JTable(model) {

	    private static final long serialVersionUID = 1L;

	    public String getToolTipText(MouseEvent e) {
		JTable table = (JTable) e.getSource();
		java.awt.Point p = e.getPoint();
		int row = rowAtPoint(p);
		int col = columnAtPoint(p);

		switch (col) {
		case COLIDX_Load:
		    return "Load this dataset (will autosave the current one)";
		case COLIDX_Del:
		    return "Delete this dataset";
		case COLIDX_Download:
		    return "Export this dataset to a ZIP file";
		default:
		    String k = (String) table.getModel().getValueAt(row, COLIDX_Name2);
		    String v = (String) table.getModel().getValueAt(row, COLIDX_Ver);
		    GIEDataset ds = GIE.getInstance().getDatasets().get(k);
		    if (ds == null)
			return null;
		    else {
			GIEDatasetVersion ver = ds.getVersions().get(v);
			if (ver == null)
			    return null;
			else
			    return ver.getDescription();
		    }
		}
	    }
	};
	table.setDefaultRenderer(Object.class, new MyTableCellRenderer());
	table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	// hide the replicate name column and the color column
	TableColumnModel tcm = table.getColumnModel();
	tcm.removeColumn(tcm.getColumn(COLIDX_COLOR));
	tcm.removeColumn(tcm.getColumn(COLIDX_Name2));

	reloadTable();
	// load button action
	Action load = new AbstractAction() {
	    /**
	     * 
	     */
	    private static final long serialVersionUID = 1L;

	    public void actionPerformed(ActionEvent e) {
		JTable table = (JTable) e.getSource();
		int row = Integer.valueOf(e.getActionCommand());
		String k = (String) table.getModel().getValueAt(row, COLIDX_Name2);
		String v = (String) table.getModel().getValueAt(row, COLIDX_Ver);
		GIE.getInstance().loadDataset(k, v);
		reloadTable();
		table.repaint();
		UndoHandler.getInstance().clear(); // no undo beyond load.
		if (GIE.getInstance().getActiveDataset() != null)
		    descr.setText(GIE.getInstance().getActiveDataset().getCurrentVersion().getDescription());
	    }
	};
	new ButtonColumn(table, load, "Load", "Load this dataset (will autosave the current one)", null, COLIDX_Load);

	// delete button action
	Action delete = new AbstractAction() {
	    private static final long serialVersionUID = 1L;

	    public void actionPerformed(ActionEvent e) {
		JTable table = (JTable) e.getSource();
		int row = Integer.valueOf(e.getActionCommand());

		int reply = JOptionPane.showConfirmDialog(null,
			"This will physically delete this dataset version and cannot be undone. Are you sure?",
			"Delete Dataset", JOptionPane.YES_NO_OPTION);
		if (reply == JOptionPane.YES_OPTION) {
		    GIE.getInstance().save();

		    GIE.getInstance().deleteDatasetVersion((String) table.getModel().getValueAt(row, COLIDX_Name2),
			    (String) table.getModel().getValueAt(row, COLIDX_Ver));
		    reloadTable();
		    repaint();
		    revalidate();

		    descr.setText("");

		    GIEDataDialog.destroyInstance();

		    // ((DefaultTableModel) table.getModel()).removeRow(row);
		} else {
		    // do nothing.
		}
	    }
	};
	new ButtonColumn(table, delete, null, "Delete this dataset", UIManager.getIcon("InternalFrame.closeIcon"),
		COLIDX_Del);

	// download button action
	Action download = new AbstractAction() {
	    private static final long serialVersionUID = 1L;

	    public void actionPerformed(ActionEvent e) {
		JTable table = (JTable) e.getSource();
		int row = Integer.valueOf(e.getActionCommand());
		try {
		    JFileChooser fDialog = new JFileChooser();
		    fDialog.setDialogTitle("Export Dataset To...");
		    String dsName = (String) table.getModel().getValueAt(row, COLIDX_Name2);
		    fDialog.setFileFilter(new FileNameExtensionFilter("ZIP File", "zip"));
		    fDialog.setSelectedFile(new File(URLEncoder.encode(dsName + ".varan.zip", "UTF-8")));
		    int userSelection = fDialog.showSaveDialog(IGV.getMainFrame());
		    if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fout = fDialog.getSelectedFile();
			int reply = JOptionPane.YES_OPTION;
			if (fout.exists()) {
			    reply = JOptionPane.showConfirmDialog(null,
				    "Are you sure you want to overwrite the existing ZIP file " + fout + "?",
				    "Export Dataset", JOptionPane.YES_NO_OPTION);

			}
			if (reply == JOptionPane.YES_OPTION) {
			    log.info("Saving " + dsName + " to file: " + fout.getAbsolutePath());
			    GIE.getInstance().exportDataset(dsName, fout);
			}

		    }

		} catch (Exception e1) {
		    JOptionPane.showMessageDialog(null, "Could not export dataset. See log for details.");
		    e1.printStackTrace();
		}

	    }
	};
	new ButtonColumn(table, download, null, "Export this dataset to a ZIP file",
		UIManager.getIcon("FileView.floppyDriveIcon"), COLIDX_Download);

	// create table
	JScrollPane tpane = new JScrollPane(table);
	tpane.setBorder(BorderFactory.createTitledBorder("Datasets"));
	contentPanel.add(tpane, BorderLayout.CENTER);

	/**
	 * buttons
	 */
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
	contentPanel.add(buttonPane, BorderLayout.SOUTH);
	{

	    JButton addButton = new JButton("New Dataset");
	    addButton.setToolTipText("Create a new dataset");
	    addButton.setHorizontalAlignment(SwingConstants.LEFT);
	    buttonPane.add(addButton);
	    addButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if (GenomeManager.getInstance().getCurrentGenome() == null)
			JOptionPane.showMessageDialog(null, "Please select a genome first");
		    else
			new GIENewDatasetDialog(IGV.getMainFrame());
		}

	    });

	    JButton importButton = new JButton("Import Datasets");
	    importButton.setToolTipText("Import datasets from a VARAN ZIP file");
	    importButton.setHorizontalAlignment(SwingConstants.LEFT);
	    buttonPane.add(importButton);
	    importButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    JFileChooser fDialog = new JFileChooser();
		    fDialog.setDialogTitle("Import Datasets From...");
		    fDialog.setFileFilter(new FileNameExtensionFilter("ZIP File", "zip"));
		    int userSelection = fDialog.showSaveDialog(IGV.getMainFrame());
		    if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fin = fDialog.getSelectedFile();
			try {
			    if (GIE.getInstance().importDatasets(fin))
				JOptionPane.showMessageDialog(null, "Imported datasets from " + fin,
					"Import information", JOptionPane.INFORMATION_MESSAGE);
			    refresh();
			} catch (Exception ex) {
			    ex.printStackTrace();
			    JOptionPane
				    .showMessageDialog(
					    null, "<html><body>Could not import from " + fin + "<br/><b>"
						    + ex.getMessage() + "</b></body></html>",
					    "Import error", JOptionPane.ERROR_MESSAGE);
			}

		    }

		}

	    });

	    addVersionButton = new JButton("Add Dataset Version");
	    addVersionButton
		    .setToolTipText("Add a version to the currently selected dataset. Will copy existing layers.");
	    addVersionButton.setHorizontalAlignment(SwingConstants.RIGHT);
	    buttonPane.add(addVersionButton);
	    addVersionButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    // if no selected dataset - do nothing
		    if (GIE.getInstance().getActiveDataset() == null)
			return;

		    // get proposed new version tag
		    String proposedTag = GIE.getInstance().getProposedNextVersiontag();
		    String tag = JOptionPane.showInputDialog(IGV.getMainFrame(), "Version tag: ",
			    proposedTag == null ? "" : proposedTag);
		    if (tag == null)
			return; // cancel

		    GIEDataset ad = GIE.getInstance().getActiveDataset();
		    boolean success = false;
		    try {
			// save current session
			ad.save();

			// create new version
			GIEDatasetVersion ver = new GIEDatasetVersion(ad,
				ad.getCurrentVersion().getDescription() + " version: " + tag, GIE.defaultAuthor, tag,
				ad.getCurrentVersion().getActiveLayer().getAnnotations());

			// copy all layers from current version
			ver.copyLayersFrom(ad.getCurrentVersion());
			success = ad.addVersion(ver);
		    } catch (IOException e1) {
			e1.printStackTrace();
			success = false;
		    }

		    if (!success) {
			JOptionPane.showMessageDialog(IGV.getMainFrame(),
				"Cannot create new version with tag '" + tag + "'.", "Error",
				JOptionPane.INFORMATION_MESSAGE);
		    } else {

			// remove gie tracks
			GIE.getInstance().removeGIETracks();

			// save active dataset to create .bed file
			ad.selectVersion(tag);
			ad.save();

			// create new version
			GIE.getInstance().loadDataset(GIE.getInstance().getActiveDatasetName(), tag);

			GIEDataDialog ddiag = GIEDataDialog.getInstance(IGV.getMainFrame());
			reloadTable();
			ddiag.refresh();
			table.repaint();
			UndoHandler.getInstance().clear(); // no undo beyond load.
		    }
		}

	    });
	    addVersionButton.setEnabled(false);

	    JButton viewIntervalsButton = new JButton("View Intervals");
	    viewIntervalsButton.setToolTipText("Show the interval window if not visible.");
	    viewIntervalsButton.setHorizontalAlignment(SwingConstants.RIGHT);
	    buttonPane.add(viewIntervalsButton);
	    viewIntervalsButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    GIEDataDialog ddiag = GIEDataDialog.getInstance(IGV.getMainFrame());
		    if (ddiag.isVisible()) {
			ddiag.pack();
		    } else {
			ddiag.setVisible(true);
			ddiag.pack();
		    }
		}

	    });

	}

	resizeColumnWidth(table);
	pack();
	setVisible(true);

    }

    public void refresh() {
	if (GIE.getInstance().getActiveDataset() != null)
	    descr.setText(GIE.getInstance().getActiveDataset().getCurrentVersion().getDescription());
	try {
	    reloadTable();
	} catch (Exception e) {
	    log.error("Could not reload table");
	    e.printStackTrace();
	}
	if (isVisible()) {
	    pack();
	} else {
	    setVisible(true);
	    pack();
	}
    }

    @Override
    public void receiveEvent(Object event) {
	// TODO Auto-generated method stub

    }

    @Override
    public void update(Observable o, Object arg) {
	reloadTable();
    }

    // /**
    // * Popup handler
    // */
    // private class MyTablePopupHandler extends MouseAdapter {
    //
    // public void mousePressed(MouseEvent e) {
    // if (SwingUtilities.isRightMouseButton(e)) {
    // Point p = e.getPoint();
    // int row = table.rowAtPoint(p);
    // if (row >= 0) {
    // JPopupMenu popupMenu = new IGVPopupMenu();
    // JMenuItem item = new JMenuItem("Rename Dataset");
    // item.addActionListener(new ActionListener() {
    // public void actionPerformed(ActionEvent e) {
    // int row = table.rowAtPoint(p);
    // if (row > 0) {
    // System.out.println("row: " + row);
    // }
    // }
    // });
    // popupMenu.add(item);
    //
    // popupMenu.show(table, p.x, p.y);
    // }
    // }
    // }
    //
    // }

    // /**
    // * Launch the application.
    // */
    // public static void main(String[] args) {
    // try {
    // GIEMainDialog.getInstance(new Frame());
    // WindowListener exitListener = new WindowAdapter() {
    // @Override
    // public void windowClosing(WindowEvent e) {
    // System.exit(0);
    // }
    // };
    // GIEMainDialog.getInstance().addWindowListener(exitListener);
    //
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }

}
