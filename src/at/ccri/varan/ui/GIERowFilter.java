package at.ccri.varan.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.RowFilter;
import javax.swing.table.TableModel;

import org.broad.igv.Globals;
import org.broad.igv.feature.Range;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.ui.IGV;

/**
 * A row filter for the interval table
 * 
 * @author niko.popitsch
 *
 */
public class GIERowFilter extends RowFilter<TableModel, Object> {

    public enum SCOPE {
	GENOME, CHROMOSOME, VISIBLE
    }

    SCOPE scope = SCOPE.GENOME;

    Range currentlyVisible = null;
    boolean defGenome = true;

    /**
     * List of attribute filters
     */
    private List<GIEAttributeFilter> attributeFilters = new ArrayList<>();

    public GIERowFilter() {

    }

    public SCOPE getScope() {
	return scope;
    }

    public void setScope(SCOPE scope) {
	this.scope = scope;
    }

    /**
     * FIXME: too slow? 
     * @return true if the view change will change filter results.
     */
    public boolean updateCurrentlyVisible() {

	this.defGenome = GenomeManager.getInstance().getCurrentGenome().getId().equals(Globals.DEFAULT_GENOME);
	Range tmp = IGV.getInstance().getSession().getReferenceFrame().getCurrentRange();
	tmp = defGenome ? new Range(CanonicalChromsomeComparator.getCanonicalMappingHuman(tmp.getChr()), tmp.getStart(),
		tmp.getEnd()) : tmp;

	boolean needsRefresh = true;
	if (currentlyVisible == null)
	    needsRefresh = true;
	else {
	    switch (scope) {
	    case GENOME:
		needsRefresh = false;
		break;
	    case CHROMOSOME:
		if (currentlyVisible.getChr().equals(tmp.getChr()))
		    needsRefresh = false;
		break;
	    }
	}
	this.currentlyVisible = tmp;
	return needsRefresh;
    }

    @Override
    public boolean include(javax.swing.RowFilter.Entry<? extends TableModel, ? extends Object> entry) {
	if (currentlyVisible == null)
	    updateCurrentlyVisible();

	String chr = (String) entry.getValue(GIEDataDialog.COLIDX_Chr);
	if (defGenome)
	    chr = CanonicalChromsomeComparator.getCanonicalMappingHuman(chr);
	int start = (Integer) entry.getValue(GIEDataDialog.COLIDX_Start);
	int end = (Integer) entry.getValue(GIEDataDialog.COLIDX_End);
	Range test = new Range(chr, start, end);

	boolean ok = true;
	if (scope == SCOPE.CHROMOSOME) {
	    if (!currentlyVisible.getChr().equals(chr))
		ok = false;
	} else if (scope == SCOPE.CHROMOSOME || scope == SCOPE.VISIBLE) {
	    if (!currentlyVisible.overlaps(test))
		ok = false;
	}
	
	if ( ok ) {
	    // test individual filters
	    for ( GIEAttributeFilter f : attributeFilters ) {
		boolean af = f.filter(entry);
		if ( ! af ) {
		    ok = false;
		    break;
		}
	    }
	}

	// System.out.println("TEST " + test + " WITH " + visible + " / scope=" + scope + " : " + ok);

	return ok;
    }

    public void addAttributeFilter(String s) throws IOException {
	attributeFilters.add(GIEAttributeFilter.parseFromString(s));
    }

    public void delAttributeFilter(String s) throws IOException {
	attributeFilters.remove(GIEAttributeFilter.parseFromString(s));
    }
    
    public List<GIEAttributeFilter> getAttributeFilters() {
        return attributeFilters;
    }

    public void clearAttributeFilters() {
        this.attributeFilters.clear();
    }

}
