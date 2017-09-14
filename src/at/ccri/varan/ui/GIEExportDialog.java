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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Observable;
import java.util.Observer;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.broad.igv.event.IGVEventObserver;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.ui.IGV;

import at.ccri.varan.GIE;
import at.ccri.varan.GIEDatasetVersionLayer;
import at.ccri.varan.util.SpringUtilities;

/**
 * @author niko.popitsch
 * 
 *         GIE Export dialog
 * 
 */
public class GIEExportDialog extends JDialog implements Observer, IGVEventObserver {

    // private static Logger log = Logger.getLogger(GIEExportDialog.class);

    private static final long serialVersionUID = 1L;

    public GIEExportDialog(Frame owner) {
	super(owner, "Data Export", true);
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
	    GIE.getInstance().getWindowCoordinates().put("GIEExportDialog", getCoords());
    }

    /**
     * Initialize the dialog.
     */
    private void init() {
	// ======== this ========
	setTitle("VARAN-GIE :: Export Data");
	setMinimumSize(new Dimension(550, 450));
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
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIEExportDialog");
	if (coords == null) {
	    setPreferredSize(new Dimension(550, 450));
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
	JLabel l = new JLabel("Type", JLabel.TRAILING);
	JComboBox<String> typeList = new JComboBox<>(new String[] { "BED", "TSV" });
	l.setLabelFor(typeList);
	formPanel.add(l);
	formPanel.add(typeList);

	// name
	JLabel l2 = new JLabel("Name", JLabel.TRAILING);
	JTextField textField = new JTextField(20);
	if (GIE.getInstance().getActiveDatasetName() != null) {
	    String name = GIE.getInstance().getActiveDatasetName();
	    if (GIE.getInstance().getActiveDataset().getCurrentVersion() != null)
		name += "_" + GIE.getInstance().getActiveDataset().getCurrentVersion().getVersionName();
	    textField.setText(name);
	}
	l2.setLabelFor(textField);
	formPanel.add(l2);
	formPanel.add(textField);

	// descr
	JLabel l3 = new JLabel("Description", JLabel.TRAILING);
	JTextArea textArea = new JTextArea(10, 20);
	textArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	JScrollPane scroll = new JScrollPane (textArea);
	if (GIE.getInstance().getActiveDatasetName() != null
		&& GIE.getInstance().getActiveDataset().getCurrentVersion() != null) {
	    String description = GIE.getInstance().getActiveDataset().getCurrentVersion().getDescription();
	    for (GIEDatasetVersionLayer layer : GIE.getInstance().getActiveDataset().getCurrentVersion().getLayers()
		    .values()) {
		if (layer.getDescription() != null)
		    description += "\n" + layer.getLayerName() + ":\n" + layer.getDescription();
	    }
	    textArea.setText(description);
	}
	l3.setLabelFor(scroll);
	formPanel.add(l3);
	formPanel.add(scroll);

	// layers
	JLabel la = new JLabel("<html><body>Exported Layers<br/>(will be merged if<br/>multiple selected)",
		JLabel.TRAILING);
	DefaultListModel<String> listModel = new DefaultListModel<>();
	if (GIE.getInstance().getActiveDatasetName() != null
		&& GIE.getInstance().getActiveDataset().getCurrentVersion() != null) {
	    for (String lay : GIE.getInstance().getActiveDataset().getCurrentVersion().getLayers().keySet())
		listModel.addElement(lay);
	}
	JList<String> ll = new JList<>(listModel);
	ll.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	ll.setLayoutOrientation(JList.HORIZONTAL_WRAP);
	ll.setVisibleRowCount(3);
	ll.setSelectionInterval(0, listModel.getSize() - 1);
	ll.setCellRenderer(new DefaultListCellRenderer() {
	    private static final long serialVersionUID = 1L;

	    @Override
	    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
		    boolean cellHasFocus) {
		JLabel listCellRendererComponent = (JLabel) super.getListCellRendererComponent(list, value, index,
			isSelected, cellHasFocus);
		listCellRendererComponent.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
		return listCellRendererComponent;
	    }
	});

	JScrollPane lls = new JScrollPane(ll);
	la.setLabelFor(lls);
	formPanel.add(la);
	formPanel.add(lls);

	// filename
	JLabel l4 = new JLabel("File", JLabel.TRAILING);
	JPanel p2 = new JPanel(new BorderLayout());
	JTextField textField2 = new JTextField(40);
	// textField2.setEditable(false);
	p2.add(textField2, BorderLayout.LINE_START);
	JButton fileBut = new JButton("File...");
	fileBut.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		JFileChooser fc = new JFileChooser();
		FileFilter filter = new FileNameExtensionFilter("BED+TSV Files", "bed", "bed.gz", "tsv");
		fc.setFileFilter(filter);
		if (GIE.getInstance().getLastAccessedDirectories().get("GIEExportDialog") != null)
		    fc.setCurrentDirectory(GIE.getInstance().getLastAccessedDirectories().get("GIEExportDialog"));
		int result = fc.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    File f = fc.getSelectedFile();
		    textField2.setText(f.getAbsolutePath());
		    GIE.getInstance().setLastAccessedDirectory("GIEExportDialog", f.getParentFile());
		}
	    }
	});
	p2.add(fileBut, BorderLayout.LINE_END);

	l4.setLabelFor(p2);
	formPanel.add(l4);
	formPanel.add(p2);

	JCheckBox ucscChrom = new JCheckBox("Prefix chromosome names with 'chr' (UCSC compatible)");
	formPanel.add(new JLabel(""));
	formPanel.add(ucscChrom);

	JCheckBox ucscBasic = new JCheckBox("Export only basic features (coordinates+name)");
	formPanel.add(new JLabel(""));
	formPanel.add(ucscBasic);

	JCheckBox ucscNoHeader = new JCheckBox("Do not write header");
	ucscNoHeader.setSelected(false);
	formPanel.add(new JLabel(""));
	formPanel.add(ucscNoHeader);

	// TODO
	// add filters (e.g., export only selected chromosomes)

	SpringUtilities.makeCompactGrid(formPanel, 8, 2, // rows, cols
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
		String type = (String) typeList.getSelectedItem();
		boolean success = false;

		if (ll.getSelectedValuesList().size() == 0)
		    return;

		SortedSet<RegionOfInterest> rois = new TreeSet<>();
		for (String lay : ll.getSelectedValuesList()) {
		    GIEDatasetVersionLayer layer = GIE.getInstance().getActiveDataset().getCurrentVersion().getLayers()
			    .get(lay);
		    rois.addAll(layer.getRegions());
		}
		String[] annotations = GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer()
			.getAnnotations();

		if (type.equals("BED")) {
		    if (!textField2.getText().toLowerCase().endsWith(".bed")) {
			JOptionPane.showMessageDialog(IGV.getMainFrame(), "File extension has to be .bed ");
			return;
		    }
		    if (new File(textField2.getText()).exists()) {
			int reply = JOptionPane.showConfirmDialog(null,
				"File exists. Do you really want to overwrite " + textField2.getText(),
				"Confirmation Dialog", JOptionPane.YES_NO_OPTION);
			if (reply == JOptionPane.NO_OPTION) {
			    return;
			}
		    }
		    success = GIE.getInstance().export2bed(rois, new File(textField2.getText()), textField.getText(),
			    textArea.getText(), ucscChrom.isSelected(), ucscBasic.isSelected(),

			    ucscNoHeader.isSelected(), false, annotations);
		} else if (type.equals("TSV")) {
		    if (!textField2.getText().toLowerCase().endsWith(".tsv")) {
			JOptionPane.showMessageDialog(IGV.getMainFrame(), "File extension has to be .tsv ");
			return;
		    }
		    success = GIE.getInstance().export2tsv(rois, new File(textField2.getText()), textField.getText(),
			    textArea.getText(), ucscChrom.isSelected(), ucscBasic.isSelected(),
			    ucscNoHeader.isSelected(), annotations);
		}

		if (success) {
		    JOptionPane.showMessageDialog(IGV.getMainFrame(), "Exported data to " + textField2.getText());
		}

		saveCoords();
		dispose();
	    }
	});
	if (GIE.getInstance().getActiveDataset() == null
		|| GIE.getInstance().getActiveDataset().getCurrentVersion() == null)
	    button1.setEnabled(false);

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
    // new GIEExportDialog(new Frame());
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
