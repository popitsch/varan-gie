package at.ccri.varan.ui;

import java.io.IOException;

import javax.swing.table.TableModel;

/**
 * An attribute filter for the interval table
 * 
 * @author niko.popitsch
 *
 */
public class GIEAttributeFilter {

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

    int colidx;
    OPERATOR operator;
    String valueSer;

    public GIEAttributeFilter(String key, String valueSer, OPERATOR op) throws IOException {
	Integer ci = GIEDataDialog.getInstance().colName2Index(key.trim());
	if (ci == null)
	    throw new IOException("Cannot create filter for unknown attribute " + key);
	this.colidx = ci;
	this.operator = op;
	this.valueSer = valueSer;
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append(GIEDataDialog.getInstance().colIndex2Name(colidx));
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

	Object val = entry.getValue(colidx);
	Object att = val;
	switch (this.operator) {
	case FLAGSET:
	case FLAGUNSET:
	    // works only with integer numbers
	    att = Integer.parseInt(val.toString());
	default:
	}

	if (att instanceof Integer) {
	    switch (this.operator) {
	    case GT:
		return ((Integer) att) > (Integer.parseInt(valueSer));
	    case GTE:
		return ((Integer) att) >= (Integer.parseInt(valueSer));
	    case LT:
		return ((Integer) att) < (Integer.parseInt(valueSer));
	    case LTE:
		return ((Integer) att) <= (Integer.parseInt(valueSer));
	    case EQ:
		return ((Integer) att).equals((Integer.parseInt(valueSer)));
	    case NEQ:
		return !((Integer) att).equals((Integer.parseInt(valueSer)));
	    case FLAGSET:
		return (((Integer) att).intValue() & (Integer.parseInt(valueSer))) == Integer.parseInt(valueSer);
	    case FLAGUNSET:
		return (((Integer) att).intValue() & (Integer.parseInt(valueSer))) != Integer.parseInt(valueSer);
	    }
	} else if (att instanceof String) {
	    switch (this.operator) {
	    case GT:
		return ((String) att).compareTo(valueSer) > 0;
	    case GTE:
		return ((String) att).compareTo(valueSer) >= 0;
	    case LT:
		return ((String) att).compareTo(valueSer) < 0;
	    case LTE:
		return ((String) att).compareTo(valueSer) <= 0;
	    case EQ:
		return ((String) att).equals(valueSer);
	    case NEQ:
		return !((String) att).equals(valueSer);
	    }
	} else
	    try {
		throw new IOException("Cannot filter attribute of type " + att.getClass() + " with operator "
			+ this.operator + ", not implemented yet!");
	    } catch (IOException e) {
		e.printStackTrace();
		return false;
	    }
	return true;
    }

}
