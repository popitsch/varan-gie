package at.ccri.varan.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A TabIterator that treats the first line as headers and allows to access data
 * fields by header names.
 * 
 * @author niko.popitschO
 * 
 */
public class TabTableIterator extends TabIterator {

    String[] headers = null;
    Map<String, Integer> hidx = new HashMap<>();
    private Character trimChar;

    /**
     * Constructor.
     * 
     * @param f
     * @throws IOException
     */
    public TabTableIterator(File f, String commentPrefix) throws IOException {
	this(f, commentPrefix, "\t", null, false, 0);
    }

    /**
     * Constructor.
     * 
     * @param f
     * @throws IOException
     */
    public TabTableIterator(File f, String commentPrefix, String SEP, Character trimChar, boolean noFirstColumnHeader)
	    throws IOException {
	this(f, commentPrefix, SEP, trimChar, noFirstColumnHeader, 0);
    }

    /**
     * Constructor.
     * 
     * @param f
     * @throws IOException
     */
    public TabTableIterator(File f, String commentPrefix, String SEP, Character trimChar, boolean noFirstColumnHeader,
	    int skip) throws IOException {
	super(f, commentPrefix, SEP);

	// skip
	for (int i = 0; i < skip; i++)
	    next();

	this.trimChar = trimChar;
	headers = next();
	if (noFirstColumnHeader) {
	    String[] newHeaders = new String[headers.length + 1];
	    newHeaders[0] = "";
	    for (int i = 0; i < headers.length; i++)
		newHeaders[i + 1] = headers[i];
	    headers = newHeaders;
	}

	for (int i = 0; i < headers.length; i++) {
	    headers[i] = trimChar(headers[i], trimChar);
	    hidx.put(headers[i], i);
	}

    }

    /**
     * Will remove enclosing trimCharacters
     * 
     * @param str
     * @param trimChar
     * @return
     */
    private String trimChar(String str, Character trimChar) {
	if (str == null)
	    return null;
	if (str.length() == 0)
	    return str;
	str = str.trim();
	if (trimChar == null)
	    return str;
	int off1 = (str.charAt(0) == trimChar ? 1 : 0);
	int off2 = (str.charAt(str.length() - 1) == trimChar ? -1 : 0);
	return str.substring(off1, str.length() + off2);
    }

    public String getField(String[] data, String fname) {
	if (data == null)
	    return null;
	Integer idx = hidx.get(fname);
	if (idx == null)
	    return null;
	if (idx >= data.length) // e.g., at end of line.
	    return null;
	return trimChar(data[idx], trimChar);
    }

    public String[] getHeaders() {
	return headers;
    }

    public Integer getFieldIndex(String fname) {
	return hidx.get(fname);
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

	// note that this reads a quoted CSV-table ("1","2", ...) and removes
	// the quotes from headers and field values accessed via getField.
	// Class does not work with ("1", "2,3", "4") !
	TabTableIterator ti = new TabTableIterator(
		new File(
			"C:/data/borrelia/FINAL-results/results/reports/Counttable-23deg-37deg-DE-23-37-tested.bed-minus.csv-RESULTS.csv"),
		null, ",", '\"', true);
	System.out.println(StringUtils.concat(ti.getHeaders(), " / "));
	String[] t = ti.next();
	System.out.println(ti.getField(t, "baseMean"));
	System.out.println(ti.getField(t, "foldChange"));
	System.out.println(ti.getField(t, "notthere"));

    }

}
