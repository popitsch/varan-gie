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
import java.io.File;
import java.io.IOException;
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
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.broad.igv.ui.IGV;

import at.ccri.varan.GIE;

/**
 * VARAN-GIE path map dialog.
 * 
 * @author niko.popitsch
 *
 */
public class GIEPathMapDialog extends JDialog {

    // private static Logger log = Logger.getLogger(GIEPathMapDialog.class);

    private static final long serialVersionUID = 1L;

    List<File> files;
    Map<String, File> extPathMapping = new HashMap<>();
    File lastDir;
    boolean wasCanceled = true;

    public static Color COL_EXISTS = new Color(100, 191, 100);
    public static Color COL_NOT_EXISTS = new Color(191, 100, 100);

    /**
     * @see https://stackoverflow.com/questions/17627431/auto-resizing-the-jtable-column-widths
     * @param table
     */
    final int[] min_widths = new int[] { 100, 390, 90 };
    final Integer[] max_widths = new Integer[] { 500, 800, 100 };
    /**
     * JTable
     */
    private JTable table;

    public GIEPathMapDialog(Frame owner, List<File> files) {
	super(owner, "Path mapper", true);
	this.files = files;
	init();
    }

    public Map<String, File> getEditedPathMapping() {
	return extPathMapping;
    }

    public File map(File f) {
	try {
	    String cp = f.getCanonicalPath();
	    if (extPathMapping.containsKey(cp))
		return extPathMapping.get(cp);
	} catch (IOException e) {
	    e.printStackTrace();
	}
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
	try {
	    DefaultTableModel model = (DefaultTableModel) table.getModel();
	    for (int i = model.getRowCount() - 1; i >= 0; i--) {
		model.removeRow(i);
	    }
	    for (File f : files) {
		String name = f.getName();
		String origPath = null;
		origPath = f.getCanonicalPath();
		File mapFile = extPathMapping.get(f.getCanonicalPath());
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
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public void resizeColumnWidth(JTable table) {
	final TableColumnModel columnModel = table.getColumnModel();
	for (int column = 0; column < table.getColumnCount(); column++) {
	    int minwidth = min_widths[column]; // Min width
	    int maxwidth = (max_widths[column] != null) ? max_widths[column] : minwidth;
	    columnModel.getColumn(column).setPreferredWidth(minwidth);
	    columnModel.getColumn(column).setMinWidth(minwidth);
	    columnModel.getColumn(column).setMaxWidth(maxwidth);
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
	// return new Integer[] { Math.max(0, (int) getLocationOnScreen().getX()),
	// Math.max(0, (int) getLocationOnScreen().getY()), getWidth(), getHeight() };
    }

    /**
     * save coordinates
     */
    private void saveCoords() {
	Integer[] coords = getCoords();
	if (coords != null)
	    GIE.getInstance().getWindowCoordinates().put("GIEPathMapDialog", getCoords());

    }

    /**
     * Initialize the dialog.
     */
    private void init() {
	// ======== this ========
	setTitle("VARAN-GIE :: Map File Paths");
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
			    try {
				extPathMapping.put(new File(remotePath).getCanonicalPath(), new File(mapPath));
			    } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			    }
			reloadTable();
		    }
		}
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
	    /**
	     * 
	     */
	    private static final long serialVersionUID = 1L;

	    public String getToolTipText(MouseEvent e) {
		JTable table = (JTable) e.getSource();
		java.awt.Point p = e.getPoint();
		int row = rowAtPoint(p);
		int col = columnAtPoint(p);
		switch (col) {
		case COLIDX_NAME:
		    return (String) table.getModel().getValueAt(row, COLIDX_REMOTEPATH);
		default:
		    String k = (row >= 0 && col >= 0) ? (String) table.getModel().getValueAt(row, col) : "";
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
		int row = table.getSelectedRow();
		String remotepath = (String) table.getModel().getValueAt(row, COLIDX_REMOTEPATH);
		String ext = new File(remotepath).getName();

		JFileChooser fDialog = new JFileChooser();
		fDialog.setDialogTitle("Map remote path: " + remotepath);
		fDialog.setFileFilter(new FileFilter() {
		    @Override
		    public boolean accept(File f) {
			if (f.isDirectory())
			    return true;
			return (f.getName().equals(ext));
		    }

		    @Override
		    public String getDescription() {
			return ext;
		    }
		});

		fDialog.setCurrentDirectory(lastDir);
		int userSelection = fDialog.showOpenDialog(null);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
		    File fin = fDialog.getSelectedFile();
		    String remotePath = (String) table.getModel().getValueAt(row, COLIDX_REMOTEPATH);
		    try {
			extPathMapping.put(new File(remotePath).getCanonicalPath(), fin);
		    } catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		    }
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
		String examplepath = (String) table.getModel().getValueAt(0, COLIDX_REMOTEPATH);
		String from = JOptionPane.showInputDialog(null,
			"<html><body>Enter the source string that should be replaced (e.g., 'c:'). <br/>"
				+ "Note that this string will be treated case-insensitive (i.e., it will match e.g., c: and C:).<br/>"
				+ "<em>Example remote path: " + examplepath + "</em></body></html>",
			examplepath);
		if (from == null)
		    return;
		String to = JOptionPane.showInputDialog(null,
			"<html><body>Enter target string that for the replacement (e.g., 'D:')<br/>"
				+ "<em>Example remote path: " + examplepath + "</em></body></html>",
			from);
		if (to == null)
		    return;

		for (File f : files) {
		    File mapped = map(f);
		    String newPath = mapped.getAbsolutePath().replaceAll("(?i)" + from, to);
		    try {
			extPathMapping.put(f.getCanonicalPath(), new File(newPath));
		    } catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		    }
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
		    wasCanceled = false;
		    saveCoords();
		    dispose();
		}
	    }
	});

	JButton button2 = new JButton("Cancel");
	button2.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
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

    // /**
    // * Launch the application.
    // */
    // public static void main(String[] args) {
    // try {
    // List<File> files = new ArrayList<>();
    // files.add(new File("X:/a.gif"));
    // files.add(new File("X:/b.gif"));
    // GIEPathMapDialog d = new GIEPathMapDialog(new Frame(), files);
    // WindowListener exitListener = new WindowAdapter() {
    // @Override
    // public void windowClosing(WindowEvent e) {
    // System.out.println("BYE");
    // System.exit(0);
    // }
    // };
    // System.out.println("Done.");
    // System.exit(0);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }

}
