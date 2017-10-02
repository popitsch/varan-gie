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
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;

import org.broad.igv.ui.IGV;

import at.ccri.varan.GIE;
import at.ccri.varan.util.SpringUtilities;

/**
 * GIE edit description dialog.
 * 
 * @author niko.popitsch
 * 
 */
public class GIEEditDescriptionDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    JTextArea textArea;

    // private static Logger log = Logger.getLogger(GIEStatsDialog.class);

    public GIEEditDescriptionDialog(Frame owner) {
	super(owner, "GIEEditDescriptionDialog", true);
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
	    GIE.getInstance().getWindowCoordinates().put("GIEEditDescriptionDialog", getCoords());
    }

    private String fmt(Integer c) {
	if (c == null)
	    return "-";
	return String.format(Locale.US, "%s", c);
    }

    /**
     * Initialize the dialog.
     */
    private void init() {
	setTitle("VARAN-GIE :: Edit Layer Description");
	setMinimumSize(new Dimension(500, 500));
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
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIEEditDescriptionDialog");
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

	JPanel formPanel = new JPanel(new SpringLayout());

	// type
	JLabel l = new JLabel("Description:");
	formPanel.add(l);

	// descr
	textArea = new JTextArea(25, 20);
	textArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	textArea.setText(GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer().getDescription());

	JScrollPane sp = new JScrollPane(textArea);
	formPanel.add(sp);

	SpringUtilities.makeCompactGrid(formPanel, 2, 1, // rows, cols
		6, 6, // initX, initY
		6, 6); // xPad, yPad

	JPanel buttonPanel = new JPanel();
	buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 6));
	buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

	// buttons
	JButton buttonOk = new JButton("OK");
	buttonOk.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer().setDescription(textArea.getText());
		saveCoords();
		dispose();
	    }
	});

	// buttons
	JButton buttonCancel = new JButton("Cancel");
	buttonCancel.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		saveCoords();
		dispose();
	    }
	});

	buttonPanel.add(Box.createHorizontalGlue());
	buttonPanel.add(buttonOk);
	buttonPanel.add(buttonCancel);

	add(formPanel, BorderLayout.NORTH);
	add(buttonPanel, BorderLayout.SOUTH);

	pack();
	setVisible(true);
    }


}
