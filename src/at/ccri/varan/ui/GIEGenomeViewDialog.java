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
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;

import org.broad.igv.feature.Chromosome;
import org.broad.igv.feature.Cytoband;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.ui.IGV;

import at.ccri.varan.GIE;
import at.ccri.varan.util.CanonicalChromsomeComparator;

/**
 * GIE genome view dialog.
 * 
 * @author niko.popitsch
 * 
 */
public class GIEGenomeViewDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    final int bandHeight = 8;
    static final public int CYTOBAND_Y_OFFSET = 5;
    private static Map<Integer, Color> stainColors = new HashMap<Integer, Color>();
    Dimension dim = new Dimension(1200, 960);

    // private static Logger log = Logger.getLogger(GIEGenomeViewDialog.class);

    public GIEGenomeViewDialog(Frame owner) {
	super(owner, "Whole Genome View", true);
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
	    GIE.getInstance().getWindowCoordinates().put("GIEGenomeViewDialog", getCoords());
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
	setTitle("VARAN-GIE :: Whole Genome View");
	setMinimumSize(dim);
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
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIEGenomeViewDialog");
	if (coords == null) {
	    setPreferredSize(new Dimension(500, 520));
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

	final class MyPanel extends JPanel {

	    MyPanel() {
		setMinimumSize(dim);
		setPreferredSize(dim);
	    }

	    @Override
	    public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Genome genome = GenomeManager.getInstance().getCurrentGenome();
		if (genome == null)
		    return;

		// find max chrlen
		int maxlen = -1;
		for (String chrName : genome.getAllChromosomeNames()) {
		    Chromosome chromosome = genome.getChromosome(chrName);
		    if (chromosome == null) {
			continue;
		    }
		    maxlen = Math.max(maxlen, chromosome.getLength());
		}

		int off = 10;
		for (String chrName : genome.getAllChromosomeNames()) {
		    Chromosome chromosome = genome.getChromosome(chrName);
		    if (chromosome == null) {
			continue;
		    }
		    if (!CanonicalChromsomeComparator
			    .isCanonical(CanonicalChromsomeComparator.getCanonicalMappingHuman(chrName)))
			continue;

		    List<Cytoband> currentCytobands = chromosome.getCytobands();
		    if (currentCytobands == null) {
			return;
		    }

		    // calc chrom width
		    int pxWidth = (int) Math.round(
			    (double) getOwner().getWidth() * ((double) chromosome.getLength() / (double) maxlen));

		    // chr name
		    g.setColor(Color.black);
		    g.drawString(chrName, 0, off);
		    // draw chrom
		    Rectangle cytoRect = new Rectangle(10, off, pxWidth, bandHeight);
		    drawBands(currentCytobands, g, cytoRect, chromosome.getLength());

		    off += 10;

		    // draw intervals
		    List<RegionOfInterest> regions = (List<RegionOfInterest>) IGV.getInstance().getSession()
			    .getRegionsOfInterest(chrName);
		    Rectangle dataRect = new Rectangle(10, off, pxWidth, bandHeight);
		    drawIntervals(regions, g, dataRect, chromosome.getLength());

		    off += 25;
		}

		g.draw3DRect(700, 550, 200, 80, true);
		g.setFont(new Font("Courier New", Font.ITALIC, 12));
		g.drawString(GIE.getInstance().getActiveDataset().getName()+", "+GIE.getInstance().getActiveDataset().getCurrentVersion().getVersionName(), 710, 570);
		g.drawString("Layer:" + GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer().getLayerName(), 710, 590);
		g.drawString("Last Modified:" + GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer().getLastModified(), 710, 610);
		
	    }
	}

	JPanel canvas = new MyPanel();
	JScrollPane sp = new JScrollPane(canvas);
	formPanel.add(sp);

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

	add(formPanel, BorderLayout.CENTER);
	add(buttonPanel, BorderLayout.SOUTH);

	pack();
	setVisible(true);
    }

    /**
     * Draw chrom bands
     * 
     * @param data
     * @param g2D
     * @param graphicRect
     * @param chromoLength
     */
    public void drawBands(List<Cytoband> data, Graphics g2D, Rectangle graphicRect, int chromoLength) {

	int[] xC = new int[3];
	int[] yC = new int[3];

	double scale = graphicRect.getWidth() / chromoLength;

	int lastPX = -1;
	for (Cytoband cytoband : data) {
	    int start = (int) (graphicRect.getX() + scale * cytoband.getStart());
	    int end = (int) (graphicRect.getX() + scale * cytoband.getEnd());
	    if (end > lastPX) {

		int y = (int) graphicRect.getY() + CYTOBAND_Y_OFFSET;
		int height = (int) graphicRect.getHeight();

		if (cytoband.getType() == 'c') { // centermere: "acen"

		    int center = (y + height / 2);
		    if (cytoband.getName().startsWith("p")) {
			xC[0] = start;
			yC[0] = (int) graphicRect.getMaxY() + CYTOBAND_Y_OFFSET;
			xC[1] = start;
			yC[1] = y;
			xC[2] = end;
			yC[2] = center;
		    } else {
			xC[0] = end;
			yC[0] = (int) graphicRect.getMaxY() + CYTOBAND_Y_OFFSET;
			xC[1] = end;
			yC[1] = y;
			xC[2] = start;
			yC[2] = center;
		    }
		    g2D.setColor(Color.RED.darker());
		    g2D.fillPolygon(xC, yC, 3);
		} else {

		    g2D.setColor(getCytobandColor(cytoband));
		    g2D.fillRect(start, y, (end - start), height);
		    g2D.setColor(Color.BLACK);
		    g2D.drawRect(start, y, (end - start), height);
		}
	    }
	    lastPX = end;
	}
    }

    private static Color getCytobandColor(Cytoband data) {
	if (data.getType() == 'p') { // positive: "gpos100"
	    int stain = data.getStain(); // + 4;

	    int shade = (int) (255 - stain);
	    Color c = stainColors.get(shade);
	    if (c == null) {
		c = new Color(shade, shade, shade);
		stainColors.put(shade, c);
	    }

	    return c;

	} else if (data.getType() == 'c') { // centermere: "acen"
	    return Color.PINK;

	} else {
	    return Color.WHITE;
	}
    }

    /**
     * Draw intervals
     * 
     * @param data
     * @param g2D
     * @param graphicRect
     * @param chromoLength
     */
    public void drawIntervals(List<RegionOfInterest> data, Graphics g2D, Rectangle graphicRect, int chromoLength) {
	if (data == null)
	    return;
	double scale = graphicRect.getWidth() / chromoLength;
	int lastPX = -1;
	for (RegionOfInterest roi : data) {
	    int start = (int) (graphicRect.getX() + scale * roi.getStart());
	    int end = (int) (graphicRect.getX() + scale * roi.getEnd());
	    int i = 0;
	    if (end > lastPX) {

		int y = (int) graphicRect.getY() + CYTOBAND_Y_OFFSET;
		int height = (int) graphicRect.getHeight();
		Color col = roi.getAWTColor();
		if (col == null)
		    col = Color.DARK_GRAY;
		if (i % 2 == 0)
		    col = col.darker();
		g2D.setColor(col);
		g2D.fillRect(start, y, (end - start), height);
		// g2D.setColor(Color.BLACK);
		// g2D.drawRect(start, y, (end - start), height);
	    }
	    lastPX = end;
	}
    }

    // /**
    // * Launch the application.
    // */
    // public static void main(String[] args) {
    // try {
    // GIEGenomeViewDialog d = new GIEGenomeViewDialog(new Frame());
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
