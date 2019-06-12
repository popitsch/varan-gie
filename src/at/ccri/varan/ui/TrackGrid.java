package at.ccri.varan.ui;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Track grid configuration
 * 
 * TODO: add color chooser?
 * 
 * @author niko.popitsch
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
public class TrackGrid {

    @XmlAttribute
    float spacing = 0f;

    public TrackGrid() {
    }

    public TrackGrid(float f) {
	this.spacing = f;
    }

    public float getSpacing() {
	return spacing;
    }

    public void setSpacing(float spacing) {
	this.spacing = spacing;
    }

}
