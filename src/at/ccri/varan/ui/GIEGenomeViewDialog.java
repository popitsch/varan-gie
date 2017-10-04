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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.ColorUIResource;

import org.broad.igv.Globals;
import org.broad.igv.feature.Chromosome;
import org.broad.igv.feature.Cytoband;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.ui.IGV;

import at.ccri.varan.GIE;
import at.ccri.varan.GIEDatasetVersion;
import at.ccri.varan.GIEDatasetVersionLayer;
import at.ccri.varan.util.CanonicalChromsomeComparator;

/**
 * GIE genome view dialog.
 * 
 * @author niko.popitsch
 * 
 */
public class GIEGenomeViewDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final int CYTOBAND_HEIGHT = 6;
    private static final  int CYTOBAND_Y_OFFSET = 3;
    private static final  int INT_HEIGHT = 6;
    private static final int INTER_CHR_SPACING = 10;

    private static Map<Integer, Color> stainColors = new HashMap<Integer, Color>();
    Dimension dim = new Dimension(1240, 960);

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

    /**
     * Initialize the dialog.
     */
    private void init() {
	setTitle("VARAN-GIE :: Whole Genome View");
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
	    setPreferredSize(dim);
	    setMinimumSize(dim);
	    setLocationRelativeTo(IGV.getMainFrame());
	} else {
	    // check compatibility with actual screen size
	    coords[0] = Math.min(coords[0], GIE.SCREEN_WIDTH - coords[2]);
	    coords[1] = Math.min(coords[1], GIE.SCREEN_HEIGHT - coords[3]);
	    setLocation(coords[0], coords[1]);
	    setMinimumSize(dim);
	    setPreferredSize(new Dimension(coords[2], coords[3]));
	}
	// +++++++++++++++++++++++++++++++++++++++++++++++

	// get active dataset version
	if (GIE.getInstance().getActiveDataset() == null
		|| GIE.getInstance().getActiveDataset().getCurrentVersion() == null)
	    return;
	GIEDatasetVersion ds = GIE.getInstance().getActiveDataset().getCurrentVersion();

	final class MyPanel extends JPanel {
	    private static final long serialVersionUID = 1L;

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
		int leftoff = 35;
		for (String chrName : genome.getAllChromosomeNames()) {

		    Chromosome chromosome = genome.getChromosome(chrName);
		    if (chromosome == null) {
			continue;
		    }

		    // skip non-canonical chroms for human genome hg19.
		    boolean canonical = CanonicalChromsomeComparator
			    .isCanonical(CanonicalChromsomeComparator.getCanonicalMappingHuman(chrName));

		    if (genome.getId().equals(Globals.DEFAULT_GENOME) && !canonical)
			continue;

		    List<Cytoband> currentCytobands = chromosome.getCytobands();
		    // calc chrom width
		    int pxWidth = (int) Math.round(
			    (double) getOwner().getWidth() * ((double) chromosome.getLength() / (double) maxlen));

		    // chr name
		    g.setColor(Color.black);
		    g.drawString(chrName, 1, off+1);
		    // draw chrom
		    Rectangle cytoRect = new Rectangle(leftoff, off, pxWidth, CYTOBAND_HEIGHT);
		    drawBands(currentCytobands, g, cytoRect, chromosome.getLength());

		    off += CYTOBAND_HEIGHT+2;

		    // draw intervals
		    Iterator<String> lns = ds.getLayers().keySet().iterator();
		    while (lns.hasNext()) {
			String lname = lns.next();
			GIEDatasetVersionLayer layer = ds.getLayers().get(lname);
			Rectangle dataRect = new Rectangle(leftoff, off, pxWidth, INT_HEIGHT);
			drawIntervals(chrName, layer.getRegions(), g, dataRect, chromosome.getLength());
			off += INT_HEIGHT + 1;
		    }
		    off += INTER_CHR_SPACING ;
		}

	    }
	}


	JPanel canvas = new MyPanel();
	// TODO calculate canvas height from #chro+#layers
	canvas.setPreferredSize(new Dimension((int) dim.getWidth(), 2000));
	canvas.setBackground(Color.WHITE);
	canvas.setOpaque(true);
	JScrollPane scrollFrame = new JScrollPane(canvas);
	canvas.setAutoscrolls(true);
	scrollFrame.setHorizontalScrollBar(null);
	add(scrollFrame, BorderLayout.CENTER);
	JPanel buttonPanel = new JPanel();
	buttonPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 6, 6));
	buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
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
	add(buttonPanel, BorderLayout.SOUTH);

	// Legend in dragabble, floating frame
	UIManager.put("InternalFrame.activeTitleBackground", new ColorUIResource(Color.black));
	UIManager.put("InternalFrame.inactiveTitleBackground", new ColorUIResource(Color.black));
	UIManager.put("InternalFrame.activeTitleForeground", new ColorUIResource(Color.WHITE));
	UIManager.put("InternalFrame.inactiveTitleForeground", new ColorUIResource(Color.WHITE));
	UIManager.put("InternalFrame.titleFont", new Font("Dialog", Font.PLAIN, 16));
	JLayeredPane layeredPane = getLayeredPane();
	JInternalFrame legendFrame = new JInternalFrame("Legend", false, false, false, false);
	legendFrame.putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);
	legendFrame.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
	legendFrame.setJMenuBar(null);
	legendFrame.setBounds(800, 800, 200, 100+ds.getLayers().size()*15);
	legendFrame.setClosable(false);
//	legendFrame.setBorder(BorderFactory.createMatteBorder(0, 1, 3, 3, Color.DARK_GRAY));
	legendFrame.setTitle("Legend");
	try {
	    legendFrame.setSelected(true);
	    legendFrame.setFrameIcon(null);
	} catch (java.beans.PropertyVetoException e2) {}

	// remove title bar
	// ((javax.swing.plaf.basic.BasicInternalFrameUI)legendFrame.getUI()).setNorthPane(null);
	JPanel legendPanel = (JPanel) legendFrame.getContentPane();
	legendPanel.setLayout(new BorderLayout());
	StringBuilder legendText = new StringBuilder();
	legendText.append("<html><body><p>");
	legendText.append("<b>" + ds.getDataset().getName() + ", " + ds.getVersionName() + "</b><br/>");
	legendText.append("Last Modified:" + ds.getActiveLayer().getLastModified() + "<br/>");
	Iterator<String> lns = ds.getLayers().keySet().iterator();
	int i = 0;
	while (lns.hasNext()) {
	    i++;
	    String lname = lns.next();
	    legendText.append("Layer #" + i + ":" + lname + "<br/>");
	}
	legendText.append("</p></body></html>");
	legendPanel.add(new JLabel(legendText.toString()), BorderLayout.CENTER);
	legendPanel.setBackground(Color.white);
	layeredPane.add(legendFrame, JLayeredPane.DRAG_LAYER);
	legendFrame.setVisible(true);



	pack();
	setModal(false);
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
	if (data == null) {
	    g2D.setColor(Color.BLACK);
	    g2D.drawRect((int) graphicRect.getX(), (int) graphicRect.getY(), (int) graphicRect.getWidth(),
		    (int) graphicRect.getHeight());
	} else
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
    public void drawIntervals(String chr, Set<RegionOfInterest> data, Graphics g2D, Rectangle graphicRect,
	    int chromoLength) {
	if (data == null)
	    return;
	double scale = graphicRect.getWidth() / chromoLength;
	int lastPX = -1;
	for (RegionOfInterest roi : data) {
	    if (!roi.getChr().equals(chr))
		continue;
	    int start = (int) (graphicRect.getX() + scale * roi.getStart());
	    int end = (int) (graphicRect.getX() + scale * roi.getEnd());
	    int i = 0;
	    if (end > lastPX) {

		int y = (int) graphicRect.getY() + CYTOBAND_Y_OFFSET;
		int height = (int) graphicRect.getHeight();
		Color col = roi.getAWTColor();
		if ( col == null || col.equals(Color.WHITE)) // cannot draw white on white
		    col=Color.GRAY;
		if (i % 2 == 0)
		    col = col.darker();
		g2D.setColor(col);
		if ((end - start) == 0)
		    g2D.fillRect(start, y, 1, height / 2);
		else
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
