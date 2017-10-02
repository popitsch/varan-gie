package at.ccri.varan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;

import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.ui.IGV;

/**
 * A particular GIE dataset version layer.
 * 
 * @author niko.popitsch
 *
 */
public class GIEDatasetVersionLayer {

    final static transient SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

    /**
     * The BED file storing the layer data
     */
    File dataFile;

    /**
     * The description of the layer
     */
    String description;

    /**
     * The name of the layer
     */
    private transient String layerName;

    /**
     * The version this layer belongs to
     */
    private transient GIEDatasetVersion version;

    /**
     * The regions of this layer.
     */
    transient SortedSet<RegionOfInterest> regions = null;;

    /**
     * Name of annotation key/value pairs
     */
    public String[] annotations = new String[] {};

    /**
     * Size of data file.
     */
    long dataFileSize;

    /**
     * Last modification date
     */
    String lastModified = null;

    /**
     * Simple constructor.
     */
    public GIEDatasetVersionLayer() {
    }

    /**
     * Constructor.
     * 
     * @param datasetName
     * @param version
     * @param layerName
     * @param annotations
     * @throws IOException
     */
    public GIEDatasetVersionLayer(GIEDatasetVersion version, String layerName, String[] annotations)
	    throws IOException {
	this.version = version;
	this.layerName = layerName;
	this.annotations = annotations;

	this.dataFile = new File(GIE.GIE_DIRECTORY, URLEncoder.encode(
		"gie." + version.getDataset().getName() + "." + version.getVersionName() + "." + layerName + ".bed",
		"UTF-8"));
	if (dataFile.exists())
	    throw new IOException("CANNOT create version as dataFile or sessionFile already exists.");

	if (dataFile.exists())
	    dataFileSize = dataFile.length();
	setLastModified(new Date());
    }

    /**
     * (Re-) load data from data file.
     * 
     * @return
     */
    public SortedSet<RegionOfInterest> load() {
	return loadFromFile(getDataFile());
    }

    /**
     * Load data from the passed file.
     * 
     * @return
     */
    public SortedSet<RegionOfInterest> loadFromFile(File inFile) {
	regions = new TreeSet<>();
	if (inFile.exists())
	    try {
		BufferedReader reader;
		if (inFile.getName().endsWith(".gz"))
		    reader = new BufferedReader(
			    new InputStreamReader(new GZIPInputStream(new FileInputStream(inFile))));
		else
		    reader = new BufferedReader(new InputStreamReader(new FileInputStream(inFile)));
		String nextLine;
		while ((nextLine = reader.readLine()) != null && (nextLine.trim().length() > 0)) {
		    String[] t = nextLine.split("\t");
		    if (t[0].startsWith("track") || t[0].startsWith("browser "))
			continue;

		    String name = t[3];
		    if (name.equals("null"))
			name = null;
		    RegionOfInterest roi = new RegionOfInterest(t[0], Integer.parseInt(t[1]), Integer.parseInt(t[2]),
			    name);
		    roi.setScore(t[4]);
		    roi.setStrand(t[5]);
		    roi.setColor(t[8]);
		    for (int i = 9; i < t.length; i++)
			roi.addAnnotation(annotations[i - 9], URLDecoder.decode(t[i], "UTF-8"));
		    regions.add(roi);
		}
		reader.close();
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	// load only if this layer is active
	if (GIE.getInstance().getActiveDataset() != null
		&& GIE.getInstance().getActiveDataset().getCurrentVersion() != null
		&& GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer() != null
		&& GIE.getInstance().getActiveDataset().getCurrentVersion().getActiveLayer().equals(this)) {
	    // update igv regions data struct
	    IGV.getInstance().getSession().clearRegionsOfInterest();
	    IGV.getInstance().addROI(regions);
	}

	return regions;
    }

    public void addRegions(List<RegionOfInterest> reg) {
	if (regions == null)
	    regions = new TreeSet<>();
	regions.addAll(reg);
	IGV.getInstance().addROI(reg);
    }

    public void removeRegions(List<RegionOfInterest> reg) {
	if (regions == null)
	    regions = new TreeSet<>();
	regions.removeAll(reg);
	IGV.getInstance().removeRegionsOfInterest(reg);
    }

    public boolean hasAnnotation(String key) {
	for (String k : annotations)
	    if (k.equals(key))
		return true;
	return false;
    }

    /**
     * Estimates the "width" of a vcf variant (the maximum extension).
     * 
     * @return
     */
    private int getVCFVariantWidth(String ref, String alt) {
	if (alt == null)
	    return 1;
	int width = 1;
	for (String aa : alt.split(",")) {
	    if (ref.startsWith(aa)) {
		// this is a deletion
		return 1;
	    } else if (aa.startsWith(ref)) {
		// this is an insertion
		width = Math.max(width, aa.length() - ref.length());
	    } else {
		// an SNV or MNP
		if (ref.length() == aa.length())
		    width = ref.length();
		else
		    width = Math.max(width, aa.length() - ref.length());
	    }
	}
	return width;
    }

    private boolean isPass(String filter) {
	return !(filter.equals("PASS") || filter.equals("."));
    }

    /**
     * Import data from external file and load into layer.
     * 
     * @return
     * @throws IOException
     */
    public SortedSet<RegionOfInterest> importAndLoad(File importLayerFile) throws IOException {
	// import layer data / copy and normalize file.

	BufferedReader reader = null;
	PrintWriter out = null;
	try {
	    out = new PrintWriter(dataFile);
	    out.println("track name=\"" + getVersion().getDataset().getName() + "." + getVersion().getVersionName()
		    + "." + layerName + "\" description=\"GIE data track\" visibility=1 useScore=1 itemRgb=\"On\"");
	    if (importLayerFile != null) {
		String fileType = null;
		String ifn = importLayerFile.getName().toLowerCase();
		if (ifn.endsWith(".gz")) {
		    reader = new BufferedReader(
			    new InputStreamReader(new GZIPInputStream(new FileInputStream(importLayerFile))));
		    if (ifn.endsWith(".bed.gz"))
			fileType = "bed";
		    else if (ifn.endsWith(".vcf.gz"))
			fileType = "vcf";
		} else {
		    reader = new BufferedReader(new InputStreamReader(new FileInputStream(importLayerFile)));
		    if (ifn.endsWith(".bed"))
			fileType = "bed";
		    else if (ifn.endsWith(".vcf"))
			fileType = "vcf";
		}

		String nextLine;
		int c = 0;
		Genome g = GenomeManager.getInstance().getCurrentGenome();

		if (fileType.equals("bed")) {
		    /********
		     * BED files
		     */
		    while ((nextLine = reader.readLine()) != null && (nextLine.trim().length() > 0)) {
			c++;
			String[] t = nextLine.split("\t");
			if (t[0].startsWith("track") || t[0].startsWith("browser ")) {
			    // FIXME: parse description if any
			    continue;
			}
			if (t.length < 4)
			    throw new IOException("Wrong format. Not a BED file?");
			String chr = t[0];
			if (g != null)
			    chr = g.getCanonicalChrName(chr);
			String start = t[1];
			String end = t[2];
			String id = t.length >= 4 ? t[3] : c + "F";
			String score = t.length >= 5 ? t[4] : "0";
			String strand = t.length >= 6 ? t[5] : "+";
			String col = "128,128,128";
			out.println(chr + "\t" + start + "\t" + end + "\t" + id + "\t" + score + "\t" + strand + "\t"
				+ start + "\t" + end + "\t" + col);

			if (c == 10000) {
			    int reply = JOptionPane.showConfirmDialog(null,
				    "File contains a large number of intervals (>10000). "
					    + "It is not recommended to run GIE with such large interval sets. "
					    + "Continue importing?",
				    "Confirmation Dialog", JOptionPane.YES_NO_OPTION);
			    if (reply != JOptionPane.YES_OPTION) {
				break;
			    }
			}
		    } // bed
		} else if (fileType.equals("vcf")) {
		    /********
		     * VCF files
		     */
		    while ((nextLine = reader.readLine()) != null && (nextLine.trim().length() > 0)) {
			c++;
			String[] t = nextLine.split("\t");
			if (t[0].startsWith("#") || t[0].startsWith("browser ")) {
			    // FIXME: parse description if any
			    continue;
			}
			if (t.length < 4)
			    throw new IOException("Wrong format. Not a VCF file?");
			String chr = t[0];
			if (g != null)
			    chr = g.getCanonicalChrName(chr);
			Integer pos = Integer.parseInt(t[1]);
			String id = t[2];
			String ref = t[3];
			String alt = t[4];
			String qual = t[5];
			String filter = t[6];
			int score = (isPass(filter) ? 1000 : 0);
			String col = (isPass(filter) ? "0,0,0" : "128,128,128");
			out.println(chr + "\t" + pos + "\t" + (pos + getVCFVariantWidth(ref, alt)) + "\t"
				+ (ref + ">" + alt + " " + id) + "\t" + score + "\t0\t" + pos + "\t"
				+ (pos + getVCFVariantWidth(ref, alt)) + "\t" + col);

			if (c == 10000) {
			    int reply = JOptionPane.showConfirmDialog(null,
				    "File contains a large number of intervals (>10000). "
					    + "It is not recommended to run GIE with such large interval sets. "
					    + "Continue importing?",
				    "Confirmation Dialog", JOptionPane.YES_NO_OPTION);
			    if (reply != JOptionPane.YES_OPTION) {
				break;
			    }
			}
		    }
		} // vcf
	    }
	} catch (Exception e) {
	    // delete outfile
	    if (out != null) {
		out.close();
		out = null;
	    }
	    dataFile.delete();
	} finally {
	    if (out != null)
		out.close();
	    if (reader != null)
		try {
		    reader.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
	return load();
    }

    /**
     * Update and save layer
     * 
     * @param rois
     */
    public void updateAndSave() {
	// System.out.println("Saving current intervals to " + getDataFile());
	if (regions == null)
	    regions = new TreeSet<>();
	regions.clear();
	regions.addAll(IGV.getInstance().getSession().getAllRegionsOfInterest());
	save();
    }

    /**
     * Save current layer
     * 
     * @param rois
     */
    public void save() {
	GIE.getInstance().export2bed(regions, getDataFile(),
		getVersion().getDataset().getName() + "." + getVersion().getVersionName() + "." + layerName,
		"GIE data track", false, false, false, true, annotations);
	if (getDataFile().length() != dataFileSize)
	    setLastModified(new Date());
	setDataFileSize(getDataFile().length());
    }

    /**
     * Delete the current layer.
     * 
     * @return
     */
    public boolean delete() {
	boolean success = true;
	if (dataFile != null)
	    success = success & dataFile.delete();
	// System.out.println("DELETING " + dataFile + ":" + success);
	return success;
    }

    public SortedSet<RegionOfInterest> getRegions() {
	if (regions == null)
	    load();
	return regions;
    }

    public void setRegions(SortedSet<RegionOfInterest> regions) {
	this.regions = regions;
    }

    public long getDataFileSize() {
	return dataFileSize;
    }

    public void setDataFileSize(long dataFileSize) {
	this.dataFileSize = dataFileSize;
    }

    public File getDataFile() {
	return dataFile;
    }

    public void setDataFile(File dataFile) {
	this.dataFile = dataFile;
    }

    public String getLayerName() {
	if (this.layerName == null)
	    this.layerName = GIE.getInstance().findDatasetVersionLayerName(this);
	return layerName;
    }

    public void setLayerName(String layerName) {
	this.layerName = layerName;
    }

    public GIEDatasetVersion getVersion() {
	if (this.version == null)
	    this.version = GIE.getInstance().findDatasetVersion(this);
	return version;
    }

    public String[] getAnnotations() {
	return annotations;
    }

    public void setAnnotations(String[] newAnno) {
	this.annotations = newAnno;
    }

    public String getLastModified() {
	return lastModified;
    }

    public void setLastModified(String lastModified) {
	this.lastModified = lastModified;
    }

    public void setLastModified(Date date) {
	this.lastModified = sdf.format(new Date());
    }

    public List<File> getAllFiles() {
	List<File> ret = new ArrayList<>();
	ret.add(dataFile);
	return ret;
    }

    public void updateFilePaths(Map<File, File> fileMap) {
	if (fileMap.containsKey(dataFile))
	    dataFile = fileMap.get(dataFile);
    }

    public String getDescription() {
	return description;
    }

    public void setDescription(String description) {
	this.description = description;
    }

    @Override
    public String toString() {
	return "[l" + layerName + "@" + dataFile + "]";
    }

}
