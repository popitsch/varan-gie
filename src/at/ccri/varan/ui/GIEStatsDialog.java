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
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.prefs.PreferencesManager;
import org.broad.igv.ui.IGV;

import at.ccri.varan.GIE;
import at.ccri.varan.util.SpringUtilities;

/**
 * GIE statistics dialog.
 * 
 * @author niko.popitsch
 * 
 */
public class GIEStatsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    JTextArea textArea;

    // private static Logger log = Logger.getLogger(GIEStatsDialog.class);

    public GIEStatsDialog(Frame owner) {
	super(owner, "Statistics", true);
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
	    GIE.getInstance().getWindowCoordinates().put("GIEStatsDialog", getCoords());
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
	setTitle("VARAN-GIE :: Statistics");
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
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIEStatsDialog");
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
	JLabel l = new JLabel("Statistics:");
	formPanel.add(l);

	// calculate stats
	// TODO: add stats such as chrom-coverage, average width, etc.
	StringBuffer stats = new StringBuffer();
	if (GIE.getInstance().getActiveDataset() != null) {
	    Map<String, Integer> counts = new HashMap<>();
	    Map<String, Integer> bps = new HashMap<>();
	    List<RegionOfInterest> now = (List<RegionOfInterest>) IGV.getInstance().getSession()
		    .getAllRegionsOfInterest();

	    for (RegionOfInterest r : now) {
		Integer b = bps.get(r.getChr());
		if (b == null)
		    b = 0;
		b += CanonicalChromsomeComparator.countBreakpoints(r);
		bps.put(r.getChr(), b);

		Integer c = counts.get(r.getChr());
		if (c == null)
		    c = 0;
		c++;
		counts.put(r.getChr(), c);
	    }
	    System.out.println(bps);
	    Genome g = GenomeManager.getInstance().getCurrentGenome();
	    if (g != null) {
		int call = 0, ball = 0;
		stats.append("Chr\tIntervals\tBreakpoints\n");
		// show stats for all chromosomes. For hg19 display only canonical chromosomes.
		for (String chr : g.getAllChromosomeNames()) {
		    boolean canonical = CanonicalChromsomeComparator
			    .isCanonical(CanonicalChromsomeComparator.getCanonicalMappingHuman(chr));
		    if (g.getId().equals(PreferencesManager.getPreferences().getDefaultGenome()) && !canonical)
			continue;
		    Integer c = counts.get(chr);
		    Integer b = bps.get(chr);
		    stats.append(chr + "\t" + fmt(c) + "\t" + fmt(b) + "\n");
		    if (c != null)
			call += c;
		    if (b != null)
			ball += b;
		}
		Integer c = counts.get("ALL");
		if (c == null)
		    c = 0;
		stats.append("Sum\t" + fmt(call) + "\t" + fmt(ball) + "\n");
	    }
	}

	// descr
	textArea = new JTextArea(27, 20);
	textArea.setEditable(false);
	textArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	textArea.setText(stats.toString());

	//
	// textArea.setText("lorem ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem
	// ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem ipsum\nlorem
	// ipsum\nlorem ipsum\nlorem ipsum\n");

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
		saveCoords();
		dispose();
	    }
	});

	buttonPanel.add(Box.createHorizontalGlue());
	buttonPanel.add(buttonOk);

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
    // GIEStatsDialog d = new GIEStatsDialog(new Frame());
    // WindowListener exitListener = new WindowAdapter() {
    // @Override
    // public void windowClosing(WindowEvent e) {
    // System.exit(0);
    // }
    // };
    // d.addWindowListener(exitListener);
    //
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }

}
