package at.ccri.varan.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.broad.igv.feature.RegionOfInterest;

/**
 * undo handler singleton. Stores stacks of added/removed intervals.
 * 
 * @author niko.popitsch
 *
 */
public class UndoHandler {

    Stack<List<RegionOfInterest>> added = new Stack<>();
    Stack<List<RegionOfInterest>> removed = new Stack<>();
    List<RegionOfInterest> previous = null;

    private static UndoHandler instance;

    private UndoHandler() {
    }

    public static synchronized UndoHandler getInstance() {
	if (UndoHandler.instance == null) {
	    UndoHandler.instance = new UndoHandler();
	}
	return UndoHandler.instance;
    }

    public void addUndoStep(List<RegionOfInterest> now) {
	List<RegionOfInterest> toadd = new ArrayList<>();
	List<RegionOfInterest> todel = new ArrayList<>();

	if (previous == null) {
	    toadd = now;
	} else {
	    // calculate diff
	    for (RegionOfInterest r : now)
		if (!previous.contains(r))
		    toadd.add(r);
	    for (RegionOfInterest r : previous)
		if (!now.contains(r))
		    todel.add(r);
	}
	previous = now;
	if (toadd.size() > 0 || todel.size() > 0) {
	    added.add(toadd);
	    removed.add(todel);
//	     System.out.println("UNDO a[" + toadd + "] d[" + todel + "]");
	}
    }

    /**
     * undo a step.
     * 
     * @return
     */
    public List<RegionOfInterest> undo() {
	if (added.isEmpty())
	    return previous;

	List<RegionOfInterest> todel = added.pop();
	List<RegionOfInterest> toadd = removed.pop();

	if (todel == null || toadd == null)
	    return null;

	previous.removeAll(todel);
	previous.addAll(toadd);

	return previous;
    }

    public void clear() {
	added.clear();
	removed.clear();
    }

    public boolean isEmpty() {
	return previous == null;
    }

}
