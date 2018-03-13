package at.ccri.varan.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.ui.IGV;

import at.ccri.varan.GIE;
import at.ccri.varan.GIEDatasetVersionLayer;
import at.ccri.varan.util.SpringUtilities;
import at.ccri.varan.util.TabTableIterator;

/**
 * Dialog for importing intervals from a TSV file.
 * 
 * @author niko.popitsch
 *
 */
public class GIEImportFromTSVDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    final String SEP = " => ";
    final String NONE = "+++ none +++";

    File file;
    StringBuffer intervalString;

    public GIEImportFromTSVDialog(Frame owner, File f) throws IOException {
	super(owner, "GIEImportFromTSVDialog", true);
	this.file = f;
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
	    GIE.getInstance().getWindowCoordinates().put("GIEImportFromTSVDialog", getCoords());
    }

    private JComboBox<String> addCombo(JPanel formPanel, String label, String field, TabTableIterator ti,
	    boolean optional) {
	// map to chr
	JLabel l1 = new JLabel(label, JLabel.TRAILING);
	JComboBox<String> comboBox = new JComboBox<>();
	if (optional)
	    comboBox.addItem(NONE);
	for (String h : ti.getHeaders())
	    comboBox.addItem(h);
	l1.setLabelFor(comboBox);
	formPanel.add(l1);
	formPanel.add(comboBox);
	for (String h : ti.getHeaders())
	    if (h.toLowerCase().startsWith(field.toLowerCase())) {
		comboBox.setSelectedItem(h);
		break;
	    }
	return comboBox;
    }

    public StringBuffer getIntervalString() {
	return intervalString;
    }

    /**
     * Initialize the dialog.
     * 
     * @throws IOException
     */
    private void init() throws IOException {
	// ======== this ========
	setTitle("VARAN-GIE :: Import Intervals From TSV");
	setMinimumSize(new Dimension(500, 500));
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
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIEImportFromTSVDialog");
	if (coords == null) {
	    setPreferredSize(new Dimension(500, 500));
	    setLocationRelativeTo(IGV.getMainFrame());
	} else {
	    // check compatibility with actual screen size
	    coords[0] = Math.min(coords[0], GIE.SCREEN_WIDTH - coords[2]);
	    coords[1] = Math.min(coords[1], GIE.SCREEN_HEIGHT - coords[3]);
	    setLocation(coords[0], coords[1]);
	    setPreferredSize(new Dimension(coords[2], coords[3]));
	}
	// +++++++++++++++++++++++++++++++++++++++++++++++
	TabTableIterator ti = new TabTableIterator(file, "#");

	JPanel formPanel = new JPanel(new SpringLayout());

	JComboBox<String> comboChr = addCombo(formPanel, "Map to chromosome:", "chr", ti, false);
	JComboBox<String> comboStart = addCombo(formPanel, "Map to start coordinate:", "start", ti, false);
	JComboBox<String> comboEnd = addCombo(formPanel, "Map to end coordinate:", "end", ti, false);
	JComboBox<String> comboName = addCombo(formPanel, "Map to name:", "name", ti, true);
	JComboBox<String> comboScore = addCombo(formPanel, "Map to Score:", "score", ti, true);
	JComboBox<String> comboStrand = addCombo(formPanel, "Map to Strand:", "strand", ti, true);
	JComboBox<String> comboColor = addCombo(formPanel, "Map to color:", "color", ti, true);

	// list
	JLabel l5 = new JLabel("<html><body>Additional<br/>mappings</body></html>", JLabel.TRAILING);
	DefaultListModel<String> listModel = new DefaultListModel<>();
	JList<String> list = new JList<>(listModel);
	list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
	list.setVisibleRowCount(5);
	list.setBorder(BorderFactory.createLineBorder(Color.BLACK));

	list.setDragEnabled(true);
	list.setDropMode(DropMode.INSERT);
	class ListTransferHandler extends TransferHandler {
	    private static final long serialVersionUID = 1L;
	    private final DataFlavor localObjectFlavor;
	    private Object[] transferedObjects = null;

	    public ListTransferHandler() {
		localObjectFlavor = new ActivationDataFlavor(Object[].class, DataFlavor.javaJVMLocalObjectMimeType,
			"Array of items");
	    }

	    @SuppressWarnings("deprecation")
	    @Override
	    protected Transferable createTransferable(JComponent c) {
		JList<?> list = (JList<?>) c;
		indices = list.getSelectedIndices();
		transferedObjects = list.getSelectedValues();
		return new DataHandler(transferedObjects, localObjectFlavor.getMimeType());
	    }

	    @Override
	    public boolean canImport(TransferSupport info) {
		if (!info.isDrop() || !info.isDataFlavorSupported(localObjectFlavor)) {
		    return false;
		}
		return true;
	    }

	    @Override
	    public int getSourceActions(JComponent c) {
		return MOVE; // TransferHandler.COPY_OR_MOVE;
	    }

	    @SuppressWarnings("unchecked")
	    @Override
	    public boolean importData(TransferSupport info) {
		if (!canImport(info)) {
		    return false;
		}
		JList<?> target = (JList<?>) info.getComponent();
		JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
		DefaultListModel<Object> listModel = (DefaultListModel<Object>) target.getModel();
		int index = dl.getIndex();
		int max = listModel.getSize();
		if (index < 0 || index > max) {
		    index = max;
		}
		addIndex = index;
		try {
		    Object[] values = (Object[]) info.getTransferable().getTransferData(localObjectFlavor);
		    addCount = values.length;
		    for (int i = 0; i < values.length; i++) {
			int idx = index++;
			listModel.add(idx, values[i]);
			target.addSelectionInterval(idx, idx);
		    }
		    return true;
		} catch (UnsupportedFlavorException ufe) {
		    ufe.printStackTrace();
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		}
		return false;
	    }

	    @Override
	    protected void exportDone(JComponent c, Transferable data, int action) {
		cleanup(c, action == MOVE);
	    }

	    private void cleanup(JComponent c, boolean remove) {
		if (remove && indices != null) {
		    JList<?> source = (JList<?>) c;
		    DefaultListModel<?> model = (DefaultListModel<?>) source.getModel();
		    if (addCount > 0) {
			// http://java-swing-tips.googlecode.com/svn/trunk/DnDReorderList/src/java/example/MainPanel.java
			for (int i = 0; i < indices.length; i++) {
			    if (indices[i] >= addIndex) {
				indices[i] += addCount;
			    }
			}
		    }
		    for (int i = indices.length - 1; i >= 0; i--) {
			model.remove(indices[i]);
		    }
		}
		indices = null;
		addCount = 0;
		addIndex = -1;
	    }

	    private int[] indices = null;
	    private int addIndex = -1; // Location where items were added
	    private int addCount = 0; // Number of items added.
	}
	list.setTransferHandler(new ListTransferHandler());

	JScrollPane sp = new JScrollPane(list);
	sp.setBorder(new TitledBorder(""));
	l5.setLabelFor(sp);
	formPanel.add(l5);
	formPanel.add(sp);
	JPanel p3 = new JPanel(new BorderLayout());
	p3.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 6));
	p3.setLayout(new BoxLayout(p3, BoxLayout.LINE_AXIS));
	JButton abut = new JButton("Add");
	abut.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {

		String column = (String) JOptionPane.showInputDialog(null, "Select the TSV column you want to map.",
			"Column;", JOptionPane.QUESTION_MESSAGE, null, ti.getHeaders(), (String) null);
		if (column != null) {

		    String[] options = GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer()
			    .getAnnotations();
		    String key = (String) JOptionPane.showInputDialog(null, "Select annotation field",
			    "Annotation field", JOptionPane.QUESTION_MESSAGE, null, options, null);
		    if (key != null)
			try {
			    String field = URLEncoder.encode(key, "UTF-8");
			    listModel.addElement(column + SEP + field);
			} catch (UnsupportedEncodingException e1) {
			    e1.printStackTrace();
			}
		}
	    }
	});
	p3.add(abut);
	p3.add(Box.createHorizontalStrut(10));
	JButton dbut = new JButton("Delete");
	dbut.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		if (!list.isSelectionEmpty())
		    listModel.remove(list.getSelectedIndex());
	    }
	});
	p3.add(dbut);
	JLabel l6 = new JLabel("", JLabel.TRAILING);
	formPanel.add(l6);
	formPanel.add(p3);

	SpringUtilities.makeCompactGrid(formPanel, 9, 2, // rows, cols
		6, 6, // initX, initY
		6, 6); // xPad, yPad

	JPanel buttonPanel = new JPanel();
	buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 6));
	buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
	// buttons
	JButton button1 = new JButton("OK");
	button1.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		// calculate column/field mapping
		Map<String, String> col2fieldMap = new HashMap<>();
		col2fieldMap.put("Chr", (String) comboChr.getSelectedItem());
		col2fieldMap.put("Start", (String) comboStart.getSelectedItem());
		col2fieldMap.put("End", (String) comboEnd.getSelectedItem());
		col2fieldMap.put("Name", (String) comboName.getSelectedItem());
		col2fieldMap.put("Score", (String) comboScore.getSelectedItem());
		col2fieldMap.put("Strand", (String) comboStrand.getSelectedItem());
		col2fieldMap.put("Color", (String) comboColor.getSelectedItem());
		String[] customFields = new String[listModel.getSize()];
		for (int i = 0; i < listModel.getSize(); i++) {
		    String[] tmp = listModel.elementAt(i).split(SEP, -1);
		    col2fieldMap.put(tmp[1], tmp[0]);
		    customFields[i] = tmp[1];
		}
		// System.out.println("Mapping " + col2fieldMap);

		// try to load data
		List<RegionOfInterest> regions = new ArrayList<>();
		boolean ok = false;
		try {
		    while (ti.hasNext()) {
			String[] t = ti.next();
			String chr = ti.getField(t, col2fieldMap.get("Chr"));
			int start = Integer.parseInt(ti.getField(t, col2fieldMap.get("Start")));
			int end = Integer.parseInt(ti.getField(t, col2fieldMap.get("End")));
			String name = ti.getField(t, col2fieldMap.get("Name"));
			String score = ti.getField(t, col2fieldMap.get("Score"));
			String strand = ti.getField(t, col2fieldMap.get("Strand"));
			String color = ti.getField(t, col2fieldMap.get("Color"));
			RegionOfInterest roi = new RegionOfInterest(chr, start, end,
				name == null ? "" : (name.equals(NONE) ? "" : name));
			if (score != null && !score.equals(NONE))
			    roi.setScore(score);
			if (strand != null && !strand.equals(NONE))
			    roi.setStrand(strand);
			if (color != null && !color.equals(NONE))
			    roi.setColor(color);

			for (String c : customFields) {
			    String val = ti.getField(t, col2fieldMap.get(c));
			    if (val != null)
				val = URLDecoder.decode(ti.getField(t, col2fieldMap.get(c)), "UTF-8");
			    roi.addAnnotation(c, val);
			}
			regions.add(roi);
		    }
		    ok = true;
		} catch (Exception e1) {
		    e1.printStackTrace();
		    ok = false;
		}
		if (!ok) {
		    JOptionPane.showMessageDialog(IGV.getMainFrame(),
			    "Could not import data from '" + file
				    + "' - check log file for details. Is TSV file properly formatted?",
			    "Error", JOptionPane.ERROR_MESSAGE);
		    return;
		}

		// add loaded data.
		if (ok) {
		    // update intervalString
		    intervalString = new StringBuffer();
		    for (RegionOfInterest r : regions)
			intervalString.append(r.toFullString() + "\n");
		}
		saveCoords();
		dispose();

	    }
	});

	JButton button2 = new JButton("Cancel");
	button2.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		// System.out.println("Cancel");
		// saveCoords();
		dispose();
	    }
	});
	;
	buttonPanel.add(Box.createHorizontalGlue());
	buttonPanel.add(button1);
	buttonPanel.add(Box.createHorizontalStrut(10));
	buttonPanel.add(button2);

	add(formPanel, BorderLayout.NORTH);
	add(buttonPanel, BorderLayout.SOUTH);
	pack();
	setVisible(true);
    }

}
