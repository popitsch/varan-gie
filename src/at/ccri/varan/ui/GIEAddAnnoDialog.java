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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;

import org.broad.igv.ui.IGV;

import at.ccri.varan.GIE;
import at.ccri.varan.GIEAnnotationTrack;
import at.ccri.varan.util.SpringUtilities;

/**
 * GIE "add annotation track" dialog.
 * 
 * @author niko.popitsch
 * 
 */
public class GIEAddAnnoDialog extends JDialog {

    // private static Logger log = Logger.getLogger(GIEAddAnnoDialog.class);

    private static final long serialVersionUID = 1L;

    public GIEAddAnnoDialog(Frame owner) {
	super(owner, "Add annotation track", true);
	init();
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
	    GIE.getInstance().getWindowCoordinates().put("GIEAddAnnoDialog", getCoords());
    }

    /**
     * Initialize the dialog.
     */
    private void init() {
	setTitle("VARAN-GIE :: Add Annotation Track");
	setMinimumSize(new Dimension(500, 250));
	setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	// +++++++++++++++++++++++++++++++++++++++++++++++
	// set location on screen
	// +++++++++++++++++++++++++++++++++++++++++++++++
	addWindowListener(new WindowAdapter() {
	    // use windowClosed here (instead of windowClosinG!)
	    @Override
	    public void windowClosing(WindowEvent e) {
		saveCoords();
	    }
	});
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIEAddAnnoDialog");
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

	// name
	JLabel l2 = new JLabel("Name", JLabel.TRAILING);
	JTextField textField = new JTextField(20);
	l2.setLabelFor(textField);

	// descr
	JLabel l3 = new JLabel("Description", JLabel.TRAILING);
	JTextArea textArea = new JTextArea(3, 20);
	textArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	l3.setLabelFor(textArea);

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
		if (GIE.getInstance().getLastAccessedDirectories().get("GIEAddAnnoDialog") != null)
		    fc.setCurrentDirectory(GIE.getInstance().getLastAccessedDirectories().get("GIEAddAnnoDialog"));
		int result = fc.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    File f = fc.getSelectedFile();
		    textField2.setText(f.getAbsolutePath());
		    if (textField.getText().equals(""))
			textField.setText(f.getName());
		    GIE.getInstance().setLastAccessedDirectory("GIEAddAnnoDialog", f.getParentFile());
		}
	    }
	});
	p2.add(fileBut, BorderLayout.LINE_END);
	l4.setLabelFor(p2);

	formPanel.add(l4);
	formPanel.add(p2);
	formPanel.add(l2);
	formPanel.add(textField);
	formPanel.add(l3);
	formPanel.add(textArea);

	// TODO
	// add filters (e.g., export only selected chromosomes)

	SpringUtilities.makeCompactGrid(formPanel, 3, 2, // rows, cols
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
		String name = textField.getText();
		File dataFile = new File(textField2.getText());
		String desc = textArea.getText();
		GIE.getInstance().addAnnotationTrack(new GIEAnnotationTrack(name, dataFile, desc));
		GIEAnnoDialog.getInstance().refresh();
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

}
