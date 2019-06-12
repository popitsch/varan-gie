package at.ccri.varan.util;

import java.util.ArrayList;
import java.util.Collection;
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
     * Test whether a feature list contains overlapping intervals
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
		    if (p != null)
			ret.add(p);
		    p = r;
		}

	    }
	}
	if (p != null)
	    ret.add(p);
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

    /**
     * Splits a list of overlapping intervals into individual (sorted) sets/layers.
     * 
     * @param reg
     * @return
     */
    public static List<List<RegionOfInterest>> splitOverlapping(List<RegionOfInterest> reg) {
	List<List<RegionOfInterest>> ret = new ArrayList<>();
	Collections.sort(reg);
	Iterator<RegionOfInterest> it = reg.iterator();
	ArrayList<RegionOfInterest> mainLayer = new ArrayList<RegionOfInterest>();
	// add first "layer"
	ret.add(mainLayer);

	while (it.hasNext()) {
	    RegionOfInterest r = it.next();
	    boolean added = false;
	    for (List<RegionOfInterest> layer : ret) {
		if (layer.size() == 0 || !layer.get(layer.size() - 1).overlaps(r)) {
		    layer.add(r);
		    added = true;
		    //System.out.println("Added to existing layer: " + r);
		    break;
		}
	    }
	    if (!added) {
		// need a new layer
		ArrayList<RegionOfInterest> newLayer = new ArrayList<RegionOfInterest>();
		newLayer.add(r);
		ret.add(newLayer);
		//System.out.println("Added to new layer " + r);
	    }
	}
	return ret;

    }

    /**
     * Test whether a ROI list contains overlapping intervals
     * 
     * @param reg
     * @return
     */
    public static boolean isOverlappingROI(List<RegionOfInterest> reg) {
	Iterator<RegionOfInterest> it = reg.iterator();
	RegionOfInterest p = null;
	while (it.hasNext()) {
	    RegionOfInterest r = it.next();
	    if (r.overlaps(p)) {
		return true;
	    } else {
		p = r;
	    }
	}
	return false;
    }

    public static boolean isOverlappingROI(List<RegionOfInterest> r1, List<RegionOfInterest> r2) {
	if (r1.size() == 0 || r2.size() == 0)
	    return false;
	Collections.sort(r1);
	Collections.sort(r2);
	Iterator<RegionOfInterest> i1 = r1.iterator();
	Iterator<RegionOfInterest> i2 = r2.iterator();
	RegionOfInterest c1 = i1.next();
	RegionOfInterest c2 = i2.next();
	boolean overlaps = false;
	boolean done = false;
	do {

	    while (!c1.overlaps(c2) && c1.compareTo(c2) < 0) {
		if (i1.hasNext())
		    c1 = i1.next();
		else {
		    done = true;
		    break;
		}
	    }
	    while (!c2.overlaps(c1) && c2.compareTo(c1) < 0) {
		if (i2.hasNext())
		    c2 = i2.next();
		else {
		    done = true;
		    break;
		}
	    }
	    // System.out.println(c1 + " vs " + c2 + " / " + i1.hasNext() + " / " + i2.hasNext() + " / "
	    // + ( c1.compareTo( c2 ) ) + " / " + (c2.compareTo( c1 ) ) + " = " + c1.overlaps(c2));
	    if (c1.overlaps(c2)) {
		// System.out.println(c1 + " overlaps " + c2);
		overlaps = true;
		break;
	    }

	} while ((i1.hasNext() || i2.hasNext()) && !done);
	return overlaps;
    }

//    //
//    public static void main(String[] args) {
//
//	// List<RegionOfInterest> r1 = new ArrayList<>();
//	// r1.add(new RegionOfInterest("1", 1, 100, "A"));
//	// r1.add(new RegionOfInterest("2", 1, 100, "B"));
//	// List<RegionOfInterest> r2 = new ArrayList<>();
//	// r2.add(new RegionOfInterest("2", 101, 150, "C"));
//	// r2.add(new RegionOfInterest("2", 200, 500, "C"));
//	// r2.add(new RegionOfInterest("2", 100, 200, "C"));
//	// System.out.println(isOverlappingROI(r2, r1));
//
//	List<RegionOfInterest> r1 = new ArrayList<>();
//	r1.add(new RegionOfInterest("1", 1, 100, "A"));
//	r1.add(new RegionOfInterest("2", 1, 100, "B"));
//	r1.add(new RegionOfInterest("1", 1, 100, "C"));
//	r1.add(new RegionOfInterest("3", 1, 100, "D"));
//
//	List<List<RegionOfInterest>> r = splitOverlapping(r1);
//	for (int i = 0; i < r.size(); i++) {
//	    System.out.println(i + " \t " + r.get(i));
//	}
//
//    }

}
