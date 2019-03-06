package at.ccri.varan.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.util.Interval;
import org.broad.igv.util.IntervalTree;

/**
 * 
 * @author niko.popitsch
 *
 */
public class RegionOfInterestTree {

    private Map<String, IntervalTree<RegionOfInterest>> trees = new HashMap<>();

    public RegionOfInterestTree() {
    }

    public RegionOfInterestTree(List<RegionOfInterest> rois) {
	insert(rois);
    }

    private IntervalTree<RegionOfInterest> getTree(String c) {
	IntervalTree<RegionOfInterest> t = trees.get(c);
	if (t == null)
	    t = new IntervalTree<RegionOfInterest>();
	return t;
    }

    public void insert(Collection<RegionOfInterest> rois) {
	Iterator<RegionOfInterest> it = rois.iterator();
	while (it.hasNext())
	    insert(it.next());
    }
    
    public Set<String> getChromosomes() {
	return trees.keySet();
    }

    public void insert(RegionOfInterest r) {
	IntervalTree<RegionOfInterest> t = getTree(r.getChr());
	t.insert(new Interval<RegionOfInterest>(r.getStart(), r.getEnd(), r));
	trees.put(r.getChr(), t);
    }
    
    public List<Interval<RegionOfInterest>> queryOverlapping(RegionOfInterest r) {
	return getTree(r.getChr()).findOverlapping(r.getStart(), r.getEnd());
    }

    public List<Interval<RegionOfInterest>> getIntervals(String c) {
	return getTree(c).getIntervals();
    }

}
