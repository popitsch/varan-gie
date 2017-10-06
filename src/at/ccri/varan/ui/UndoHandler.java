package at.ccri.varan.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.ui.util.ReorderableJList;

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

    Stack<List<RegionOfInterest>> redoAdded = new Stack<>();
    Stack<List<RegionOfInterest>> redoRemoved = new Stack<>();
    List<RegionOfInterest> lastRedoAdded = new ArrayList<>();
    List<RegionOfInterest> lastRedoRemoved = new ArrayList<>();
    
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
	    // clear redo buffeers unless last action was redo.
	    if ( ! (lastRedoAdded.equals(toadd) && lastRedoRemoved.equals(todel))) {
		redoAdded.clear();
		redoRemoved.clear();
	    }
	    
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

	redoAdded.push(todel);
	redoRemoved.push(toadd);

	return previous;
    }

    /**
     * redo a step.
     * 
     * @return
     */
    public List<RegionOfInterest> redo() {
	if (redoAdded.isEmpty())
	    return previous;
	
	List<RegionOfInterest> todel = redoRemoved.pop();
	List<RegionOfInterest> toadd = redoAdded.pop();

	lastRedoAdded = toadd;
	lastRedoRemoved = todel;
//	
//	
//	System.out.println("REDO " + todel + " / " + toadd + " / " + previous);
//	
	if (todel == null || toadd == null)
	    return null;

	previous.removeAll(todel);
	previous.addAll(toadd);

	
	added.push(toadd);
	removed.push(todel);
	
	
	return previous;
    }
    
    
    public void clear() {
	added.clear();
	removed.clear();
	redoAdded.clear();
	redoRemoved.clear();
	lastRedoAdded.clear();
	lastRedoRemoved.clear();
    }

    public boolean isEmpty() {
	return previous == null;
    }

    
    public static void main(String[] args) {
	 SortedSet<Date> backupSnapshots = new TreeSet<>();
	 backupSnapshots.add( new Date(2017, 8, 10));
	 backupSnapshots.add( new Date(2017, 9, 12));
	 backupSnapshots.add( new Date(2017, 9, 13));
	 
	 
	 // get latest date
	System.out.println( backupSnapshots.last() );
	 
	 System.out.println(backupSnapshots);
	 
    }
    
}
