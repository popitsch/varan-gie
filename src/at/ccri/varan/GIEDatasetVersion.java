package at.ccri.varan;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.broad.igv.ui.IGV;

import at.ccri.varan.ui.UndoHandler;

/**
 * A particular GIE dataset version.
 * 
 * @author niko.popitsch
 *
 */
public class GIEDatasetVersion {

    private static Logger log = Logger.getLogger(GIEDatasetVersion.class);

    /**
     * Default name of main layer.
     */
    public final static String defaultLayerName = "main";

    File sessionFile;

    String author;

    private transient GIEDataset dataset;

    private transient String versionName;

    String description;

    /**
     * All layers
     */
    Map<String, GIEDatasetVersionLayer> layers = new LinkedHashMap<>();

    /**
     * The currently active layer.
     */
    transient GIEDatasetVersionLayer activeLayer = null;

    transient String[] annotations = null;

    public GIEDatasetVersion() {
    }

    public GIEDatasetVersion(GIEDataset dataset, String description, String author, String versionName,
	    String[] annotations) throws IOException {

	this.dataset = dataset;
	this.versionName = versionName;
	this.author = author;
	this.description = description;

	this.sessionFile = new File(GIE.GIE_DIRECTORY,
		URLEncoder.encode("gie." + dataset.getName() + "." + versionName + ".igvsession.xml", "UTF-8"));
	if (sessionFile.exists())
	    throw new IOException("CANNOT create version as dataFile or sessionFile already exists.");

	// create a default layer
	activeLayer = new GIEDatasetVersionLayer(this, defaultLayerName, annotations);
	this.layers.put(defaultLayerName, activeLayer);

    }

    public boolean renameLayer(String oldName, String newName) throws IOException {
	if (newName == null || oldName == null || !layers.containsKey(oldName) || oldName.equals(defaultLayerName))
	    return false;
	newName = newName.trim();

	GIEDatasetVersionLayer layer = layers.remove(oldName);
	layer.setLayerName(newName);
	this.layers.put(newName, layer);
	save();
	return true;
    }

    /**
     * Remove a layer.
     * 
     * @param layerName
     * @return
     * @throws IOException
     */
    public boolean delLayer(String layerName) throws IOException {
	if (!layers.containsKey(layerName) || layerName.equals(defaultLayerName))
	    return false;
	GIEDatasetVersionLayer layer = layers.get(layerName);
	if (!layer.delete())
	    return false;
	this.layers.remove(layerName);
	// delete layer track
	GIE.getInstance().removeActiveLayerTrack();

	this.activeLayer = getDefaultLayer();

	IGV.getInstance().getSession().clearRegionsOfInterest();
	IGV.getInstance().addROI(activeLayer.getRegions());
	GIE.getInstance().reloadActiveDataset();

	UndoHandler.getInstance().clear(); // no undo before this point

	return true;
    }

    /**
     * Copy layers + data from other version.
     * 
     * @param ver
     * @throws IOException
     */
    public void copyLayersFrom(GIEDatasetVersion ver) throws IOException {
	// copy default layer
	layers.get(defaultLayerName).loadFromFile(ver.getLayers().get(defaultLayerName).getDataFile());
	// copy all other layers.
	for (String k : ver.getLayers().keySet()) {
	    if (k.equals(defaultLayerName))
		continue;
	    if (layers.get(k) == null) {
		GIEDatasetVersionLayer l = new GIEDatasetVersionLayer(this, k, getDefaultLayer().getAnnotations());
		this.layers.put(k, l);
	    }
	    layers.get(k).loadFromFile(ver.getLayers().get(k).getDataFile());
	}
    }

    /**
     * Add a layer.
     * 
     * @param layerName
     * @return
     * @throws IOException
     */
    public boolean addLayer(String layerName) throws IOException {
	if (layers.containsKey(layerName))
	    return false;

	// save current layer
	activeLayer.updateAndSave();

	// create new layer
	layerName = layerName.trim();
	activeLayer = new GIEDatasetVersionLayer(this, layerName, getDefaultLayer().getAnnotations());
	activeLayer.load();

	this.layers.put(layerName, activeLayer);

	GIE.getInstance().reloadActiveDataset();

	UndoHandler.getInstance().clear(); // no undo before this point

	return true;
    }

    public void setDefaultActiveLayer() {
	setActiveLayer(defaultLayerName);
    }

    public void setActiveLayer(String layerName) {
	if (!layers.containsKey(layerName)) {
	    log.error("Could not find layer " + layerName);
	    return;
	}

	// save current layer
	this.activeLayer.updateAndSave();

	// set new active layer
	this.activeLayer = layers.get(layerName);

	if (this.activeLayer.getRegions() == null)
	    this.activeLayer.load();
	else
	    IGV.getInstance().getSession().clearRegionsOfInterest();
	IGV.getInstance().addROI(this.activeLayer.getRegions());

	GIE.getInstance().reloadActiveDataset();

	UndoHandler.getInstance().clear(); // no undo before this point
    }

    public GIEDatasetVersionLayer getDefaultLayer() {
	return layers.get(defaultLayerName);
    }

    public GIEDatasetVersionLayer getActiveLayer() {
	if (activeLayer == null) {
	    activeLayer = getDefaultLayer();
	}
	return activeLayer;
    }

    public File getSessionFile() {
	return sessionFile;
    }

    public String getAuthor() {
	return author;
    }

    public void setAuthor(String author) {
	this.author = author;
    }

    /**
     * 
     * @return version name
     */
    public String getVersionName() {
	if (this.versionName == null)
	    this.versionName = GIE.getInstance().findDatasetVersionName(this);
	return this.versionName;
    }

    public void setVersionName(String versionName) {
	this.versionName = versionName;
    }

    /**
     * 
     * @return the dataset of this version.
     */
    public GIEDataset getDataset() {
	if (this.dataset == null) {
	    this.dataset = GIE.getInstance().findDataset(this);
	}
	return dataset;
    }

    public boolean delete() {
	boolean success = true;
	// delete all layers
	for (GIEDatasetVersionLayer l : layers.values())
	    success = success & l.delete();
	// delete the associated IGV session
	if (sessionFile != null)
	    success = success & sessionFile.delete();

	UndoHandler.getInstance().clear(); // no undo before this point

	return success;
    }

    public String getDescription() {
	return description;
    }

    public void setDescription(String description) {
	this.description = description;
    }

    public Map<String, GIEDatasetVersionLayer> getLayers() {
	return layers;
    }

    @Override
    public String toString() {
	return "[v " + versionName + "]";
    }

    public void save() {
	if (activeLayer == null)
	    throw new RuntimeException("No active layer - cannot save");
	activeLayer.updateAndSave();
    }

    public List<File> getAllFiles() {
	List<File> ret = new ArrayList<>();
	ret.add(sessionFile);
	for (GIEDatasetVersionLayer l : layers.values())
	    ret.addAll(l.getAllFiles());
	return ret;
    }

    public void updateFilePaths(Map<File, File> fileMap) {
	if (fileMap.containsKey(sessionFile))
	    sessionFile = fileMap.get(sessionFile);
	for (GIEDatasetVersionLayer l : layers.values())
	    l.updateFilePaths(fileMap);
    }

}
