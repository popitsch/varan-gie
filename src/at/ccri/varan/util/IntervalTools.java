package at.ccri.varan.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.broad.igv.feature.IGVFeature;
import org.broad.igv.feature.Range;
import org.broad.igv.feature.RegionOfInterest;

import htsjdk.tribble.Feature;

/**
 * TODO: add additional interval set tools
 * 
 * @author niko.popitsch
 * 
 */
public class IntervalTools {

    private static RegionOfInterest mergeROI(RegionOfInterest a, RegionOfInterest b) {
	String description = a.getDescription().equals(b.getDescription()) ? a.getDescription()
		: a.getDescription() + "+" + b.getDescription();
	RegionOfInterest r = new RegionOfInterest(a.getChr(), Math.min(a.getStart(), b.getStart()),
		Math.max(a.getEnd(), b.getEnd()), description);
	return r;
    }

    /**
     * ASSERT p < r
     * 
     * @param p
     * @param r
     * @return
     */
    private static boolean overlaps(Feature p, Feature r) {
	if (p == null || r == null)
	    return false;
	if (!p.getContig().equals(r.getContig()))
	    return false;
	return Math.max(p.getStart(), r.getStart()) <= Math.min(p.getEnd(), r.getEnd());
    }

    /**
     * Theset whether a feature list contains ovelapping intervals
     * 
     * @param reg
     * @return
     */
    public static boolean isOverlapping(List<Feature> reg) {
	Iterator<Feature> it = reg.iterator();
	Feature p = null;
	while (it.hasNext()) {
	    Feature r = it.next();
	    if (overlaps(p, r)) {
		return true;
	    } else {
		p = r;
	    }
	}
	return false;
    }

    /**
     * Converts a list of features to ROIs. Overlapping features will be merged. the list is filtered by by the passed "visible" range if any.
     * 
     * @param fs
     * @param visible
     * @return
     */
    public static List<RegionOfInterest> feature2roi(List<Feature> fs, Range visible) {
	List<RegionOfInterest> ret = new ArrayList<>();
	RegionOfInterest p = null;
	for (Feature f : fs) {
	    RegionOfInterest r = new RegionOfInterest(f.getContig(), f.getStart(), f.getEnd(), null);
	    if (visible == null || r.getRange().overlaps(visible)) {
		if (f instanceof IGVFeature) {
		    IGVFeature igvf = (IGVFeature) f;
		    r.setDescription(igvf.getName());
		    if (!Double.isNaN(igvf.getScore()))
			r.setScore((int) igvf.getScore() + "");
		    r.setStrand(igvf.getStrand());
		}

		// merge?
		if (p != null && p.getRange().overlaps(r.getRange())) {
		    p = mergeROI(p, r);
		} else {
		    if ( p != null) ret.add(p);
		    p = r;
		}

	    }
	}
	if ( p != null) ret.add(p);
	return ret;
    }

    /**
     * Collapse a list of overlapping intervals. Overlapping intervals are merged.
     * 
     * @param reg
     * @return
     */
    public static List<RegionOfInterest> collapse(List<RegionOfInterest> reg) {
	List<RegionOfInterest> ret = new ArrayList<>();
	// sort by coordinates
	Collections.sort(reg);
	Iterator<RegionOfInterest> it = reg.iterator();
	RegionOfInterest p = null;
	while (it.hasNext()) {
	    RegionOfInterest r = it.next();
	    if (p != null && p.getRange().overlaps(r.getRange())) {
		p = mergeROI(p, r);
	    } else {
		ret.add(p);
		p = r;
	    }
	}
	if (p != null)
	    ret.add(p);
	return ret;
    }

}
