package at.ccri.varan.util;

import java.util.Collection;

/**
 * String utility methods.
 * 
 * @author niko.popitsch
 * 
 */
public class StringUtils {

    public static Enum<?> findInEnum(String value, Enum<?>[] values) {
	if (value == null)
	    return null;
	for (Enum<?> val : values) {

	    if (value.equalsIgnoreCase(val.name()))
		return val;
	}
	return null;
    }

    public static String concat(Collection<?> t, String SEP) {
	if (t == null || t.isEmpty())
	    return "";
	Object[] x = t.toArray();
	String[] s = new String[t.size()];
	for (int i = 0; i < t.size(); i++)
	    s[i] = x[i].toString();
	return concat(s, SEP);
    }

    public static String concat(String[] t, String SEP) {
	if (t == null)
	    return "";
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < t.length; i++) {
	    if (i > 0)
		sb.append(SEP);
	    sb.append(t[i]);
	}
	return sb.toString();
    }

    public static String concat(double[] t, String SEP) {
	return concat(t, 0, t.length, SEP);
    }

    public static String concat(double[] t, int from, int to, String SEP) {
	if (t == null)
	    return "";
	StringBuilder sb = new StringBuilder();
	for (int i = from; i < to; i++) {
	    if (i > from)
		sb.append(SEP);
	    sb.append(t[i]);
	}
	return sb.toString();
    }

}
