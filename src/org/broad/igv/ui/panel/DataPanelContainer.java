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

package org.broad.igv.ui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.broad.igv.exceptions.DataLoadException;
import org.broad.igv.renderer.DataRange;
import org.broad.igv.track.AttributeManager;
import org.broad.igv.track.MergedTracks;
import org.broad.igv.track.Range;
import org.broad.igv.track.ScalableTrack;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackGroup;
import org.broad.igv.ui.AbstractDataPanelTool;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.MessageCollection;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.util.HttpUtils;
import org.broad.igv.util.ResourceLocator;

/**
 * @author jrobinso
 * @date Sep 8, 2010
 */
public class DataPanelContainer extends TrackPanelComponent implements Paintable {

    private static Logger log = Logger.getLogger(DataPanelContainer.class);

    TrackPanel parent;

    public DataPanelContainer(TrackPanel trackPanel) {
	super(trackPanel);

	DropTarget target = new DropTarget(this, new FileDropTargetListener(trackPanel));
	setDropTarget(target);
	target.setActive(true);
	this.setLayout(new DataPanelLayout());
	this.parent = trackPanel;
	createDataPanels();

    }

    public void createDataPanels() {
	removeAll();
	for (ReferenceFrame f : FrameManager.getFrames()) {
	    if (f.isVisible()) {
		DataPanel dp = new DataPanel(f, this);
		add(dp);
	    }
	}
	invalidate();
    }

    @Override
    public void setBackground(Color color) {
	super.setBackground(color);
	for (Component c : this.getComponents()) {
	    if (c instanceof DataPanel) {
		c.setBackground(color);
	    }
	}
    }

    public Collection<TrackGroup> getTrackGroups() {
	TrackPanel dataTrackView = (TrackPanel) getParent();
	return dataTrackView.getGroups();
    }

    public int getVisibleHeight() {
	TrackPanel dataTrackView = (TrackPanel) getParent();
	return dataTrackView.getVisibleRect().height;
    }

    public void setCurrentTool(final AbstractDataPanelTool tool) {
	for (Component c : this.getComponents()) {
	    if (c instanceof DataPanel) {
		((DataPanel) c).setCurrentTool(tool);
	    }
	}
    }

    /**
     * Paint to an offscreen graphic, e.g. a graphic for an image or svg file.
     *
     * @param g
     * @param rect
     */
    public void paintOffscreen(Graphics2D g, Rectangle rect) {

	// Get the components of the sort by X position.
	Component[] components = getComponents();
	Arrays.sort(components, new Comparator<Component>() {
	    public int compare(Component component, Component component1) {
		return component.getX() - component1.getX();
	    }
	});

	for (Component c : this.getComponents()) {

	    if (c instanceof DataPanel) {
		Graphics2D g2d = (Graphics2D) g.create();
		Rectangle clipRect = new Rectangle(c.getBounds());
		clipRect.height = rect.height;
		g2d.setClip(clipRect);
		g2d.translate(c.getX(), 0);
		((DataPanel) c).paintOffscreen(g2d, rect);

	    }
	}
	// super.paintBorder(g);
    }

    @Override
    protected void paintChildren(Graphics g) {

	autoscale();

	super.paintChildren(g);
	if (IGV.getInstance().isRulerEnabled()) {
	    int start = MouseInfo.getPointerInfo().getLocation().x - getLocationOnScreen().x;
	    g.setColor(Color.BLACK);
	    g.drawLine(start, 0, start, getHeight());
	}
    }

    private class FileDropTargetListener implements DropTargetListener {

	private TrackPanel panel;

	public FileDropTargetListener(TrackPanel dataPanel) {
	    panel = dataPanel;
	}

	public void dragEnter(DropTargetDragEvent event) {

	    if (!isDragAcceptable(event)) {
		event.rejectDrag();
		return;
	    }
	}

	public void dragExit(DropTargetEvent event) {
	}

	public void dragOver(DropTargetDragEvent event) {
	    // you can provide visual feedback here
	}

	public void dropActionChanged(DropTargetDragEvent event) {
	    if (!isDragAcceptable(event)) {
		event.rejectDrag();
		return;
	    }
	}

	public void drop(DropTargetDropEvent event) {
	    if (!isDropAcceptable(event)) {
		event.rejectDrop();
		return;
	    }

	    event.acceptDrop(DnDConstants.ACTION_COPY);

	    Transferable transferable = event.getTransferable();

	    MessageCollection messages = new MessageCollection();
	    try {
		List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

		for (File file : files) {
		    try {
			ResourceLocator locator = new ResourceLocator(file.getAbsolutePath());
			IGV.getInstance().load(locator, panel);
		    } catch (DataLoadException de) {
			messages.append(de.getMessage());
		    }
		}
		String obj = transferable.getTransferData(DataFlavor.stringFlavor).toString();
		if (HttpUtils.isRemoteURL(obj)) {
		    IGV.getInstance().load(new ResourceLocator(obj), panel);
		}
		if (messages != null && !messages.isEmpty()) {
		    log.error(messages.getFormattedMessage());
		    MessageUtils.showMessage(messages.getFormattedMessage());
		}
	    } catch (Exception e) {
		String obj = null;
		try {
		    obj = transferable.getTransferData(DataFlavor.stringFlavor).toString();
		    if (HttpUtils.isRemoteURL(obj)) {
			IGV.getInstance().load(new ResourceLocator(obj), panel);
		    }
		} catch (Exception e1) {
		    log.error(e1);
		    if (messages != null && !messages.isEmpty()) {
			MessageUtils.showMessage(messages.getFormattedMessage());
		    }
		}
	    }
	    IGV.getMainFrame().repaint();
	    event.dropComplete(true);
	}

	public boolean isDragAcceptable(DropTargetDragEvent event) { // usually, you
	    // check the
	    // available
	    // data flavors
	    // here
	    // in this program, we accept all flavors
	    return (event.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0;
	}

	public boolean isDropAcceptable(DropTargetDropEvent event) { // usually, you
	    // check the
	    // available
	    // data flavors
	    // here
	    // in this program, we accept all flavors
	    return (event.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0;
	}
    }

    private void autoscale() {

	final Collection<Track> trackList = IGV.getInstance().getAllTracks();

	Map<String, List<Track>> autoscaleGroups = new HashMap<String, List<Track>>();

	for (Track track : trackList) {

	    if (!track.isVisible())
		continue;

	    String asGroup = track.getAttributeValue(AttributeManager.GROUP_AUTOSCALE);
	    if (asGroup != null) {
		if (!autoscaleGroups.containsKey(asGroup)) {
		    autoscaleGroups.put(asGroup, new ArrayList<Track>());
		}

		if (track instanceof MergedTracks) {
		    for (Track mt : ((MergedTracks) track).getMemberTracks()) {
			autoscaleGroups.get(asGroup).add(track);
		    }
		} else {
		    autoscaleGroups.get(asGroup).add(track);
		}
	    } else if (track.getAutoScale()) {

		if (track instanceof MergedTracks) {
		    for (Track mt : ((MergedTracks) track).getMemberTracks()) {
			autoscaleGroup(Arrays.asList(mt));
		    }
		} else {
		    autoscaleGroup(Arrays.asList(track));
		}
	    }

	}

	if (autoscaleGroups.size() > 0) {
	    for (List<Track> tracks : autoscaleGroups.values()) {
		autoscaleGroup(tracks);
	    }
	}
    }

    private void autoscaleGroup(List<Track> trackList) {

	List<ReferenceFrame> frames = FrameManager.isGeneListMode() ? FrameManager.getFrames()
		: Arrays.asList(FrameManager.getDefaultFrame());

	List<Range> inViewRanges = new ArrayList<Range>();

	synchronized (trackList) {
	    for (Track track : trackList) {
		if (track instanceof ScalableTrack) {
		    for (ReferenceFrame frame : frames) {
			Range range = ((ScalableTrack) track).getInViewRange(frame);
			if (range != null) {
			    inViewRanges.add(range);
			}
		    }
		}
	    }

	    if (inViewRanges.size() > 0) {

		Range inter = computeScale(inViewRanges);

		for (Track track : trackList) {

		    DataRange dr = track.getDataRange();
		    float min = Math.min(0, inter.min);
		    float base = Math.max(min, dr.getBaseline());
		    float max = inter.max;
		    // Pathological case where min ~= max (no data in view)
		    if (max - min <= (2 * Float.MIN_VALUE)) {
			max = min + 1;
		    }

		    DataRange newDR = new DataRange(min, base, max, dr.isDrawBaseline());
		    newDR.setType(dr.getType());
		    track.setDataRange(newDR);

		}
	    }
	}
    }

    public static Range computeScale(List<Range> ranges) {

	float min = 0;
	float max = 0;

	if (ranges.size() > 0) {
	    max = ranges.get(0).max;
	    min = ranges.get(0).min;

	    for (int i = 1; i < ranges.size(); i++) {

		Range r = ranges.get(i);
		max = Math.max(r.max, max);
		min = Math.min(r.min, min);

	    }
	}

	return new Range(min, max);
    }

}
