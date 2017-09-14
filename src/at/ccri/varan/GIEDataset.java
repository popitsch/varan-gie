package at.ccri.varan;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class GIEDataset {

    private static Logger log = Logger.getLogger(GIEDataset.class);

    /**
     * Default name of initial version.
     */
    public final static String defaultVersionTag = "ver1";

    /**
     * The original BED file
     */
    File orig = null;

    /**
     * All edited versions. The last one is the autosave version.
     */
    Map<String, GIEDatasetVersion> versions = new LinkedHashMap<>();

    /**
     * Category for organizing datasets.
     */
    String category;

    /**
     * Currently selected version
     */
    transient GIEDatasetVersion currentVersion;

    /**
     * Name of this dataset
     */
    private transient String name;

    public GIEDataset() {
    }

    public GIEDataset(String datasetName, String description, File orig, String[] annotations) throws IOException {

	this.name = datasetName;

	// create initial version
	currentVersion = new GIEDatasetVersion(this, description, GIE.defaultAuthor, defaultVersionTag, annotations);
	versions.put(defaultVersionTag, currentVersion);

	// import data and load dataset
	this.orig = orig;
	currentVersion.getActiveLayer().importAndLoad(orig);
    }

    public File getOrig() {
	return orig;
    }

    public void setOrig(File orig) {
	this.orig = orig;
    }

    public Map<String, GIEDatasetVersion> getVersions() {
	return versions;
    }

    public GIEDatasetVersion getCurrentVersion() {
	return currentVersion;
    }

    public void setCurrentVersion(GIEDatasetVersion currentVersion) {
	this.currentVersion = currentVersion;
    }

    /**
     * Select the dataset version with the passed tag
     * 
     * @param tag
     * @return
     */
    public boolean selectVersion(String tag) {
	if (!versions.containsKey(tag))
	    return false;
	this.currentVersion = versions.get(tag);
	return true;
    }

    @Override
    public String toString() {

	return "[" + getName() + (orig != null ? "(from " + orig.getName() + ")" : "") + " (" + versions.size()
		+ " versions)]";
    }

    /**
     * 
     * @return dataset name
     */
    public String getName() {
	if (this.name == null)
	    this.name = GIE.getInstance().findDatasetName(this);
	return this.name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getCategory() {
	return category;
    }

    public void setCategory(String category) {
	this.category = category;
    }

    /**
     * Adds a version to an existing dataset. The versiontag must be unique.
     * 
     * @param k
     * @throws
     */
    public boolean addVersion(GIEDatasetVersion ver) {
	try {

	    if (ver.getVersionName() == null)
		return false;
	    if (versions.keySet().contains(ver.getVersionName()))
		return false;

	    versions.put(ver.getVersionName(), ver);
	    return true;
	} catch (Exception e) {
	    e.printStackTrace();
	    log.error("Error adding new dataset version: " + e.getMessage());
	    return false;
	}
    }

    /**
     * Deletes a version from this dataset
     * 
     * @param version
     * @return
     */
    public boolean deleteVersion(String version) {
	GIEDatasetVersion ver = getVersions().get(version);
	if (ver == null)
	    return false;
	if (!ver.delete())
	    return false;
	versions.remove(version);
	return true;
    }

    /**
     * Rename a version
     * 
     * @throws IOException
     */
    public boolean renameVersion(String oldKey, String newKey) {
	if (versions.containsKey(newKey) || !versions.containsKey(oldKey)) {
	    log.error("Cannot rename versions '" + oldKey + "' to '" + newKey + "'.");
	    return false;
	}
	GIEDatasetVersion obj = versions.remove(oldKey);
	obj.setVersionName(newKey);
	versions.put(newKey, obj);
	save();
	return true;
    }

    /**
     * Load current version/layer into a list of ROIs.
     * 
     * @return
     */
    public void save() {
	if (currentVersion == null) {
	    log.error("No current version - cannot save");
	} else
	    currentVersion.save();
    }

    /**
     * Return the last created version.
     */
    public GIEDatasetVersion getLatestCreatedVersion() {
	if (versions == null || versions.size() == 0)
	    return null;
	ArrayList<GIEDatasetVersion> v = new ArrayList<>(versions.values());
	return v.get(v.size() - 1);
    }

    public List<File> getAllFiles() {
	List<File> ret = new ArrayList<>();
	for (GIEDatasetVersion v : versions.values())
	    ret.addAll(v.getAllFiles());
	return ret;
    }

    public void updateFilePaths(Map<File, File> fileMap) {
	for (GIEDatasetVersion v : versions.values())
	    v.updateFilePaths(fileMap);
    }

}
