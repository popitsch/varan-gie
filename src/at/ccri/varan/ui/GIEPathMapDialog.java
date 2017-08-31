package at.ccri.varan.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.broad.igv.ui.IGV;

import at.ccri.varan.GIE;

public class GIEPathMapDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    List<File> files;
    Map<File, File> extPathMapping = new HashMap<>();
    File lastDir;
    boolean wasCanceled = false;

    public static Color COL_EXISTS = new Color(100, 191, 100);
    public static Color COL_NOT_EXISTS = new Color(191, 100, 100);

    /**
     * @see https://stackoverflow.com/questions/17627431/auto-resizing-the-jtable-column-widths
     * @param table
     */
    final int[] min_widths = new int[] { 50, 490, 60 };
    final Integer[] max_widths = new Integer[] { 50, 490, 60 };
    /**
     * JTable
     */
    private JTable table;

    public GIEPathMapDialog(Frame owner, List<File> files) {
	super(owner, "Path mapper", true);
	this.files = files;
	init();
    }

    public Map<File, File> getEditedPathMapping() {
	return extPathMapping;
    }

    public File map(File f) {
	if (extPathMapping.containsKey(f))
	    return extPathMapping.get(f);
	return f;
    }
    
    public boolean wasCanceled() {
	return wasCanceled;
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
	for (File f : files) {
	    String name = f.getName();
	    String origPath = f.getAbsolutePath();
	    File mapFile = extPathMapping.get(f);
	    String mapPath = mapFile == null ? null : mapFile.getAbsolutePath();

	    List<String> entry = new ArrayList<String>();
	    entry.add(name);
	    entry.add(mapPath == null ? "" : mapPath);
	    entry.add("");
	    entry.add(origPath);
	    model.addRow(entry.toArray());
	}
	model.fireTableDataChanged();
	table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	resizeColumnWidth(table);
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

    /**
     * @return x,y, width, height of current window
     */
    public Integer[] getCoords() {
	if (!isShowing())
	    return null;
	return new Integer[] { (int) getLocationOnScreen().getX(), (int) getLocationOnScreen().getY(), getWidth(),
		getHeight() };
    }

    /**
     * save coordinates
     */
    private void saveCoords() {
	Integer[] coords = getCoords();
	// if (coords != null)
	// GIE.getInstance().getWindowCoordinates().put("GIEPathMapDialog", getCoords());
    }

    /**
     * Initialize the dialog.
     */
    private void init() {
	// ======== this ========
	setTitle("Create new Dataset");
	setMinimumSize(new Dimension(650, 350));
	setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	// +++++++++++++++++++++++++++++++++++++++++++++++
	// set location on screen
	// +++++++++++++++++++++++++++++++++++++++++++++++
	addWindowListener(new WindowAdapter() {
	    @Override
	    public void windowClosing(WindowEvent e) {
		saveCoords();
	    }
	});
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIEPathMapDialog");
	if (coords == null) {
	    setPreferredSize(new Dimension(650, 350));
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

	/**
	 * table
	 */
	final int COLIDX_NAME = 0;
	final int COLIDX_LOCALPATH = 1;
	final int COLIDX_LOAD = 2;
	final int COLIDX_REMOTEPATH = 3;
	Object columnNames[] = { "Name", "Local Path", "", "remotepath" };
	final class MyTableModel extends DefaultTableModel {
	    /**
	     * 
	     */
	    private static final long serialVersionUID = 1L;

	    public MyTableModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	    }

	    public boolean isCellEditable(int row, int col) {
		return (col == COLIDX_LOCALPATH || col == COLIDX_LOAD);
	    }

	    public void setValueAt(Object newValue, int row, int col) {

		if (col == COLIDX_LOCALPATH) {
		    String remotePath = (String) getValueAt(row, COLIDX_REMOTEPATH);
		    String mapPath = (String) newValue;
		    if (mapPath != null) {
			if (mapPath.equals(""))
			    extPathMapping.remove(new File(remotePath));
			else
			    extPathMapping.put(new File(remotePath), new File(mapPath));
			reloadTable();
		    }
		}
	    }
	}
	;

	// render the table
	final class MyTableCellRenderer extends DefaultTableCellRenderer {

	    Color BG = UIManager.getColor("Panel.background");

	    /**
	     * 
	     */
	    private static final long serialVersionUID = 1L;

	    @Override
	    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
		    boolean hasFocus, int row, int column) {
		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		boolean exists = map(files.get(row)).exists() && map(files.get(row)).isFile();
		c.setForeground(Color.BLACK);
		if (exists)
		    c.setBackground(COL_EXISTS);
		else
		    c.setBackground(COL_NOT_EXISTS);
		return c;
	    }
	}
	MyTableModel model = new MyTableModel(columnNames, 0);
	table = new JTable(model) {
	    public String getToolTipText(MouseEvent e) {
		JTable table = (JTable) e.getSource();
		java.awt.Point p = e.getPoint();
		int row = rowAtPoint(p);
		int col = columnAtPoint(p);
		switch (col) {
		case COLIDX_NAME:
		    return (String) table.getModel().getValueAt(row, COLIDX_REMOTEPATH);
		default:
		    String k = ( row>=0 && col >=0) ? (String) table.getModel().getValueAt(row, col) : "";
		    return k;
		}
	    }
	};
	table.setDefaultRenderer(Object.class, new MyTableCellRenderer());
	table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	// load button action
	Action mapAction = new AbstractAction() {
	    private static final long serialVersionUID = 1L;

	    public void actionPerformed(ActionEvent e) {
		JFileChooser fDialog = new JFileChooser();
		fDialog.setDialogTitle("Map file to...");

		fDialog.setCurrentDirectory(lastDir);
		// *!!*!*!*!*!*!*!
		// int userSelection = fDialog.showSaveDialog(IGV.getMainFrame());
		int userSelection = fDialog.showSaveDialog(null);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
		    File fin = fDialog.getSelectedFile();
		    int row = table.getSelectedRow();
		    String remotePath = (String) table.getModel().getValueAt(row, COLIDX_REMOTEPATH);
		    extPathMapping.put(new File(remotePath), fin);
		    lastDir = fin.getParentFile();
		    reloadTable();
		}
	    }

	};
	new ButtonColumn(table, mapAction, "Select...", "Select a local file.", null, COLIDX_LOAD);

	// hide the remote path column.
	TableColumnModel tcm = table.getColumnModel();
	tcm.removeColumn(tcm.getColumn(COLIDX_REMOTEPATH));
	reloadTable();

	// create table
	JScrollPane tpane = new JScrollPane(table);
	tpane.setBorder(BorderFactory.createTitledBorder("Referenced Files"));
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
	contentPanel.add(tpane, BorderLayout.CENTER);

	JPanel buttonPanel = new JPanel();
	buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 6));
	buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

	JButton button0 = new JButton("Find/Replace");
	button0.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		// *!*!*!*!
		// String from = JOptionPane.showInputDialog(IGV.getMainFrame(),
		String from = JOptionPane.showInputDialog(null,
			"<html><body>Enter the source string that should be replaced (e.g., 'c:'). Note that this string will be treated case-insensitive (i.e., it will match e.g., c: and C:)</body></html>",
			"");
		if (from == null)
		    return;
		String to = JOptionPane.showInputDialog(null,
			"<html><body>Enter target string that for the replacement (e.g., 'D:')</body></html>", "");
		if (to == null)
		    return;

		for (File f : files) {
		    File mapped = map(f);
		    String newPath = mapped.getAbsolutePath().replaceAll("(?i)" + from, to);
		    extPathMapping.put(f, new File(newPath));
		}
		reloadTable();
	    }
	});

	JButton button1 = new JButton("OK");
	button1.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		// check files
		boolean ok = true;
		for (File f : files) {
		    File mapped = map(f);
		    if (!mapped.exists() || !mapped.isFile())
			ok = false;
		}

		int reply = JOptionPane.YES_OPTION;
		if (!ok)
		    reply = JOptionPane.showConfirmDialog(null,
			    "Not all local paths seem to exist. this will result in a broken IGV sesssion. Continue anyway?",
			    "Continue?", JOptionPane.YES_NO_OPTION);

		if (reply == JOptionPane.YES_OPTION) {
		    // GIEMainDialog.getInstance().refresh();
		    saveCoords();
		    dispose();
		}
	    }
	});

	JButton button2 = new JButton("Cancel");
	button2.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		wasCanceled = true;
		saveCoords();
		dispose();
	    }
	});

	buttonPanel.add(Box.createHorizontalStrut(6));
	buttonPanel.add(button0);
	buttonPanel.add(Box.createHorizontalGlue());
	buttonPanel.add(button1);
	buttonPanel.add(Box.createHorizontalStrut(10));
	buttonPanel.add(button2);

	add(formPanel, BorderLayout.NORTH);
	add(buttonPanel, BorderLayout.SOUTH);

	resizeColumnWidth(table);
	pack();
	setVisible(true);
    }

//    /**
//     * Launch the application.
//     */
//    public static void main(String[] args) {
//	try {
//	    List<File> files = new ArrayList<>();
//	    files.add(new File("X:/a.gif"));
//	    files.add(new File("X:/b.gif"));
//	    GIEPathMapDialog d = new GIEPathMapDialog(new Frame(), files);
//	    WindowListener exitListener = new WindowAdapter() {
//		@Override
//		public void windowClosing(WindowEvent e) {
//		    System.out.println("BYE");
//		    System.exit(0);
//		}
//	    };
//	    System.out.println("Done.");
//	    System.exit(0);
//	} catch (Exception e) {
//	    e.printStackTrace();
//	}
//    }

}
