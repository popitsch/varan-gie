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

    boolean isNumeric;
    
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

    public GIEAttributeFilter(String key, String valueSer, OPERATOR op) throws IOException {
	if ( !GIEDataDialog.getInstance().colNameMap.containsKey(key) )
	    throw new IOException("Cannot create filter for unknown attribute " + key);
	this.colName = key;
	this.operator = op;
	this.valueSer = valueSer;
	this.isNumeric = NumberUtils.isNumber(valueSer);
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

	// If the current dtaaset does not support the attribute: ignore
	if ( !GIEDataDialog.getInstance().colNameMap.containsKey(colName) )
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
	if ( isNumeric && ! (val instanceof Integer) && !val.equals("")) {
	    // try to convert to a number?
	    try {
	    Integer test = NumberUtils.createInteger((String) val);
	    if ( test != null )
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
		    System.out.println(valueSer + " => " + compValue);
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
		return ((String) val).equals(valueSer);
	    case NEQ:
		return !((String) val).equals(valueSer);
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
