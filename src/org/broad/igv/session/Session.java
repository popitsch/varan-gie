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

package org.broad.igv.session;

import static org.broad.igv.prefs.Constants.COLOR_MUTATIONS;
import static org.broad.igv.prefs.Constants.OVERLAY_ATTRIBUTE_KEY;
import static org.broad.igv.prefs.Constants.OVERLAY_MUTATION_TRACKS;
import static org.broad.igv.prefs.Constants.SHOW_DEFAULT_TRACK_ATTRIBUTES;
import static org.broad.igv.prefs.Constants.TRACK_ATTRIBUTE_NAME_KEY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.event.IGVEventBus;
import org.broad.igv.event.IGVEventObserver;
import org.broad.igv.event.ViewChange;
import org.broad.igv.feature.Range;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.lists.GeneList;
import org.broad.igv.prefs.IGVPreferences;
import org.broad.igv.prefs.PreferencesManager;
import org.broad.igv.renderer.ContinuousColorScale;
import org.broad.igv.sam.InsertionManager;
import org.broad.igv.track.AttributeManager;
import org.broad.igv.track.TrackType;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.TrackFilter;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.util.ObservableForObject;

import at.ccri.varan.ui.CanonicalChromsomeComparator;
import at.ccri.varan.ui.GIEDataDialog;

/**
 * @author eflakes
 */
public class Session implements IGVEventObserver {

    private static Logger log = Logger.getLogger(Session.class);

    // This doesn't mean genelist or not, the same way it does in FrameManager
    public enum GeneListMode {
	NORMAL, CURSOR
    }

    private int version;
    private String path;
    private String groupTracksBy;
    public boolean expandInsertions = false; // false;
    private int nextAutoscaleGroup;
    private ReferenceFrame referenceFrame = FrameManager.getDefaultFrame();
    private TrackFilter filter;
    private HashMap<String, String> preferences;
    private HashMap<TrackType, ContinuousColorScale> colorScales;
    private boolean removeEmptyPanels = false;
    double[] dividerFractions = null;

    private History history;

    /**
     * Map of chromosome -> regions of interest
     */
    private Map<String, List<RegionOfInterest>> regionsOfInterest = new LinkedHashMap<>();

    // An Observable that notifies observers of changes to the regions of interest. Its
    // setChangedAndNotify() method should be called after any regions change.
    private ObservableForObject<Map<String, List<RegionOfInterest>>> regionsOfInterestObservable = new ObservableForObject<>(
	    regionsOfInterest);

    private GeneList currentGeneList;
    private GeneListMode geneListMode = GeneListMode.NORMAL;
    private Set<String> hiddenAttributes;
    private String locus;

    public Session(String path) {
	log.debug("New session");
	reset(path);
    }

    public void reset(String path) {

	this.path = path;
	this.nextAutoscaleGroup = 1;
	// regionsOfInterest = new LinkedHashMap<>();
	// regionsOfInterestObservable = new ObservableForObject<>(regionsOfInterest);

	preferences = new HashMap<>();
	colorScales = new HashMap<>();
	history = new History(100);

	boolean resetRequired = FrameManager.getFrames().size() > 1;
	setCurrentGeneList(null);
	if (resetRequired) {
	    IGV.getInstance().resetFrames();
	}

	InsertionManager.getInstance().clear();
	IGVEventBus.getInstance().subscribe(ViewChange.class, this);
    }

    public void receiveEvent(Object event) {
	if (event instanceof ViewChange) {
	    ViewChange e = (ViewChange) event;
	    if (e.recordHistory()) {
		recordHistory();
	    }
	} else {
	    log.info("Unknown event type: " + event.getClass());
	}
    }

    public void clearDividerLocations() {
	dividerFractions = null;
    }

    public void setDividerFractions(double[] divs) {
	this.dividerFractions = divs;
    }

    public double[] getDividerFractions() {
	return dividerFractions;
    }

    public void recordHistory() {
	final ReferenceFrame defaultFrame = FrameManager.getDefaultFrame();
	history.push(defaultFrame.getFormattedLocusString(), defaultFrame.getZoom());
    }

    public void clearHistory() {
	history.clear();
    }

    public String getSessionVersion() {
	return String.valueOf(version);
    }

    /**
     * @return the absolute path to the file associated with this session
     */
    public String getPath() {
	return path;
    }

    public void setPreference(String key, String value) {
	preferences.put(key, value);
    }

    public void setColorScale(TrackType trackType, ContinuousColorScale colorScale) {
	colorScales.put(trackType, colorScale);
    }

    public ContinuousColorScale getColorScale(TrackType trackType) {
	if (colorScales.containsKey(trackType)) {
	    return colorScales.get(trackType);
	} else {
	    return PreferencesManager.getPreferences().getColorScale(trackType);
	}
    }

    public boolean getPreferenceAsBoolean(String key) {
	if (preferences.containsKey(key)) {
	    try {
		return Boolean.parseBoolean(preferences.get(key));
	    } catch (Exception e) {
		log.error("Error converting boolean preference " + key + "=" + preferences.get(key));
	    }
	}
	return PreferencesManager.getPreferences().getAsBoolean(key);

    }

    public String getPreference(String key) {
	if (preferences.containsKey(key)) {
	    return (preferences.get(key));
	} else {
	    return PreferencesManager.getPreferences().get(key);
	}
    }

    /**
     * @param key
     * @param def
     *            Default value. Not saved
     * @return Preference if found, or else default value
     * @see IGVPreferences#getPersistent(String, String)
     */
    public String getPersistent(String key, String def) {
	if (preferences.containsKey(key)) {
	    return (preferences.get(key));
	} else {
	    return PreferencesManager.getPreferences().getPersistent(key, def);
	}
    }

    public boolean getOverlayMutationTracks() {
	final String key = OVERLAY_MUTATION_TRACKS;
	if (preferences.containsKey(key)) {
	    try {
		return Boolean.parseBoolean(preferences.get(key));
	    } catch (Exception e) {
		log.error("Error converting boolean preference " + key + "=" + preferences.get(key));
	    }
	}
	return PreferencesManager.getPreferences().getAsBoolean(OVERLAY_MUTATION_TRACKS);

    }

    public boolean getColorOverlay() {
	final String key = COLOR_MUTATIONS;
	if (preferences.containsKey(key)) {
	    try {
		return Boolean.parseBoolean(preferences.get(key));
	    } catch (Exception e) {
		log.error("Error converting boolean preference " + key + "=" + preferences.get(key));
	    }
	}
	return PreferencesManager.getPreferences().getAsBoolean(COLOR_MUTATIONS);

    }

    public String getOverlayAttribute() {
	final String key = OVERLAY_ATTRIBUTE_KEY;
	if (preferences.containsKey(key)) {
	    return preferences.get(key);

	}
	return PreferencesManager.getPreferences().get(OVERLAY_ATTRIBUTE_KEY);
    }

    public String getTrackAttributeName() {
	final String key = TRACK_ATTRIBUTE_NAME_KEY;
	if (preferences.containsKey(key)) {
	    return preferences.get(key);

	}
	return PreferencesManager.getPreferences().get(TRACK_ATTRIBUTE_NAME_KEY);
    }

    public String getLocusString() {
	if (getReferenceFrame().getChrName().equals(Globals.CHR_ALL)) {
	    return Globals.CHR_ALL;
	}
	Range range = getReferenceFrame().getCurrentRange();
	String startStr = String.valueOf(range.getStart());
	String endStr = String.valueOf(range.getEnd());
	String position = range.getChr() + ":" + startStr + "-" + endStr;
	return position;
    }

    public void setLocus(String locus) {
	this.locus = locus;
    }

    public String getLocus() {
	return locus;
    }

    public TrackFilter getFilter() {
	return filter;
    }

    public void setFilter(TrackFilter filter) {
	this.filter = filter;
    }

    public String getGroupTracksBy() {
	return groupTracksBy;
    }

    public void setGroupTracksBy(String groupTracksBy) {
	this.groupTracksBy = groupTracksBy;
    }

    public ReferenceFrame getReferenceFrame() {
	return referenceFrame;
    }

    /**
     * WARNING! This method should never be used for update of the regions collection.
     * If this gets abused, this method could be changed to create a new Collection so that updates
     * have no effect on the real one.
     *
     * @param chr
     * @return
     */
    public Collection<RegionOfInterest> getRegionsOfInterest(String chr) {
	List<RegionOfInterest> roiList = null;
	if (chr.equals(Globals.CHR_ALL)) {
	    return getAllRegionsOfInterest();
	} else {
	    roiList = (ArrayList<RegionOfInterest>) regionsOfInterest.get(chr);
	}
	return roiList;
    }

    public Collection<RegionOfInterest> getAllRegionsOfInterest() {
	ArrayList<RegionOfInterest> roiList = new ArrayList<RegionOfInterest>();
	List<String> sortedChr = new ArrayList<String>(regionsOfInterest.keySet());
	Collections.sort(sortedChr, new CanonicalChromsomeComparator());
	for (String chr : sortedChr)
	    roiList.addAll(regionsOfInterest.get(chr));
	return roiList;
    }

    /**
     * Removes the regions of interest. returns global success/failure.
     * This method to remove multiple regions at once exists because, if you do them one at a time,
     * it throws the RegionNavigatorDialog table rows off.
     *
     * @param rois
     * @return whether all specified regions were found for removal
     */
    public boolean removeROI(RegionOfInterest roi) {
	boolean result = true;
	Collection<RegionOfInterest> roiList = regionsOfInterest.get(roi.getChr());
	if (roiList != null) {
	    result = result && roiList.remove(roi);
	}
	// notify all observers that regions have changed.
	regionsOfInterestObservable.setChangedAndNotify();
	return result;
    }

    /**
     * Removes the regions of interest. returns global success/failure.
     * This method to remove multiple regions at once exists because, if you do them one at a time,
     * it throws the RegionNavigatorDialog table rows off.
     *
     * @param rois
     * @return whether all specified regions were found for removal
     */
    public boolean removeROI(Collection<RegionOfInterest> rois) {
	boolean result = true;

	for (RegionOfInterest roi : rois) {
	    Collection<RegionOfInterest> roiList = regionsOfInterest.get(roi.getChr());
	    if (roiList != null) {
		result = result && roiList.remove(roi);
	    }
	}

	// notify all observers that regions have changed.
	regionsOfInterestObservable.setChangedAndNotify();
	return result;
    }

    public void mergeROI(RegionOfInterest newRoi, boolean informListeners) {
	SortedSet<RegionOfInterest> sortedOverlapping = new TreeSet<>();
	String chr = newRoi.getChr();
	List<RegionOfInterest> roiList = regionsOfInterest.get(chr);
	if (roiList != null)
	    for (RegionOfInterest r : roiList)
		if (r.getRange().overlaps(newRoi.getRange()))
		    sortedOverlapping.add(r);

	if (sortedOverlapping.size() <= 0)
	    return;
	RegionOfInterest merge = new RegionOfInterest(chr, sortedOverlapping.first().getStart(),
		sortedOverlapping.last().getEnd(), sortedOverlapping.first().getDescription());
	addROI(merge, false, informListeners);
    }

    /**
     * Add a roi. GIE split functionality added.
     * 
     * @param newRoi
     */
    public void addROI(RegionOfInterest newRoi, boolean clipRegion, boolean informListeners) {
	String chr = newRoi.getChr();
	List<RegionOfInterest> roiList = regionsOfInterest.get(chr);
	if (roiList == null) {
	    roiList = new ArrayList<RegionOfInterest>();
	    regionsOfInterest.put(chr, roiList);
	}

	// if (mergeWithOverlap) {
	// // ---------------------------------------------
	// // find overlapping ROIs
	// // ---------------------------------------------
	// List<RegionOfInterest> tomerge = new ArrayList<RegionOfInterest>();
	// for (RegionOfInterest oldRoi : roiList) {
	// if (oldRoi.getRange().overlaps(newRoi.getRange())) {
	// tomerge.add(oldRoi);
	// }
	// }
	// tomerge.add(newRoi);
	//
	// // merge
	// if ( GIEDataDialog.getInstance() == null )
	// GIEDataDialog.getInstance(IGV.getMainFrame());
	//
	// GIEDataDialog.getInstance().mergeRegions(tomerge);
	// } else {

	// ---------------------------------------------
	// split overlapping intervals
	// ---------------------------------------------
	List<RegionOfInterest> todel = new ArrayList<RegionOfInterest>();
	List<RegionOfInterest> toadd = new ArrayList<RegionOfInterest>();
	for (RegionOfInterest oldRoi : roiList) {
	    if (oldRoi.getRange().overlaps(newRoi.getRange())) {
		// System.out.println("Merging intervals");
		if (newRoi.getRange().contains(oldRoi.getRange())) {
		    // remove contained smaller ROIs
		    // [---------------new-------] => [---------------new-------]
		    // ......[---old--]
		    todel.add(oldRoi);
		} else if (oldRoi.getRange().contains(newRoi.getRange())) {
		    // split ROI
		    // [---------------old-------] => [----l---][---new--][---r---]
		    // ......[---new--]
		    RegionOfInterest right = oldRoi.deepClone();
		    right.setStart(newRoi.getEnd());
		    if (right.getLength() > 0)
			toadd.add(right);

		    RegionOfInterest left = oldRoi.deepClone();
		    left.setEnd(newRoi.getStart());
		    if (left.getLength() > 0)
			toadd.add(left);

		    todel.add(oldRoi);
		} else {
		    if (oldRoi.getStart() < newRoi.getStart()) {
			// [-------old-------] => [----l---][---new--]
			// ...........[---new--]
			RegionOfInterest left = oldRoi.deepClone();
			left.setEnd(newRoi.getStart());
			toadd.add(left);

			todel.add(oldRoi);
		    } else {
			// .....[-------old-------] => [---new--][---r--]
			// [---new--]
			RegionOfInterest right = oldRoi.deepClone();
			right.setStart(newRoi.getEnd());
			toadd.add(right);
			todel.add(oldRoi);

		    }

		}

	    }
	}

	roiList.removeAll(todel);
	roiList.addAll(toadd);
	// add passed roi?
	if (!clipRegion)
	    roiList.add(newRoi);

	Collections.sort(roiList);

	// }

	// notify all observers that regions have changed.
	if (informListeners)
	    regionsOfInterestObservable.setChangedAndNotify();
    }

    public void replaceRegionsOfInterest(Collection<RegionOfInterest> rois) {
	if (rois == null)
	    return;
	regionsOfInterest = new HashMap<>();

	for (RegionOfInterest r : rois) {
	    List<RegionOfInterest> roiList = regionsOfInterest.get(r.getChr());
	    if (roiList == null) {
		roiList = new ArrayList<RegionOfInterest>();
	    }
	    roiList.add(r);
	    Collections.sort(roiList);
	    regionsOfInterest.put(r.getChr(), roiList);
	}

	// notify all observers that regions have changed.
	regionsOfInterestObservable.setChangedAndNotify();

	// repaint
	IGV.getInstance().revalidateTrackPanels();
    }

    public void clearRegionsOfInterest() {
	if (regionsOfInterest != null) {
	    regionsOfInterest.clear();
	}
	// notify all observers that regions have changed.
	regionsOfInterestObservable.setChangedAndNotify();
    }

    public void setPath(String path) {
	this.path = path;
    }

    public History getHistory() {
	return history;
    }

    public List<History.Entry> getAllHistory() {
	return getHistory().getAllHistory();
    }

    public GeneList getCurrentGeneList() {
	return currentGeneList;
    }

    public void setCurrentGeneList(GeneList currentGeneList) {

	boolean frameReset = (currentGeneList != null || FrameManager.isGeneListMode());
	this.currentGeneList = currentGeneList;

	if (frameReset) {
	    FrameManager.resetFrames(currentGeneList);
	}
    }

    public GeneListMode getGeneListMode() {
	return geneListMode;
    }

    public void setGeneListMode(GeneListMode geneListMode) {
	this.geneListMode = geneListMode;
    }

    /**
     * Add a gene to the current list. This is a transient change (not saved to disk)
     *
     * @param gene
     */
    public void addGene(String gene) {
	if (getCurrentGeneList() != null) {
	    getCurrentGeneList().add(gene);
	    setCurrentGeneList(getCurrentGeneList());
	}
    }

    public void sortGeneList(Comparator<String> comparator) {
	getCurrentGeneList().sort(comparator);
	this.setCurrentGeneList(getCurrentGeneList());
    }

    public int getVersion() {
	return version;
    }

    public void setVersion(int version) {
	this.version = version;
    }

    public void setNextAutoscaleGroup(int nextAutoscaleGroup) {
	this.nextAutoscaleGroup = nextAutoscaleGroup;
    }

    public int getNextAutoscaleGroup() {
	return this.nextAutoscaleGroup++;
    }

    public Set<String> getHiddenAttributes() {

	Set<String> extendedHiddenAttributes = new HashSet<String>();
	if (hiddenAttributes != null) {
	    extendedHiddenAttributes.addAll(hiddenAttributes);
	}
	if (!PreferencesManager.getPreferences().getAsBoolean(SHOW_DEFAULT_TRACK_ATTRIBUTES)) {

	    extendedHiddenAttributes.addAll(AttributeManager.defaultTrackAttributes);
	}

	return extendedHiddenAttributes;
    }

    public void setHiddenAttributes(Set<String> attributes) {
	this.hiddenAttributes = attributes;

    }

    public boolean isRemoveEmptyPanels() {
	return removeEmptyPanels;
    }

    public void setRemoveEmptyPanels(boolean removeEmptyPanels) {
	this.removeEmptyPanels = removeEmptyPanels;
    }

    static class Locus {
	String chr;
	int start;
	int end;

	Locus(String chr, int start, int end) {
	    this.chr = chr;
	    this.start = start;
	    this.end = end;
	}

    }

    /**
     * Return the start and end positions as a 2 element array for the input
     * position string. UCSC conventions are followed for coordinates,
     * specifically the internal representation is "zero" based (first base is
     * numbered 0) but the display representation is "one" based (first base is
     * numbered 1). Consequently 1 is substracted from the parsed positions
     */
    private static int[] getStartEnd(String posString) {
	try {
	    String[] posTokens = posString.split("-");
	    String startString = posTokens[0].replaceAll(",", "");
	    int start = Math.max(0, Integer.parseInt(startString)) - 1;

	    // Default value for end

	    int end = start + 1;
	    if (posTokens.length > 1) {
		String endString = posTokens[1].replaceAll(",", "");
		end = Integer.parseInt(endString);
	    }

	    if (posTokens.length == 1 || (end - start) < 10) {
		int center = (start + end) / 2;
		start = center - 20;
		end = center + 20;
	    } else {
		String endString = posTokens[1].replaceAll(",", "");

		// Add 1 bp to end position t make it "inclusive"
		end = Integer.parseInt(endString);
	    }

	    return new int[] { Math.min(start, end), Math.max(start, end) };
	} catch (NumberFormatException numberFormatException) {
	    return null;
	}

    }

    /**
     * Allows access to the Observable that notifies of changes to the regions of interest
     *
     * @return
     */
    public ObservableForObject<Map<String, List<RegionOfInterest>>> getRegionsOfInterestObservable() {
	return regionsOfInterestObservable;
    }
}
