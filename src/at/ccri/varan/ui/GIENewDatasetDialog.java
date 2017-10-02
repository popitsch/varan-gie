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
 * 
 * 
 * Auto-completion related source code adapted from http://docs.oracle.com/javase/tutorial/uiswing/examples/components/TextAreaDemoProject/src/components/TextAreaDemo.java 
 * 
 */

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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.broad.igv.ui.IGV;

import at.ccri.varan.GIE;
import at.ccri.varan.util.AutoCompletionListener;
import at.ccri.varan.util.SpringUtilities;

/**
 * @author niko.popitsch
 * 
 *         GIE new dataset dialog
 * 
 */
public class GIENewDatasetDialog extends JDialog {

    // private static Logger log = Logger.getLogger(GIENewDatasetDialog.class);

    private static final long serialVersionUID = 1L;

    private JList<String> list;

    // category text field + autocompletion
    private JTextField textField0;
    private AutoCompletionListener autoComplete;

    public GIENewDatasetDialog(Frame owner) {
	super(owner, "New Dataset", true);
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
	    GIE.getInstance().getWindowCoordinates().put("GIENewDatasetDialog", getCoords());
    }

    /**
     * Initialize the dialog.
     */
    private void init() {
	// ======== this ========
	setTitle("VARAN-GIE :: Create New Dataset");
	setMinimumSize(new Dimension(650, 420));
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
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIENewDatasetDialog");
	if (coords == null) {
	    setPreferredSize(new Dimension(650, 420));
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

	// category
	JLabel l1 = new JLabel("Category", JLabel.TRAILING);
	textField0 = new JTextField(20);
	autoComplete = new AutoCompletionListener(textField0, GIE.getInstance().getCategories());
	l1.setLabelFor(textField0);
	formPanel.add(l1);
	formPanel.add(textField0);

	// name
	JLabel l2 = new JLabel("Name", JLabel.TRAILING);
	JTextField textField1 = new JTextField(20);
	l2.setLabelFor(textField1);
	formPanel.add(l2);
	formPanel.add(textField1);

	// descr
	JLabel l3 = new JLabel("Description", JLabel.TRAILING);
	JTextArea textArea = new JTextArea(3, 20);
	textArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	l3.setLabelFor(textArea);
	formPanel.add(l3);
	formPanel.add(textArea);

	// filename
	JLabel l4 = new JLabel("<html><body>Initial interval<br>set (optional)</body></html>", JLabel.TRAILING);
	JPanel p2 = new JPanel(new BorderLayout());
	JTextField textField2 = new JTextField(35);
	// textField2.setEditable(false);
	p2.add(textField2, BorderLayout.LINE_START);
	JButton fileBut = new JButton("Select BED/VCF ...");
	fileBut.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		JFileChooser fc = new JFileChooser();
		FileFilter filter = new FileNameExtensionFilter("BED+VCF Files", "bed", "vcf", "gz");
		fc.setFileFilter( filter);
		if (GIE.getInstance().getLastAccessedDirectories().get("GIENewDatasetDialog") != null)
		    fc.setCurrentDirectory(GIE.getInstance().getLastAccessedDirectories().get("GIENewDatasetDialog"));
		int result = fc.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    File f = fc.getSelectedFile();
		    GIE.getInstance().setLastAccessedDirectory("GIENewDatasetDialog", f.getParentFile());
		    textField2.setText(f.getAbsolutePath());
		    if (textField1.getText().equals("")) {
			String name = f.getName();
			if (name.lastIndexOf(".") >= 0)
			    name = name.substring(0, name.lastIndexOf("."));
			textField1.setText(name);
		    }
		}
	    }
	});
	p2.add(fileBut, BorderLayout.LINE_END);
	l4.setLabelFor(p2);
	formPanel.add(l4);
	formPanel.add(p2);

	// list
	JLabel l5 = new JLabel("<html><body>Additional<br/>annotation<br/>fields</body></html>", JLabel.TRAILING);
	DefaultListModel<String> listModel = new DefaultListModel<>();
	list = new JList<>(listModel);
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
	sp.setBorder(new TitledBorder("NOTE that annotations are layer-specific"));
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
		String key = JOptionPane.showInputDialog("Annotation name", "");
		if (key != null)
		    try {
			String field = URLEncoder.encode(key, "UTF-8");
			for (int i = 0; i < listModel.getSize(); i++) // skip existing fields
			    if (listModel.elementAt(i).equals(field))
				return;
			listModel.addElement(field);
		    } catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
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

	// TODO
	// add filters (e.g., export only selected chromosomes)
	SpringUtilities.makeCompactGrid(formPanel, 6, 2, // rows, cols
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

		// create dataset
		String category = textField0.getText();
		String name = textField1.getText();
		String description = textArea.getText();
		File file = textField2.getText().trim().equals("") ? null : new File(textField2.getText());
		List<String> annotations = new ArrayList<>();
		for (int i = 0; i < listModel.getSize(); i++)
		    annotations.add(listModel.elementAt(i));
		boolean ok = false;
		try {
		    ok = GIE.getInstance().addDataset(category, name, description,
			    annotations.toArray(new String[annotations.size()]), file);
		} catch (IOException e1) {
		    e1.printStackTrace();
		    ok = false;
		}
		if (!ok) {
		    JOptionPane.showMessageDialog(IGV.getMainFrame(),
			    "Could not create new dataset '" + name
				    + "' - check log file for details. Is dataset name unique? Is the initial interval set file a valid BED file?",
			    "Error", JOptionPane.ERROR_MESSAGE);
		    return;
		}

		GIE.getInstance().loadDataset(name, null); // will automatically select latest created version.
		// GIEDataDialog ddiag = GIEDataDialog.getInstance(IGV.getMainFrame()); // show data table
		GIEMainDialog.getInstance().refresh();
		saveCoords();
		dispose();
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

    // /**
    // * Launch the application.
    // */
    // public static void main(String[] args) {
    // try {
    // new GIENewDatasetDialog(new Frame());
    // WindowListener exitListener = new WindowAdapter() {
    // @Override
    // public void windowClosing(WindowEvent e) {
    // System.out.println("BYE");
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
