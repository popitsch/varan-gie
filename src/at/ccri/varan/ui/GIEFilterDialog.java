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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.ParseException;
import java.util.Observable;
import java.util.Observer;

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

import org.broad.igv.event.IGVEventObserver;
import org.broad.igv.ui.IGV;

import at.ccri.varan.GIE;
import at.ccri.varan.ui.GIERowFilter.SCOPE;
import at.ccri.varan.util.SpringUtilities;

/**
 * @author niko.popitsch
 * 
 *         GIE Filter dialog
 * 
 */
public class GIEFilterDialog extends JDialog implements Observer, IGVEventObserver {

    // private static Logger log = Logger.getLogger(GIEFilterDialog.class);

    private static final long serialVersionUID = 1L;

    public GIEFilterDialog(Frame owner) {
	super(owner, "Data Filter", true);
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
	    GIE.getInstance().getWindowCoordinates().put("GIEFilterDialog", getCoords());
    }

    /**
     * Initialize the dialog.
     */
    private void init() {
	// ======== this ========
	setTitle("VARAN-GIE :: Data Filter");
	setMinimumSize(new Dimension(200, 250));
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
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIEFilterDialog");
	if (coords == null) {
	    setPreferredSize(new Dimension(200, 450));
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

	// type
	JLabel l = new JLabel("Scope:");
	JComboBox<String> scopeSelect = new JComboBox<>(new String[] { "Genome", "Chromosome", "Visible" });
	// System.out.println("SCOPE " + GIE.getInstance().getRowFilter().getScope());
	switch (GIE.getInstance().getRowFilter().getScope()) {
	case GENOME:
	    scopeSelect.setSelectedItem("Genome");
	    break;
	case CHROMOSOME:
	    scopeSelect.setSelectedItem("Chromosome");
	    break;
	case VISIBLE:
	    scopeSelect.setSelectedItem("Visible");
	    break;
	}

	l.setLabelFor(scopeSelect);
	formPanel.add(l);
	formPanel.add(scopeSelect);

	// list
	DefaultListModel<String> listModel = new DefaultListModel<>();
	for (GIEAttributeFilter f : GIE.getInstance().getRowFilter().getAttributeFilters())
	    listModel.addElement(f.toString());
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
	formPanel.add(new JLabel("<html><body>Attribute filters. Reorder with D'n'd.<br/>"
		+ "Example Filters: 'Score>100', 'Chr=12', 'Width>10kb'<br/>" + "Possible Attributes: "
		+ GIEDataDialog.getInstance().getColumnNames() + "</body></html>", JLabel.LEADING));
	formPanel.add(sp);

	// buttons
	JPanel p3 = new JPanel(new BorderLayout());
	p3.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 6));
	p3.setLayout(new BoxLayout(p3, BoxLayout.LINE_AXIS));
	JButton abut = new JButton("Add");
	abut.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		String key = JOptionPane.showInputDialog("Filter", "");
		if (key != null) {
		    try {
			GIEAttributeFilter fil = GIEAttributeFilter.parseFromString(key);
			if (fil == null)
			    throw new ParseException("Invalid operator in filter string: " + key, 1);
			if (!GIEDataDialog.getInstance().getColumnNames().contains(fil.getKey()))
			    throw new ParseException("Invalid attribute name in filter string: " + key, 1);
			listModel.addElement(key);
		    } catch (Exception ex) {
			JOptionPane.showMessageDialog(null,
				"<html><body>Error parsing filter:<br>" + ex + "</body></html>", "Error",
				JOptionPane.ERROR_MESSAGE);
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
	p3.add(Box.createHorizontalStrut(10));
	JButton ebut = new JButton("Edit");
	ebut.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		if (!list.isSelectionEmpty()) {
		    int idx = list.getSelectedIndex();
		    String prev = list.getSelectedValue();
		    String key = JOptionPane.showInputDialog("Filter", prev);
		    if (key != null) {
			listModel.setElementAt(key, idx);
		    }
		}
	    }
	});
	p3.add(ebut);
	formPanel.add(p3);

	// JCheckBox ucscNoHeader = new JCheckBox("Do not write header");
	// ucscNoHeader.setSelected(false);
	// formPanel.add(new JLabel(""));
	// formPanel.add(ucscNoHeader);

	// TODO
	// add filters (e.g., export only selected chromosomes)

	SpringUtilities.makeCompactGrid(formPanel, 5, 1, // rows, cols
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
		GIERowFilter filter = GIE.getInstance().getRowFilter();
		// update row filter
		switch (scopeSelect.getSelectedItem().toString()) {
		case "Genome":
		    filter.setScope(SCOPE.GENOME);
		    break;
		case "Chromosome":
		    filter.setScope(SCOPE.CHROMOSOME);
		    break;
		case "Visible":
		    filter.setScope(SCOPE.VISIBLE);
		    break;
		}

		filter.clearAttributeFilters();
		for (int i = 0; i < listModel.getSize(); i++) {
		    try {
			filter.addAttributeFilter((String) listModel.getElementAt(i));
		    } catch (IOException e1) {
			e1.printStackTrace();
		    }
		}
		GIEDataDialog.getInstance().filterUpdate();
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

	buttonPanel.add(Box.createHorizontalGlue());
	buttonPanel.add(button1);
	buttonPanel.add(Box.createHorizontalStrut(10));
	buttonPanel.add(button2);

	add(formPanel, BorderLayout.NORTH);
	add(buttonPanel, BorderLayout.SOUTH);

	pack();
	setVisible(true);
    }

    @Override
    public void receiveEvent(Object event) {
    }

    @Override
    public void update(Observable o, Object arg) {
    }

    // /**
    // * Launch the application.
    // */
    // public static void main(String[] args) {
    // try {
    // new GIEFilterDialog(new Frame());
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
