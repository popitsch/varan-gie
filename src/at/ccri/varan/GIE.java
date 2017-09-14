package at.ccri.varan;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.broad.igv.DirectoryManager;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.feature.genome.GenomeListItem;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.track.Track;
import org.broad.igv.ui.GlobalKeyDispatcher;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.Main;
import org.broad.igv.ui.WaitCursorManager;
import org.broad.igv.ui.action.SaveSessionMenuAction;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.ui.util.ProgressBar;
import org.broad.igv.ui.util.ProgressMonitor;
import org.broad.igv.ui.util.UIUtilities;
import org.broad.igv.util.LongRunningTask;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.util.UnzipGenomes;
import org.broad.igv.util.Utilities;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import at.ccri.varan.ui.CanonicalChromsomeComparator;
import at.ccri.varan.ui.GIEDataDialog;
import at.ccri.varan.ui.GIEMainDialog;
import at.ccri.varan.ui.GIEPathMapDialog;

/**
 * Main GIE singleton.
 * 
 * Contains core GIE functionality to add/remove datasets/versions and annotation tracks and represents central configuration that is serialized to
 * "gie.conf.json".
 * 
 * @author niko.popitsch
 *
 */
public class GIE {

    private static Logger log = Logger.getLogger(GIE.class);

    /**
     * GIE Version
     */
    public static final String VERSION = "0.2.3";

    public static final String AUTHORS = "niko.popitsch@ccri.at";

    /**
     * The GIE application directory
     */
    static File GIE_DIRECTORY;

    /**
     * The GIE config file
     */
    static File GIE_CONFIG_FILE;

    /**
     * author
     */
    public static final String defaultAuthor = "GIE";

    /**
     * Default TSV export headers
     */
    public String[] TSVHeaders = new String[] { "Chr", "Start", "End", "Name", "Strand", "Score", "Color" };

    /**
     * All GIE datasets
     */
    Map<String, GIEDataset> datasets = new LinkedHashMap<>();

    /**
     * the currently active dataset
     */
    transient private GIEDataset activeDataset = null;

    /**
     * current genomic interval BED tracks
     */
    transient private List<Track> activeDatasetVersionTracks = new ArrayList<>();

    /**
     * current layer BED tracks
     */
    transient private Track layerTrack = null;

    /**
     * singleton instance.
     */
    transient private static GIE instance;

    /**
     * File to check whether GIE is running
     */
    transient private static File lockFile;

    /**
     * GIE annotation tracks
     */
    List<GIEAnnotationTrack> annotationTracks = new ArrayList<>();

    /**
     * Last coordinates of the GIE windows.
     */
    Map<String, Integer[]> windowCoordinates = new HashMap<String, Integer[]>();

    /**
     * Last accessed directories. Used for FileChooser dialog.
     */
    Map<String, File> lastAccessedDirectories = new HashMap<String, File>();

    private boolean showRefLines;;

    /**
     * Workaround for GSON bug with serialization of windows UNC paths.
     * TODO add checksum test
     * 
     * @author niko.popitsch
     *
     */
    class FileTypeAdaptor implements JsonDeserializer<File>, JsonSerializer<File> {

	@Override
	public File deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
		throws JsonParseException {
	    return new File(FilenameUtils.separatorsToSystem(((JsonPrimitive) json).getAsString()));
	}

	@Override
	public JsonElement serialize(File f, Type typeOfT, JsonSerializationContext context) {
	    return new JsonPrimitive(f.getAbsolutePath());
	}

    }

    public static int SCREEN_WIDTH = 0;
    public static int SCREEN_HEIGHT = 0;

    /**
     * Contructor
     */
    private GIE() {
	// calculate full screen size in multi-screen settings
	SCREEN_WIDTH = 0;
	SCREEN_HEIGHT = 0;
	GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	GraphicsDevice[] gs = ge.getScreenDevices();
	for (GraphicsDevice curGs : gs) {
	    DisplayMode mode = curGs.getDisplayMode();
	    SCREEN_WIDTH += mode.getWidth();
	    SCREEN_HEIGHT = mode.getHeight();
	}
    }

    /**
     * 
     * @return the singleton instance. Writes a .lock file to ensure that only one instance of igv+gie is running. Deserializes configuration properties from
     *         "gie.conf.json"
     */
    public static synchronized GIE getInstance() {
	if (GIE.instance == null) {

	    // central GIE directory
	    GIE_DIRECTORY = new File(DirectoryManager.getIgvDirectory(), "gie");
	    if (!GIE_DIRECTORY.exists())
		GIE_DIRECTORY.mkdir();
	    log.info("GIE Directory: " + GIE_DIRECTORY.getAbsolutePath());

	    // check for .lock file
	    try {
		lockFile = new File(GIE_DIRECTORY, "gie.lock");
		if (lockFile.exists()) {
		    int reply = JOptionPane.showConfirmDialog(null,
			    "Cannot start new GIE instance as .lock file was found. Please make sure that no GIE instance is running and press YES to remove the .lock file or NO to exit",
			    "Confirmation Dialog", JOptionPane.YES_NO_OPTION);
		    if (reply != JOptionPane.YES_OPTION) {
			System.exit(1);
		    }
		}
		// create lockfile.
		lockFile.createNewFile();
	    } catch (IOException e) {
		JOptionPane.showMessageDialog(IGV.getMainFrame(), e.getMessage(), "Error",
			JOptionPane.INFORMATION_MESSAGE);
		log.error(e.getMessage());
		System.exit(1);
	    }

	    GIE.instance = new GIE();
	    // deserialize from central configuration file
	    GIE_CONFIG_FILE = new File(GIE_DIRECTORY, "gie.conf.json");
	    if (!GIE_CONFIG_FILE.exists()) {
		// start with empty config
	    } else {
		Gson gson = new GsonBuilder().registerTypeAdapter(File.class, GIE.instance.new FileTypeAdaptor())
			.create();
		try (Reader reader = new InputStreamReader(new FileInputStream(GIE_CONFIG_FILE), "UTF-8")) {
		    GIE.instance = gson.fromJson(reader, GIE.class);
		} catch (Exception e) {
		    e.printStackTrace();
		    log.error("Error initializing GIE: " + e.getMessage());
		}
	    }
	}
	return GIE.instance;
    }

    /**
     * Autosave the GIE configuration to "gie.conf.json"
     */
    public void save() {
	try {
	    // save current active dataset
	    if (activeDataset != null) {
		// System.err.println("************************************");
		log.info("**** GIE AUTOSAVE " + activeDataset + " *******");
		// System.err.println("************************************");

		activeDataset.save();
		// save IGV session
		// log.info("Save session to " + activeDataset.getCurrentVersion().getSessionFile() + " / "
		// + IGV.getInstance().getAllTracks().size() + " > locus="
		// + IGV.getInstance().getSession().getLocusString());
		if (activeDataset.getCurrentVersion().getSessionFile() != null) {
		    SaveSessionMenuAction.saveSession(IGV.getInstance(),
			    activeDataset.getCurrentVersion().getSessionFile());
		} else
		    log.error("Could not save session");
	    }

	    // save config to string
	    StringWriter sw = new StringWriter();
	    Gson gson = new GsonBuilder().registerTypeAdapter(File.class, new FileTypeAdaptor()).setPrettyPrinting()
		    .create();
	    gson.toJson(this, sw);
	    sw.close();

	    // store to file
	    Writer wout = new OutputStreamWriter(new FileOutputStream(GIE_CONFIG_FILE), "UTF-8");
	    wout.write(sw.toString());
	    wout.close();

	} catch (Exception e) {
	    e.printStackTrace();
	    log.error("Error saving active dataset: " + e.getMessage());
	}
    }

    /**
     * Reload the active dataset
     * 
     * @return
     */
    public boolean reloadActiveDataset() {
	if (activeDataset == null)
	    return false;
	return loadDataset(activeDataset, activeDataset.getCurrentVersion().getVersionName());
    }

    /**
     * Load a dataset version by name.
     * 
     * @param name
     * @return
     */
    public boolean loadDataset(String name, String ver) {
	if (!datasets.containsKey(name)) {
	    log.error("Dataset '" + name + "' was not found.");
	    return false;
	}
	return loadDataset(datasets.get(name), ver);
    }

    public String findDatasetName(GIEDataset ds) {
	for (final String key : datasets.keySet()) {
	    if (datasets.get(key).equals(ds))
		return key;
	}
	return null;
    }

    public GIEDataset findDataset(GIEDatasetVersion ver) {
	for (final GIEDataset ds : datasets.values()) {
	    if (ds.getVersions().containsValue(ver))
		return ds;
	}
	return null;
    }

    public String findDatasetVersionName(GIEDatasetVersion ver) {
	for (final GIEDataset ds : datasets.values()) {
	    for (final String key : ds.getVersions().keySet())
		if (ds.getVersions().get(key).equals(ver))
		    return key;
	}
	return null;
    }

    public GIEDatasetVersion findDatasetVersion(GIEDatasetVersionLayer lay) {
	for (final GIEDataset ds : datasets.values()) {
	    for (final GIEDatasetVersion ver : ds.getVersions().values()) {
		if (ver.getLayers().containsValue(lay))
		    return ver;
	    }
	}
	return null;
    }

    public String findDatasetVersionLayerName(GIEDatasetVersionLayer lay) {
	for (final GIEDataset ds : datasets.values()) {
	    for (final GIEDatasetVersion ver : ds.getVersions().values()) {
		for (final String key : ver.getLayers().keySet())
		    if (ver.getLayers().get(key).equals(lay))
			return key;
	    }
	}
	return null;
    }

    /**
     * remove active dataset tracks
     */
    public void removeGIETracks() {
	if (getActiveDatasetTracks() != null) {
	    IGV.getInstance().removeTracks(getActiveDatasetTracks());
	}
	layerTrack = null;
    }

    /**
     * remove active layer track
     */
    public void removeActiveLayerTrack() {
	if (layerTrack != null) {
	    ArrayList<Track> tracksToRemove = new ArrayList<>();
	    tracksToRemove.add(layerTrack);
	    IGV.getInstance().removeTracks(tracksToRemove);
	}
	layerTrack = null;
    }

    public void updateLayerTrack() {
	if (activeDataset == null)
	    layerTrack = null;
	else
	    for (Track t : IGV.getInstance().getAllTracks()) {
		if (t.getResourceLocator() != null) {
		    String tpath = new File(t.getResourceLocator().getPath()).getAbsolutePath();
		    if (tpath.equals(
			    activeDataset.getCurrentVersion().getActiveLayer().getDataFile().getAbsolutePath())) {
			layerTrack = t;
			t.load(FrameManager.getDefaultFrame());
		    }
		}
	    }
    }

    /**
     * Load a dataset version by name.
     * 
     * @param merge
     *            if true, the current IGV session is merged with the newly loaded one.
     * @return
     */
    private boolean loadDataset(GIEDataset ds, String ver) {

	GlobalKeyDispatcher.blockKeys = true;
	GIEDataDialog.blockReload = true;

	try {

	    final Runnable runnable = new Runnable() {
		ProgressMonitor monitor;
		ProgressBar.ProgressDialog progressDialog;

		public void run() {
		    UIUtilities.invokeAndWaitOnEventThread(() -> {
			monitor = new ProgressMonitor();
			progressDialog = ProgressBar.showProgressDialog(IGV.getMainFrame(),
				"Loading Dataset " + ds.getName() + "...", monitor, false);
		    });

		    try {

			// store current dataset?
			if (activeDataset != null)
			    save();

			// unload current data track if any
			removeGIETracks();

			if (ds != null) {
			    if (ver == null)
				ds.setCurrentVersion(ds.getLatestCreatedVersion());
			    else if (!ds.selectVersion(ver)) {
				throw new RuntimeException("Could not select version " + ver);
			    }
			}
			//
			// System.out.println("selected version " + ver + ": " + ds.getCurrentVersion());

			if (ds != null && ds.getCurrentVersion() != null
				&& ds.getCurrentVersion().getSessionFile() != null
				&& ds.getCurrentVersion().getSessionFile().exists()) {

			    // restore session for new active track (will autosave current session)
			    IGV.getInstance().restoreSessionSynchronous(
				    ds.getCurrentVersion().getSessionFile().getAbsolutePath(), null, false);

			} else {
			    // new session (will autosave current session)
			    IGV.getInstance().newSession();
			}

			// set new active dataset
			activeDataset = ds;

			// make sure that associated data tracks are loaded
			if (activeDataset != null) {
			    Set<String> loadedPaths = new HashSet<>();
			    for (Track t : IGV.getInstance().getAllTracks()) {
				if (t.getResourceLocator() != null) {
				    String tpath = new File(t.getResourceLocator().getPath()).getAbsolutePath();
				    loadedPaths.add(tpath);
				}
			    }

			    List<ResourceLocator> toLoad = new ArrayList<ResourceLocator>();
			    Set<String> neededPaths = new HashSet<>();
			    for (GIEDatasetVersionLayer layer : activeDataset.getCurrentVersion().getLayers()
				    .values()) {
				String rpath = layer.getDataFile().getAbsolutePath();
				neededPaths.add(rpath);
				if (!loadedPaths.contains(rpath)) {
				    ResourceLocator locator = new ResourceLocator(rpath);
				    toLoad.add(locator);
				}
			    }

			    IGV.getInstance().loadResources(toLoad);

			    activeDatasetVersionTracks.clear();
			    for (Track t : IGV.getInstance().getAllTracks()) {
				if (t.getResourceLocator() != null) {
				    String tpath = new File(t.getResourceLocator().getPath()).getAbsolutePath();
				    if (tpath.equals(activeDataset.getCurrentVersion().getActiveLayer().getDataFile()
					    .getAbsolutePath()))
					layerTrack = t;
				    if (neededPaths.contains(tpath))
					activeDatasetVersionTracks.add(t);
				}
			    }

			    // load genomic regions
			    activeDataset.getCurrentVersion().getActiveLayer().load();
			    // update igv regions data struct
			    IGV.getInstance().getSession().clearRegionsOfInterest();
			    IGV.getInstance().addROI(activeDataset.getCurrentVersion().getActiveLayer().getRegions());
			}

			// show region navigator
			if (GIEMainDialog.getInstance() != null) {
			    GIEMainDialog.getInstance().refresh();
			}
			IGV.getInstance().doRefresh();

			// reload data window if any
			if (GIEDataDialog.getInstance() != null) {
			    GIEDataDialog.destroyInstance();
			}
			GIEDataDialog.getInstance(IGV.getMainFrame());

			log.info("Loaded dataset" + ds.getName());

		    } finally {
			if (progressDialog != null) {
			    UIUtilities.invokeOnEventThread(() -> progressDialog.setVisible(false));
			}
			GlobalKeyDispatcher.blockKeys = false;
			GIEDataDialog.blockReload = false;
			if (GIEDataDialog.getInstance() != null)
			    GIEDataDialog.getInstance().refresh();
		    }
		}
	    };

	    if (SwingUtilities.isEventDispatchThread()) {
		LongRunningTask.submit(runnable);
	    } else {
		runnable.run();
	    }

	} catch (

	Exception ex) {
	    ex.printStackTrace();
	    return false;
	}
	return true;
    }

    /**
     * Rename a dataset
     * 
     * @throws IOException
     */
    public boolean renameDataset(String oldKey, String newKey) throws IOException {
	if (datasets.containsKey(newKey) || !datasets.containsKey(oldKey)) {
	    log.error("Cannot rename dataset '" + oldKey + "' to '" + newKey + "'.");
	    return false;
	}
	// FIXME: also rename data files.
	GIEDataset obj = datasets.remove(oldKey);
	obj.setName(newKey);
	datasets.put(newKey, obj);
	save();
	return true;
    }

    /**
     * Add a new dataset
     * 
     * @throws IOException
     * 
     */
    public boolean addDataset(String datasetName, String description, String[] annotations, File orig)
	    throws IOException {

	try {

	    if (orig != null) {
		if (!orig.exists()) {
		    log.error("Dataset file " + orig + " not found!");
		    return false;
		}
		log.info("Loading intervals from " + orig);
	    }

	    if (datasetName == null || datasetName.equals("")) {
		log.error("Dataset '" + datasetName + "' not allowed.");
		return false;
	    }

	    if (datasets.containsKey(datasetName)) {
		log.error("Dataset '" + datasetName
			+ "' already exists. Delete first to replace or choose different name.");
		return false;
	    }

	    GIEDataset ds = new GIEDataset(datasetName, description, orig, annotations);
	    datasets.put(datasetName, ds);
	    log.info("Added dataset " + datasetName);
	    return true;
	} catch (UnsupportedEncodingException e1) {
	    e1.printStackTrace();
	    return false;
	}
    }

    /**
     * Deletes a dataset version + the dataset if this was its only version.
     * 
     */
    public boolean deleteDatasetVersion(String name, String version) {
	if (!datasets.containsKey(name)) {
	    log.error("Dataset '" + name + "' not found.");
	    return false;
	}
	GIEDataset d = datasets.get(name);

	if (!d.deleteVersion(version)) {
	    JOptionPane.showMessageDialog(IGV.getMainFrame(),
		    "Could not delete dataset '" + name + "' version " + version, "Error", JOptionPane.ERROR_MESSAGE);
	    log.error("Could not delete dataset '" + name + "' version " + version);
	    return false;
	}
	if (d.getVersions().size() == 0)
	    datasets.remove(name);
	activeDataset = null;
	save();
	return true;
    }

    /**
     * Deletes a dataset and all its versions.
     * 
     * @param name
     * @param orig
     */
    public boolean deleteDataset(String name) {
	if (!datasets.containsKey(name)) {
	    log.error("Dataset '" + name + "' not found.");
	    return false;
	}
	GIEDataset d = datasets.get(name);
	for (GIEDatasetVersion v : d.getVersions().values()) {
	    if (!v.delete()) {
		JOptionPane.showMessageDialog(IGV.getMainFrame(),
			"Could not delete dataset '" + name + "' version file " + v, "Error",
			JOptionPane.ERROR_MESSAGE);
		log.error("Could not delete dataset '" + name + "' version file " + v);
		return false;
	    }
	}
	datasets.remove(name);
	activeDataset = null;
	save();
	return true;
    }

    public boolean exportDataset(String name, File outFile) throws IOException {
	List<String> names = new ArrayList<>();
	names.add(name);
	return exportDatasets(names, outFile);
    }

    public final static String DATASET_JSON_FN = "GIE.dataset.json";

    /**
     * Exports datasets and all their versions.
     * 
     * @param name
     * @param orig
     * @throws IOException
     */
    public boolean exportDatasets(List<String> names, File outFile) throws IOException {
	Map<String, GIEDataset> dsMap = new LinkedHashMap<>();
	List<File> allFiles = new ArrayList<>();
	for (String name : names) {
	    if (!datasets.containsKey(name)) {
		log.error("Dataset '" + name + "' not found.");
		return false;
	    }
	    GIEDataset d = datasets.get(name);
	    allFiles.addAll(d.getAllFiles());
	    dsMap.put(name, d);
	}

	// autosave
	save();

	// save dsMap to file
	String tDir = System.getProperty("java.io.tmpdir");
	File tmpFile = new File(tDir, DATASET_JSON_FN);
	tmpFile.deleteOnExit();
	PrintWriter out = new PrintWriter(tmpFile);
	Gson gson = new GsonBuilder().registerTypeAdapter(File.class, new FileTypeAdaptor()).setPrettyPrinting()
		.create();
	gson.toJson(dsMap, out);
	out.close();

	allFiles.add(tmpFile);
	boolean ret = zipFiles(allFiles, outFile);
	tmpFile.delete();
	return ret;
    }

    /**
     * Import a dataset from a ZIP file.
     * 
     * @param zipFile
     * @return
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    @SuppressWarnings("unchecked")
    public boolean importDatasets(File zipFile) throws IOException, ParserConfigurationException, SAXException {
	File tempDir = null;
	try {
	    // create temp dir
	    tempDir = createTempDirectory();
	    UnzipGenomes.unzip(zipFile, tempDir);

	    File jfile = new File(tempDir, DATASET_JSON_FN);

	    Gson gson = new GsonBuilder().registerTypeAdapter(File.class, GIE.instance.new FileTypeAdaptor()).create();
	    Reader reader = new InputStreamReader(new FileInputStream(jfile), "UTF-8");
	    Type listType = new TypeToken<LinkedHashMap<String, GIEDataset>>() {
	    }.getType();
	    LinkedHashMap<String, GIEDataset> dsMap = (LinkedHashMap<String, GIEDataset>) gson.fromJson(reader,
		    listType);
	    for (String k : dsMap.keySet()) {
		if (datasets.containsKey(k)) {
		    throw new IOException("Cannot import dataset " + k + " as dataset with same name already exists!");
		}
		GIEDataset ds = dsMap.get(k);
		// System.out.println("Importing dataset " + k + " / " + ds);

		// check whether we would overwrite existing files
		for (File origF : ds.getAllFiles()) {
		    File df = new File(GIE_DIRECTORY, origF.getName());
		    if (df.exists())
			throw new IOException(
				"Cannot import dataset as datafile " + df + " already exists in home dir.");
		}

		// copy files
		Map<File, File> pathMap = new HashMap<>();
		for (File origF : ds.getAllFiles()) {

		    // copy files to gie home dir and update file paths.
		    File tempF = new File(tempDir, origF.getName());
		    File df = new File(GIE_DIRECTORY, origF.getName());

		    if (origF.getAbsolutePath().endsWith("igvsession.xml")) {

			/**
			 * Extract genome id and file paths from igv session file
			 * 
			 */
			Document document = null;
			FileInputStream is = null;

			List<File> externalPaths = new ArrayList<>();
			try {
			    is = new FileInputStream(tempF);
			    document = Utilities.createDOMDocumentFromXmlStream(is);

			    // get the remote genome id
			    String remoteGenomeId = document.getElementsByTagName("Session").item(0).getAttributes()
				    .getNamedItem("genome").getNodeValue();
			    String localgenomeId = remoteGenomeId;
			    // get available genome ids
			    Map<String, String> existingGenomeIds = new HashMap<>();
			    for (GenomeListItem gi : GenomeManager.getInstance().getGenomeListItems())
				existingGenomeIds.put(gi.getId(), gi.getDisplayableName());
			    if (!existingGenomeIds.containsKey(remoteGenomeId)) {
				String[] choices = existingGenomeIds.keySet()
					.toArray(new String[existingGenomeIds.size()]);
				// we have to map the genome id first
				String choice = null;
				if (choices.length > 0)
				    choice = (String) JOptionPane.showInputDialog(null,
					    "<html><body>This dataset refers to a genome with id <b>'" + remoteGenomeId
						    + "'</b> but no local genome with that ID was found.<br/>"
						    + "Select the respective local genome or cancel and add genome first</body></html>"
						    + "",
					    "Map genome id", JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]); // Initial choice
				if (choice == null)
				    throw new IOException("Cannot import dataset as referenced genome " + remoteGenomeId
					    + " was not found");
				localgenomeId = choice;
			    }

			    // get the remote home directory
			    File oldHomeDir = new File(
				    FilenameUtils.separatorsToSystem(document.getElementsByTagName("Session").item(0)
					    .getAttributes().getNamedItem("path").getNodeValue())).getParentFile();

			    // get paths from Resource ids
			    NodeList resources = document.getElementsByTagName("Resource");
			    for (int i = 0; i < resources.getLength(); i++) {
				File resFile = new File(FilenameUtils.separatorsToSystem(
					resources.item(i).getAttributes().getNamedItem("path").getNodeValue()));
				if (resFile.getParentFile() == null)
				    continue;
				// if (!resFile.isAbsolute())
				// continue;
				if (resFile.getParentFile() == null)
				    continue;
				if (resFile.getParentFile().getCanonicalPath().equals(oldHomeDir.getCanonicalPath()))
				    continue;
				if (externalPaths.contains(resFile))
				    continue;
				externalPaths.add(resFile);
			    }

			    boolean mappingComplete = true;
			    if (externalPaths != null && externalPaths.size() > 0) {
				for (File oldFile : externalPaths) {
				    if (!oldFile.exists()) {
					mappingComplete = false;
					// String newPath = JOptionPane.showInputDialog(IGV.getMainFrame(),
					// "<html><body>The following file could not be located on your local system:<br/><b>"
					// + oldFile.getAbsolutePath() + "</b><br/>"
					// + "Please provide a new (valid) location for this file or cancel import:",
					// oldFile.getAbsolutePath());
					// if (newPath == null)
					// return false;
					// extPathMapping.put(oldFile, new File(newPath));
				    }
				}
			    }

			    Map<String, File> extPathMapping = new HashMap<>();
			    if (!mappingComplete) {
				JOptionPane.showMessageDialog(null,
					"<html><body>"
						+ "Some file links in the imported dataset are not valid/broken on your local system.<br/>"
						+ "The following dialog enables you to 'fix' these links by providing valid file referrences.<br/>"
						+ "If you want to replace a subpath string (e.g., replace 'c:/' with 'd:/'), you may use the <br/>"
						+ "find/replace functionality.");
				GIEPathMapDialog d = new GIEPathMapDialog(IGV.getMainFrame(), externalPaths);
				if (d.wasCanceled()) {
				    return false;
				}
				extPathMapping = d.getEditedPathMapping();
			    }

			    // copy + re-root the file
			    rerootIgvSession(tempF, df, extPathMapping, localgenomeId);
			} finally {
			    try {
				is.close();
			    } catch (IOException e) {
				e.printStackTrace();
			    }
			}

		    } else {
			// just copy the file
			FileUtils.copyFile(tempF, df);
		    }
		    if (!df.exists())
			log.warn("WARNING: file " + df + " was not found.");
		    pathMap.put(origF, df);
		}
		ds.updateFilePaths(pathMap);
		datasets.put(k, dsMap.get(k));
		log.info("Added " + datasets.get(k));
	    }

	} finally {
	    if (tempDir != null)
		tempDir.delete();
	}
	return true;
    }

    /**
     * Will read a IGV session.xml file from oldSessF and move it to newSessF. Relative paths in the new session
     * file will be updated accordingly.
     * 
     * @param oldSessF
     * @param newSessF
     * @return
     * @throws IOException
     */
    public static void rerootIgvSession(File oldSessF, File newSessF, Map<String, File> extPathMapping,
	    String localgenomeId) throws IOException {
	Document document = null;
	FileInputStream is = null;
	PrintWriter out = null;
	try {
	    is = new FileInputStream(oldSessF);
	    document = Utilities.createDOMDocumentFromXmlStream(is);
	    NodeList resources = document.getElementsByTagName("Session");
	    // get the old home directory
	    File oldHomeDir = new File(FilenameUtils
		    .separatorsToSystem(resources.item(0).getAttributes().getNamedItem("path").getNodeValue()))
			    .getParentFile();
	    File newHomeDir = newSessF.getParentFile();

	    // set new genome id
	    resources.item(0).getAttributes().getNamedItem("genome").setNodeValue(localgenomeId);

	    // set new igv_session path
	    resources.item(0).getAttributes().getNamedItem("path").setNodeValue(newSessF.getCanonicalPath());

	    // update all resource paths
	    resources = document.getElementsByTagName("Resource");
	    for (int i = 0; i < resources.getLength(); i++) {
		File resFile = new File(FilenameUtils
			.separatorsToSystem(resources.item(i).getAttributes().getNamedItem("path").getNodeValue()));
		if (resFile.getParentFile() != null
			&& resFile.getParentFile().getCanonicalPath().equals(oldHomeDir.getCanonicalPath())) {
		    resFile = newHomeDir == null ? new File(resFile.getName())
			    : new File(newHomeDir, resFile.getName());
		    // System.err.println("UPDATED PATH " + resources.item(i).getAttributes().getNamedItem("path").getNodeValue()+" TO " + resFile);
		    resources.item(i).getAttributes().getNamedItem("path").setNodeValue(resFile.getCanonicalPath());
		}
		// System.out.println("Searching for " + resFile.getCanonicalPath() + " in " + extPathMapping.keySet());
		for (String exF : extPathMapping.keySet()) {
		    if (resFile.getCanonicalPath().startsWith(exF)) {
			// use startsWith() to handle "Id's" that were postfixed with "_"...
			String postFix = resFile.getCanonicalPath().substring(exF.length());
			resFile = new File(extPathMapping.get(exF).getCanonicalPath() + postFix);
			// System.err.println("UPDATED PATH2 " + resources.item(i).getAttributes().getNamedItem("path").getNodeValue()+" TO " + resFile);
			resources.item(i).getAttributes().getNamedItem("path").setNodeValue(resFile.getCanonicalPath());
		    }
		}

	    }
	    // update track ids
	    resources = document.getElementsByTagName("Track");
	    for (int i = 0; i < resources.getLength(); i++) {
		File resFile = new File(FilenameUtils
			.separatorsToSystem(resources.item(i).getAttributes().getNamedItem("id").getNodeValue()));
		boolean changed = false;
		if (resFile.getParentFile() != null
			&& resFile.getParentFile().getCanonicalPath().equals(oldHomeDir.getCanonicalPath())) {
		    resFile = newHomeDir == null ? new File(resFile.getName())
			    : new File(newHomeDir, resFile.getName());
		    // System.err.println("UPDATED TRACK HOME PATH " + resFile);
		    changed = true;
		}
		// System.out.println("Searching for " + resFile.getCanonicalPath() + " in " + extPathMapping.keySet());
		for (String exF : extPathMapping.keySet()) {
		    if (resFile.getCanonicalPath().startsWith(exF)) {
			// use startsWith() to handle "Id's" that were postfixed with "_"...
			String postFix = resFile.getCanonicalPath().substring(exF.length());
			resFile = new File(extPathMapping.get(exF).getCanonicalPath() + postFix);
			changed = true;
		    }
		}
		if (changed) {
		    // System.err.println("UPDATED ID " + resources.item(i).getAttributes().getNamedItem("id").getNodeValue()+" TO " + resFile);
		    resources.item(i).getAttributes().getNamedItem("id").setNodeValue(resFile.getCanonicalPath());
		}
	    }

	    // write session
	    String xmlString = Utilities.getString(document);
	    out = new PrintWriter(newSessF);
	    out.println(xmlString);

	} catch (Exception e) {
	    log.error("Load session error", e);
	    throw new IOException(e);
	} finally {
	    is.close();
	    if (out != null)
		out.close();
	}

    }

    /**
     * Create temporary dir.
     * 
     * @return
     * @throws IOException
     */
    public static File createTempDirectory() throws IOException {
	final File temp;
	temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
	if (!(temp.delete())) {
	    throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
	}
	if (!(temp.mkdir())) {
	    throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
	}
	temp.deleteOnExit();
	return (temp);
    }

    /**
     * ZIP a list of files w/o path.
     * 
     * @param files
     * @param outFile
     * @return
     */
    public boolean zipFiles(List<File> files, File outFile) {
	ZipOutputStream zos = null;
	try {
	    zos = new ZipOutputStream(new FileOutputStream(outFile));
	    byte[] readBuffer = new byte[2156];
	    int bytesIn = 0;
	    for (File f : files) {
		FileInputStream fis = new FileInputStream(f);
		zos.putNextEntry(new ZipEntry(f.getName())); // filename w/o path
		// now write the content of the file to the ZipOutputStream
		while ((bytesIn = fis.read(readBuffer)) != -1) {
		    zos.write(readBuffer, 0, bytesIn);
		}
		// close the Stream
		fis.close();
	    }
	    return true;
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	} finally {
	    if (zos != null)
		try {
		    zos.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
    }

    public Set<String> getCategories() {
	SortedSet<String> ret = new TreeSet<>();
	for (GIEDataset ds : getDatasets().values())
	    if (ds.getCategory() != null)
		ret.add(ds.getCategory());
	return ret;
    }

    public List<GIEDataset> getDatasets(String category) {
	List<GIEDataset> ret = new ArrayList<>();
	for (GIEDataset ds : getDatasets().values()) {
	    if (ds.getCategory() == null || ds.getCategory().equals("") || ds.getCategory().equals(category)
		    || category.equals(GIEMainDialog.FILTER_SHOW_ALL))
		ret.add(ds);
	}
	if (ret.size() == 0)
	    ret.addAll(datasets.values());
	return ret;
    }

    public Map<String, GIEDataset> getDatasets() {
	return datasets;
    }

    public void setDatasets(Map<String, GIEDataset> datasets) {
	this.datasets = datasets;
    }

    public GIEDataset getActiveDataset() {
	return activeDataset;
    }

    public String getActiveDatasetName() {
	if (activeDataset == null)
	    return null;
	return activeDataset.getName();
    }

    public String getActiveDatasetVersion() {
	if (activeDataset == null)
	    return null;
	if (activeDataset.getCurrentVersion() == null)
	    return null;
	return activeDataset.getCurrentVersion().getVersionName();
    }

    public void setActiveDataset(GIEDataset activeDataset) {
	this.activeDataset = activeDataset;
    }

    /**
     * @return a proposed tag for the next version of the active dataset or null if none.
     */
    public String getProposedNextVersiontag() {
	if (activeDataset == null)
	    return null;
	String tag = activeDataset.getLatestCreatedVersion().getVersionName();
	// parse trailing number from tag if any
	StringBuilder num = new StringBuilder();
	String prefix = "";
	for (int i = tag.length() - 1; i >= 0; i--) {
	    char c = tag.charAt(i);
	    if (Character.isDigit(c)) {
		num.insert(0, c);
	    } else {
		prefix = tag.substring(0, i + 1);
		break;
	    }
	}
	int v = 0;
	if (num.length() > 0)
	    v = Integer.parseInt(num.toString()) + 1;
	return prefix + v;
    }

    /**
     * Check whether the passed track is the currently active GIE track.
     * 
     * @param t
     * @return
     */
    public boolean isActiveTrack(Track t) {
	if (layerTrack == null)
	    return false;
	return layerTrack.equals(t);
    }

    public List<Track> getActiveDatasetTracks() {
	return activeDatasetVersionTracks;
    }

    public Track getLayerTrack() {
	return layerTrack;
    }

    public String[] getTSVHeaders() {
	return TSVHeaders;
    }

    public void addAnnotationTrack(GIEAnnotationTrack t) {
	this.annotationTracks.add(t);
    }

    public Map<String, Integer[]> getWindowCoordinates() {
	return windowCoordinates;
    }

    public void setWindowCoordinates(Map<String, Integer[]> windowCoordinates) {
	this.windowCoordinates = windowCoordinates;
    }

    public void delAnnotationTrack(GIEAnnotationTrack t) {
	this.annotationTracks.remove(t);
    }

    public List<GIEAnnotationTrack> getAnnotationTracks() {
	return annotationTracks;
    }

    public void setAnnotationTracks(List<GIEAnnotationTrack> annotationTracks) {
	this.annotationTracks = annotationTracks;
    }

    public Map<String, File> getLastAccessedDirectories() {
	return lastAccessedDirectories;
    }

    public void setLastAccessedDirectories(Map<String, File> lastAccessedDirectories) {
	this.lastAccessedDirectories = lastAccessedDirectories;
    }

    public void setLastAccessedDirectory(String key, File f) {
	this.lastAccessedDirectories.put(key, f);
    }

    public void close() {
	save();
	if (lockFile != null && !lockFile.delete())
	    log.error("Could not remove .lock file " + lockFile);
    }

    /**
     * helper function.
     * 
     * @param t
     * @param alt
     * @return
     */
    private String noNullNoTab(String t, String alt) {
	if (t == null)
	    return alt;
	return t.replaceAll("\t", "");
    }

    /**
     * Export data to UCSC file
     * 
     * @param file
     */
    public boolean export2bed(Collection<RegionOfInterest> rois, File outFile, String name, String description,
	    boolean prefixChr, boolean exportOnlyBasic, boolean noHeader, boolean includeAnnotations,
	    String[] annotations) {

	if (!outFile.getParentFile().exists()) {
	    JOptionPane.showMessageDialog(IGV.getMainFrame(),
		    "Cannot export to non-existing directory " + outFile.getParentFile());
	    return false;
	}
	if (!outFile.getName().toLowerCase().endsWith(".bed")) {
	    JOptionPane.showMessageDialog(IGV.getMainFrame(), "File extension has to be .bed ");
	    return false;
	}
	description = description.replaceAll("\"", "'");

	PrintWriter out = null;
	WaitCursorManager.CursorToken token = WaitCursorManager.showWaitCursor();
	try {
	    out = new PrintWriter(outFile);
	    if (!noHeader) {
		if (exportOnlyBasic) {
		    out.println("track name=\"" + name + "\" description=\"" + description + "\"");
		} else {
		    out.println("track name=\"" + name + "\" description=\"" + description
			    + "\" visibility=1 useScore=1 itemRgb=\"On\"");
		}
	    }
	    for (RegionOfInterest r : rois) {
		String chr = r.getChr();
		if (prefixChr) {
		    if (!chr.startsWith("chr"))
			chr = "chr" + chr;
		}
		if (exportOnlyBasic) {
		    out.println(chr + "\t" + r.getStart() + "\t" + r.getEnd() + "\t" + r.getDescription());
		} else {
		    out.print(chr + "\t" + r.getStart() + "\t" + r.getEnd() + "\t" + r.getDescription() + "\t"
			    + r.getScore() + "\t" + r.getStrand() + "\t" + r.getStart() + "\t" + r.getEnd() + "\t"
			    + r.getColor());
		    for (String cn : annotations)
			try {
			    String a = r.getAnnotation(cn);
			    if (a == null || a.equals(""))
				out.print("\t");
			    else
				out.print("\t" + URLEncoder.encode(r.getAnnotation(cn), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
			    e.printStackTrace();
			}
		    out.println();
		}
	    }
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	    JOptionPane.showMessageDialog(IGV.getMainFrame(), "There was an error exporting the data");
	    return false;
	} finally {
	    WaitCursorManager.removeWaitCursor(token);
	    IGV.getInstance().resetStatusMessage();
	    if (out != null)
		out.close();
	}
	return true;
    }

    /**
     * Export data to UCSC file
     * 
     * @param file
     * @throws IOException
     */
    public boolean export2tsv(Collection<RegionOfInterest> intervals, File outFile, String name, String description,
	    boolean prefixChr, boolean exportOnlyBasic, boolean noHeader, String[] annotations) {

	if (!outFile.getParentFile().exists()) {
	    JOptionPane.showMessageDialog(IGV.getMainFrame(),
		    "Cannot export to non-existing directory " + outFile.getParentFile());
	    return false;
	}
	if (!outFile.getName().toLowerCase().endsWith(".tsv")) {
	    JOptionPane.showMessageDialog(IGV.getMainFrame(), "File extension has to be .tsv ");
	    return false;
	}
	if (description != null)
	    description = description.replaceAll("\"", "'");

	PrintWriter out = null;
	WaitCursorManager.CursorToken token = WaitCursorManager.showWaitCursor();
	try {
	    out = new PrintWriter(outFile);
	    if (!noHeader) {
		String[] h = GIE.getInstance().getTSVHeaders();
		if (exportOnlyBasic) {
		    // "Chr\tStart\tEnd\tName"
		    out.println(h[0] + "\t" + h[1] + "\t" + h[2] + "\t" + h[3]);
		} else {
		    // "Chr\tStart\tEnd\tName\tStrand\tScore\tColor"
		    if (description != null) {
			BufferedReader reader = new BufferedReader(new StringReader(description));
			String line = null;
			try {
			    while ((line = reader.readLine()) != null) {
				out.println("#" + line);
			    }
			} catch (IOException e) {
			    e.printStackTrace();
			}
		    }

		    out.print(h[0] + "\t" + h[1] + "\t" + h[2] + "\t" + h[3] + "\t" + h[4] + "\t" + h[5] + "\t" + h[6]);
		    for (String cn : annotations)
			out.print("\t" + cn);
		    out.println();
		}
	    }
	    for (RegionOfInterest r : intervals) {
		String chr = r.getChr();
		if (prefixChr) {
		    if (!chr.startsWith("chr"))
			chr = "chr" + chr;
		} else {
		    chr = CanonicalChromsomeComparator.getCanonicalMappingHuman(chr);
		}

		if (exportOnlyBasic) {
		    out.println(
			    chr + "\t" + r.getStart() + "\t" + r.getEnd() + "\t" + noNullNoTab(r.getDescription(), ""));
		} else {
		    out.print(chr + "\t" + r.getStart() + "\t" + r.getEnd() + "\t" + noNullNoTab(r.getDescription(), "")
			    + "\t" + noNullNoTab(r.getStrand(), "") + "\t" + noNullNoTab(r.getScore(), "") + "\t"
			    + noNullNoTab(r.getColor(), ""));
		    for (String cn : annotations)
			out.print("\t" + noNullNoTab(r.getAnnotation(cn), ""));
		    out.println();
		}
	    }
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	    JOptionPane.showMessageDialog(IGV.getMainFrame(), "There was an error exporting the data");
	    return false;
	} finally {
	    WaitCursorManager.removeWaitCursor(token);
	    IGV.getInstance().resetStatusMessage();
	    if (out != null)
		out.close();
	}
	return true;
    }

    public void toggleRefLines() {
	showRefLines = !showRefLines;
    }

    public boolean isShowRefLines() {
	return showRefLines;
    }

    public void setShowRefLines(boolean showRefLines) {
	this.showRefLines = showRefLines;
    }

    public static void main(String[] args) {
	Main.main(args);

    }

}
