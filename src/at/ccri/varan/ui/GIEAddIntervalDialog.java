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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.zip.GZIPInputStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;
import org.broad.igv.event.IGVEventObserver;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.ui.IGV;

import at.ccri.varan.GIE;
import at.ccri.varan.GIEDatasetVersionLayer;
import at.ccri.varan.ui.ROILink.TYPE;
import at.ccri.varan.util.CanonicalChromsomeComparator;
import at.ccri.varan.util.IntervalTools;
import at.ccri.varan.util.SpringUtilities;

/**
 * @author niko.popitsch
 * 
 *         GIE add interval
 * 
 */
public class GIEAddIntervalDialog extends JDialog implements Observer, IGVEventObserver {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(GIEAddIntervalDialog.class);

    private static String lastText = "";

    private static enum IMPORT_FORMAT {
	SIMPLE1, SIMPLE2, BEDPE, FULL
    };

    public GIEAddIntervalDialog(Frame owner) {
	super(owner, "Add Intervals", true);
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
	    GIE.getInstance().getWindowCoordinates().put("GIEAddIntervalDialog", getCoords());
    }

    /**
     * Initialize the dialog.
     */
    private void init() {
	// ======== this ========
	setTitle("VARAN-GIE :: Add Genomic Intervals");
	setMinimumSize(new Dimension(500, 350));
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
	Integer[] coords = GIE.getInstance().getWindowCoordinates().get("GIEAddIntervalDialog");
	if (coords == null) {
	    setPreferredSize(new Dimension(500, 350));
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
	JLabel l = new JLabel(
		"Enter one interval per line. Format: \"chr1:1-100 \\t label\" or \"chr \\t 1 \\t 100 \\t label\" or \"chr1 \\t 1 \\t 100 \\t chr2 \\t 1 \\t 100 \\t label\" ");
	formPanel.add(l);

	// descr

	JTextArea textArea = new JTextArea(17, 20);
	JScrollPane sp = new JScrollPane(textArea);
	textArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	if (lastText != null)
	    textArea.setText(lastText);
	formPanel.add(sp);

	SpringUtilities.makeCompactGrid(formPanel, 2, 1, // rows, cols
		6, 6, // initX, initY
		6, 6); // xPad, yPad

	JPanel buttonPanel = new JPanel();
	buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 6));
	buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

	// load from BED file button
	JButton buttonLoad = new JButton("Load BED data...");
	buttonLoad.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		JFileChooser fc = new JFileChooser();
		if (GIE.getInstance().getLastAccessedDirectories().get("GIEAddIntervalDialog") != null)
		    fc.setCurrentDirectory(GIE.getInstance().getLastAccessedDirectories().get("GIEAddIntervalDialog"));
		int result = fc.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    File f = fc.getSelectedFile();
		    BufferedReader reader = null;
		    StringBuffer sb = new StringBuffer();
		    try {

			if (f.getName().endsWith(".gz"))
			    reader = new BufferedReader(
				    new InputStreamReader(new GZIPInputStream(new FileInputStream(f))));
			else
			    reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));

			String nextLine;

			int c = 0;
			while ((nextLine = reader.readLine()) != null && (nextLine.trim().length() > 0)) {
			    if (nextLine.startsWith("track") || nextLine.startsWith("browser ")
				    || nextLine.trim().equals(""))
				continue;
			    c++;
			    if (c == 10000) {
				int reply = JOptionPane.showConfirmDialog(null,
					"File contains a large number of intervals (>10000). "
						+ "It is not recommended to run GIE with such large interval sets. "
						+ "Continue importing?",
					"Confirmation Dialog", JOptionPane.YES_NO_OPTION);
				if (reply != JOptionPane.YES_OPTION) {
				    break;
				}
			    }
			    sb.append(nextLine + "\n");
			}
		    } catch (IOException e1) {
			JOptionPane.showMessageDialog(IGV.getMainFrame(), "Error " + e1.getMessage(), "Error",
				JOptionPane.ERROR_MESSAGE);
			e1.printStackTrace();
			return;
		    } finally {
			if (reader != null)
			    try {
				reader.close();
			    } catch (IOException e1) {
				e1.printStackTrace();
			    }
		    }
		    textArea.setText(sb.toString());
		    GIE.getInstance().setLastAccessedDirectory("GIEAddIntervalDialog", f.getParentFile());
		}
	    }
	});

	// load from TSV file button
	JButton buttonLoad2 = new JButton("Load TSV data...");
	buttonLoad2.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		JFileChooser fc = new JFileChooser();
		if (GIE.getInstance().getLastAccessedDirectories().get("GIEImportFromTSVDialog") != null)
		    fc.setCurrentDirectory(
			    GIE.getInstance().getLastAccessedDirectories().get("GIEImportFromTSVDialog"));
		int result = fc.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    File fin = fc.getSelectedFile();
		    GIEImportFromTSVDialog diag;
		    try {
			diag = new GIEImportFromTSVDialog(IGV.getMainFrame(), fin);
			StringBuffer regions = diag.getIntervalString();
			if (regions != null)
			    textArea.setText(regions.toString());
			GIE.getInstance().setLastAccessedDirectory("GIEImportFromTSVDialog", fin.getParentFile());
		    } catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		    }
		}
	    }
	});

	// buttons
	JButton buttonOk = new JButton("OK");
	buttonOk.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		try {
		    int c = 0;
		    Genome g = GenomeManager.getInstance().getCurrentGenome();
		    List<RegionOfInterest> rois = new ArrayList<>();
		    List<ROILink> links = new ArrayList<>();
		    String[] tmp = textArea.getText().split("\n", -1);
		    IMPORT_FORMAT format = null;
		    for (int ln = 0; ln < tmp.length; ln++) {
			if (tmp[ln].trim().equals(""))
			    continue;
			if (tmp[ln].trim().startsWith("#"))
			    continue;
			String[] tabtest = tmp[ln].split("\t", -1);

			// guess format from 2st entry (or ask if unclear)
			if (format == null) {
			    if (tabtest.length <= 2)
				format = IMPORT_FORMAT.SIMPLE1;
			    else if (tabtest.length == 3 || tabtest.length == 4)
				format = IMPORT_FORMAT.SIMPLE2;
			    else if (tabtest.length == 6 || tabtest.length == 7 )
				format = IMPORT_FORMAT.BEDPE;
			    else if (tabtest.length >= 8) {
				// ask user
				int reply = JOptionPane.showConfirmDialog(null,
					"Ambiguous data format. Import as BEDPE (YES) or FULL (NO)?",
					"Ambiguous data format", JOptionPane.YES_NO_OPTION);
				if (reply == JOptionPane.YES_OPTION) {
				    format = IMPORT_FORMAT.BEDPE;
				} else
				    format = IMPORT_FORMAT.FULL;
			    } else
				throw new ParseException("Unknown data format!", 0);
			}

			if (format == IMPORT_FORMAT.SIMPLE1) {
			    // Simple1 FORMAT: chr:start-end [\t label]
			    String[] s1 = tmp[ln].split(":", -1);
			    if (s1.length < 2)
				throw new ParseException("No chromosome separator found in '" + tmp[ln], ln);

			    String[] s2 = s1[1].split("-", -1);
			    if (s2.length < 2)
				throw new ParseException("No interval separator found in '" + tmp[ln], ln);

			    String[] s3 = s2[1].split("\t", -1);
			    try {
				c++;
				String chr = s1[0];
				if (g != null)
				    chr = g.getCanonicalChrName(chr);
				if (chr.contains(":"))
				    throw new ParseException("Parse error for format 'chr \t start\t end [\t label]'",
					    1);

				Integer start = CanonicalChromsomeComparator.parseCoordinate(chr, null, s2[0]);
				Integer end = CanonicalChromsomeComparator.parseCoordinate(chr, null, s3[0]);
				String description = "imported" + c;
				if (s3.length > 1)
				    description = s3[1];
				RegionOfInterest r = new RegionOfInterest(chr, start, end, description);
				rois.add(r);
			    } catch (Exception ex1) {
				throw new ParseException(ex1.getMessage(), ln);
			    }
			} else if (format == IMPORT_FORMAT.SIMPLE2) {
			    // Simple2 FORMAT: chr \t start\t end [\t label]
			    try {
				c++;
				String chr = tabtest[0];
				if (chr.contains(":"))
				    throw new ParseException("Parse error for format 'chr \t start\t end [\t label]'",
					    1);
				if (g != null)
				    chr = g.getCanonicalChrName(chr);

				Integer start = CanonicalChromsomeComparator.parseCoordinate(chr, null, tabtest[1]);
				Integer end = CanonicalChromsomeComparator.parseCoordinate(chr, null, tabtest[2]);

				String description = "imported" + c;
				if (tabtest.length > 3)
				    description = tabtest[3];
				RegionOfInterest r = new RegionOfInterest(chr, start, end, description);
				rois.add(r);
			    } catch (Exception ex1) {
				throw new ParseException(ex1.getMessage(), ln);
			    }

			} else if (format == IMPORT_FORMAT.BEDPE) {
			    // BEDPE FORMAT: chr1 \t start1 \t end1 \t chr2 \t start2 \t end2 [\t label]
			    try {
				c++;
				String chr1 = tabtest[0];
				String chr2 = tabtest[3];
				if (chr1.contains(":") || chr2.contains(":"))
				    throw new ParseException(
					    "Parse error for format 'chr1 \t start1 \t end1 \t chr2 \t start2 \t end2 [\t label \t score \t strand1 \t strand2]'",
					    1);
				if (g != null) {
				    chr1 = g.getCanonicalChrName(chr1);
				    chr2 = g.getCanonicalChrName(chr2);
				}

				Integer start1 = CanonicalChromsomeComparator.parseCoordinate(chr1, null, tabtest[1]);
				Integer end1 = CanonicalChromsomeComparator.parseCoordinate(chr1, null, tabtest[2]);
				Integer start2 = CanonicalChromsomeComparator.parseCoordinate(chr2, null, tabtest[4]);
				Integer end2 = CanonicalChromsomeComparator.parseCoordinate(chr2, null, tabtest[5]);
				String description = "imported" + c;
				if (tabtest.length >= 7)
				    description = tabtest[6];
				RegionOfInterest r1 = new RegionOfInterest(chr1, start1, end1, description);
				RegionOfInterest r2 = new RegionOfInterest(chr2, start2, end2, description);
				if (tabtest.length >= 8) {
				    try {
					r1.setScore(Double.parseDouble(tabtest[7]));
					r2.setScore(Double.parseDouble(tabtest[7]));
				    } catch (NumberFormatException ex) {
				    }
				}
				if (tabtest.length >= 9) {
				    r1.setStrand(tabtest[8]);
				}
				if (tabtest.length >= 10) {
				    r2.setStrand(tabtest[9]);
				}
				rois.add(r1);
				rois.add(r2);
				ROILink rl = new ROILink(r1, r2, TYPE.FUSION);
				links.add(rl);
			    } catch (Exception ex1) {
				throw new ParseException(ex1.getMessage(), ln);
			    }
			} else if (format == IMPORT_FORMAT.FULL) {
			    // FULL FORMAT: chr1 \t start \t end \t width(will be calculated) \t name \t score \t strand \t color [\t custom1=val1 \t custom2=val2 \t ...]
			    try {
				c++;
				String chr = tabtest[0];
				if (chr.contains(":"))
				    throw new ParseException(
					    "Parse error for format 'chr1 \t start \t end \t width(will be calculated) \t name \t score \t strand \t color [\t custom1=val1 \t custom2=val2 \t ...'",
					    1);
				if (g != null) {
				    chr = g.getCanonicalChrName(chr);
				}
				Integer start = CanonicalChromsomeComparator.parseCoordinate(chr, null, tabtest[1]);
				Integer end = CanonicalChromsomeComparator.parseCoordinate(chr, null, tabtest[2]);
				String description = tabtest[4];
				RegionOfInterest r = new RegionOfInterest(chr, start, end, description);
				try {
				    Double score = Double.parseDouble(tabtest[5]);
				    r.setScore(score);
				} catch (NumberFormatException ex) {
				}
				r.setStrand(tabtest[6]);
				r.setColor(tabtest[7]);
				for (int i = 8; i < tabtest.length; i++) {
				    String[] x = tabtest[i].split("=", -1);
				    if (x.length == 2)
					r.addAnnotation(x[0], x[1]);
				}
				rois.add(r);
			    } catch (Exception ex1) {
				throw new ParseException(ex1.getMessage(), ln);
			    }
			} else
			    throw new ParseException("Unknown format", 0);
		    }
		    log.info("Importing " + rois.size() + " intervals");

		    // are the intervals overlapping?
		    // List<RegionOfInterest> allROI = (List<RegionOfInterest>) IGV.getInstance().getSession().getAllRegionsOfInterest();
		    List<RegionOfInterest> allROI = new ArrayList<>();
		    allROI.addAll(
			    GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer().getRegions());
		    boolean overlapsCurrent = (IntervalTools.isOverlappingROI(allROI, rois));
		    List<List<RegionOfInterest>> layersToImport = IntervalTools.splitOverlapping(rois);
		    boolean isOverlapping = layersToImport.size() > 1;

		    if (overlapsCurrent || isOverlapping) {
			int reply = JOptionPane.showConfirmDialog(null,
				"<html><body>The imported " + rois.size()
					+ " intervals overlap with the existing intervals "
					+ "in the current layer or among each other. <br/>Shall VARAN-GIE import the intervals "
					+ "into new layers of non-overlapping intervals (YES) <br/> "
					+ "or merge overlapping intervals into the currently selected layer (NO)?",
				"Overlapping Intervals", JOptionPane.YES_NO_OPTION);
			if (reply == JOptionPane.YES_OPTION) {

			    try {
				String prefix = "importedLayer";
				int idx = 1;
				Map<String, GIEDatasetVersionLayer> layers = GIE.getInstance().getActiveDataset()
					.getCurrentVersion().getLayers();
				for (String ln : layers.keySet()) {
				    if (ln.startsWith(prefix)) {
					idx = Math.max(idx, Integer.parseInt(ln.substring(prefix.length())) + 1);
				    }
				}
				for (List<RegionOfInterest> rois2import : layersToImport) {
				    String lname = prefix + idx;
				    idx++;
				    GIE.getInstance().getActiveDataset().getCurrentVersion().addLayer(lname);
				    // ensure that new table is added to IGV session
				    GIEDatasetVersionLayer layer = GIE.getInstance().getActiveDataset()
					    .getCurrentVersion().getActiveLayer();
				    layer.addRegions(rois2import);
				    // add links
				    for (ROILink rl : links)
					GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer()
						.addLink(rl);
				    layer.updateAndSave();
				}

				GIE.getInstance().reloadActiveDataset();
			    } catch (Exception ex) {
				JOptionPane.showMessageDialog(IGV.getMainFrame(), "Importing Error " + ex.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
			    }
			} else if (reply == JOptionPane.NO_OPTION) {
			    IGV.getInstance().addROI(rois);
			}

		    } else {
			IGV.getInstance().addROI(rois);
			// add links
			for (ROILink rl : links)
			    GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer().addLink(rl);
		    }
		    JOptionPane.showMessageDialog(IGV.getMainFrame(), "Imported " + c + " intervals");
		    dispose();
		} catch (ParseException ex) {
		    JOptionPane.showMessageDialog(IGV.getMainFrame(), "Parsing Error " + ex.getMessage(), "Error",
			    JOptionPane.ERROR_MESSAGE);
		}
	    }

	});
	JButton buttonCancel = new JButton("Cancel");
	buttonCancel.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		lastText = textArea.getText();
		dispose();
	    }
	});
	;

	buttonPanel.add(Box.createHorizontalStrut(5));
	buttonPanel.add(buttonLoad);
	buttonPanel.add(Box.createHorizontalStrut(5));
	buttonPanel.add(buttonLoad2);
	buttonPanel.add(Box.createHorizontalGlue());
	buttonPanel.add(buttonOk);
	buttonPanel.add(Box.createHorizontalStrut(10));
	buttonPanel.add(buttonCancel);

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
    // new GIEAddIntervalDialog(new Frame());
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
