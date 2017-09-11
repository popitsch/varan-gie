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

package org.broad.igv.feature;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.broad.igv.feature.genome.ChromosomeNameComparator;

/**
 * @author eflakes
 */
public class RegionOfInterest implements Comparable<RegionOfInterest> {

    private String chr;
    private String description;
    private int start; // In Chromosome coordinates
    private int end; // In Chromosome coordinates
    private static Color backgroundColor = new Color(255, 0, 0);
    private static Color foregroundColor = Color.LIGHT_GRAY;
    boolean selected = false;
    private Map<String, String> anno = new HashMap<>();

    // np
    private String strand = null; // default strand is null
    private String score = "0"; // default score is 0 to enable proper IGV rendering
    private String color;

    /**
     * Clone a roi.
     * 
     * @return
     */
    public RegionOfInterest deepClone() {
	RegionOfInterest ret = new RegionOfInterest(chr, start, end, description);
	ret.setColor(getColor());
	ret.setStrand(getStrand());
	ret.setScore(getScore());
	ret.setAnnotations(getAnnotations());
	return ret;
    }

    /**
     * A bounded region on a chromosome.
     *
     * @param chromosomeName
     * @param start
     *            The region starting position on the chromosome.
     * @param end
     *            The region starting position on the chromosome.
     * @param description
     */
    public RegionOfInterest(String chromosomeName, int start, int end, String description) {
	this.chr = chromosomeName;
	if (start > end) {
	    int tmp = start;
	    start = end;
	    end = tmp;
	}
	this.start = start;
	this.end = end;
	setDescription(description);
    }

    /**
     * NOTE: returns interval [start+1; end] due to implementation of Range
     * 
     * @return
     */
    public Range getRange() {
	return new Range(chr, start + 1, end);
    }

    public String getTooltip() {
	return description == null ? chr + ":" + getDisplayStart() + "-" + getDisplayEnd() : description;
    }

    public void setChr(String c) {
	this.chr = c;
    }

    public String getChr() {
	return chr;
    }

    public void setEnd(int e) {
	if (this.start > e) {
	    this.end = this.start;
	    this.start = e;
	} else
	    this.end = e;
    }

    public void setStart(int s) {
	if (this.end < s) {
	    this.start = this.end;
	    this.end = s;
	} else
	    this.start = s;
    }

    public int getEnd() {
	return end;
    }

    /**
     * locations displayed to the user are 1-based. start and end are 0-based.
     * 
     * @return
     */
    public int getDisplayEnd() {
	return getEnd();
    }

    public int getStart() {
	return start;
    }

    public int getCenter() {
	return (start + end) / 2;
    }

    public int getLength() {
	return end - start;
    }

    /**
     * locations displayed to the user are 1-based. start and end are 0-based.
     * 
     * @return
     */
    public int getDisplayStart() {
	return getStart() + 1;
    }

    public static Color getBackgroundColor() {
	return backgroundColor;
    }

    public static Color getForegroundColor() {
	return foregroundColor;
    }

    public String getLocusString() {
	return getChr() + ":" + getDisplayStart() + "-" + getDisplayEnd();
    }

    @Override
    public int compareTo(RegionOfInterest o) {
	if (!o.getChr().equals(getChr())) {
	    return ChromosomeNameComparator.get().compare(getChr(), o.getChr());
	}
	if (getStart() < o.getStart())
	    return -1;
	if (getStart() > o.getStart())
	    return 1;
	return 0;
    }

    private static boolean isInterpunkt(Character c) {
	if (c == '!')
	    return true;
	if (c == '"')
	    return true;
	if (c == '$')
	    return true;
	if (c == '%')
	    return true;
	if (c == '&')
	    return true;
	if (c == '/')
	    return true;
	if (c == '(')
	    return true;
	if (c == ')')
	    return true;
	if (c == '=')
	    return true;
	if (c == '?')
	    return true;
	if (c == '_')
	    return true;
	if (c == '-')
	    return true;
	if (c == '.')
	    return true;
	if (c == ',')
	    return true;
	if (c == ';')
	    return true;
	if (c == ':')
	    return true;
	if (c == '#')
	    return true;
	if (c == '+')
	    return true;
	if (c == '*')
	    return true;
	return false;
    }

    /**
     * Escape characters not supported in BED interval names
     * 
     * @param s
     * @return
     */
    public static String encodeDescription(String s) {
	int len = s.length();
	StringBuilder sb = new StringBuilder(len);
	final char escape = '%';
	for (int i = 0; i < len; i++) {
	    char ch = s.charAt(i);
	    if (Character.isSpaceChar(ch)) {
		sb.append('_');
	    } else if (Character.isLetterOrDigit(ch) || isInterpunkt(ch)) {
		sb.append(ch);
	    } else {
		sb.append(escape);
		if (ch < 0x10) {
		    sb.append('0');
		}
		sb.append(Integer.toHexString(ch));
	    }
	}
	return sb.toString();
    }

    public void setDescription(String description) {

	if (description == null || description.equals("-") || description.equalsIgnoreCase("NA")
		|| description.equalsIgnoreCase("null"))
	    this.description = null;
	else {
	    this.description = encodeDescription(description);
	}
    }

    public String getDescription() {
	return description;
    }

    public String getScore() {
	return score;
    }

    public void setScore(String score) {
	if (score == null || score.equals("-") || score.equalsIgnoreCase("NA") || score.equalsIgnoreCase("null")
		|| score.equalsIgnoreCase("NaN"))
	    this.score = null;
	else
	    this.score = score;
    }

    public String getStrand() {
	return strand;
    }

    public void setStrand(Strand s) {
	if (s == null)
	    this.strand = null;
	else
	    switch (s) {
	    case NEGATIVE:
		this.strand = "-";
		break;
	    case POSITIVE:
		this.strand = "+";
		break;
	    default:
		this.strand = "0";
		break;
	    }
    }

    public void setStrand(String strand) {
	if (strand == null || strand.equalsIgnoreCase("NA") || strand.equalsIgnoreCase("null"))
	    this.strand = null;
	else
	    this.strand = strand;
    }

    public String getColor() {
	return color;
    }

    public static Color getAWTColor(String c) {
	if (c == null)
	    return null;
	String[] rgb = c.split(",");
	if (rgb.length != 3)
	    return null;
	return new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
    }

    public Color getAWTColor() {
	return getAWTColor(color);
    }

    public void setColor(String color) {
	if (color == null || color.equals("-") || color.equalsIgnoreCase("NA") || color.equalsIgnoreCase("null"))
	    this.color = null;
	else
	    this.color = color;
    }

    public String getAnnotation(String key) {
	return anno.get(key);
    }

    public Map<String, String> getAnnotations() {
	return anno;
    }

    public void addAnnotation(String key, String value) {
	this.anno.put(key, value);
    }

    public void setAnnotations(Map<String, String> a) {
	this.anno = a;
    }

    @Override
    public String toString() {
	return chr + ":" + start + "-" + end;
    }
    
}
