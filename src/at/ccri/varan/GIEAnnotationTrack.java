package at.ccri.varan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * An annotation track.
 * 
 * @author niko.popitsch
 *
 */
public class GIEAnnotationTrack {

    String name;

    File dataFile;

    String description;

    String info;

    public GIEAnnotationTrack(String name, File dataFile, String description) {
	this.name = name;
	this.dataFile = dataFile;
	this.description = description;
	this.info = calcInfo();
    }

    public GIEAnnotationTrack() {

    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public File getDataFile() {
	return dataFile;
    }

    public void setDataFile(File dataFile) {
	this.dataFile = dataFile;
    }

    public String getDescription() {
	return description;
    }

    public void setDescription(String description) {
	this.description = description;
    }

    public String getInfo() {
	return info;
    }

    public void setInfo(String info) {
	this.info = info;
    }

    public String calcInfo() {

	if (getDataFile().getAbsolutePath().toLowerCase().endsWith(".bed")
		|| getDataFile().getAbsolutePath().toLowerCase().endsWith(".bed.gz")) {
	    /**
	     * Count number iof BED intervals
	     */
	    BufferedReader reader = null;
	    try {
		if (getDataFile().getAbsolutePath().toLowerCase().endsWith(".bed"))
		    reader = new BufferedReader(new FileReader(getDataFile()));
		else if (getDataFile().getAbsolutePath().toLowerCase().endsWith(".bed.gz"))
		    reader = new BufferedReader(
			    new InputStreamReader(new GZIPInputStream(new FileInputStream(getDataFile())), "UTF-8"));

		String nextLine;
		int c = 0;
		while ((nextLine = reader.readLine()) != null && (nextLine.trim().length() > 0)) {
		    String[] t = nextLine.split("\t");
		    if (t[0].startsWith("track") || t[0].startsWith("browser "))
			continue;

		    c++;
		}
		reader.close();
		return "Intervals: " + c;
	    } catch (IOException e) {
		e.printStackTrace();
		return "";
	    }
	} else {
	    return "Filesize: " + getDataFile().length();
	}
    }

}
