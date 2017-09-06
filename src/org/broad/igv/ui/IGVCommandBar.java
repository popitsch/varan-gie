/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/*
 * IGVCommandBar.java
 *
 * Created on April 5, 2008, 10:02 AM
 */
package org.broad.igv.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;

import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.event.GenomeChangeEvent;
import org.broad.igv.event.GenomeResetEvent;
import org.broad.igv.event.IGVEventBus;
import org.broad.igv.event.IGVEventObserver;
import org.broad.igv.event.ViewChange;
import org.broad.igv.feature.Chromosome;
import org.broad.igv.feature.Cytoband;
import org.broad.igv.feature.FeatureDB;
import org.broad.igv.feature.NamedFeature;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.GenomeListItem;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.feature.genome.GenomeServerException;
import org.broad.igv.prefs.Constants;
import org.broad.igv.prefs.PreferencesManager;
import org.broad.igv.session.History;
import org.broad.igv.ui.action.FitDataToWindowMenuAction;
import org.broad.igv.ui.action.SearchCommand;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.ui.panel.IGVPopupMenu;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.ui.panel.ZoomSliderPanel;
import org.broad.igv.ui.util.IconFactory;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.ui.util.ProgressBar;
import org.broad.igv.ui.util.ProgressMonitor;
import org.broad.igv.ui.util.UIUtilities;
import org.broad.igv.util.LongRunningTask;

import com.jidesoft.hints.ListDataIntelliHints;
import com.jidesoft.swing.JideBoxLayout;
import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

import at.ccri.varan.ui.GIEMainDialog;
import at.ccri.varan.ui.UndoHandler;

/**
 * @author jrobinso
 */
public class IGVCommandBar extends javax.swing.JPanel implements IGVEventObserver {

    private static Logger log = Logger.getLogger(IGVCommandBar.class);

    final static String MODIFY_DETAILS_TOOLTIP = "Modify popup text behavior in data panels";

    private JComboBox chromosomeComboBox;
    private JComboBox genomeComboBox;
    // private JPanel geneListPanel;
    // private JideButton geneListLabel;
    private JideButton goButton;
    private JideButton homeButton;
    private JPanel locationPanel;
    private JideButton refreshButton;
    private JideToggleButton roiToggleButton;
    private JideToggleButton roiClipToggleButton;
    private JideToggleButton roiMergeToggleButton;
    private JideButton undoButton;
    private JideButton detailsBehaviorButton;
    private JideToggleButton rulerLineButton;
    private JTextField searchTextField;
    private JPanel toolPanel;
    private JPanel zoomControl;
    final private int DEFAULT_CHROMOSOME_DROPDOWN_WIDTH = 120;
    private JideButton backButton;
    private JideButton forwardButton;
    private JideButton fitToWindowButton;

    public enum SHOW_DETAILS_BEHAVIOR {
	HOVER("Show Details on Hover"), CLICK("Show Details on Click"), NEVER("Never Show Details");

	private final String label;

	private SHOW_DETAILS_BEHAVIOR(String label) {
	    this.label = label;
	}

	public String getLabel() {
	    return this.label;
	}
    }

    private SHOW_DETAILS_BEHAVIOR detailsBehavior = SHOW_DETAILS_BEHAVIOR.valueOf((PreferencesManager.getPreferences()
	    .get(Constants.DETAILS_BEHAVIOR_KEY, SHOW_DETAILS_BEHAVIOR.HOVER.name()).toUpperCase()));

    public SHOW_DETAILS_BEHAVIOR getDetailsBehavior() {
	return detailsBehavior;
    }

    public IGVCommandBar() {

	initComponents();

	// Initialize controls
	new SearchHints(this.searchTextField); // This has the side-effect, apparently, of enabling hints

	String currentChr = getDefaultReferenceFrame().getChrName();
	boolean isWholeGenome = currentChr.equals(Globals.CHR_ALL);

	chromosomeComboBox.setSelectedItem(currentChr);
	roiToggleButton.setEnabled(!isWholeGenome);
	roiClipToggleButton.setEnabled(!isWholeGenome);
	roiMergeToggleButton.setEnabled(!isWholeGenome);
	zoomControl.setEnabled(!isWholeGenome);

	detailsBehaviorButton.addMouseListener(new MouseAdapter() {
	    public void mousePressed(MouseEvent e) {
		getPopupMenuToolTipBehavior().show(e.getComponent(), e.getX(), e.getY());
	    }
	});

	IGVEventBus.getInstance().subscribe(ViewChange.class, this);
	IGVEventBus.getInstance().subscribe(GenomeChangeEvent.class, this);
	IGVEventBus.getInstance().subscribe(GenomeResetEvent.class, this);

    }

    private JPopupMenu getPopupMenuToolTipBehavior() {
	final JPopupMenu popup = new IGVPopupMenu();
	for (final SHOW_DETAILS_BEHAVIOR behavior : SHOW_DETAILS_BEHAVIOR.values()) {
	    JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(behavior.getLabel());
	    menuItem.setSelected(detailsBehavior == behavior);
	    menuItem.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    detailsBehavior = behavior;
		    PreferencesManager.getPreferences().put(Constants.DETAILS_BEHAVIOR_KEY, behavior.name());
		}
	    });
	    popup.add(menuItem);
	}
	return popup;
    }

    /**
     * This method is called once on startup
     *
     * @param monitor
     * @throws FileNotFoundException
     * @throws NoRouteToHostException
     */
    public void initializeGenomeList(final ProgressMonitor monitor)
	    throws FileNotFoundException, NoRouteToHostException {

	if (log.isDebugEnabled()) {
	    log.debug("Enter initializeGenomeList");
	}

	if (monitor != null) {
	    UIUtilities.invokeOnEventThread(() -> monitor.fireProgressChange(1));
	}

	UIUtilities.invokeAndWaitOnEventThread(() -> {
	    genomeComboBox.removeAllItems();
	    genomeComboBox.setRenderer(new ComboBoxRenderer());
	    genomeComboBox.setToolTipText(UIConstants.CHANGE_GENOME_TOOLTIP);
	});

	GenomeManager.getInstance().buildGenomeItemList();
	refreshGenomeListComboBox();

	if (monitor != null) {
	    UIUtilities.invokeOnEventThread(() -> monitor.fireProgressChange(50));
	}

	genomeComboBox.addActionListener(new GenomeBoxActionListener());

	// Post creation widget setup.
	searchTextField.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent actionevent) {
		goButtonActionPerformed(actionevent);
	    }
	});

	if (log.isDebugEnabled()) {
	    log.debug("Exit initializeGenomeList");
	}

    }

    class GenomeBoxActionListener implements ActionListener {

	public void actionPerformed(ActionEvent actionEvent) {

	    Object selItem = genomeComboBox.getSelectedItem();
	    if (!(selItem instanceof GenomeListItem)) {
		return;
	    }
	    GenomeListItem genomeListItem = (GenomeListItem) selItem;
	    loadGenomeListItem(genomeListItem);
	}

	private void loadGenomeListItem(final GenomeListItem genomeListItem) {
	    log.info("Enter genome combo box");

	    // If we haven't changed genomes do nothing
	    if (genomeListItem.getId().equalsIgnoreCase(GenomeManager.getInstance().getGenomeId())) {
		return;
	    }
	    log.info("Loading " + genomeListItem.printString());

	    final Runnable runnable = new Runnable() {

		ProgressMonitor monitor;
		ProgressBar.ProgressDialog progressDialog;

		public void run() {
		    if (genomeListItem != null && genomeListItem.getLocation() != null) {

			log.info("Loading " + genomeListItem.getId());

			// User selected "more", pull up dialog and revert combo box
			if (genomeListItem == GenomeListItem.ITEM_MORE) {
			    IGV.getInstance().loadGenomeFromServer();
			    return;
			}

			UIUtilities.invokeAndWaitOnEventThread(() -> {
			    monitor = new ProgressMonitor();
			    progressDialog = ProgressBar.showProgressDialog(IGV.getMainFrame(), "Loading Genome...",
				    monitor, false);
			});

			try {
			    GenomeManager.getInstance().loadGenome(genomeListItem.getLocation(), monitor);

			} catch (GenomeServerException e) {
			    log.error("Error loading genome: " + genomeListItem.getId() + "  "
				    + genomeListItem.getLocation(), e);
			    JOptionPane.showMessageDialog(IGV.getMainFrame(),
				    "Error loading genome: " + genomeListItem.getDisplayableName());
			} catch (Exception e) {
			    log.error(e);
			    int choice = JOptionPane.showConfirmDialog(IGV.getMainFrame(),
				    "The genome [" + genomeListItem.getId()
					    + "] could not be read. Would you like to remove the selected entry?",
				    "", JOptionPane.OK_CANCEL_OPTION);

			    if (choice == JOptionPane.OK_OPTION) {
				removeGenomeFromList();
				log.error("Error initializing genome", e);
			    }
			} finally {
			    if (progressDialog != null) {
				UIUtilities.invokeOnEventThread(() -> progressDialog.setVisible(false));
			    }
			}

			try {
			    // ++++++++++++++++++++++++++++++++++++++++++++++++
			    // Start GIE main dialog
			    if (GIEMainDialog.getInstance() == null)
				GIEMainDialog.getInstance(IGV.getMainFrame());
			    // ++++++++++++++++++++++++++++++++++++++++++++++++
			} catch (Exception e) {
			    log.error("Could not start VARAN-GIE");
			    e.printStackTrace();
			}
		    }
		}

		private void removeGenomeFromList() {
		    GenomeManager.getInstance().removeGenomeListItem(genomeListItem);
		    refreshGenomeListComboBox();
		}
	    };

	    // If we're on the dispatch thread spawn a worker, otherwise just execute.
	    if (SwingUtilities.isEventDispatchThread())

	    {
		LongRunningTask.submit(runnable);
	    } else {
		runnable.run();
	    }
	}
    }

    void updateChromosomeDropdown() {

	final Genome genome = GenomeManager.getInstance().getCurrentGenome();
	if (genome == null)
	    return;

	UIUtilities.invokeAndWaitOnEventThread(() -> {

	    List<String> tmp = new ArrayList<String>(genome.getAllChromosomeNames().size());
	    tmp.addAll(genome.getAllChromosomeNames());
	    if (tmp.size() > 1) {
		String homeChr = genome.getHomeChromosome();
		if (homeChr.equals(Globals.CHR_ALL)) {
		    tmp.add(0, Globals.CHR_ALL);
		}
	    }

	    Graphics2D graphics2D = (Graphics2D) chromosomeComboBox.getGraphics();
	    Font font = chromosomeComboBox.getFont();
	    FontMetrics fontMetrics = chromosomeComboBox.getFontMetrics(font);

	    int w = DEFAULT_CHROMOSOME_DROPDOWN_WIDTH;
	    for (String chromosomeName : tmp) {
		Rectangle2D textBounds = fontMetrics.getStringBounds(chromosomeName, graphics2D);
		if (textBounds != null) {
		    int width = textBounds.getBounds().width + 50;

		    // int width = chromosomeName.length()*fontSize-(fontSize*4); // TODO Hack figure out whats's wrong with previous line
		    if (width > w) {
			w = width;
		    }
		}
	    }

	    Object[] chomosomeNames = tmp.toArray();
	    final DefaultComboBoxModel defaultModel = new DefaultComboBoxModel(chomosomeNames);
	    final int dropdownWidth = w;

	    chromosomeComboBox.setModel(defaultModel);
	    chromosomeComboBox.setSelectedItem(genome.getHomeChromosome());
	    adjustChromosomeDropdownWidth(dropdownWidth);
	});

    }

    public void updateCurrentCoordinates() {

	String p = "";

	final String chrName = getDefaultReferenceFrame().getChrName();
	if (!Globals.CHR_ALL.equals(chrName) && !FrameManager.isGeneListMode()) {
	    p = getDefaultReferenceFrame().getFormattedLocusString();
	}
	final String position = p;
	final History history = IGV.getInstance().getSession().getHistory();

	UIUtilities.invokeOnEventThread(new Runnable() {
	    public void run() {
		searchTextField.setText(position);
		forwardButton.setEnabled(history.canGoForward());
		backButton.setEnabled(history.canGoBack());
		roiToggleButton.setEnabled(!Globals.CHR_ALL.equals(chrName));
		roiClipToggleButton.setEnabled(!Globals.CHR_ALL.equals(chrName));
		roiMergeToggleButton.setEnabled(!Globals.CHR_ALL.equals(chrName));
		zoomControl.setEnabled(!Globals.CHR_ALL.equals(chrName));
	    }
	});

    }

    private ReferenceFrame getDefaultReferenceFrame() {
	return FrameManager.getDefaultFrame();
    }

    public void setGeneListMode(boolean geneListMode) {

	genomeComboBox.setEnabled(!geneListMode);
	// locationPanel.setEnabled(!geneListMode);
	chromosomeComboBox.setEnabled(!geneListMode);
	// searchTextField.setEnabled(!geneListMode);
	// goButton.setEnabled(!geneListMode);
	zoomControl.setEnabled(!geneListMode);
	// homeButton.setEnabled(true);
	// roiToggleButton.setEnabled(!geneListMode);
    }

    static class ComboBoxRenderer implements ListCellRenderer {

	JSeparator separator;

	/**
	 * Constructs ...
	 */
	public ComboBoxRenderer() {
	    separator = new JSeparator(JSeparator.HORIZONTAL);
	}

	/**
	 * Method description
	 *
	 * @param list
	 * @param value
	 * @param index
	 * @param isSelected
	 * @param cellHasFocus
	 * @return
	 */
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
		boolean cellHasFocus) {
	    String text = (value == null) ? "" : value.toString();

	    Component renderer = null;

	    if (UIConstants.GENOME_LIST_SEPARATOR.equals(text)) {
		return separator;
	    }

	    if (text.equals(UIConstants.REMOVE_GENOME_LIST_MENU_ITEM)) {
		JLabel label = new JLabel(text);

		label.setOpaque(true);
		label.setBorder(new EmptyBorder(1, 1, 1, 1));
		renderer = label;
	    } else {

		JLabel label = new JLabel(text);

		label.setOpaque(true);
		label.setBorder(new EmptyBorder(1, 1, 1, 1));
		label.setSize(label.getWidth() + 10, label.getHeight());
		renderer = label;
	    }

	    // We call with a null list when setting width
	    if (list != null) {
		if (isSelected) {
		    renderer.setBackground(list.getSelectionBackground());
		    renderer.setForeground(list.getSelectionForeground());
		} else {
		    renderer.setBackground(list.getBackground());
		    renderer.setForeground(list.getForeground());
		}
		renderer.setFont(list.getFont());
	    }

	    return renderer;
	}
    }

    /**
     * Selects the first genome from the list which matches this genomeId.
     * If not found, checks genomes from the server/user-defined list
     *
     * @param genomeId
     */
    public void selectGenome(String genomeId) {
	log.info("Selecting genome " + genomeId);
	if (!GenomeManager.getInstance().getSelectableGenomeIDs().contains(genomeId)) {

	    boolean found = false;
	    try {
		found = GenomeManager.getInstance().loadFromArchive(genomeId);
	    } catch (IOException e) {
		MessageUtils.showErrorMessage("Error checking server/cache for genomeId " + genomeId, e);
	    }
	    if (found) {
		refreshGenomeListComboBox();
	    }
	}

	// Now select this item in the comboBox
	GenomeListItem matchingItem = GenomeManager.getInstance().getLoadedGenomeListItemById(genomeId);
	if (matchingItem != null) {
	    UIUtilities.invokeAndWaitOnEventThread(() -> genomeComboBox.setSelectedItem(matchingItem));
	}
    }

    public void updateChromosFromGenome(Genome genome) {

	for (Chromosome chr : genome.getChromosomes()) {
	    final List<Cytoband> cytobands = chr.getCytobands();
	    if (cytobands != null) {
		for (Cytoband cyto : cytobands) {
		    FeatureDB.addFeature(cyto.getLongName(), cyto, genome);
		}
	    }
	}
	updateChromosomeDropdown();

    }

    public void refreshGenomeListComboBox() {
	UIUtilities.invokeAndWaitOnEventThread(() -> {
	    genomeComboBox.setModel(getModelForGenomeListComboBox());
	    String curId = GenomeManager.getInstance().getGenomeId();
	    Object item = GenomeManager.getInstance().getLoadedGenomeListItemById(curId);
	    genomeComboBox.setSelectedItem(item);
	});
    }

    /**
     * Build a model for the genome combo box
     *
     * @return
     */
    private DefaultComboBoxModel getModelForGenomeListComboBox() {
	List<GenomeListItem> genomes = GenomeManager.getInstance().getGenomeListItems();
	genomes.add(GenomeListItem.ITEM_MORE);
	return new DefaultComboBoxModel(genomes.toArray(new GenomeListItem[0]));
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {

	setMinimumSize(new Dimension(200, 32));

	// setPreferredSize(new Dimension(800, 32));

	JideBoxLayout layout = new JideBoxLayout(this, JideBoxLayout.X_AXIS);

	setLayout(layout);

	// This controls the vertical height of the command bar

	locationPanel = new javax.swing.JPanel();
	locationPanel.setBorder(new LineBorder(Color.lightGray, 1, true));

	// BorderFactory.createMatteBorder(2, 2, 2, 2, Color.lightGray));
	// new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
	locationPanel.setPreferredSize(new java.awt.Dimension(150, 20));
	locationPanel.setLayout(new JideBoxLayout(locationPanel, JideBoxLayout.X_AXIS));
	locationPanel.setAlignmentY(CENTER_ALIGNMENT);
	locationPanel.add(Box.createRigidArea(new Dimension(10, 36)), JideBoxLayout.FIX);

	genomeComboBox = new JComboBox();
	genomeComboBox.setMinimumSize(new Dimension(180, 27));
	genomeComboBox.setPreferredSize(new Dimension(180, 27));

	genomeComboBox.addPopupMenuListener(new PopupMenuListener() {
	    @Override
	    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		try {
		    adjustPopupWidth(genomeComboBox);
		} catch (Exception e1) {
		    log.warn(e1.getMessage(), e1);
		}
	    }

	    @Override
	    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		// TODO
	    }

	    @Override
	    public void popupMenuCanceled(PopupMenuEvent e) {
		// TODO
	    }
	});

	locationPanel.add(genomeComboBox, JideBoxLayout.FIX);
	locationPanel.add(Box.createHorizontalStrut(5), JideBoxLayout.FIX);

	chromosomeComboBox = new javax.swing.JComboBox();
	chromosomeComboBox.setToolTipText("Select a chromosome to view");
	chromosomeComboBox.setMaximumSize(new java.awt.Dimension(DEFAULT_CHROMOSOME_DROPDOWN_WIDTH, 30));
	chromosomeComboBox.setMinimumSize(new java.awt.Dimension(DEFAULT_CHROMOSOME_DROPDOWN_WIDTH, 30));
	chromosomeComboBox.setPreferredSize(new java.awt.Dimension(DEFAULT_CHROMOSOME_DROPDOWN_WIDTH, 30));
	chromosomeComboBox.addActionListener(evt -> chromosomeComboBoxActionPerformed(evt));
	locationPanel.add(chromosomeComboBox, JideBoxLayout.FIX);
	locationPanel.add(Box.createHorizontalStrut(5), JideBoxLayout.FIX);

	searchTextField = new JTextField();
	searchTextField.setToolTipText("Enter a gene or locus, e.f. EGFR,   chr1,   or chr1:100,000-200,000");
	searchTextField.setMaximumSize(new java.awt.Dimension(250, 15));
	searchTextField.setMinimumSize(new java.awt.Dimension(100, 28));
	searchTextField.setPreferredSize(new java.awt.Dimension(230, 28));
	searchTextField.setAlignmentY(CENTER_ALIGNMENT);

	locationPanel.add(searchTextField, JideBoxLayout.FIX);

	goButton = new JideButton("Go");
	// goButton.setButtonStyle(ButtonStyle.TOOLBOX_STYLE);

	// goButton.setPreferredSize(new java.awt.Dimension(30, 30));
	// goButton.setMaximumSize(new java.awt.Dimension(30, 30));
	// goButton.setMinimumSize(new java.awt.Dimension(30, 30));
	// goButton.setText("Go");
	goButton.setToolTipText("Jump to gene or locus");
	goButton.addActionListener(new java.awt.event.ActionListener() {

	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		goButtonActionPerformed(evt);
	    }
	});
	locationPanel.add(goButton, JideBoxLayout.FIX);

	add(locationPanel, JideBoxLayout.FIX);

	add(Box.createHorizontalStrut(10), JideBoxLayout.FIX);

	toolPanel = new javax.swing.JPanel();
	toolPanel.setAlignmentX(RIGHT_ALIGNMENT);
	toolPanel.setLayout(new JideBoxLayout(toolPanel, JideBoxLayout.X_AXIS));
	// final Border toolButtonBorder = BorderFactory.createLineBorder(Color.gray, 1);

	homeButton = new com.jidesoft.swing.JideButton();
	homeButton.setAlignmentX(RIGHT_ALIGNMENT);
	// homeButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
	// homeButton.setBorder(toolButtonBorder);
	homeButton.setIcon(
		new javax.swing.ImageIcon(getClass().getResource("/toolbarButtonGraphics/navigation/Home24.gif")));
	homeButton.setMaximumSize(new java.awt.Dimension(32, 32));
	homeButton.setMinimumSize(new java.awt.Dimension(32, 32));
	homeButton.setPreferredSize(new java.awt.Dimension(32, 32));
	homeButton.setToolTipText("Jump to whole genome view");
	homeButton.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		homeButtonActionPerformed(evt);
	    }
	});
	toolPanel.add(homeButton, JideBoxLayout.FIX);

	// toolPanel.setBorder(
	// new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
	backButton = new JideButton();
	// backButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
	// backButton.setBorder(toolButtonBorder);
	backButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/left-arrow.gif")));
	backButton.setToolTipText("Go back");
	backButton.setMaximumSize(new java.awt.Dimension(32, 32));
	backButton.setMinimumSize(new java.awt.Dimension(32, 32));
	backButton.setPreferredSize(new java.awt.Dimension(32, 32));
	backButton.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		IGV.getInstance().getSession().getHistory().back();

	    }
	});
	backButton.setEnabled(false);
	toolPanel.add(backButton, JideBoxLayout.FIX);

	forwardButton = new JideButton();
	// forwardButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
	// forwardButton.setBorder(toolButtonBorder);
	forwardButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/right-arrow.gif")));
	forwardButton.setToolTipText("Go forward");
	forwardButton.setMaximumSize(new java.awt.Dimension(32, 32));
	forwardButton.setMinimumSize(new java.awt.Dimension(32, 32));
	forwardButton.setPreferredSize(new java.awt.Dimension(32, 32));
	forwardButton.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		IGV.getInstance().getSession().getHistory().forward();
	    }
	});
	forwardButton.setEnabled(false);
	toolPanel.add(forwardButton, JideBoxLayout.FIX);

	refreshButton = new com.jidesoft.swing.JideButton();
	// refreshButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
	// refreshButton.setBorder(toolButtonBorder);
	refreshButton.setAlignmentX(RIGHT_ALIGNMENT);
	refreshButton.setIcon(
		new javax.swing.ImageIcon(getClass().getResource("/toolbarButtonGraphics/general/Refresh24.gif"))); // NOI18N
	refreshButton.setMaximumSize(new java.awt.Dimension(32, 32));
	refreshButton.setMinimumSize(new java.awt.Dimension(32, 32));
	refreshButton.setPreferredSize(new java.awt.Dimension(32, 32));
	refreshButton.setToolTipText("Refresh the screen");
	refreshButton.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		refreshButtonActionPerformed(evt);
	    }
	});
	toolPanel.add(refreshButton, JideBoxLayout.FIX);

	Icon regionOfInterestIcon = IconFactory.getInstance().getIcon(IconFactory.IconID.REGION_OF_INTEREST);
	roiToggleButton = new JideToggleButton(regionOfInterestIcon);
	// roiToggleButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
	// roiToggleButton.setBorder(toolButtonBorder);
	roiToggleButton.setAlignmentX(RIGHT_ALIGNMENT);
	roiToggleButton.setToolTipText("Add genomic region (CTRL-ALT-A).");
	roiToggleButton.setMaximumSize(new java.awt.Dimension(32, 32));
	roiToggleButton.setMinimumSize(new java.awt.Dimension(32, 32));
	roiToggleButton.setPreferredSize(new java.awt.Dimension(32, 32));
	roiToggleButton.addActionListener(new java.awt.event.ActionListener() {

	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		roiToggleButtonActionPerformed(evt);
	    }
	});
	toolPanel.add(roiToggleButton, JideBoxLayout.FIX);

	Icon clipRegionOfInterestIcon = IconFactory.getInstance().getIcon(IconFactory.IconID.CLIP_REGION_OF_INTEREST);
	roiClipToggleButton = new JideToggleButton(clipRegionOfInterestIcon);
	roiClipToggleButton.setAlignmentX(RIGHT_ALIGNMENT);
	roiClipToggleButton.setToolTipText("Clip genomic region (CTRL-ALT-C).");
	roiClipToggleButton.setMaximumSize(new java.awt.Dimension(32, 32));
	roiClipToggleButton.setMinimumSize(new java.awt.Dimension(32, 32));
	roiClipToggleButton.setPreferredSize(new java.awt.Dimension(32, 32));
	roiClipToggleButton.addActionListener(new java.awt.event.ActionListener() {

	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		roiClipToggleButtonActionPerformed(evt);
	    }
	});
	toolPanel.add(roiClipToggleButton, JideBoxLayout.FIX);

	Icon mergeRegionOfInterestIcon = IconFactory.getInstance().getIcon(IconFactory.IconID.MERGE_REGION_OF_INTEREST);
	roiMergeToggleButton = new JideToggleButton(mergeRegionOfInterestIcon);
	roiMergeToggleButton.setAlignmentX(RIGHT_ALIGNMENT);
	roiMergeToggleButton.setToolTipText("Merge genomic regions (CTRL-ALT-M)");
	roiMergeToggleButton.setMaximumSize(new java.awt.Dimension(32, 32));
	roiMergeToggleButton.setMinimumSize(new java.awt.Dimension(32, 32));
	roiMergeToggleButton.setPreferredSize(new java.awt.Dimension(32, 32));
	roiMergeToggleButton.addActionListener(new java.awt.event.ActionListener() {

	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		roiMergeToggleButtonActionPerformed(evt);
	    }
	});
	toolPanel.add(roiMergeToggleButton, JideBoxLayout.FIX);

	Icon undoRegionOfInterestIcon = IconFactory.getInstance().getIcon(IconFactory.IconID.UNDO_REGION_OF_INTEREST);
	undoButton = new JideButton(undoRegionOfInterestIcon);
	undoButton.setAlignmentX(RIGHT_ALIGNMENT);
	undoButton.setToolTipText("Undo VARAN changes (CTRL-Z).");
	undoButton.setMaximumSize(new java.awt.Dimension(32, 32));
	undoButton.setMinimumSize(new java.awt.Dimension(32, 32));
	undoButton.setPreferredSize(new java.awt.Dimension(32, 32));
	undoButton.addActionListener(new java.awt.event.ActionListener() {

	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		IGV.getInstance().getSession().replaceRegionsOfInterest(UndoHandler.getInstance().undo());
	    }
	});
	toolPanel.add(undoButton, JideBoxLayout.FIX);

	fitToWindowButton = new JideButton();
	// fitToWindowButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
	// fitToWindowButton.setBorder(toolButtonBorder);
	fitToWindowButton.setAlignmentX(RIGHT_ALIGNMENT);
	fitToWindowButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/collapseall.gif")));
	fitToWindowButton.setMaximumSize(new java.awt.Dimension(32, 32));
	fitToWindowButton.setMinimumSize(new java.awt.Dimension(32, 32));
	fitToWindowButton.setPreferredSize(new java.awt.Dimension(32, 32));
	fitToWindowButton.setToolTipText("Resize tracks to fit in window.");
	fitToWindowButton.addActionListener(new java.awt.event.ActionListener() {

	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		(new FitDataToWindowMenuAction(null, 0, IGV.getInstance())).actionPerformed(evt);
	    }
	});
	toolPanel.add(fitToWindowButton, JideBoxLayout.FIX);

	final Icon noTooltipIcon = IconFactory.getInstance().getIcon(IconFactory.IconID.NO_TOOLTIP);
	final Icon tooltipIcon = IconFactory.getInstance().getIcon(IconFactory.IconID.TOOLTIP);
	detailsBehaviorButton = new JideButton(noTooltipIcon);

	// detailsBehaviorButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
	// detailsBehaviorButton.setBorder(toolButtonBorder);
	detailsBehaviorButton.setAlignmentX(RIGHT_ALIGNMENT);
	detailsBehaviorButton.setToolTipText(MODIFY_DETAILS_TOOLTIP);
	detailsBehaviorButton.setMaximumSize(new java.awt.Dimension(32, 32));
	detailsBehaviorButton.setMinimumSize(new java.awt.Dimension(32, 32));
	detailsBehaviorButton.setPreferredSize(new java.awt.Dimension(32, 32));
	toolPanel.add(detailsBehaviorButton, JideBoxLayout.FIX);

	rulerLineButton = new JideToggleButton();
	// roiToggleButton.setButtonStyle(JideButton.TOOLBOX_STYLE);
	// roiToggleButton.setBorder(toolButtonBorder);
	rulerLineButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/vertical-line.gif")));
	rulerLineButton.setAlignmentX(RIGHT_ALIGNMENT);
	rulerLineButton.setToolTipText("Enable ruler line in data panels");
	rulerLineButton.setMaximumSize(new java.awt.Dimension(32, 32));
	rulerLineButton.setMinimumSize(new java.awt.Dimension(32, 32));
	rulerLineButton.setPreferredSize(new java.awt.Dimension(32, 32));
	rulerLineButton.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		IGV.getInstance().setRulerEnabled(rulerLineButton.isSelected());
		IGV.getInstance().revalidateTrackPanels();
	    }
	});
	toolPanel.add(rulerLineButton, JideBoxLayout.FIX);

	this.add(toolPanel);

	this.add(Box.createHorizontalGlue(), JideBoxLayout.VARY);

	zoomControl = new ZoomSliderPanel();

	// zoomControl.setAlignmentX(RIGHT_ALIGNMENT);
	Dimension dimSize = new Dimension(200, 30);
	zoomControl.setPreferredSize(dimSize);
	zoomControl.setMinimumSize(dimSize);
	zoomControl.setMaximumSize(dimSize);
	zoomControl.setToolTipText("Click + to zoom in,  - to zoom out");
	zoomControl.setOpaque(false);
	this.add(zoomControl, JideBoxLayout.FIX);

	this.add(Box.createHorizontalStrut(20), JideBoxLayout.FIX);
    }

    /**
     * Method description
     *
     * @return
     */
    public GenomeListItem getGenomeSelectedInDropdown() {
	return (GenomeListItem) genomeComboBox.getSelectedItem();
    }

    private void adjustChromosomeDropdownWidth(int width) {

	int newWidth = (width > DEFAULT_CHROMOSOME_DROPDOWN_WIDTH) ? width : DEFAULT_CHROMOSOME_DROPDOWN_WIDTH;

	chromosomeComboBox.setMaximumSize(new java.awt.Dimension(newWidth, 35));
	chromosomeComboBox.setMinimumSize(new java.awt.Dimension(newWidth, 27));
	chromosomeComboBox.setPreferredSize(new java.awt.Dimension(newWidth, 16));
	revalidate();
    }

    /**
     * Adjust the popup for the combobox to be at least as wide as
     * the widest item.
     *
     * @param box
     */
    private void adjustPopupWidth(JComboBox box) {
	if (box.getItemCount() == 0)
	    return;
	Object comp = box.getUI().getAccessibleChild(box, 0);
	if (!(comp instanceof JPopupMenu)) {
	    return;
	}
	JPopupMenu popup = (JPopupMenu) comp;
	JScrollPane scrollPane = null;
	for (Component scomp : popup.getComponents()) {
	    if (scomp instanceof JScrollPane) {
		scrollPane = (JScrollPane) scomp;
	    }
	}
	if (scrollPane == null)
	    return;

	// Loop through and set width to widest component, plus some padding
	int rendererWidth = box.getWidth();
	for (int index = 0; index < box.getItemCount(); index++) {
	    Object value = box.getItemAt(index);
	    Component rendererComp = box.getRenderer().getListCellRendererComponent(null, value, index, false, false);
	}

	Dimension size = scrollPane.getPreferredSize();
	size.width = Math.max(size.width, rendererWidth);
	scrollPane.setPreferredSize(size);
	scrollPane.setMaximumSize(size);
	scrollPane.revalidate();
    }

    // <editor-fold desc="Action methods">
    private void homeButtonActionPerformed(java.awt.event.ActionEvent evt) {
	Genome genome = GenomeManager.getInstance().getCurrentGenome();
	if (FrameManager.isGeneListMode()) {
	    IGV.getInstance().setGeneList(null);
	}
	if (genome != null) {
	    String chrName = genome.getHomeChromosome();
	    if (chrName != null && !chrName.equals(chromosomeComboBox.getSelectedItem())) {
		getDefaultReferenceFrame().changeChromosome(chrName, false);
	    }
	}
    }

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {
	IGV.getInstance().doRefresh();
	System.gc();
    }

    private void chromosomeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {
	JComboBox combobox = (JComboBox) evt.getSource();
	final String chrName = (String) combobox.getSelectedItem();
	if (chrName != null & !chrName.equals(getDefaultReferenceFrame().getChrName())) {
	    getDefaultReferenceFrame().changeChromosome(chrName, true);
	}
    }

    private void goButtonActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_goButtonActionPerformed
	String searchText = searchTextField.getText();
	searchByLocus(searchText);
    }

    private void roiToggleButtonActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_roiToggleButtonActionPerformed
	roiClipToggleButton.setSelected(false);
	roiMergeToggleButton.setSelected(false);
	if (roiToggleButton.isSelected()) {
	    IGV.getInstance().beginROI(roiToggleButton);
	} else {
	    IGV.getInstance().endROI();
	}
    }

    private void roiClipToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {
	roiToggleButton.setSelected(false);
	roiMergeToggleButton.setSelected(false);
	if (roiClipToggleButton.isSelected()) {
	    IGV.getInstance().beginROIClip(roiClipToggleButton);
	} else {
	    IGV.getInstance().endROIClip();
	}
    }

    private void roiMergeToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {
	roiToggleButton.setSelected(false);
	roiClipToggleButton.setSelected(false);
	if (roiMergeToggleButton.isSelected()) {
	    IGV.getInstance().beginROIMerge(roiMergeToggleButton);
	} else {
	    IGV.getInstance().endROIMerge();
	}
    }

    // </editor-fold>

    public void receiveEvent(Object e) {

	if (e instanceof ViewChange) {
	    ViewChange event = (ViewChange) e;
	    if (event.type == ViewChange.Type.ChromosomeChange || event.type == ViewChange.Type.LocusChange) {
		String chrName = getDefaultReferenceFrame().getChrName();
		roiToggleButton.setEnabled(!Globals.CHR_ALL.equals(chrName));
		roiClipToggleButton.setEnabled(!Globals.CHR_ALL.equals(chrName));
		roiMergeToggleButton.setEnabled(!Globals.CHR_ALL.equals(chrName));
		zoomControl.setEnabled(!Globals.CHR_ALL.equals(chrName));
		if (!chrName.equals(chromosomeComboBox.getSelectedItem())) {
		    chromosomeComboBox.setSelectedItem(chrName);
		}
	    }

	    updateCurrentCoordinates();
	    repaint(); // TODO Is this neccessary?
	} else if (e instanceof GenomeChangeEvent) {
	    GenomeChangeEvent event = (GenomeChangeEvent) e;
	    Genome genome = event.genome;
	    refreshGenomeListComboBox();
	    updateChromosFromGenome(genome);
	} else if (e instanceof GenomeResetEvent) {
	    refreshGenomeListComboBox();
	} else {
	    log.info("Unknown event class: " + e.getClass());
	}
    }

    // <editor-fold desc="Search box">
    // Set the focus in the search box

    public void focusSearchBox() {
	searchTextField.requestFocusInWindow();
	searchTextField.selectAll();
    }

    public void searchByLocus(final String searchText) {

	if (log.isDebugEnabled()) {
	    log.debug("Enter search by locus: " + searchText);
	}

	if ((searchText != null) && (searchText.length() > 0)) {
	    String homeChr = IGV.getInstance().getGenomeManager().getCurrentGenome().getHomeChromosome();
	    if (searchText.equalsIgnoreCase("home") || searchText.equalsIgnoreCase(homeChr)) {
		homeButtonActionPerformed(null);
	    } else {
		searchTextField.setText(searchText);
		(new SearchCommand(getDefaultReferenceFrame(), searchText)).execute();
	    }
	}

	if (log.isDebugEnabled()) {
	    log.debug("Exit search by locus: " + searchText);
	}
    }

    private class SearchHints extends ListDataIntelliHints<String> {

	public SearchHints(JTextComponent jTextComponent) {
	    super(jTextComponent, new String[] {});
	}

	@Override
	public void acceptHint(Object context) {
	    String text = (String) context;
	    super.acceptHint(context);
	    searchByLocus(text);
	}

	@Override
	public boolean updateHints(Object context) {
	    String text = (String) context;
	    if (text.length() <= 1) {
		return false;
	    } else {
		// TODO Uncomment to use comprehensive feature search, note that it should support partial matches
		// List<NamedFeature> features = SearchCommand.comprehensiveFeatureSearch(text);
		List<NamedFeature> features = FeatureDB.getFeaturesList(text, SearchCommand.SEARCH_LIMIT);
		final List<SearchCommand.SearchResult> results = SearchCommand.getResults(features);
		Object[] list = SearchCommand.getSelectionList(results, false);
		if (list.length >= 1) {
		    this.setListData(list);
		    return true;
		}
	    }
	    return false;
	}
    }
    // </editor-fold>

    public JideToggleButton getRoiToggleButton() {
	return roiToggleButton;
    }

    public JideToggleButton getRoiClipToggleButton() {
	return roiClipToggleButton;
    }

    public JideToggleButton getRoiMergeToggleButton() {
	return roiMergeToggleButton;
    }
}
