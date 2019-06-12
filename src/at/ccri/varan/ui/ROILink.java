package at.ccri.varan.ui;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.broad.igv.feature.RegionOfInterest;

/**
 * Links two regions of interest (e.g.,
 * 
 * @author niko.popitsch
 *
 */
public class ROILink {

    public enum TYPE {
	FUSION
    };

    private RegionOfInterest source;
    private RegionOfInterest target;
    private TYPE type;

    public ROILink(RegionOfInterest source, RegionOfInterest target, TYPE type) {
	this.source = source;
	this.target = target;
	this.type = type;
    }

    public RegionOfInterest getSource() {
	return source;
    }

    public void setSource(RegionOfInterest source) {
	this.source = source;
    }

    public RegionOfInterest getTarget() {
	return target;
    }

    public void setTarget(RegionOfInterest target) {
	this.target = target;
    }

    public TYPE getType() {
	return type;
    }

    public void setType(TYPE type) {
	this.type = type;
    }

    @Override
    public boolean equals(Object o) {
	if (o instanceof ROILink) {
	    ROILink rl = (ROILink) o;
	    return getSource().equals(rl.getSource()) && getTarget().equals(rl.getTarget())
		    && getType().equals(rl.getType());
	}
	return false;
    }

    @Override
    public int hashCode() {
	return new HashCodeBuilder(17, 31).append(source).append(target).append(type).toHashCode();
    }

    @Override
    public String toString() {
	return "[" + source + " => " + target + " : " + type.name() + "]";
    }

    
    // public static void main(String[] args) {
    // Set<ROILink> s = new HashSet<>();
    // s.add(new ROILink(new RegionOfInterest("1", 100, 1000, "A"), new RegionOfInterest("2", 100, 1000, "B"), TYPE.FUSION));
    // s.add(new ROILink(new RegionOfInterest("1", 100, 1000, "A"), new RegionOfInterest("2", 100, 1000, "B"), TYPE.FUSION));
    // System.out.println(s);
    // }
    //
    
}
