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
package org.broad.igv.ui;

import static org.broad.igv.prefs.Constants.SAM_COMPLETE_READS_ONLY;
import static org.broad.igv.prefs.Constants.SAM_SORT_BY_TAG;
import static org.broad.igv.prefs.Constants.SAM_SORT_OPTION;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;

import org.apache.log4j.Logger;
import org.broad.igv.charts.ScatterPlotUtils;
import org.broad.igv.feature.BasicFeature;
import org.broad.igv.feature.Exon;
import org.broad.igv.feature.Range;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.prefs.IGVPreferences;
import org.broad.igv.prefs.PreferencesManager;
import org.broad.igv.sam.AlignmentTrack;
import org.broad.igv.track.FeatureTrack;
import org.broad.igv.track.Track;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.ui.panel.RegionNavigatorDialog;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.variant.VariantTrack;

import at.ccri.varan.GIE;
import at.ccri.varan.ui.GIEDataDialog;
import at.ccri.varan.ui.UndoHandler;
import htsjdk.tribble.Feature;

/**
 * @author jrobinso
 */
public class GlobalKeyDispatcher implements KeyEventDispatcher {

    private static Logger log = Logger.getLogger(GlobalKeyDispatcher.class);

    private final InputMap inputMap = new InputMap();
    private final ActionMap actionMap = new ActionMap();

    public static boolean blockKeys = false;

    public GlobalKeyDispatcher() {
	init();
    }

    public boolean dispatchKeyEvent(KeyEvent event) {

	if (blockKeys) {
	    System.out.println("Wait!");
	    return true;
	}

	KeyStroke ks = KeyStroke.getKeyStrokeForEvent(event);
	String actionKey = (String) inputMap.get(ks);

	// Disable tooltip if any modifier control key is pressed
	if (event.getKeyCode() == KeyEvent.VK_CONTROL || event.getKeyCode() == KeyEvent.VK_ALT) {
	    boolean flag = !(event.isControlDown() || event.isAltDown() || event.isMetaDown());
	    ToolTipManager.sharedInstance().setEnabled(flag);
	}

	if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
	    IGV.getInstance().clearSelections();
	    IGV.getInstance().repaint();
	    return true;
	}

	if (actionKey != null) {
	    Action action = actionMap.get(actionKey);
	    if (action != null && action.isEnabled()) {
		// I'm not sure about the parameters
		action.actionPerformed(new ActionEvent(event.getSource(), event.getID(), actionKey,
			((KeyEvent) event).getModifiers()));
		return true; // consume event
	    }
	}

	return false;
    }

    /**
     * Initialize the input and action map. The indirection here strikes me as odd but it is apparently the standard pattern.
     */
    public void init() {

	final IGV igv = IGV.getInstance();
	final IGVPreferences prefMgr = PreferencesManager.getPreferences();

	// Next feature
	final KeyStroke nextKey = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK, false);
	final Action nextAction = new EnableWrappedAction(new AbstractAction() {
	    public void actionPerformed(ActionEvent e) {
		nextFeature(true);
	    }
	});
	inputMap.put(nextKey, "nextFeature");
	actionMap.put("nextFeature", nextAction);

	// Previous feature
	final KeyStroke prevKey = KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_MASK, false);
	final Action prevAction = new EnableWrappedAction(new AbstractAction() {

	    public void actionPerformed(ActionEvent e) {
		nextFeature(false);
	    }
	});
	inputMap.put(prevKey, "prevFeature");
	actionMap.put("prevFeature", prevAction);

	// Next exon
	final KeyStroke nextExonKey = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK + KeyEvent.SHIFT_MASK,
		false);
	final Action nextExonAction = new EnableWrappedAction(new AbstractAction() {
	    public void actionPerformed(ActionEvent e) {
		nextExon(true);
	    }
	});
	inputMap.put(nextExonKey, "nextExon");
	actionMap.put("nextExon", nextExonAction);

	// Previous exon
	final KeyStroke prevExonKey = KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_MASK + KeyEvent.SHIFT_MASK,
		false);
	final Action prevExonAction = new EnableWrappedAction(new AbstractAction() {

	    public void actionPerformed(ActionEvent e) {
		nextExon(false);
	    }
	});
	inputMap.put(prevExonKey, "prevExon");
	actionMap.put("prevExon", prevExonAction);

	// Set the focus to the "search" box
	final KeyStroke searchBoxKeyCtrl = KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_MASK, false);
	final KeyStroke searchBoxKeyMeta = KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.META_MASK, false);
	final Action searchBoxAction = new EnableWrappedAction(new AbstractAction() {
	    public void actionPerformed(ActionEvent e) {
		igv.focusSearchBox();
	    };
	});
	inputMap.put(searchBoxKeyCtrl, "focusSearch");
	inputMap.put(searchBoxKeyMeta, "focusSearch");
	actionMap.put("focusSearch", searchBoxAction);

	// Show extras menu
	final KeyStroke extrasKey = KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.ALT_MASK, false);
	final Action extrasAction = new EnableWrappedAction(new AbstractAction() {
	    public void actionPerformed(ActionEvent e) {
		igv.enableExtrasMenu();
	    }
	});
	inputMap.put(extrasKey, "tools");
	actionMap.put("tools", extrasAction);

	// // Create region-on-interest
	// final KeyStroke regionKey = KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK, false);
	// final Action regionAction = new EnableWrappedAction(new AbstractAction() {
	//
	// public void actionPerformed(ActionEvent e) {
	// if (FrameManager.isGeneListMode()) {
	// return;
	// }
	// Range currentRange = FrameManager.getDefaultFrame().getCurrentRange();
	// RegionOfInterest regionOfInterest = new RegionOfInterest(currentRange.getChr(), currentRange.getStart(),
	// currentRange.getEnd(), null);
	// igv.addRegionOfInterest(regionOfInterest);
	// }
	// });
	// inputMap.put(regionKey, "region");
	// actionMap.put("region", regionAction);

	// RELOAD GIE TRACK
	final KeyStroke regionKey2 = KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK + KeyEvent.ALT_MASK,
		false);
	final Action regionAction2 = new EnableWrappedAction(new AbstractAction() {

	    public void actionPerformed(ActionEvent e) {
		if (FrameManager.isGeneListMode()) {
		    return;
		}
		GIE.getInstance().reloadActiveDataset();
	    }
	});
	inputMap.put(regionKey2, "region2");
	actionMap.put("region2", regionAction2);

	// // Create region-of-interest at center of view (1 bp wide)
	// final KeyStroke regionCenterKey = KeyStroke.getKeyStroke(KeyEvent.VK_R,
	// KeyEvent.CTRL_MASK + KeyEvent.SHIFT_MASK, false);
	// final Action regionCenterAction = new EnableWrappedAction(new AbstractAction() {
	//
	// public void actionPerformed(ActionEvent e) {
	// if (FrameManager.isGeneListMode()) {
	// return;
	// }
	// int center = (int) FrameManager.getDefaultFrame().getCenter();
	// RegionOfInterest regionOfInterest = new RegionOfInterest(FrameManager.getDefaultFrame().getChrName(),
	// center, center + 1, null);
	// igv.addRegionOfInterest(regionOfInterest);
	// }
	// });
	// inputMap.put(regionCenterKey, "regionCenter");
	// actionMap.put("regionCenter", regionCenterAction);

	// Sort alignments
	final KeyStroke sortByLastKey = KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, false);
	final Action sorAlignmentTracksAction = new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		String sortOptionString = prefMgr.get(SAM_SORT_OPTION);
		if (sortOptionString != null) {
		    try {
			AlignmentTrack.SortOption option = AlignmentTrack.SortOption.valueOf(sortOptionString);
			String lastSortTag = prefMgr.get(SAM_SORT_BY_TAG);

			igv.sortAlignmentTracks(option, lastSortTag);
			igv.revalidateTrackPanels();
		    } catch (IllegalArgumentException e1) {
			log.error("Unrecognized sort option: " + sortOptionString);
		    }
		}
	    }
	};
	inputMap.put(sortByLastKey, "sortByLast");
	actionMap.put("sortByLast", sorAlignmentTracksAction);

	// Open scatter plot
	final KeyStroke scatterplotKey = KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK, false);
	final Action scatterplotAction = new AbstractAction() {

	    public void actionPerformed(ActionEvent e) {
		if (ScatterPlotUtils.hasPlottableTracks()) {
		    ReferenceFrame defaultFrame = FrameManager.getDefaultFrame();
		    String chr = defaultFrame.getChrName();
		    int start = (int) defaultFrame.getOrigin();
		    int end = (int) defaultFrame.getEnd();
		    int zoom = defaultFrame.getZoom();
		    ScatterPlotUtils.openPlot(chr, start, end, zoom);
		}
	    }
	};
	inputMap.put(scatterplotKey, "scatterPlot");
	actionMap.put("scatterPlot", scatterplotAction);

	// Back button
	final KeyStroke backKey1 = KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, KeyEvent.META_DOWN_MASK, false);
	final KeyStroke backKey2 = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK, false);
	final Action backAction = new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		igv.getSession().getHistory().back();
	    }
	};
	inputMap.put(backKey1, "back");

	inputMap.put(backKey2, "back");
	actionMap.put("back", backAction);

	// Forward button
	final KeyStroke forwardKey1 = KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, KeyEvent.META_DOWN_MASK, false);
	final KeyStroke forwardKey2 = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK, false);
	final Action forwardAction = new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		igv.getSession().getHistory().forward();
	    }
	};
	inputMap.put(forwardKey1, "forward");
	inputMap.put(forwardKey2, "forward");

	// Toggle alignment "complete read only" option
	final KeyStroke completeReadKey = KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.CTRL_DOWN_MASK, false);
	final Action completeReadAction = new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		boolean currentSetting = prefMgr.getAsBoolean(SAM_COMPLETE_READS_ONLY);
		prefMgr.put(SAM_COMPLETE_READS_ONLY, !currentSetting);
		igv.revalidateTrackPanels();
	    }
	};
	inputMap.put(completeReadKey, "completeReads");
	actionMap.put("completeReads", completeReadAction);

	// add roi
	final KeyStroke addROIKey1 = KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK + KeyEvent.ALT_MASK,
		false);
	final KeyStroke addROIKey2 = KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK + KeyEvent.SHIFT_MASK,
		false);
	final Action addROIAction = new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		IGV.getInstance().getContentPane().getCommandBar().getRoiToggleButton().doClick();
	    }
	};
	inputMap.put(addROIKey1, "addROI");
	inputMap.put(addROIKey2, "addROI");
	actionMap.put("addROI", addROIAction);

	// clip roi
	final KeyStroke clipROIKey1 = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK + KeyEvent.ALT_MASK,
		false);
	final KeyStroke clipROIKey2 = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK + KeyEvent.SHIFT_MASK,
		false);
	final Action clipROIAction = new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		if (GIEDataDialog.getInstance() != null && KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.getFocusOwner() == GIEDataDialog.getInstance().getTable()) {
		    GIEDataDialog.getInstance().copyRows();
		} else {
		    IGV.getInstance().getContentPane().getCommandBar().getRoiClipToggleButton().doClick();

		}
	    }
	};
	inputMap.put(clipROIKey1, "clipROI");
	inputMap.put(clipROIKey2, "clipROI");
	actionMap.put("clipROI", clipROIAction);

	// merge roi
	final KeyStroke mergeROIKey1 = KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK + KeyEvent.ALT_MASK,
		false);
	final KeyStroke mergeROIKey2 = KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK + KeyEvent.SHIFT_MASK,
		false);
	final Action mergeROIAction = new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		List<RegionOfInterest> selectedRegions = new ArrayList<>();
		if (GIEDataDialog.getInstance() != null && KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.getFocusOwner() == GIEDataDialog.getInstance().getTable()) {
		    // merge selected rows
		    selectedRegions = GIEDataDialog.getInstance().getSelectedRegions();
		    GIEDataDialog.getInstance().mergeRegions(selectedRegions);
		    if (RegionNavigatorDialog.activeInstance != null)
			RegionNavigatorDialog.activeInstance.synchRegions();
		    IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();
		    IGV.getInstance().revalidateTrackPanels();
		} else {
		    IGV.getInstance().getContentPane().getCommandBar().getRoiMergeToggleButton().doClick();
		}

	    }
	};
	inputMap.put(mergeROIKey1, "mergeROI");
	inputMap.put(mergeROIKey2, "mergeROI");
	actionMap.put("mergeROI", mergeROIAction);

	// delete roi
	final KeyStroke delROIKey1 = KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_MASK,
		false);
	final KeyStroke delROIKey2 = KeyStroke.getKeyStroke(KeyEvent.VK_D,
		KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_MASK, false);
	final Action delROIAction = new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		List<RegionOfInterest> selectedRegions = new ArrayList<>();
		if (GIEDataDialog.getInstance() != null && KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.getFocusOwner() == GIEDataDialog.getInstance().getTable()) {
		    // del selected rows
		    selectedRegions = GIEDataDialog.getInstance().getSelectedRegions();
		} else {
		    // del visible regions
		    Range visible = IGV.getInstance().getSession().getReferenceFrame().getCurrentRange();
		    for (RegionOfInterest r : IGV.getInstance().getSession().getAllRegionsOfInterest())
			if (visible.overlaps(r.getRange()))
			    selectedRegions.add(r);
		}
		GIEDataDialog.getInstance().deleteRegions(selectedRegions);

		if (RegionNavigatorDialog.activeInstance != null)
		    RegionNavigatorDialog.activeInstance.synchRegions();
		IGV.getInstance().getSession().getRegionsOfInterestObservable().setChangedAndNotify();
		IGV.getInstance().revalidateTrackPanels();
	    }
	};
	inputMap.put(delROIKey1, "delROI");
	inputMap.put(delROIKey2, "delROI");
	actionMap.put("delROI", delROIAction);

	// select a layer
	for (int i = 1; i <= 9; i++) {
	    int ke = 0;
	    switch (i) {
	    case 1:
		ke = KeyEvent.VK_1;
		break;
	    case 2:
		ke = KeyEvent.VK_2;
		break;
	    case 3:
		ke = KeyEvent.VK_3;
		break;
	    case 4:
		ke = KeyEvent.VK_4;
		break;
	    case 5:
		ke = KeyEvent.VK_5;
		break;
	    case 6:
		ke = KeyEvent.VK_6;
		break;
	    case 7:
		ke = KeyEvent.VK_7;
		break;
	    case 8:
		ke = KeyEvent.VK_8;
		break;
	    case 9:
		ke = KeyEvent.VK_9;
		break;
	    default:
	    }
	    final KeyStroke selectLayerKey = KeyStroke.getKeyStroke(ke, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_MASK,
		    false);
	    final Action selectLayerAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
		    if (GIE.getInstance().getActiveDataset() == null)
			return;
		    int layernum = Integer.parseInt(e.getActionCommand().substring("select layer ".length()));
		    List<String> lns = new ArrayList<>();
		    lns.addAll(GIE.getInstance().getActiveDataset().getCurrentVersion().getLayers().keySet());
		    if (layernum <= lns.size()) {
			String current = GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer()
				.getLayerName();
			String lname = lns.get(layernum - 1);
			if (current != lname) {
			    System.out.println("Select layer " + layernum + " : " + lname);
			    GIE.getInstance().getActiveDataset().getCurrentVersion().setActiveLayer(lname);
			    if (GIEDataDialog.getInstance() != null)
				GIEDataDialog.getInstance().refresh();
			    IGV.getInstance().repaintNamePanels();
			}
		    }
		}
	    };
	    inputMap.put(selectLayerKey, "select layer " + i);
	    actionMap.put("select layer " + i, selectLayerAction);
	}

	// UNDO
	final KeyStroke undoKey = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK, false);
	final Action undoAction = new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		IGV.getInstance().getSession().replaceRegionsOfInterest(UndoHandler.getInstance().undo());
	    }
	};
	inputMap.put(undoKey, "undo");
	actionMap.put("undo", undoAction);

	
	// Show/Hide reference lines
	final KeyStroke toggleRefLineKey = KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK, false);
	final Action toggleRefLineAction = new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		GIE.getInstance().toggleRefLines();
		System.out.println("Show lines: " + GIE.getInstance().isShowRefLines());
		IGV.getInstance().getMainPanel().repaint();
	    }
	};
	inputMap.put(toggleRefLineKey, "toggleRefLine");
	actionMap.put("toggleRefLine", toggleRefLineAction);

	
    }

    /**
     * Move to the next exon in the feature located at the center, if:
     * -there is such a feature
     * -the track is expanded
     * -a single feature row is selected
     * -the feature has multiple exons
     * -there is an exon forward or backward to jump to
     *
     * @param forward
     */
    private void nextExon(boolean forward) {

	// Ignore (Disable) if we are in gene list mode
	if (FrameManager.isGeneListMode()) {
	    return;
	}

	ReferenceFrame vc = FrameManager.getDefaultFrame();
	Collection<Track> tracks = IGV.getInstance().getSelectedTracks();
	if (tracks.size() != 1 && GIE.getInstance() != null && GIE.getInstance().getLayerTrack() != null) {
	    tracks = new ArrayList<Track>();
	    tracks.add(GIE.getInstance().getLayerTrack());
	}

	if (tracks.size() == 1) {
	    Track t = tracks.iterator().next();
	    if (!(t instanceof FeatureTrack)) {
		// JOptionPane.showMessageDialog(IGV.getInstance(),
		// "Track panning is not enabled for data tracks.");
		return;
	    }

	    Exon e = null;
	    if (t instanceof FeatureTrack) {
		int center = (int) vc.getCenter();
		FeatureTrack ft = (FeatureTrack) t;
		if (ft.getDisplayMode() == Track.DisplayMode.COLLAPSED
			|| ft.getSelectedFeatureRowIndex() == FeatureTrack.NO_FEATURE_ROW_SELECTED) {
		    MessageUtils.showMessage("Exon navigation is only allowed when track is expanded and a single "
			    + "feature row is selected.");
		    return;
		}
		List<Feature> featureList = ft.getFeaturesAtPositionInFeatureRow(center,
			ft.getSelectedFeatureRowIndex(), vc);
		Feature feature = featureList != null && featureList.size() > 0 ? featureList.get(0) : null;

		if (feature == null)
		    return;
		if (feature instanceof BasicFeature) {
		    BasicFeature bf = (BasicFeature) feature;
		    java.util.List<Exon> exons = bf.getExons();
		    if (exons == null || exons.isEmpty()) {
			MessageUtils.showMessage("At least one centered feature does not have exon structure");
			return;
		    }

		    if (forward) {
			for (Exon exon : bf.getExons()) {
			    // the "+ 1" here is necessary because the rounding in the recentering method
			    // sometimes places the center one base off. This should be perfectly safe,
			    // but it does assume no one's abusing the exon datastructure and creating
			    // exons that are right next to each other.
			    if (exon.getStart() > vc.getCenter() + 1) {
				e = exon;
				break;
			    }
			}
		    } else {
			for (int i = exons.size() - 1; i >= 0; i--) {
			    Exon exon = exons.get(i);
			    if (exon.getEnd() < vc.getCenter()) {
				e = exon;
				break;
			    }
			}
		    }
		}

		if (e != null) {
		    vc.centerOnLocation(forward ? e.getStart() : e.getEnd());
		    int i = 3;
		}

	    }
	    // todo: implement handling for VariantTrack

	} else {
	    MessageUtils.showMessage("To use track panning you must first select a single feature track.");
	}
    }

    /**
     * Skip to the next feature in the selected track.
     *
     * @param forward
     *            the direction, true for forward and false for back
     */
    private void nextFeature(boolean forward) {

	// Ignore (Disable) if we are in gene list mode
	if (FrameManager.isGeneListMode()) {
	    return;
	}

	ReferenceFrame frame = FrameManager.getDefaultFrame();
	Collection<Track> tracks = IGV.getInstance().getSelectedTracks();
	if (tracks.size() != 1 && GIE.getInstance() != null && GIE.getInstance().getLayerTrack() != null) {
	    tracks = new ArrayList<Track>();
	    tracks.add(GIE.getInstance().getLayerTrack());
	}
	if (tracks.size() == 1) {
	    try {
		Track t = tracks.iterator().next();
		if (!(t instanceof FeatureTrack || t instanceof VariantTrack)) {
		    // JOptionPane.showMessageDialog(IGV.getInstance(),
		    // "Track panning is not enabled for data tracks.");
		    return;
		}

		Feature f = null;
		if (t instanceof FeatureTrack) {
		    f = ((FeatureTrack) t).nextFeature(frame.getChrName(), frame.getCenter(), forward, frame);
		} else if (t instanceof VariantTrack) {
		    f = ((VariantTrack) t).nextFeature(frame.getChrName(), frame.getCenter(), forward, frame);
		}

		if (f != null) {
		    String chr = GenomeManager.getInstance().getCurrentGenome().getCanonicalChrName(f.getChr());
		    double newCenter = f.getStart();
		    if (!chr.equals(frame.getChrName())) {
			// Switch chromosomes. We have to do some tricks to maintain the same resolution scale.
			double range = frame.getEnd() - frame.getOrigin();
			int newOrigin = (int) Math.max(newCenter - range / 2, 0);
			int newEnd = (int) (newOrigin + range);
			frame.jumpTo(chr, newOrigin, newEnd);
		    } else {
			frame.centerOnLocation(newCenter);
		    }
		}
	    } catch (IOException e) {
		MessageUtils.showErrorMessage("Error encountered reading features: " + e.getMessage(), e);

	    }
	} else {
	    MessageUtils.showMessage("To use track panning you must first select a single feature track.");
	}

    }

    /**
     * TODO I'm actually pretty sure this class doesn't do what it's intended to do,
     * but I just refactored it to condense code, there were no functional changes.
     * -JS
     */
    private class EnableWrappedAction extends AbstractAction {

	private Action action;

	private EnableWrappedAction(Action action) {
	    this.action = action;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    setEnabled(false); // stop any other events from interfering
	    this.action.actionPerformed(e);
	    setEnabled(true);

	}
    }

}
