package at.ccri.varan.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.feature.genome.GenomeManager;

/**
 * Compare chromosomes in a canonical fashion. Order is 1, 2, 3, ..., 22, M, X,
 * Y, <other-char-ordered>
 * 
 * @author niko.popitsch@univie.ac.at
 * 
 */
public class CanonicalChromsomeComparator implements Comparator<String> {

    private static Logger log = Logger.getLogger(CanonicalChromsomeComparator.class);

    public static String[] canonicalChromosomes = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
	    "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "M", "X", "Y" };
    public static long[] canonicalChromosomeSizes = new long[] { 249250621l, 243199373l, 198022430l, 191154276l,
	    180915260l, 171115067l, 159138663l, 146364022l, 141213431l, 135534747l, 135006516l, 133851895l, 115169878l,
	    107349540l, 102531392l, 90354753l, 81195210l, 78077248l, 59128983l, 63025520l, 48129895l, 51304566l, 16569l,
	    155270560l, 59373566l };
    private static Map<String, Integer> canIdx = new HashMap<String, Integer>();
    private static Map<String, Long> chromSizes = new HashMap<String, Long>();

    public static enum CHROM_CATEGORY {
	AUTOSOME, X, Y, MT, OTHER
    };

    static {
	for (int i = 0; i < canonicalChromosomes.length; i++)
	    canIdx.put(canonicalChromosomes[i], i);
	for (int i = 0; i < canonicalChromosomes.length; i++)
	    chromSizes.put(canonicalChromosomes[i], canonicalChromosomeSizes[i]);
    }

    /**
     * Canonical mapping for human chroms
     */
    private static Map<String, String> canonicalMappingHuman = new HashMap<String, String>();

    static {

	// indices
	for (int i = 0; i < canonicalChromosomes.length; i++)
	    canIdx.put(canonicalChromosomes[i], i);

	// mappings
	for (int i = 1; i <= 22; i++)
	    canonicalMappingHuman.put("chr" + i, "" + i);
	canonicalMappingHuman.put("x", "X");
	canonicalMappingHuman.put("chrx", "X");
	canonicalMappingHuman.put("chrX", "X");

	canonicalMappingHuman.put("y", "Y");
	canonicalMappingHuman.put("chry", "Y");
	canonicalMappingHuman.put("chrY", "Y");

	canonicalMappingHuman.put("MT", "M");
	canonicalMappingHuman.put("m", "M");
	canonicalMappingHuman.put("mt", "M");
	canonicalMappingHuman.put("chrm", "M");
	canonicalMappingHuman.put("chrM", "M");
	canonicalMappingHuman.put("chrmt", "M");
	canonicalMappingHuman.put("chrMT", "M");

    }
    
    /**
     * Count the breakpoints
     * @param r
     * @return
     */
    public static int countBreakpoints(RegionOfInterest r) {
	int chrlen = GenomeManager.getInstance().getCurrentGenome().getChromosome(r.getChr()).getLength();
	int bp = 0;
	if ( r.getStart() > 0 && r.getStart() < chrlen)
	    bp++;
	if ( r.getEnd() > 0 && r.getEnd() < chrlen)
	    bp++;
	return bp;
    }

    /**
     * Parse a genomic coordinate. Supports special strings such as qter and pter
     * and relative coordinates such as +100
     * 
     * @param chr
     * @param newString
     * @return
     */
    public static int parseCoordinate(String chr, Integer previous, String newString) {
	if (previous == null)
	    previous = 0;
	if (newString.toLowerCase().startsWith("pter"))
	    // set to pter
	    return 0;
	else if (newString.toLowerCase().startsWith("qter")) {
	    // set to qter
	    try {
		return GenomeManager.getInstance().getCurrentGenome().getChromosome(chr).getLength();
	    } catch (Exception e) {
		log.error(e.getMessage());
		return 0;
	    }
	} else {
	    // set to offset
	    try {
		if (newString.startsWith("+"))
		    return (previous + Math.max(0,Integer.parseInt(newString.substring(1))));
		else if (newString.startsWith("-"))
		    return (previous - Math.max(0, Integer.parseInt(newString.substring(1))));
		else
		    return (Math.max(0, Integer.parseInt(newString)));
	    } catch (Exception e) {
		log.error(e.getMessage());
		return 0;
	    }
	}
    }

    /**
     * Determines the chrom categor.
     * 
     * @param chr
     * @return
     */
    public static CHROM_CATEGORY getCategory(String chr) {
	String can = getCanonicalMappingHuman(chr);
	if (can.equals("X"))
	    return CHROM_CATEGORY.X;
	if (can.equals("Y"))
	    return CHROM_CATEGORY.Y;
	if (can.equals("M"))
	    return CHROM_CATEGORY.MT;
	// others should be autosomes
	if (canonicalMappingHuman.values().contains(can))
	    return CHROM_CATEGORY.AUTOSOME;
	return CHROM_CATEGORY.OTHER;
    }

    /**
     * @param can
     * @returnb true if can is a canonical name
     */
    public static boolean isCanonical(String can) {
	if (can == null)
	    return false;
	return canonicalMappingHuman.values().contains(can);
    }

    public static String getCanonicalMappingHuman(String chr) {
	String chr2 = canonicalMappingHuman.get(chr);
	if (chr2 == null)
	    return chr;
	return chr2;
    }

    public static Long getChromLength(String chr) {
	String c = getCanonicalMappingHuman(chr);
	return chromSizes.get(c);
    }

    /**
     * Compares its two arguments for order. Returns a negative integer, zero,
     * or a positive integer as the first argument is less than, equal to, or
     * greater than the second.
     * 
     * In the foregoing description, the notation sgn(expression) designates the
     * mathematical signum function, which is defined to return one of -1, 0, or
     * 1 according to whether the value of expression is negative, zero or
     * positive.
     * 
     * The implementor must ensure that sgn(compare(x, y)) == -sgn(compare(y,
     * x)) for all x and y. (This implies that compare(x, y) must throw an
     * exception if and only if compare(y, x) throws an exception.)
     * 
     * The implementor must also ensure that the relation is transitive:
     * ((compare(x, y)>0) && (compare(y, z)>0)) implies compare(x, z)>0.
     * 
     * Finally, the implementor must ensure that compare(x, y)==0 implies that
     * sgn(compare(x, z))==sgn(compare(y, z)) for all z.
     * 
     * It is generally the case, but not strictly required that (compare(x,
     * y)==0) == (x.equals(y)). Generally speaking, any comparator that violates
     * this condition should clearly indicate this fact. The recommended
     * language is "Note: this comparator imposes orderings that are
     * inconsistent with equals."
     */
    @Override
    public int compare(String o1, String o2) {
	if (o1 == null) {
	    if (o2 == null)
		return 0;
	    return -1;
	}
	if (o2 == null)
	    return 1;

	String c1 = getCanonicalMappingHuman(o1);
	String c2 = getCanonicalMappingHuman(o2);
	if (c1.equals(c2))
	    return 0;

	Integer i1 = canIdx.get(c1);
	Integer i2 = canIdx.get(c2);

	if (i1 == null) { // no canonical chr
	    if (i2 == null)
		return c1.compareTo(c2);
	    return 1;
	}
	if (i2 == null)
	    return -1;

	return i1.compareTo(i2);
    }

}
