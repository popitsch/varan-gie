package at.ccri.varan.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * A simple, generic iterator of TAB-separated files (e.g., BED files). May skip
 * comment lines that start with a specified prefix.
 * Supports gzipped files.O
 * 
 * @author niko.popitsch
 * 
 */
public class TabIterator implements Iterator<String[]> {
    BufferedReader br = null;
    boolean started = false;
    String line = null;
    private String commentPrefix = null;
    String SEP;
    private boolean noEmptyFields = false;
    private File sourceFile = null;

    /**
     * Constructor. Here one may set a separator different from \t
     * 
     * @param f
     * @throws IOException
     */
    public TabIterator(Reader r, String commentPrefix, String SEP) throws IOException {
	this.sourceFile = null;
	this.commentPrefix = commentPrefix;
	this.SEP = SEP;

	br = new BufferedReader(r);
	do {
	    line = br.readLine();
	    if (line == null)
		break;
	} while (!acceptLine(line));
    }

    /**
     * Constructor. Here one may set a separator different from \t
     * 
     * @param f
     * @throws IOException
     */
    public TabIterator(File f, String commentPrefix) throws IOException {
	this(f, commentPrefix, "\t");
    }

    /**
     * Constructor.
     * 
     * @param f
     * @throws IOException
     */
    public TabIterator(File f, String commentPrefix, String SEP) throws IOException {
	this.sourceFile = f;
	this.commentPrefix = commentPrefix;
	this.SEP = SEP;

	if (f.getName().equals("-")) {
	    br = new BufferedReader(new InputStreamReader(System.in));
	} else {
	    if (!f.exists())
		throw new IOException("File " + f + " not found.");
	    if (f.getAbsolutePath().endsWith(".gz"))
		br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f)), "UTF-8"));
	    else
		br = new BufferedReader(new FileReader(f));
	}

	do {
	    line = br.readLine();
	    if (line == null)
		break;
	} while (!acceptLine(line));

    }

    /**
     * Close this iterator.
     * 
     * @throws IOException
     */
    public void close() throws IOException {
	if (br != null)
	    br.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
	return (line != null);
    }

    /**
     * Filter lines.
     * 
     * @param line
     * @return
     */
    public boolean acceptLine(String line) {

	if (line == null)
	    return false;
	if (line.equals(""))
	    return false;
	if (this.commentPrefix != null)
	    if (line.startsWith(this.commentPrefix))
		return false;
	return true;
    }

    private String[] removeEmpty(String[] fields) {
	List<String> ret = new ArrayList<String>();
	for (String s : fields)
	    if (s != null && s.length() > 0)
		ret.add(s);
	return ret.toArray(new String[ret.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] next() {
	try {
	    String[] fields = line.split(SEP);
	    if (noEmptyFields)
		fields = removeEmpty(fields);
	    do {
		line = br.readLine();
		if (line == null)
		    break;
	    } while (!acceptLine(line));
	    return fields;
	} catch (IOException e) {
	    return null;
	}
    }

    /**
     * Drop all that start with the passed prefix.
     * 
     * @param prefix
     */
    public void skipLinesStartingWith(String prefix) {
	while ((line != null) && line.startsWith(prefix) && hasNext()) {
	    next();
	}
    }

    public boolean hasNoEmptyFields() {
	return noEmptyFields;
    }

    public void setNoEmptyFields(boolean noEmptyFields) {
	this.noEmptyFields = noEmptyFields;
    }

    /**
     * {@inheritDoc}. Not implemented.
     */
    @Override
    public void remove() {
    }

    /**
     * @return the source file
     */
    public File getSourceFile() {
	return sourceFile;
    }

    /**
     * Reads a map from a file. First column: key, second column: value.
     * 
     * @param f
     * @param commentChar
     * @return
     * @throws IOException
     */
    public static Map<String, String> readMapFromFile(File f, String commentChar) throws IOException {
	TabIterator ti = new TabIterator(f, commentChar);
	Map<String, String> ret = new HashMap<String, String>();
	int l = 1;
	while (ti.hasNext()) {
	    String t[] = ti.next();
	    if (t.length != 2)
		throw new IOException("Wrong field count in " + f + " on line " + l);
	    ret.put(t[0], t[1]);
	    l++;
	}
	return ret;
    }

    /**
     * Reads a map from a file. First column: value, second column: key.
     * 
     * @param f
     * @param commentChar
     * @return
     * @throws IOException
     */
    public static Map<String, String> readReverseMapFromFile(File f, String commentChar) throws IOException {
	TabIterator ti = new TabIterator(f, commentChar);
	Map<String, String> ret = new HashMap<String, String>();
	int l = 1;
	while (ti.hasNext()) {
	    String t[] = ti.next();
	    if (t.length != 2)
		throw new IOException("Wrong field count in " + f + " on line " + l);
	    ret.put(t[1], t[0]);
	    l++;
	}
	return ret;
    }

    public static List<String> readFromFile(File f, String commentChar) throws IOException {
	return readFromFile(f, commentChar, "\t", "\t", null, null);
    }

    /**
     * Reads lines from a (small!) file. Entries are tokenized by sepIn when
     * read and then concatenated with sepOut when written to the list. Only the
     * columns startCol-endCol are written to the output (e.g.,startCol=0,
     * endCol=1 will output the first 2 columns; startCol=1, endCol=null will
     * output all but the first column; ).
     * 
     * @param f
     * @param commentChar
     * @return
     * @throws IOException
     */
    public static List<String> readFromFile(File f, String commentChar, String sepIn, String sepOut, Integer startCol,
	    Integer endCol) throws IOException {
	TabIterator ti = new TabIterator(f, commentChar, sepIn);
	List<String> ret = new ArrayList<String>();
	while (ti.hasNext()) {
	    String t[] = ti.next();
	    List<String> out = new ArrayList<>();
	    int start = (startCol == null ? 0 : startCol);
	    int end = (endCol == null ? t.length : endCol);
	    for (int i = start; i <= Math.min(end, t.length - 1); i++)
		out.add(t[i]);
	    ret.add(StringUtils.concat(out, sepOut));
	}
	return ret;
    }

}
