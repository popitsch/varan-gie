package at.ccri.varan.ui;

import java.io.IOException;
import java.text.ParseException;

import javax.swing.table.TableModel;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

/**
 * An attribute filter for the interval table
 * 
 * @author niko.popitsch
 *
 */
public class GIEAttributeFilter {

    private static Logger log = Logger.getLogger(GIEAttributeFilter.class);

    public static enum OPERATOR {
	EQ, NEQ, GT, LT, GTE, LTE, FLAGSET, FLAGUNSET
    }

    public static String[] OPERATOR_STR = { "=", "!=", ">", "<", ">=", "<=", "&&", "^^" };

    

    public static String op2str(OPERATOR op) {
	switch (op) {
	case EQ:
	    return OPERATOR_STR[0];
	case NEQ:
	    return OPERATOR_STR[1];
	case GT:
	    return OPERATOR_STR[2];
	case GTE:
	    return OPERATOR_STR[3];
	case LT:
	    return OPERATOR_STR[4];
	case LTE:
	    return OPERATOR_STR[5];
	case FLAGSET:
	    return OPERATOR_STR[6];
	case FLAGUNSET:
	    return OPERATOR_STR[7];
	}
	return "?";
    }

    OPERATOR operator;
    String valueSer;
    String colName;
    boolean isNumeric;
    boolean isPrefix;
    boolean isContains;

    public GIEAttributeFilter(String key, String valueSer, OPERATOR op) throws IOException {
	if (!GIEDataDialog.getInstance().colNameMap.containsKey(key))
	    throw new IOException("Cannot create filter for unknown attribute " + key);
	this.colName = key;
	this.operator = op;
	this.valueSer = valueSer;
	this.isNumeric = NumberUtils.isNumber(valueSer);
	this.isPrefix = valueSer.endsWith("*");
	this.isContains = valueSer.startsWith("*") && valueSer.endsWith("*");
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append(colName);
	sb.append(op2str(operator));
	sb.append(valueSer);
	return sb.toString();
    }

    public static GIEAttributeFilter parseFromString(String s) throws IOException {
	if (s == null)
	    return null;
	if (s.toLowerCase().contains("&&")) {
	    String[] tmp = s.split("&&");
	    return new GIEAttributeFilter(tmp[0], tmp[1], OPERATOR.FLAGSET);
	} else if (s.toLowerCase().contains("^^")) {
	    String[] tmp = s.split("\\^\\^");
	    return new GIEAttributeFilter(tmp[0], tmp[1], OPERATOR.FLAGUNSET);
	} else if (s.contains("<=")) {
	    String[] tmp = s.split("<=");
	    return new GIEAttributeFilter(tmp[0], tmp[1], OPERATOR.LTE);
	} else if (s.contains(">=")) {
	    String[] tmp = s.split(">=");
	    return new GIEAttributeFilter(tmp[0], tmp[1], OPERATOR.GTE);
	} else if (s.contains("!=")) {
	    String[] tmp = s.split("!=");
	    return new GIEAttributeFilter(tmp[0], tmp[1], OPERATOR.NEQ);
	} else if (s.contains("=")) {
	    String[] tmp = s.split("=");
	    return new GIEAttributeFilter(tmp[0], tmp[1], OPERATOR.EQ);
	} else if (s.contains("<")) {
	    String[] tmp = s.split("<");
	    return new GIEAttributeFilter(tmp[0], tmp[1], OPERATOR.LT);
	} else if (s.contains(">")) {
	    String[] tmp = s.split(">");
	    return new GIEAttributeFilter(tmp[0], tmp[1], OPERATOR.GT);
	} else
	    return null;
    }

    public boolean filter(javax.swing.RowFilter.Entry<? extends TableModel, ? extends Object> entry) {

	// If the current dataset does not support the attribute: ignore
	if (GIEDataDialog.getInstance() == null
		|| !GIEDataDialog.getInstance().colNameMap.containsKey(colName))
	    return true;

	// value of the interval
	int colidx = GIEDataDialog.getInstance().colName2Index(colName);

	Object val = entry.getValue(colidx);
	if (colidx == GIEDataDialog.COLIDX_Width) {
	    try {
		val = GIEDataDialog.getInstance().parseIntervalWidth((String) val);
	    } catch (ParseException e) {
		log.error("Error parsing length value: " + e.getMessage());
		return false;
	    }
	}

	/**
	 * Convert to int if possible
	 */
	if (isNumeric && (val instanceof String) && !val.equals("")) {
	    // try to convert to a number?
	    try {
		Integer test = NumberUtils.createInteger((String) val);
		if (test != null)
		    val = test;
	    } catch (NumberFormatException ex) {
		//
	    }
	}

	switch (this.operator) {
	case FLAGSET:
	case FLAGUNSET:
	    // works only with integer numbers
	    val = Integer.parseInt(val.toString());
	default:
	}

	if (val instanceof Integer) {
	    // filter value
	    Integer compValue = null;
	    if (colidx == GIEDataDialog.COLIDX_Width) {
		try {
		    compValue = GIEDataDialog.getInstance().parseIntervalWidth(valueSer);
		} catch (ParseException e) {
		    log.error("Error parsing length value: " + e.getMessage());
		    return false;
		}
	    } else
		compValue = Integer.parseInt(valueSer);

	    switch (this.operator) {
	    case GT:
		return ((Integer) val) > (compValue);
	    case GTE:
		return ((Integer) val) >= (compValue);
	    case LT:
		return ((Integer) val) < (compValue);
	    case LTE:
		return ((Integer) val) <= (compValue);
	    case EQ:
		return ((Integer) val).equals((compValue));
	    case NEQ:
		return !((Integer) val).equals((compValue));
	    case FLAGSET:
		return (((Integer) val).intValue() & (compValue)) == compValue;
	    case FLAGUNSET:
		return (((Integer) val).intValue() & (compValue)) != compValue;
	    }
	} else if (val instanceof Double) {
	    // filter value
	    Double compValue = Double.parseDouble(valueSer);

	    switch (this.operator) {
	    case GT:
		return ((Double) val) > (compValue);
	    case GTE:
		return ((Double) val) >= (compValue);
	    case LT:
		return ((Double) val) < (compValue);
	    case LTE:
		return ((Double) val) <= (compValue);
	    case EQ:
		return ((Double) val).equals((compValue));
	    case NEQ:
		return !((Double) val).equals((compValue));
	    case FLAGSET: // TODO undefined
	    case FLAGUNSET:
		return true;
	    }
	} else if (val instanceof String) {
	    switch (this.operator) {
	    case GT:
		return ((String) val).compareTo(valueSer) > 0;
	    case GTE:
		return ((String) val).compareTo(valueSer) >= 0;
	    case LT:
		return ((String) val).compareTo(valueSer) < 0;
	    case LTE:
		return ((String) val).compareTo(valueSer) <= 0;
	    case EQ:
		String v = ((String) val);
		if ( isContains )
		    return v.contains( valueSer.substring(1, valueSer.length()-1));
		if ( isPrefix )
		    return v.startsWith(valueSer.substring(0, valueSer.length()-1));
		return v.equals(valueSer);
	    case NEQ:
		v = ((String) val);
		if ( isContains )
		    return !v.contains( valueSer.substring(1, valueSer.length()-1));
		if ( isPrefix )
		    return ! v.startsWith(valueSer.substring(0, valueSer.length()-1));
		return !((String) val).equals(valueSer);
	    case FLAGSET: // TODO undefined
	    case FLAGUNSET:
		return true;
	    }
	} else
	    try {
		throw new IOException("Cannot filter attribute of type " + val.getClass() + " with operator "
			+ this.operator + ", not implemented yet!");
	    } catch (IOException e) {
		e.printStackTrace();
		return false;
	    }
	return true;
    }

}
