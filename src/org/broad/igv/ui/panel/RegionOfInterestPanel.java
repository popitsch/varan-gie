/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Broad Institute
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.ui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.event.MouseInputAdapter;

import org.broad.igv.Globals;
import org.broad.igv.feature.Range;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.feature.Strand;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.ui.IGV;
import org.broad.igv.util.LongRunningTask;
import org.broad.igv.util.NamedRunnable;
import org.broad.igv.util.blat.BlatClient;

import at.ccri.varan.GIE;
import at.ccri.varan.GIEDatasetVersionLayer;
import at.ccri.varan.ui.GIEDataDialog;
import at.ccri.varan.ui.ROILink;

/**
 * @author eflakes
 */
public class RegionOfInterestPanel extends JPanel {

    PopupMenu popup;

    ReferenceFrame frame;

    // There can only be 1 selected region, irrespective of the number of panels
    private static RegionOfInterest selectedRegion = null;

    public RegionOfInterestPanel(ReferenceFrame frame) {

	setToolTipText("Regions of Interest");
	this.frame = frame;
	MouseInputAdapter ma = new ROIMouseAdapater();
	addMouseListener(ma);
	addMouseMotionListener(ma);
    }

    @Override
    public void paintComponent(final Graphics g) {

	super.paintComponent(g);

	// Draw regions of interest?
	drawRegionsOfInterest((Graphics2D) g, getHeight());

	g.setColor(Color.GRAY);
	g.drawRect(0, 0, getWidth(), getHeight());
    }

    public void drawRegionsOfInterest(final Graphics2D g, int height) {

	Collection<RegionOfInterest> regions = getRegions();

	if (regions == null || regions.isEmpty()) {
	    return;
	}

	int c = 0;
	for (RegionOfInterest regionOfInterest : regions) {

	    int regionStart = regionOfInterest.getStart();
	    int regionEnd = regionOfInterest.getEnd();

	    // This is ugly, but neccessary the way the "whole genome" is
	    // treated as another chromosome
	    if (frame.getChrName().equals(Globals.CHR_ALL)) {
		Genome genome = GenomeManager.getInstance().getCurrentGenome();
		regionStart = genome.getGenomeCoordinate(regionOfInterest.getChr(), regionStart);
		regionEnd = genome.getGenomeCoordinate(regionOfInterest.getChr(), regionEnd);
	    }

	    int start = frame.getScreenPosition(regionStart);
	    int end = frame.getScreenPosition(regionEnd);
	    int regionWidth = Math.max(1, end - start);

	    Color col = regionOfInterest.getAWTColor();
	    if (col == null)
		col = regionOfInterest.getBackgroundColor();
	    c++;
	    if (c % 2 == 0)
		g.setColor(col.darker());
	    else
		g.setColor(col.brighter());
	    g.fillRect(start, 0, regionWidth, height);

	}
    }

    /**
     * Return the region of interest at the screen pixel location.
     *
     * @param px
     * @return
     */
    RegionOfInterest getRegionOfInterest(int px) {

	double pos = frame.getChromosomePosition(px);

	Collection<RegionOfInterest> roiList = getRegions();
	if (roiList != null) {
	    for (RegionOfInterest roi : roiList) {
		if (pos > roi.getStart() && pos < roi.getEnd()) {
		    return roi;
		}
	    }
	}

	return null;

    }

    protected static JPopupMenu getPopupMenu(final Component parent, final RegionOfInterest roi,
	    final ReferenceFrame frame) {

	// Set<TrackType> loadedTypes = IGV.getInstance().getLoadedTypes();

	JPopupMenu popupMenu = new RegionMenu(roi, frame);

	JMenuItem item = new JMenuItem("Select in data table");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		if (GIEDataDialog.getInstance().isVisible()) {
		    GIEDataDialog.getInstance().selectRegion(roi);
		}
	    }
	});
	popupMenu.add(item);

	item = new JMenuItem("Select visible in data table");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		if (GIEDataDialog.getInstance().isVisible()) {
		    GIEDataDialog.getInstance().selectVisibleRegions();
		}
	    }
	});
	popupMenu.add(item);

	popupMenu.addSeparator();

	item = new JMenuItem("Show start");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		String locusString = roi.getChr() + ":" + (roi.getStart() - 1000) + "-" + (roi.getStart() + 1000);
		frame.jumpTo(roi.getChr(), roi.getStart() - 1000, roi.getStart() + 1000);
		IGV.getInstance().getSession().getHistory().push(locusString, frame.getZoom());
	    }
	});
	popupMenu.add(item);

	item = new JMenuItem("Show end");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		String locusString = roi.getChr() + ":" + (roi.getEnd() - 1000) + "-" + (roi.getEnd() + 1000);
		frame.jumpTo(roi.getChr(), roi.getEnd() - 1000, roi.getEnd() + 1000);
		IGV.getInstance().getSession().getHistory().push(locusString, frame.getZoom());
	    }
	});
	popupMenu.add(item);

	item = new JMenuItem("Show all");
	item.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {

		frame.jumpTo(roi.getChr(), roi.getStart(), roi.getEnd());

		String locusString = roi.getLocusString();
		IGV.getInstance().getSession().getHistory().push(locusString, frame.getZoom());

	    }
	});
	popupMenu.add(item);

	// linked?
	GIEDatasetVersionLayer activeLayer = null;
	if (GIE.getInstance().getActiveDataset() != null
		&& GIE.getInstance().getActiveDataset().getCurrentVersion() != null)
	    activeLayer = GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer();
	if (activeLayer != null && activeLayer.getLinkedROIs().contains(roi)) {
	    for (ROILink rl : activeLayer.getLinks()) {
		if (rl.getSource().equals(selectedRegion)) {
		    JMenuItem viewItem4 = new JMenuItem("Show linked target [" + rl.getTarget().toString() + "]");
		    viewItem4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    String locusString = rl.getTarget().getChr() + ":"
				    + (rl.getTarget().getDisplayStart() - 1000) + "-"
				    + (rl.getTarget().getDisplayEnd() + 1000);
			    frame.jumpTo(rl.getTarget().getChr(), rl.getTarget().getDisplayStart() - 1000,
				    rl.getTarget().getDisplayEnd() + 1000);
			    IGV.getInstance().getSession().getHistory().push(locusString, frame.getZoom());
			}
		    });
		    popupMenu.add(viewItem4);

		} else if (rl.getTarget().equals(selectedRegion)) {
		    JMenuItem viewItem4 = new JMenuItem("Show linked source [" + rl.getSource().toString() + "]");
		    viewItem4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    String locusString = rl.getSource().getChr() + ":"
				    + (rl.getSource().getDisplayStart() - 1000) + "-"
				    + (rl.getSource().getDisplayEnd() + 1000);
			    frame.jumpTo(rl.getSource().getChr(), rl.getSource().getDisplayStart() - 1000,
				    rl.getSource().getDisplayEnd() + 1000);
			    IGV.getInstance().getSession().getHistory().push(locusString, frame.getZoom());
			}
		    });
		    popupMenu.add(viewItem4);
		}
	    }
	}

	popupMenu.addSeparator();

	item = new JMenuItem("Merge all visible");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		Range visible = IGV.getInstance().getSession().getReferenceFrame().getCurrentRange();
		List<RegionOfInterest> selectedRegions = new ArrayList<>();
		for (RegionOfInterest r : IGV.getInstance().getSession().getAllRegionsOfInterest())
		    if (visible.overlaps(r.getRange()))
			selectedRegions.add(r);

		GIEDataDialog.getInstance().mergeRegions(selectedRegions);

		if (RegionNavigatorDialog.activeInstance != null)
		    RegionNavigatorDialog.activeInstance.synchRegions();
		IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();
		IGV.getInstance().revalidateTrackPanels();
	    }
	});
	popupMenu.add(item);

	item = new JMenuItem("Merge with upstream");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		RegionOfInterest p = null;
		SortedSet<RegionOfInterest> rois = new TreeSet<>();
		rois.addAll(IGV.getInstance().getSession().getAllRegionsOfInterest());
		for (RegionOfInterest r : rois) {
		    if (r.equals(roi))
			break;
		    p = r;
		}
		if (p == null || !roi.getChr().equals(p.getChr())) {
		    System.out.println("Could not find upstream ROI of " + roi);
		    return;
		}

		GIEDataDialog.getInstance().mergeRegions(p, roi);

		if (RegionNavigatorDialog.activeInstance != null)
		    RegionNavigatorDialog.activeInstance.synchRegions();
		IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();
		IGV.getInstance().revalidateTrackPanels();
	    }
	});
	popupMenu.add(item);

	item = new JMenuItem("Merge with downstream");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		RegionOfInterest p = null;
		SortedSet<RegionOfInterest> rois = new TreeSet<>();
		rois.addAll(IGV.getInstance().getSession().getAllRegionsOfInterest());
		for (RegionOfInterest r : rois) {
		    if (p != null && p.equals(roi)) {
			p = r;
			break;
		    }
		    p = r;
		}
		if (p == null || !roi.getChr().equals(p.getChr()) || p.equals(roi)) {
		    System.out.println("Could not find downstream ROI of " + roi);
		    return;
		}

		GIEDataDialog.getInstance().mergeRegions(p, roi);

		if (RegionNavigatorDialog.activeInstance != null)
		    RegionNavigatorDialog.activeInstance.synchRegions();
		IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();
		IGV.getInstance().revalidateTrackPanels();
	    }
	});
	popupMenu.add(item);

	popupMenu.addSeparator();

	item = new JMenuItem("Attach to upstream");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		RegionOfInterest p = null;
		SortedSet<RegionOfInterest> rois = new TreeSet<>();
		rois.addAll(IGV.getInstance().getSession().getAllRegionsOfInterest());
		for (RegionOfInterest r : rois) {
		    if (r.equals(roi))
			break;
		    p = r;
		}
		if (p == null || !roi.getChr().equals(p.getChr())) {
		    System.out.println("Could not find upstream ROI of " + roi);
		    return;
		}

		if (roi.getStart() <= p.getEnd()) // nothing to do.
		    return;

		RegionOfInterest attached = new RegionOfInterest(roi.getChr(), p.getEnd(), roi.getEnd(),
			roi.getDescription());
		IGV.getInstance().getSession().addROI(attached, false, true);
		ArrayList<RegionOfInterest> todel = new ArrayList<RegionOfInterest>();
		todel.add(roi);
		IGV.getInstance().getSession().removeROI(todel);

		if (RegionNavigatorDialog.activeInstance != null)
		    RegionNavigatorDialog.activeInstance.synchRegions();
		IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();
		IGV.getInstance().revalidateTrackPanels();
	    }
	});
	popupMenu.add(item);

	item = new JMenuItem("Attach to dowstream");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		RegionOfInterest p = null;
		SortedSet<RegionOfInterest> rois = new TreeSet<>();
		rois.addAll(IGV.getInstance().getSession().getAllRegionsOfInterest());
		for (RegionOfInterest r : rois) {
		    if (p != null && p.equals(roi)) {
			p = r;
			break;
		    }
		    p = r;
		}
		if (p == null || !roi.getChr().equals(p.getChr()) || p.equals(roi)) {
		    System.out.println("Could not find downstream ROI of " + roi);
		    return;
		}

		if (roi.getEnd() >= p.getStart()) // nothing to do.
		    return;

		RegionOfInterest attached = new RegionOfInterest(roi.getChr(), roi.getStart(), p.getStart(),
			roi.getDescription());
		IGV.getInstance().getSession().addROI(attached, false, true);
		ArrayList<RegionOfInterest> todel = new ArrayList<RegionOfInterest>();
		todel.add(roi);
		IGV.getInstance().getSession().removeROI(todel);

		if (RegionNavigatorDialog.activeInstance != null)
		    RegionNavigatorDialog.activeInstance.synchRegions();
		IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();
		IGV.getInstance().revalidateTrackPanels();
	    }
	});
	popupMenu.add(item);

	popupMenu.addSeparator();

	item = new JMenuItem("Add X bp upstream");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		String slop = JOptionPane.showInputDialog(null, "Number of bp to add upstream: ", "10");
		if (slop != null) {
		    RegionOfInterest merged = new RegionOfInterest(roi.getChr(),
			    roi.getStart() - Integer.parseInt(slop), roi.getEnd(), roi.getDescription());
		    IGV.getInstance().getSession().addROI(merged, false, true);
		    ArrayList<RegionOfInterest> todel = new ArrayList<RegionOfInterest>();
		    todel.add(roi);
		    IGV.getInstance().getSession().removeROI(todel);

		    if (RegionNavigatorDialog.activeInstance != null)
			RegionNavigatorDialog.activeInstance.synchRegions();
		    IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();
		    IGV.getInstance().revalidateTrackPanels();
		}
	    }
	});
	popupMenu.add(item);

	item = new JMenuItem("Add X bp downstream");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		String slop = JOptionPane.showInputDialog(null, "Number of bp to add downstream: ", "10");
		if (slop != null) {
		    RegionOfInterest merged = new RegionOfInterest(roi.getChr(), roi.getStart(),
			    roi.getEnd() + Integer.parseInt(slop), roi.getDescription());
		    IGV.getInstance().getSession().addROI(merged, false, true);
		    ArrayList<RegionOfInterest> todel = new ArrayList<RegionOfInterest>();
		    todel.add(roi);
		    IGV.getInstance().getSession().removeROI(todel);

		    if (RegionNavigatorDialog.activeInstance != null)
			RegionNavigatorDialog.activeInstance.synchRegions();
		    IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();
		    IGV.getInstance().revalidateTrackPanels();
		}
	    }
	});
	popupMenu.add(item);

	item = new JMenuItem("Delete all visible");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		Range visible = IGV.getInstance().getSession().getReferenceFrame().getCurrentRange();
		List<RegionOfInterest> selectedRegions = new ArrayList<>();
		for (RegionOfInterest r : IGV.getInstance().getSession().getAllRegionsOfInterest())
		    if (visible.overlaps(r.getRange()))
			selectedRegions.add(r);

		IGV.getInstance().getSession().removeROI(selectedRegions);

		if (RegionNavigatorDialog.activeInstance != null)
		    RegionNavigatorDialog.activeInstance.synchRegions();
		IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();
		IGV.getInstance().revalidateTrackPanels();
	    }
	});
	popupMenu.add(item);

	popupMenu.addSeparator();

	item = new JMenuItem("Edit name...");
	item.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		String desc = JOptionPane.showInputDialog(parent, "Edit region name:", roi.getDescription());
		roi.setDescription(desc);
		IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();

	    }
	});
	popupMenu.add(item);

	item = new JMenuItem("Edit score...");
	item.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		String desc = JOptionPane.showInputDialog(parent, "Edit region score:", roi.getScore());
		roi.setScore(desc);
		IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();

	    }
	});
	popupMenu.add(item);

	item = new JMenuItem("Edit strand...");
	item.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {

		String[] list = { "+", "-", "0" };
		JComboBox<String> jcb = new JComboBox<>(list);
		jcb.setEditable(true);
		JOptionPane.showMessageDialog(null, jcb, "Edit region score:", JOptionPane.QUESTION_MESSAGE);
		String strand = (String) jcb.getSelectedItem();
		roi.setStrand(strand);
		IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();

	    }
	});
	popupMenu.add(item);

	popupMenu.addSeparator();

	item = new JMenuItem("Copy sequence");
	item.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {

		LongRunningTask.submit(new NamedRunnable() {
		    public String getName() {
			return "Copy sequence";
		    }

		    public void run() {
			Genome genome = GenomeManager.getInstance().getCurrentGenome();
			IGV.copySequenceToClipboard(genome, roi.getChr(), roi.getStart(), roi.getEnd(), Strand.NONE);
		    }
		});
	    }
	});
	popupMenu.add(item);
	// Disable copySequence if region exceeds 1 MB
	if (roi.getEnd() - roi.getStart() > 1000000) {
	    item.setEnabled(false);
	}
	popupMenu.add(item);

	item = new JMenuItem("Blat sequence");
	item.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		BlatClient.doBlatQuery(roi.getChr(), roi.getStart(), roi.getEnd(), Strand.NONE);
	    }
	});
	popupMenu.add(item);

	popupMenu.add(new JSeparator());

	item = new JMenuItem("Delete");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		IGV.getInstance().getSession().getRegionsOfInterest(frame.getChrName()).remove(roi);
		IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();
		IGV.getInstance().revalidateTrackPanels();
		if (RegionNavigatorDialog.activeInstance != null)
		    RegionNavigatorDialog.activeInstance.synchRegions();
	    }
	});
	popupMenu.add(item);

	return popupMenu;
    }

    public static RegionOfInterest getSelectedRegion() {
	return selectedRegion;
    }

    public static void setSelectedRegion(RegionOfInterest region) {
	selectedRegion = region;
    }

    class ROIMouseAdapater extends MouseInputAdapter {

	@Override
	public void mousePressed(MouseEvent e) {
	    showPopup(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	    // showPopup(e);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	    RegionOfInterest roi = getRegionOfInterest(e.getX());
	    if (roi != null) {
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setToolTipText(roi.getTooltip());
		if (selectedRegion != roi) {
		    selectedRegion = roi;
		    IGV.getInstance().revalidateTrackPanels();
		}

	    } else {
		if (selectedRegion != null) {
		    selectedRegion = null;
		    IGV.getInstance().revalidateTrackPanels();
		}
		setToolTipText("");
		setCursor(Cursor.getDefaultCursor());
	    }
	}

	@Override
	public void mouseExited(MouseEvent mouseEvent) {
	    if (selectedRegion != null) {
		selectedRegion = null;
		IGV.getInstance().revalidateTrackPanels();
	    }
	}

	private void showPopup(MouseEvent e) {

	    RegionOfInterest roi = getRegionOfInterest(e.getX());
	    if (roi != null) {

		getPopupMenu(RegionOfInterestPanel.this, roi, frame).show(e.getComponent(), e.getX(), e.getY());
	    }

	}
    }

    /**
     * A convenience method for returning the regions of interest for the
     * current frame.
     */

    private Collection<RegionOfInterest> getRegions() {
	return IGV.getInstance().getSession().getRegionsOfInterest(frame.getChrName());
    }
}
