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

package org.broad.igv.sam;


import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.feature.FeatureUtils;
import org.broad.igv.feature.Locus;
import org.broad.igv.feature.Range;
import org.broad.igv.feature.Strand;
import org.broad.igv.feature.genome.ChromosomeNameComparator;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.lists.GeneList;
import org.broad.igv.prefs.Constants;
import org.broad.igv.prefs.IGVPreferences;
import org.broad.igv.prefs.PreferenceEditorFX;
import org.broad.igv.prefs.PreferencesManager;
import org.broad.igv.renderer.GraphicUtils;
import org.broad.igv.session.IGVSessionReader;
import org.broad.igv.session.Session;
import org.broad.igv.session.SubtlyImportant;
import org.broad.igv.tools.PFMExporter;
import org.broad.igv.track.*;
import org.broad.igv.ui.FontManager;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.InsertSizeSettingsDialog;
import org.broad.igv.ui.SashimiPlot;
import org.broad.igv.ui.color.ColorTable;
import org.broad.igv.ui.color.ColorUtilities;
import org.broad.igv.ui.color.PaletteColorTable;
import org.broad.igv.event.AlignmentTrackEvent;
import org.broad.igv.event.IGVEventBus;
import org.broad.igv.event.IGVEventObserver;
import org.broad.igv.ui.panel.DataPanel;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.ui.panel.IGVPopupMenu;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.ui.util.UIUtilities;
import org.broad.igv.util.Pair;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.util.StringUtils;
import org.broad.igv.util.Utilities;
import org.broad.igv.util.blat.BlatClient;
import org.broad.igv.util.collections.CollUtils;
import org.broad.igv.util.extview.ExtendViewClient;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

import static org.broad.igv.prefs.Constants.*;

/**
 * @author jrobinso
 */
@XmlType(factoryMethod = "getNextTrack")
@XmlSeeAlso(AlignmentTrack.RenderOptions.class)

public class AlignmentTrack extends AbstractTrack implements IGVEventObserver {

    private static Logger log = Logger.getLogger(AlignmentTrack.class);

    public static final int GROUP_LABEL_HEIGHT = 10;

    static final int GROUP_MARGIN = 5;
    static final int TOP_MARGIN = 20;
    static final int DS_MARGIN_0 = 2;
    static final int DOWNAMPLED_ROW_HEIGHT = 3;
    static final int INSERTION_ROW_HEIGHT = 9;
    static final int DS_MARGIN_2 = 5;

    private final AlignmentRenderer renderer;

    private boolean removed = false;
    private RenderRollback renderRollback;
    private boolean showGroupLine;
    private Map<ReferenceFrame, List<InsertionInterval>> insertionIntervalsMap;

    public enum ShadeBasesOption {
        NONE, QUALITY
    }

    enum ColorOption {
        INSERT_SIZE, READ_STRAND, FIRST_OF_PAIR_STRAND, PAIR_ORIENTATION, SAMPLE, READ_GROUP, LIBRARY, BISULFITE, NOMESEQ,
        TAG, NONE, UNEXPECTED_PAIR, MAPPED_SIZE, LINK_STRAND
    }

    public enum SortOption {
        START, STRAND, NUCLEOTIDE, QUALITY, SAMPLE, READ_GROUP, INSERT_SIZE, FIRST_OF_PAIR_STRAND, MATE_CHR, TAG, SUPPLEMENTARY, NONE;
    }

    public enum GroupOption {
        STRAND, SAMPLE, READ_GROUP, LIBRARY, FIRST_OF_PAIR_STRAND, TAG, PAIR_ORIENTATION, MATE_CHROMOSOME, NONE, SUPPLEMENTARY, BASE_AT_POS
    }

    public enum BisulfiteContext {
        CG, CHH, CHG, HCG, GCH, WCG, NONE
    }

    enum OrientationType {
        RR, LL, RL, LR, UNKNOWN
    }

    protected static final Map<BisulfiteContext, String> bisulfiteContextToPubString = new HashMap<BisulfiteContext, String>();

    static {
        bisulfiteContextToPubString.put(BisulfiteContext.CG, "CG");
        bisulfiteContextToPubString.put(BisulfiteContext.CHH, "CHH");
        bisulfiteContextToPubString.put(BisulfiteContext.CHG, "CHG");
        bisulfiteContextToPubString.put(BisulfiteContext.HCG, "HCG");
        bisulfiteContextToPubString.put(BisulfiteContext.GCH, "GCH");
        bisulfiteContextToPubString.put(BisulfiteContext.WCG, "WCG");
        bisulfiteContextToPubString.put(BisulfiteContext.NONE, "None");
    }

    protected static final Map<BisulfiteContext, Pair<byte[], byte[]>> bisulfiteContextToContextString = new HashMap<BisulfiteContext, Pair<byte[], byte[]>>();

    static {
        bisulfiteContextToContextString.put(BisulfiteContext.CG, new Pair<byte[], byte[]>(new byte[]{}, new byte[]{'G'}));
        bisulfiteContextToContextString.put(BisulfiteContext.CHH, new Pair<byte[], byte[]>(new byte[]{}, new byte[]{'H', 'H'}));
        bisulfiteContextToContextString.put(BisulfiteContext.CHG, new Pair<byte[], byte[]>(new byte[]{}, new byte[]{'H', 'G'}));
        bisulfiteContextToContextString.put(BisulfiteContext.HCG, new Pair<byte[], byte[]>(new byte[]{'H'}, new byte[]{'G'}));
        bisulfiteContextToContextString.put(BisulfiteContext.GCH, new Pair<byte[], byte[]>(new byte[]{'G'}, new byte[]{'H'}));
        bisulfiteContextToContextString.put(BisulfiteContext.WCG, new Pair<byte[], byte[]>(new byte[]{'W'}, new byte[]{'G'}));
    }

    static final BisulfiteContext DEFAULT_BISULFITE_CONTEXT = BisulfiteContext.CG;

    private SequenceTrack sequenceTrack;
    private CoverageTrack coverageTrack;
    private SpliceJunctionTrack spliceJunctionTrack;

    private RenderOptions renderOptions = new RenderOptions(AlignmentDataManager.ExperimentType.OTHER);

    private int expandedHeight = 14;
    private int collapsedHeight = 9;
    private int maxSquishedHeight = 5;
    private int squishedHeight = maxSquishedHeight;

    private int minHeight = 50;
    private AlignmentDataManager dataManager;
    private Rectangle alignmentsRect;
    private Rectangle downsampleRect;
    private Rectangle insertionRect;
    private ColorTable readNamePalette;

    // Dynamic fields
    // The "DataPanel" containing the track.  This field might be null at any given time.  It is updated each repaint.
    private JComponent dataPanel;
    private HashMap<String, Color> selectedReadNames = new HashMap();
    private HashMap<Rectangle, String> groupNames = new HashMap<>();

    public static void sortAlignmentTracks(SortOption option, String tag) {

        IGV.getInstance().sortAlignmentTracks(option, tag);
        Collection<IGVPreferences> allPrefs = PreferencesManager.getAllPreferences();
        for (IGVPreferences prefs : allPrefs) {
            prefs.put(SAM_SORT_OPTION, option.toString());
            prefs.put(SAM_SORT_BY_TAG, tag);
        }
        refresh();
    }


    /**
     * Create a new alignment track
     *
     * @param locator
     * @param dataManager
     * @param genome
     */
    public AlignmentTrack(ResourceLocator locator, AlignmentDataManager dataManager, Genome genome) {
        super(locator);

        this.dataManager = dataManager;

        minimumHeight = 50;
        maximumHeight = Integer.MAX_VALUE;

        IGVPreferences prefs = getPreferences();

        renderer = new AlignmentRenderer(this);

        showGroupLine = getPreferences().getAsBoolean(SAM_SHOW_GROUP_SEPARATOR);
        setDisplayMode(DisplayMode.EXPANDED);

        if (prefs.getAsBoolean(SAM_SHOW_REF_SEQ)) {
            sequenceTrack = new SequenceTrack("Reference sequence");
            sequenceTrack.setHeight(14);
        }

        readNamePalette = new PaletteColorTable(ColorUtilities.getDefaultPalette());

        this.insertionIntervalsMap = Collections.synchronizedMap(new HashMap<>());

        IGVEventBus.getInstance().subscribe(FrameManager.ChangeEvent.class, this);
        IGVEventBus.getInstance().subscribe(ExperimentTypeChangeEvent.class, this);
        IGVEventBus.getInstance().subscribe(AlignmentTrackEvent.class, this);
    }


    @Override
    public void receiveEvent(Object event) {
        // Trim insertionInterval map to current frames
        if (event instanceof FrameManager.ChangeEvent) {
            Map<ReferenceFrame, List<InsertionInterval>> newMap = Collections.synchronizedMap(new HashMap<>());
            for (ReferenceFrame frame : ((FrameManager.ChangeEvent) event).getFrames()) {
                if (insertionIntervalsMap.containsKey(frame)) {
                    newMap.put(frame, insertionIntervalsMap.get(frame));
                }
            }
            insertionIntervalsMap = newMap;
        } else if (event instanceof ExperimentTypeChangeEvent) {
            if (((ExperimentTypeChangeEvent) event).source == dataManager) {
                log.info("Experiment type = " + ((ExperimentTypeChangeEvent) event).type);
                renderOptions = new RenderOptions(((ExperimentTypeChangeEvent) event).type);

                boolean showJunction = getPreferences().getAsBoolean(Constants.SAM_SHOW_JUNCTION_TRACK);
                if (showJunction != spliceJunctionTrack.isVisible()) {
                    spliceJunctionTrack.setVisible(showJunction);
                    IGV.getInstance().revalidateTrackPanels();
                }
            }

        } else if (event instanceof AlignmentTrackEvent) {
            AlignmentTrackEvent e = (AlignmentTrackEvent) event;
            AlignmentTrackEvent.Type eventType = e.getType();
            switch (eventType) {
                case ALLELE_THRESHOLD:
                    dataManager.alleleThresholdChanged();
                    break;
                case RELOAD:
                    clearCaches();
                case REFRESH:
                    setRenderOptions(new RenderOptions(dataManager.getType()));
                    refresh();
                    break;
            }

        }
    }


    public void setCoverageTrack(CoverageTrack coverageTrack) {
        this.coverageTrack = coverageTrack;
        this.coverageTrack.setRenderOptions(this.renderOptions);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible != this.isVisible()) {
            super.setVisible(visible);
            if (dataManager != null) {
                dataManager.setShowAlignments(visible);
            }
            if(IGV.hasInstance()) IGV.getInstance().getMainPanel().revalidate();
        }
    }

    @XmlElement(name = RenderOptions.NAME)
    private void setRenderOptions(RenderOptions renderOptions) {
        this.renderOptions = renderOptions;
        if (this.coverageTrack != null) {
            this.coverageTrack.setRenderOptions(this.renderOptions);
        }
    }

    @SubtlyImportant
    private RenderOptions getRenderOptions() {
        return this.renderOptions;
    }

    public CoverageTrack getCoverageTrack() {
        return coverageTrack;
    }

    public void setSpliceJunctionTrack(SpliceJunctionTrack spliceJunctionTrack) {
        this.spliceJunctionTrack = spliceJunctionTrack;
    }


    public SpliceJunctionTrack getSpliceJunctionTrack() {
        return spliceJunctionTrack;
    }


    @Override
    public IGVPopupMenu getPopupMenu(TrackClickEvent te) {

        Alignment alignment = getAlignment(te);
        if (alignment != null && alignment.getInsertions() != null) {
            for (AlignmentBlock block : alignment.getInsertions()) {
                if (block.containsPixel(te.getMouseEvent().getX())) {
                    return new InsertionMenu(block);
                }
            }
        }


        return new PopupMenu(te);
    }

    @Override
    public void setHeight(int preferredHeight) {
        super.setHeight(preferredHeight);
        minimumHeight = preferredHeight;
    }

    @Override
    public int getHeight() {

        if (dataPanel != null
                && (dataPanel instanceof DataPanel && ((DataPanel) dataPanel).getFrame().getScale() > dataManager.getMinVisibleScale())) {
            return minimumHeight;
        }

        int nGroups = dataManager.getMaxGroupCount();

        int h = Math.max(minHeight, getNLevels() * getRowHeight() + nGroups * GROUP_MARGIN + TOP_MARGIN
                + DS_MARGIN_0 + DOWNAMPLED_ROW_HEIGHT + DS_MARGIN_2);

        //if (insertionRect != null) {   // TODO - replace with expand insertions preference
        h += INSERTION_ROW_HEIGHT + DS_MARGIN_0;
        //}


        h = Math.min(maximumHeight, h);
        return h;
    }

    private int getRowHeight() {
        if (getDisplayMode() == DisplayMode.EXPANDED) {
            return expandedHeight;
        } else if (getDisplayMode() == DisplayMode.COLLAPSED) {
            return collapsedHeight;
        } else {
            return squishedHeight;
        }
    }

    private int getNLevels() {
        return dataManager.getNLevels();
    }

    @Override
    public boolean isReadyToPaint(ReferenceFrame frame) {
        if (frame.getScale() > dataManager.getMinVisibleScale()) {
            return true;   // Nothing to paint
        } else {

        }
        List<InsertionInterval> insertionIntervals = getInsertionIntervals(frame);
        insertionIntervals.clear();
        return dataManager.isLoaded(frame);
    }


    @Override
    public void load(ReferenceFrame referenceFrame) {
        dataManager.load(referenceFrame, renderOptions, true);
    }

    public void render(RenderContext context, Rectangle rect) {

        Graphics2D g = context.getGraphics2D("LABEL");
        g.setFont(FontManager.getFont(GROUP_LABEL_HEIGHT));

        dataPanel = context.getPanel();

        // Split track rectangle into sections.
        int seqHeight = sequenceTrack == null ? 0 : sequenceTrack.getHeight();
        if (seqHeight > 0) {
            Rectangle seqRect = new Rectangle(rect);
            seqRect.height = seqHeight;
            sequenceTrack.render(context, seqRect);
        }

        // Top gap.
        rect.y += DS_MARGIN_0;

        if (context.getScale() > dataManager.getMinVisibleScale()) {
            Rectangle visibleRect = context.getVisibleRect().intersection(rect);
            Graphics2D g2 = context.getGraphic2DForColor(Color.gray);
            GraphicUtils.drawCenteredText("Zoom in to see alignments.", visibleRect, g2);
            return;
        }

        downsampleRect = new Rectangle(rect);
        downsampleRect.height = DOWNAMPLED_ROW_HEIGHT;
        renderDownsampledIntervals(context, downsampleRect);

        if (renderOptions.drawInsertionIntervals) {
            insertionRect = new Rectangle(rect);
            insertionRect.y += DOWNAMPLED_ROW_HEIGHT + DS_MARGIN_0;
            insertionRect.height = INSERTION_ROW_HEIGHT;
            renderInsertionIntervals(context, insertionRect);
            rect.y = insertionRect.y + insertionRect.height;
        }

        alignmentsRect = new Rectangle(rect);
        alignmentsRect.y += 2;
        alignmentsRect.height -= (alignmentsRect.y - rect.y);
        renderAlignments(context, alignmentsRect);
    }

    private void renderDownsampledIntervals(RenderContext context, Rectangle downsampleRect) {

        // Might be offscreen
        if (!context.getVisibleRect().intersects(downsampleRect)) return;

        final AlignmentInterval loadedInterval = dataManager.getLoadedInterval(context.getReferenceFrame());
        if (loadedInterval == null) return;

        Graphics2D g = context.getGraphic2DForColor(Color.black);

        List<DownsampledInterval> intervals = loadedInterval.getDownsampledIntervals();
        for (DownsampledInterval interval : intervals) {
            final double scale = context.getScale();
            final double origin = context.getOrigin();

            int x0 = (int) ((interval.getStart() - origin) / scale);
            int x1 = (int) ((interval.getEnd() - origin) / scale);
            int w = Math.max(1, x1 - x0);
            // If there is room, leave a gap on one side
            if (w > 5) w--;
            // Greyscale from 0 -> 100 downsampled
            //int gray = 200 - interval.getCount();
            //Color color = (gray <= 0 ? Color.black : ColorUtilities.getGrayscaleColor(gray));
            g.fillRect(x0, downsampleRect.y, w, downsampleRect.height);
        }
    }

    private List<InsertionInterval> getInsertionIntervals(ReferenceFrame frame) {
        List<InsertionInterval> insertionIntervals = insertionIntervalsMap.get(frame);
        if (insertionIntervals == null) {
            insertionIntervals = new ArrayList<>();
            insertionIntervalsMap.put(frame, insertionIntervals);
        }
        return insertionIntervals;
    }

    private void renderInsertionIntervals(RenderContext context, Rectangle rect) {

        // Might be offscreen
        if (!context.getVisibleRect().intersects(rect)) return;

        List<InsertionMarker> intervals = context.getInsertionMarkers();
        if (intervals == null) return;

        InsertionMarker selected = InsertionManager.getInstance().getSelectedInsertion(context.getChr());

        int w = (int) ((1.41 * rect.height) / 2);


        boolean hideSmallIndex = getPreferences().getAsBoolean(SAM_HIDE_SMALL_INDEL);
        int smallIndelThreshold = getPreferences().getAsInt(SAM_SMALL_INDEL_BP_THRESHOLD);

        List<InsertionInterval> insertionIntervals = getInsertionIntervals(context.getReferenceFrame());
        insertionIntervals.clear();
        for (InsertionMarker insertionMarker : intervals) {

            if (hideSmallIndex && insertionMarker.size < smallIndelThreshold) continue;

            final double scale = context.getScale();
            final double origin = context.getOrigin();
            int midpoint = (int) ((insertionMarker.position - origin) / scale);
            int x0 = midpoint - w;
            int x1 = midpoint + w;

            Rectangle iRect = new Rectangle(x0 + context.translateX, rect.y, 2 * w, rect.height);

            insertionIntervals.add(new InsertionInterval(iRect, insertionMarker));

            Color c = (selected != null && selected.position == insertionMarker.position) ? Color.red : AlignmentRenderer.purple;
            Graphics2D g = context.getGraphic2DForColor(c);


            g.fillPolygon(new Polygon(new int[]{x0, x1, midpoint},
                    new int[]{rect.y, rect.y, rect.y + rect.height}, 3));
        }
    }


    private void renderAlignments(RenderContext context, Rectangle inputRect) {

        groupNames.clear();

        //log.debug("Render features");
        PackedAlignments groups = dataManager.getGroups(context, renderOptions);
        if (groups == null) {
            //Assume we are still loading.
            //This might not always be true
            return;
        }

        Map<String, PEStats> peStats = dataManager.getPEStats();
        if (peStats != null) {
            renderOptions.peStats = peStats;
        }

        Rectangle visibleRect = context.getVisibleRect();
        final boolean leaveMargin = (getDisplayMode() != DisplayMode.SQUISHED);


        maximumHeight = Integer.MAX_VALUE;

        // Divide rectangle into equal height levels
        double y = inputRect.getY();
        double h;
        if (getDisplayMode() == DisplayMode.EXPANDED) {
            h = expandedHeight;
        } else if (getDisplayMode() == DisplayMode.COLLAPSED) {
            h = collapsedHeight;
        } else {

            int visHeight = visibleRect.height;
            int depth = dataManager.getNLevels();
            if (depth == 0) {
                squishedHeight = Math.min(maxSquishedHeight, Math.max(1, expandedHeight));
            } else {
                squishedHeight = Math.min(maxSquishedHeight, Math.max(1, Math.min(expandedHeight, visHeight / depth)));
            }
            h = squishedHeight;
        }

        // Loop through groups
        Graphics2D groupBorderGraphics = context.getGraphic2DForColor(AlignmentRenderer.GROUP_DIVIDER_COLOR);
        int nGroups = groups.size();
        int groupNumber = 0;
        GroupOption groupOption = renderOptions.getGroupByOption();
        String groupByTag = renderOptions.getGroupByTag();

        for (Map.Entry<String, List<Row>> entry : groups.entrySet()) {

            groupNumber++;
            double yGroup = y;  // Remember this for label

            // Loop through the alignment rows for this group
            List<Row> rows = entry.getValue();
            for (Row row : rows) {
                if ((visibleRect != null && y > visibleRect.getMaxY())) {
                    return;
                }

                if (y + h > visibleRect.getY()) {
                    Rectangle rowRectangle = new Rectangle(inputRect.x, (int) y, inputRect.width, (int) h);
                    AlignmentCounts alignmentCounts = dataManager.getLoadedInterval(context.getReferenceFrame()).getCounts();
                    renderer.renderAlignments(row.alignments, context, rowRectangle,
                            inputRect, renderOptions, leaveMargin, selectedReadNames, alignmentCounts, getPreferences());
                    row.y = y;
                    row.h = h;
                }
                y += h;
            }
            if (groupOption != GroupOption.NONE) {
                // Draw a subtle divider line between groups
                if (showGroupLine) {
                    if (groupNumber < nGroups) {
                        int borderY = (int) y + GROUP_MARGIN / 2;
                        GraphicUtils.drawDottedDashLine(groupBorderGraphics, inputRect.x, borderY, inputRect.width, borderY);
                    }
                }

                // Label the group, if there is room

                double groupHeight = rows.size() * h;
                if (groupHeight > GROUP_LABEL_HEIGHT + 2) {
                    String groupName = entry.getKey();
                    Graphics2D g = context.getGraphics2D("LABEL");
                    FontMetrics fm = g.getFontMetrics();
                    Rectangle2D stringBouds = fm.getStringBounds(groupName, g);
                    Rectangle rect = new Rectangle(inputRect.x, (int) yGroup, (int) stringBouds.getWidth() + 10, (int) stringBouds.getHeight());
                    GraphicUtils.drawVerticallyCenteredText(
                            groupName, 5, rect, context.getGraphics2D("LABEL"), false, true);

                    groupNames.put(new Rectangle(inputRect.x, (int) yGroup, inputRect.width, (int) (y - yGroup)), groupName);
                }

            }
            y += GROUP_MARGIN;


        }

        final int bottom = inputRect.y + inputRect.height;
        groupBorderGraphics.drawLine(inputRect.x, bottom, inputRect.width, bottom);
    }


    public void renderExpandedInsertion(InsertionMarker insertionMarker, RenderContext context, Rectangle inputRect) {


        boolean leaveMargin = getDisplayMode() != DisplayMode.COLLAPSED.SQUISHED;

        // Insertion interval
        Graphics2D g = context.getGraphic2DForColor(Color.red);
        Rectangle iRect = new Rectangle(inputRect.x, insertionRect.y, inputRect.width, insertionRect.height);
        g.fill(iRect);
        List<InsertionInterval> insertionIntervals = getInsertionIntervals(context.getReferenceFrame());

        iRect.x += context.translateX;
        insertionIntervals.add(new InsertionInterval(iRect, insertionMarker));


        inputRect.y += DS_MARGIN_0 + DOWNAMPLED_ROW_HEIGHT + DS_MARGIN_0 + INSERTION_ROW_HEIGHT + DS_MARGIN_2;

        //log.debug("Render features");
        PackedAlignments groups = dataManager.getGroups(context, renderOptions);
        if (groups == null) {
            //Assume we are still loading.
            //This might not always be true
            return;
        }

        Rectangle visibleRect = context.getVisibleRect();


        maximumHeight = Integer.MAX_VALUE;

        // Divide rectangle into equal height levels
        double y = inputRect.getY();
        double h;
        if (getDisplayMode() == DisplayMode.EXPANDED) {
            h = expandedHeight;
        } else {

            int visHeight = visibleRect.height;
            int depth = dataManager.getNLevels();
            if (depth == 0) {
                squishedHeight = Math.min(maxSquishedHeight, Math.max(1, expandedHeight));
            } else {
                squishedHeight = Math.min(maxSquishedHeight, Math.max(1, Math.min(expandedHeight, visHeight / depth)));
            }
            h = squishedHeight;
        }


        for (Map.Entry<String, List<Row>> entry : groups.entrySet()) {


            // Loop through the alignment rows for this group
            List<Row> rows = entry.getValue();
            for (Row row : rows) {
                if ((visibleRect != null && y > visibleRect.getMaxY())) {
                    return;
                }

                if (y + h > visibleRect.getY()) {
                    Rectangle rowRectangle = new Rectangle(inputRect.x, (int) y, inputRect.width, (int) h);
                    renderer.renderExpandedInsertion(insertionMarker, row.alignments, context, rowRectangle, leaveMargin);
                    row.y = y;
                    row.h = h;
                }
                y += h;
            }

            y += GROUP_MARGIN;


        }

    }

//
//    public void renderExpandedInsertions(RenderContext context, Rectangle inputRect) {
//
//
//        boolean leaveMargin = getDisplayMode() != DisplayMode.COLLAPSED.SQUISHED;
//
//        inputRect.y += DOWNAMPLED_ROW_HEIGHT + DS_MARGIN_2;
//
//        //log.debug("Render features");
//        PackedAlignments groups = dataManager.getGroups(context, renderOptions);
//        if (groups == null) {
//            //Assume we are still loading.
//            //This might not always be true
//            return;
//        }
//
//        Rectangle visibleRect = context.getVisibleRect();
//
//
//        maximumHeight = Integer.MAX_VALUE;
//
//        // Divide rectangle into equal height levels
//        double y = inputRect.getY();
//        double h;
//        if (getDisplayMode() == DisplayMode.EXPANDED) {
//            h = expandedHeight;
//        } else {
//
//            int visHeight = visibleRect.height;
//            int depth = dataManager.getNLevels();
//            if (depth == 0) {
//                squishedHeight = Math.min(maxSquishedHeight, Math.max(1, expandedHeight));
//            } else {
//                squishedHeight = Math.min(maxSquishedHeight, Math.max(1, Math.min(expandedHeight, visHeight / depth)));
//            }
//            h = squishedHeight;
//        }
//
//
//        for (Map.Entry<String, List<Row>> entry : groups.entrySet()) {
//
//
//            // Loop through the alignment rows for this group
//            List<Row> rows = entry.getValue();
//            for (Row row : rows) {
//                if ((visibleRect != null && y > visibleRect.getMaxY())) {
//                    return;
//                }
//
//                if (y + h > visibleRect.getY()) {
//                    Rectangle rowRectangle = new Rectangle(inputRect.x, (int) y, inputRect.width, (int) h);
//                    renderer.renderExpandedInsertions(row.alignments, context, rowRectangle, leaveMargin);
//                    row.y = y;
//                    row.h = h;
//                }
//                y += h;
//            }
//
//            y += GROUP_MARGIN;
//
//
//        }
//    }

    /**
     * Sort alignment rows based on alignments that intersect location
     *
     * @return Whether sorting was performed. If data is still loading, this will return false
     */
    public boolean sortRows(SortOption option, ReferenceFrame referenceFrame, double location, String tag) {
        return dataManager.sortRows(option, referenceFrame, location, tag);
    }

    /**
     * Visually regroup alignments by the provided {@code GroupOption}.
     *
     * @param option
     * @see AlignmentDataManager#packAlignments
     */
    public void groupAlignments(GroupOption option, String tag, Range pos) {
        if (option == GroupOption.TAG && tag != null) {
            renderOptions.setGroupByTag(tag);
        }
        if (option == GroupOption.BASE_AT_POS && pos != null) {
            renderOptions.setGroupByPos(pos);
        }
        renderOptions.groupByOption = (option == GroupOption.NONE ? null : option);
        dataManager.packAlignments(renderOptions);
    }

    public void packAlignments() {
        dataManager.packAlignments(renderOptions);
    }

    /**
     * Copy the contents of the popup text to the system clipboard.
     */
    public void copyToClipboard(final TrackClickEvent e, Alignment alignment, double location, int mouseX) {

        if (alignment != null) {
            StringBuffer buf = new StringBuffer();
            buf.append(alignment.getValueString(location, mouseX, null).replace("<br>", "\n"));
            buf.append("\n");
            buf.append("Alignment start position = " + alignment.getChr() + ":" + (alignment.getAlignmentStart() + 1));
            buf.append("\n");
            buf.append(alignment.getReadSequence());
            StringSelection stringSelection = new StringSelection(buf.toString());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        }

    }

    /**
     * Jump to the mate region
     */
    public void gotoMate(final TrackClickEvent te, Alignment alignment) {


        if (alignment != null) {
            ReadMate mate = alignment.getMate();
            if (mate != null && mate.isMapped()) {

                setSelected(alignment);

                String chr = mate.getChr();
                int start = mate.start - 1;

                // Don't change scale
                double range = te.getFrame().getEnd() - te.getFrame().getOrigin();
                int newStart = (int) Math.max(0, (start + (alignment.getEnd() - alignment.getStart()) / 2 - range / 2));
                int newEnd = newStart + (int) range;
                te.getFrame().jumpTo(chr, newStart, newEnd);
                te.getFrame().recordHistory();
            } else {
                MessageUtils.showMessage("Alignment does not have mate, or it is not mapped.");
            }
        }
    }

    /**
     * Split the screen so the current view and mate region are side by side.
     * Need a better name for this method.
     */
    public void splitScreenMate(final TrackClickEvent te, Alignment alignment) {

        if (alignment != null) {
            ReadMate mate = alignment.getMate();
            if (mate != null && mate.isMapped()) {

                setSelected(alignment);

                String mateChr = mate.getChr();
                int mateStart = mate.start - 1;

                ReferenceFrame frame = te.getFrame();
                String locus1 = frame.getFormattedLocusString();

                // Generate a locus string for the read mate.  Keep the window width (in base pairs) == to the current range
                Range range = frame.getCurrentRange();
                int length = range.getLength();
                int s2 = Math.max(0, mateStart - length / 2);
                int e2 = s2 + length;
                String startStr = NumberFormat.getInstance().format(s2);
                String endStr = NumberFormat.getInstance().format(e2);
                String mateLocus = mateChr + ":" + startStr + "-" + endStr;

                Session currentSession = IGV.getInstance().getSession();

                List<String> loci = null;
                if (FrameManager.isGeneListMode()) {
                    loci = new ArrayList<>(FrameManager.getFrames().size());
                    for (ReferenceFrame ref : FrameManager.getFrames()) {
                        //If the frame-name is a locus, we use it unaltered
                        //Don't want to reprocess, easy to get off-by-one
                        String name = ref.getName();
                        if (Locus.fromString(name) != null) {
                            loci.add(name);
                        } else {
                            loci.add(ref.getFormattedLocusString());
                        }

                    }
                    loci.add(mateLocus);
                } else {
                    loci = Arrays.asList(locus1, mateLocus);
                }

                StringBuffer listName = new StringBuffer();
                for (String s : loci) {
                    listName.append(s + "   ");
                }

                GeneList geneList = new GeneList(listName.toString(), loci, false);
                currentSession.setCurrentGeneList(geneList);

                Comparator<String> geneListComparator = new Comparator<String>() {
                    @Override
                    public int compare(String n0, String n1) {
                        ReferenceFrame f0 = FrameManager.getFrame(n0);
                        ReferenceFrame f1 = FrameManager.getFrame(n1);

                        String chr0 = f0 == null ? "" : f0.getChrName();
                        String chr1 = f1 == null ? "" : f1.getChrName();
                        int s0 = f0 == null ? 0 : f0.getCurrentRange().getStart();
                        int s1 = f1 == null ? 0 : f1.getCurrentRange().getStart();

                        int chrComp = ChromosomeNameComparator.get().compare(chr0, chr1);
                        if (chrComp != 0) return chrComp;
                        return s0 - s1;
                    }
                };

                //Need to sort the frames by position
                currentSession.sortGeneList(geneListComparator);
                IGV.getInstance().resetFrames();
            } else {
                MessageUtils.showMessage("Alignment does not have mate, or it is not mapped.");
            }
        }
    }

    public boolean isLogNormalized() {
        return false;
    }

    public float getRegionScore(String chr, int start, int end, int zoom, RegionScoreType type, String frameName) {
        return 0.0f;
    }

    public AlignmentDataManager getDataManager() {
        return dataManager;
    }

    public String getValueStringAt(String chr, double position, int mouseX, int mouseY, ReferenceFrame frame) {

        if (downsampleRect != null && mouseY > downsampleRect.y && mouseY <= downsampleRect.y + downsampleRect.height) {
            AlignmentInterval loadedInterval = dataManager.getLoadedInterval(frame);
            if (loadedInterval == null) {
                return null;
            } else {
                List<DownsampledInterval> intervals = loadedInterval.getDownsampledIntervals();
                DownsampledInterval interval = (DownsampledInterval) FeatureUtils.getFeatureAt(position, 0, intervals);
                if (interval != null) {
                    return interval.getValueString();
                }
                return null;
            }
        } else {

            InsertionInterval insertionInterval = getInsertionInterval(frame, mouseX, mouseY);
            if (insertionInterval != null) {
                return "Insertions (" + insertionInterval.insertionMarker.size + " bases)";
            } else {
                Alignment feature = getAlignmentAt(position, mouseY, frame);
                if (feature != null) {
                    return feature.getValueString(position, mouseX, getWindowFunction());
                } else {
                    for (Map.Entry<Rectangle, String> groupNameEntry : groupNames.entrySet()) {
                        Rectangle r = groupNameEntry.getKey();
                        if (mouseY >= r.y && mouseY < r.y + r.height) {
                            return groupNameEntry.getValue();
                        }
                    }
                }
            }

        }
        return null;
    }


    private Alignment getAlignment(final TrackClickEvent te) {
        MouseEvent e = te.getMouseEvent();
        final ReferenceFrame frame = te.getFrame();
        if (frame == null) {
            return null;
        }
        final double location = frame.getChromosomePosition(e.getX());
        return getAlignmentAt(location, e.getY(), frame);
    }

    private Alignment getAlignmentAt(double position, int y, ReferenceFrame frame) {

        if (alignmentsRect == null) {
            return null;   // <= not loaded yet
        }
        PackedAlignments groups = dataManager.getGroupedAlignmentsContaining(position, frame);

        if (groups == null || groups.isEmpty()) {
            return null;
        }

        for (List<Row> rows : groups.values()) {
            for (Row row : rows) {
                if (y >= row.y && y <= row.y + row.h) {
                    List<Alignment> features = row.alignments;

                    // No buffer for alignments,  you must zoom in far enough for them to be visible
                    int buffer = 0;
                    return FeatureUtils.getFeatureAt(position, buffer, features);
                }
            }
        }
        return null;
    }


    /**
     * Get the most "specific" alignment at the specified location.  Specificity refers to the smallest alignemnt
     * in a group that contains the location (i.e. if a group of linked alignments overlap take the smallest one).
     *
     * @param te
     * @return
     */
    private Alignment getSpecficAlignment(TrackClickEvent te) {

        Alignment alignment = getAlignment(te);
        if (alignment != null) {
            final ReferenceFrame frame = te.getFrame();
            MouseEvent e = te.getMouseEvent();
            final double location = frame.getChromosomePosition(e.getX());

            if (alignment instanceof LinkedAlignment) {

                Alignment sa = null;
                for (Alignment a : ((LinkedAlignment) alignment).alignments) {
                    if (a.contains(location)) {
                        if (sa == null || (a.getAlignmentEnd() - a.getAlignmentStart() < sa.getAlignmentEnd() - sa.getAlignmentStart())) {
                            sa = a;
                        }
                    }
                }
                alignment = sa;

            } else if (alignment instanceof PairedAlignment) {
                Alignment sa = null;
                if (((PairedAlignment) alignment).firstAlignment.contains(location)) {
                    sa = ((PairedAlignment) alignment).firstAlignment;
                } else if (((PairedAlignment) alignment).secondAlignment.contains(location)) {
                    sa = ((PairedAlignment) alignment).secondAlignment;
                }
                alignment = sa;
            }
        }
        return alignment;
    }

    @Override
    public boolean handleDataClick(TrackClickEvent te) {

        MouseEvent e = te.getMouseEvent();
        if (Globals.IS_MAC && e.isMetaDown() || (!Globals.IS_MAC && e.isControlDown())) {
            // Selection
            final ReferenceFrame frame = te.getFrame();
            if (frame != null) {
                selectAlignment(e, frame);
                if (dataPanel != null) {
                    dataPanel.repaint();
                }
                return true;
            }
        }

        InsertionInterval insertionInterval = getInsertionInterval(te.getFrame(), te.getMouseEvent().getX(), te.getMouseEvent().getY());
        if (insertionInterval != null) {

            final String chrName = te.getFrame().getChrName();
            InsertionMarker currentSelection = InsertionManager.getInstance().getSelectedInsertion(chrName);
            if (currentSelection != null && currentSelection.position == insertionInterval.insertionMarker.position) {
                InsertionManager.getInstance().clearSelected();
            } else {
                InsertionManager.getInstance().setSelected(chrName, insertionInterval.insertionMarker.position);
            }

            IGVEventBus.getInstance().post(new InsertionSelectionEvent(insertionInterval.insertionMarker));

            return true;
        }


        if (IGV.getInstance().isShowDetailsOnClick()) {
            openTooltipWindow(te);
            return true;
        }

        return false;
    }

    private void selectAlignment(MouseEvent e, ReferenceFrame frame) {
        double location = frame.getChromosomePosition(e.getX());
        Alignment alignment = this.getAlignmentAt(location, e.getY(), frame);
        if (alignment != null) {
            if (selectedReadNames.containsKey(alignment.getReadName())) {
                selectedReadNames.remove(alignment.getReadName());
            } else {
                setSelected(alignment);
            }

        }
    }

    private InsertionInterval getInsertionInterval(ReferenceFrame frame, int x, int y) {
        List<InsertionInterval> insertionIntervals = getInsertionIntervals(frame);
        for (InsertionInterval i : insertionIntervals) {
            if (i.rect.contains(x, y)) return i;
        }

        return null;
    }

    private void setSelected(Alignment alignment) {
        Color c = readNamePalette.get(alignment.getReadName());
        selectedReadNames.put(alignment.getReadName(), c);
    }

    public void clearCaches() {
        if (dataManager != null) dataManager.clear();
        if (spliceJunctionTrack != null) spliceJunctionTrack.clear();
    }

    public static void refresh() {
        IGV.getInstance().getContentPane().getMainPanel().invalidate();
        IGV.getInstance().revalidateTrackPanels();
    }

    public static boolean isBisulfiteColorType(ColorOption o) {
        return (o.equals(ColorOption.BISULFITE) || o.equals(ColorOption.NOMESEQ));
    }

    public static String getBisulfiteContextPubStr(BisulfiteContext item) {
        return bisulfiteContextToPubString.get(item);
    }

    public static byte[] getBisulfiteContextPreContext(BisulfiteContext item) {
        Pair<byte[], byte[]> pair = AlignmentTrack.bisulfiteContextToContextString.get(item);
        return pair.getFirst();
    }

    public static byte[] getBisulfiteContextPostContext(BisulfiteContext item) {
        Pair<byte[], byte[]> pair = AlignmentTrack.bisulfiteContextToContextString.get(item);
        return pair.getSecond();
    }

    @Override
    public void restorePersistentState(Node node, int version) throws JAXBException {
        super.restorePersistentState(node, version);

        //For legacy sessions (<= v4. RenderOptions used to be stuffed in
        //with Track tag, now it's a sub element
        boolean hasRenderSubTag = false;
        try {
            if (node.hasChildNodes()) {
                NodeList list = node.getChildNodes();
                for (int ii = 0; ii < list.getLength(); ii++) {
                    Node item = list.item(ii);
                    if (item.getNodeName().equals(RenderOptions.NAME)) {
                        hasRenderSubTag = true;
                        break;
                    }
                }
            }
            if (hasRenderSubTag) return;
            RenderOptions ro = IGVSessionReader.getJAXBContext().createUnmarshaller().unmarshal(node, RenderOptions.class).getValue();

            String shadeBasesKey = "shadeBases";
            String value = Utilities.getNullSafe(node.getAttributes(), shadeBasesKey);  // For older sessions
            if (value != null) {
                if (value.equals("false")) {
                    ro.shadeBasesOption = ShadeBasesOption.NONE;
                } else if (value.equals("true")) {
                    ro.shadeBasesOption = ShadeBasesOption.QUALITY;
                }
            }

            this.setRenderOptions(ro);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public void setViewAsPairs(boolean vAP) {
        // TODO -- generalize this test to all incompatible pairings
        if (vAP && renderOptions.groupByOption == GroupOption.STRAND) {
            boolean ungroup = MessageUtils.confirm("\"View as pairs\" is incompatible with \"Group by strand\". Ungroup?");
            if (ungroup) {
                renderOptions.groupByOption = null;
            } else {
                return;
            }
        }

        dataManager.setViewAsPairs(vAP, renderOptions);
        refresh();
    }


    class RenderRollback {
        ColorOption colorOption;
        GroupOption groupByOption;
        String groupByTag;
        String colorByTag;
        String linkByTag;
        DisplayMode displayMode;
        int expandedHeight;
        boolean showGroupLine;

        RenderRollback(RenderOptions renderOptions, DisplayMode displayMode) {
            this.colorOption = renderOptions.colorOption;
            this.groupByOption = renderOptions.groupByOption;
            this.colorByTag = renderOptions.colorByTag;
            this.groupByTag = renderOptions.groupByTag;
            this.displayMode = displayMode;
            this.expandedHeight = AlignmentTrack.this.expandedHeight;
            this.showGroupLine = AlignmentTrack.this.showGroupLine;
            this.linkByTag = renderOptions.linkByTag;
        }

        void restore(RenderOptions renderOptions) {
            renderOptions.colorOption = this.colorOption;
            renderOptions.groupByOption = this.groupByOption;
            renderOptions.colorByTag = this.colorByTag;
            renderOptions.groupByTag = this.groupByTag;
            renderOptions.linkByTag = this.linkByTag;
            AlignmentTrack.this.expandedHeight = this.expandedHeight;
            AlignmentTrack.this.showGroupLine = this.showGroupLine;
            AlignmentTrack.this.setDisplayMode(this.displayMode);
        }
    }


    public boolean isRemoved() {
        return removed;
    }

    IGVPreferences getPreferences() {
        return getPreferences(dataManager.getType());
    }

    private static IGVPreferences getPreferences(AlignmentDataManager.ExperimentType type) {

        String prefKey = Constants.NULL_CATEGORY;
        if (type == AlignmentDataManager.ExperimentType.THIRD_GEN) {
            prefKey = Constants.THIRD_GEN;
        } else if (type == AlignmentDataManager.ExperimentType.RNA) {
            prefKey = Constants.RNA;
        }

        return PreferencesManager.getPreferences(prefKey);
    }


    @Override
    public void dispose() {
        super.dispose();
        clearCaches();
        if (dataManager != null) {
            dataManager.dumpAlignments();
            IGVEventBus.getInstance().unsubscribe(dataManager);
        }
        dataManager = null;
        removed = true;
        setVisible(false);
    }

    @XmlType(name = RenderOptions.NAME)
    @XmlAccessorType(XmlAccessType.NONE)

    public static class RenderOptions implements Cloneable {

        public static final String NAME = "RenderOptions";

        AlignmentDataManager.ExperimentType experimentType;

        @XmlAttribute
        ShadeBasesOption shadeBasesOption;
        @XmlAttribute
        boolean shadeCenters;
        @XmlAttribute
        boolean flagUnmappedPairs;
        @XmlAttribute
        boolean showAllBases;
        boolean showMismatches = true;
        private boolean computeIsizes;
        @XmlAttribute
        private int minInsertSize;
        @XmlAttribute
        private int maxInsertSize;
        private double minInsertSizePercentile;
        private double maxInsertSizePercentile;
        @XmlAttribute
        private ColorOption colorOption;
        @XmlAttribute
        GroupOption groupByOption = null;
        //ContinuousColorScale insertSizeColorScale;
        @XmlAttribute
        private boolean viewPairs = false;
        private boolean pairedArcView = false;
        public boolean flagZeroQualityAlignments = true;
        Map<String, PEStats> peStats;
        @XmlAttribute
        private String colorByTag;
        @XmlAttribute
        private String groupByTag;
        @XmlAttribute
        private String sortByTag;
        @XmlAttribute
        private String linkByTag;
        @XmlAttribute
        private boolean linkedReads;


        @XmlAttribute
        boolean quickConsensusMode;


        BisulfiteContext bisulfiteContext;

        private Range groupByPos = null;
        public boolean drawInsertionIntervals = false;


        public RenderOptions() {
            this(AlignmentDataManager.ExperimentType.OTHER);
        }

        RenderOptions(AlignmentDataManager.ExperimentType experimentType) {

            IGVPreferences prefs = getPreferences(experimentType);

            this.experimentType = experimentType;

            String shadeOptionString = prefs.get(SAM_SHADE_BASES);
            if (shadeOptionString.equals("false")) {
                shadeBasesOption = ShadeBasesOption.NONE;
            } else if (shadeOptionString.equals("true")) {
                shadeBasesOption = ShadeBasesOption.QUALITY;
            } else {
                shadeBasesOption = ShadeBasesOption.valueOf(shadeOptionString);
            }
            drawInsertionIntervals = prefs.getAsBoolean(SAM_SHOW_INSERTION_MARKERS);
            shadeCenters = prefs.getAsBoolean(SAM_SHADE_CENTER);
            flagUnmappedPairs = prefs.getAsBoolean(SAM_FLAG_UNMAPPED_PAIR);
            computeIsizes = prefs.getAsBoolean(SAM_COMPUTE_ISIZES);
            minInsertSize = prefs.getAsInt(SAM_MIN_INSERT_SIZE_THRESHOLD);
            maxInsertSize = prefs.getAsInt(SAM_MAX_INSERT_SIZE_THRESHOLD);
            minInsertSizePercentile = prefs.getAsFloat(SAM_MIN_INSERT_SIZE_PERCENTILE);
            maxInsertSizePercentile = prefs.getAsFloat(SAM_MAX_INSERT_SIZE_PERCENTILE);
            showAllBases = prefs.getAsBoolean(SAM_SHOW_ALL_BASES);
            quickConsensusMode = prefs.getAsBoolean(SAM_QUICK_CONSENSUS_MODE);
            colorOption = CollUtils.valueOf(ColorOption.class, prefs.get(SAM_COLOR_BY), ColorOption.NONE);
            groupByOption = CollUtils.valueOf(GroupOption.class, prefs.get(SAM_GROUP_OPTION), GroupOption.NONE);
            flagZeroQualityAlignments = prefs.getAsBoolean(SAM_FLAG_ZERO_QUALITY);
            bisulfiteContext = DEFAULT_BISULFITE_CONTEXT;


            colorByTag = prefs.get(SAM_COLOR_BY_TAG);
            sortByTag = prefs.get(SAM_SORT_BY_TAG);
            groupByTag = prefs.get(SAM_GROUP_BY_TAG);
            setGroupByPos(prefs.get(SAM_GROUP_BY_POS));

            //updateColorScale();

            peStats = new HashMap<String, PEStats>();

            linkedReads = prefs.getAsBoolean(SAM_LINK_READS);
            linkByTag = prefs.get(SAM_LINK_TAG);
        }


        private <T extends Enum<T>> T getFromMap(Map<String, String> attributes, String key, Class<T> clazz, T defaultValue) {
            String value = attributes.get(key);
            if (value == null) {
                return defaultValue;
            }
            return CollUtils.<T>valueOf(clazz, value, defaultValue);
        }

        private String getFromMap(Map<String, String> attributes, String key, String defaultValue) {
            String value = attributes.get(key);
            if (value == null) {
                return defaultValue;
            }
            return value;
        }

        public void setShowAllBases(boolean showAllBases) {
            this.showAllBases = showAllBases;
            if (showAllBases) this.showMismatches = false;
        }

        public void setShowMismatches(boolean showMismatches) {
            this.showMismatches = showMismatches;
            if (showMismatches) this.showAllBases = false;
        }

        public int getMinInsertSize() {
            return minInsertSize;
        }

        public void setMinInsertSize(int minInsertSize) {
            this.minInsertSize = minInsertSize;
            //updateColorScale();
        }

        public int getMaxInsertSize() {
            return maxInsertSize;

        }

        public boolean isViewPairs() {
            return viewPairs;
        }

        public void setViewPairs(boolean viewPairs) {
            this.viewPairs = viewPairs;
        }

        public boolean isComputeIsizes() {
            return computeIsizes;
        }

        public void setComputeIsizes(boolean computeIsizes) {
            this.computeIsizes = computeIsizes;
        }

        public double getMinInsertSizePercentile() {
            return minInsertSizePercentile;
        }

        public void setMinInsertSizePercentile(double minInsertSizePercentile) {
            this.minInsertSizePercentile = minInsertSizePercentile;
        }

        public double getMaxInsertSizePercentile() {
            return maxInsertSizePercentile;
        }

        public void setMaxInsertSizePercentile(double maxInsertSizePercentile) {
            this.maxInsertSizePercentile = maxInsertSizePercentile;
        }

        public void setMaxInsertSize(int maxInsertSize) {
            this.maxInsertSize = maxInsertSize;
        }

        public ColorOption getColorOption() {
            return colorOption;
        }

        public void setColorOption(ColorOption colorOption) {
            this.colorOption = colorOption;
        }

        public void setColorByTag(String colorByTag) {
            this.colorByTag = colorByTag;
            getPreferences(experimentType).put(SAM_COLOR_BY_TAG, colorByTag);
        }

        public String getColorByTag() {
            return colorByTag;
        }

        public String getSortByTag() {
            return sortByTag;
        }

        public void setSortByTag(String sortByTag) {
            this.sortByTag = sortByTag;
        }

        public String getGroupByTag() {
            return groupByTag;
        }

        public void setGroupByTag(String groupByTag) {
            this.groupByTag = groupByTag;
        }

        public Range getGroupByPos() {
            return groupByPos;
        }

        public void setGroupByPos(Range groupByPos) {
            this.groupByPos = groupByPos;
        }

        public void setGroupByPos(String pos) {
            if (pos == null) {
                this.groupByPos = null;
            } else {
                String[] posParts = pos.split(" ");
                if (posParts.length != 2) {
                    this.groupByPos = null;
                } else {
                    int posChromStart = Integer.valueOf(posParts[1]);
                    this.groupByPos = new Range(posParts[0], posChromStart, posChromStart + 1);
                }
            }
        }

        public String getLinkByTag() {
            return linkByTag;
        }

        public void setLinkByTag(String linkByTag) {
            this.linkByTag = linkByTag;
        }

        public GroupOption getGroupByOption() {
            return groupByOption;
        }

        public boolean isLinkedReads() {
            return linkedReads;
        }

        public void setLinkedReads(boolean linkedReads) {
            this.linkedReads = linkedReads;
        }

        public boolean isQuickConsensusMode() {
            return quickConsensusMode;
        }

        public void setQuickConsensusMode(boolean quickConsensusMode) {
            this.quickConsensusMode = quickConsensusMode;
        }
    }

    public boolean isLinkedReads() {
        return renderOptions.linkedReads;
    }

    public void setLinkedReads(boolean linkedReads, String tag) {

        renderOptions.linkedReads = linkedReads;
        if (linkedReads == true) {
            this.renderRollback = new RenderRollback(renderOptions, getDisplayMode());

            renderOptions.setLinkByTag(tag);

            if ("READNAME".equals(tag)) {
                renderOptions.setColorOption(ColorOption.LINK_STRAND);
            } else {
                // TenX -- ditto
                renderOptions.setColorOption(ColorOption.TAG);
                renderOptions.setColorByTag(tag);

                if (dataManager.isPhased()) {
                    renderOptions.groupByOption = GroupOption.TAG;
                    renderOptions.setGroupByTag("HP");
                }
                expandedHeight = 10;
                showGroupLine = false;
                setDisplayMode(DisplayMode.SQUISHED);
            }
        } else {
            if (this.renderRollback != null) {
                this.renderRollback.restore(renderOptions);
            }
        }

        dataManager.packAlignments(renderOptions);
        refresh();
    }

    /**
     * Listener for deselecting one component when another is selected
     */
    private static class Deselector implements ActionListener {

        private JMenuItem toDeselect;
        private JMenuItem parent;

        Deselector(JMenuItem parent, JMenuItem toDeselect) {
            this.parent = parent;
            this.toDeselect = toDeselect;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (this.parent.isSelected()) {
                this.toDeselect.setSelected(false);
            }
        }
    }


    @SubtlyImportant
    private static AlignmentTrack getNextTrack() {
        return (AlignmentTrack) IGVSessionReader.getNextTrack();
    }

    private static class InsertionInterval {

        Rectangle rect;
        InsertionMarker insertionMarker;

        public InsertionInterval(Rectangle rect, InsertionMarker insertionMarker) {
            this.rect = rect;
            this.insertionMarker = insertionMarker;
        }
    }


    class PopupMenu extends IGVPopupMenu {

        PopupMenu(final TrackClickEvent e) {

            Collection<Track> tracks = new ArrayList();
            tracks.add(AlignmentTrack.this);

            JLabel popupTitle = new JLabel("  " + AlignmentTrack.this.getName(), JLabel.CENTER);

            Font newFont = getFont().deriveFont(Font.BOLD, 12);
            popupTitle.setFont(newFont);
            if (popupTitle != null) {
                add(popupTitle);
            }
            addSeparator();
            add(TrackMenuUtils.getTrackRenameItem(tracks));
            addCopyToClipboardItem(e);

            //         addSeparator();
            //          addExpandInsertions();

            if (dataManager.isTenX()) {
                addTenXItems();
            } else {
                addSupplItems();     // Are SA tags mutually exlcusive with 10X?
            }

            addSeparator();
            addGroupMenuItem(e);
            addSortMenuItem();
            addColorByMenuItem();
            addPackMenuItem();

            addSeparator();
            addShadeBaseByMenuItem();
            JMenuItem misMatchesItem = addShowMismatchesMenuItem();
            JMenuItem showAllItem = addShowAllBasesMenuItem();

            misMatchesItem.addActionListener(new Deselector(misMatchesItem, showAllItem));
            showAllItem.addActionListener(new Deselector(showAllItem, misMatchesItem));

            addQuickConsensusModeItem();

            addSeparator();
            addViewAsPairsMenuItem();

            addGoToMate(e);
            showMateRegion(e);
            addInsertSizeMenuItem();

            addSeparator();
            TrackMenuUtils.addDisplayModeItems(tracks, this);

            addSeparator();
            addSelectByNameItem();
            addClearSelectionsMenuItem();

            addSeparator();
            addCopySequenceItem(e);
            addExtViewItem(e);
            addBlatItem(e);
            addConsensusSequence(e);

            boolean showSashimi = true;//Globals.isDevelopment();

            if (showSashimi) {
                addSeparator();
                JMenuItem sashimi = new JMenuItem("Sashimi Plot");
                sashimi.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        SashimiPlot.getSashimiPlot(null);
                    }
                });
                add(sashimi);
            }

            addSeparator();
            addShowItems();

        }

        public JMenuItem addExpandInsertions() {

            final JMenuItem item = new JCheckBoxMenuItem("Expand insertions");
            final Session session = IGV.getInstance().getSession();
            item.setSelected(session.expandInsertions);

            item.addActionListener(aEvt -> {
                session.expandInsertions = !session.expandInsertions;
                refresh();
            });
            add(item);
            return item;
        }

        /**
         * Item for exporting "consensus" sequence of region, based
         * on loaded alignments.
         *
         * @param e
         */
        private void addConsensusSequence(TrackClickEvent e) {
            //Export consensus sequence
            JMenuItem item = new JMenuItem("Copy consensus sequence");


            final ReferenceFrame frame;
            if (e.getFrame() == null && FrameManager.getFrames().size() == 1) {
                frame = FrameManager.getFrames().get(0);
            } else {
                frame = e.getFrame();
            }

            item.setEnabled(frame != null);
            add(item);

            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    //This shouldn't ever be true, but just in case it's more user-friendly
                    if (frame == null) {
                        MessageUtils.showMessage("Unknown region bounds, cannot export consensus");
                        return;
                    }
                    final int start = (int) frame.getOrigin();
                    final int end = (int) frame.getEnd();
                    if ((end - start) > 1000000) {
                        MessageUtils.showMessage("Cannot export region more than 1 Megabase");
                        return;
                    }
                    AlignmentInterval interval = dataManager.getLoadedInterval(frame);
                    AlignmentCounts counts = interval.getCounts();
                    String text = PFMExporter.createPFMText(counts, frame.getChrName(), start, end);
                    StringUtils.copyTextToClipboard(text);
                }
            });


        }

        private JMenu getBisulfiteContextMenuItem(ButtonGroup group) {
            // Change track height by attribute
            //JMenu bisulfiteContextMenu = new JMenu("Bisulfite Contexts");
            JMenu bisulfiteContextMenu = new JMenu("bisulfite mode");


            JRadioButtonMenuItem nomeESeqOption = null;
            boolean showNomeESeq = getPreferences().getAsBoolean(SAM_NOMESEQ_ENABLED);
            if (showNomeESeq) {
                nomeESeqOption = new JRadioButtonMenuItem("NOMe-seq bisulfite mode");
                nomeESeqOption.setSelected(renderOptions.colorOption == ColorOption.NOMESEQ);
                nomeESeqOption.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent aEvt) {
                        setColorOption(ColorOption.NOMESEQ);
                        refresh();
                    }
                });
                group.add(nomeESeqOption);
            }

            for (final BisulfiteContext item : BisulfiteContext.values()) {

                String optionStr = getBisulfiteContextPubStr(item);
                JRadioButtonMenuItem m1 = new JRadioButtonMenuItem(optionStr);
                m1.setSelected(renderOptions.bisulfiteContext == item);
                m1.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent aEvt) {
                        setColorOption(ColorOption.BISULFITE);
                        setBisulfiteContext(item);
                        refresh();
                    }
                });
                bisulfiteContextMenu.add(m1);
                group.add(m1);
            }

            if (nomeESeqOption != null) {
                bisulfiteContextMenu.add(nomeESeqOption);
            }

            return bisulfiteContextMenu;

        }

        public void addSelectByNameItem() {
            // Change track height by attribute
            JMenuItem item = new JMenuItem("Select by name...");
            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    String val = MessageUtils.showInputDialog("Enter read name: ");
                    if (val != null && val.trim().length() > 0) {
                        selectedReadNames.put(val, readNamePalette.get(val));
                        refresh();
                    }
                }
            });

            add(item);
        }

        private JCheckBoxMenuItem getGroupMenuItem(String label, final GroupOption option) {
            JCheckBoxMenuItem mi = new JCheckBoxMenuItem(label);
            mi.setSelected(renderOptions.groupByOption == option);
            if (option == GroupOption.NONE) {
                mi.setSelected(renderOptions.groupByOption == null);
            }
            mi.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    IGV.getInstance().groupAlignmentTracks(option, null, null);
                    refresh();

                }
            });

            return mi;
        }

        public void addGroupMenuItem(final TrackClickEvent te) {//ReferenceFrame frame) {
            final MouseEvent me = te.getMouseEvent();
            ReferenceFrame frame = te.getFrame();
            if (frame == null) {
                frame = FrameManager.getDefaultFrame();  // Clicked over name panel, not a specific frame
            }
            final Range range = frame.getCurrentRange();
            final String chrom = range.getChr();
            final int chromStart = (int) frame.getChromosomePosition(me.getX());
            // Change track height by attribute
            JMenu groupMenu = new JMenu("Group alignments by");
            ButtonGroup group = new ButtonGroup();

            Map<String, GroupOption> mappings = new LinkedHashMap<String, GroupOption>();
            mappings.put("none", GroupOption.NONE);
            mappings.put("read strand", GroupOption.STRAND);
            mappings.put("first-in-pair strand", GroupOption.FIRST_OF_PAIR_STRAND);
            mappings.put("sample", GroupOption.SAMPLE);
            mappings.put("library", GroupOption.LIBRARY);
            mappings.put("read group", GroupOption.READ_GROUP);
            mappings.put("chromosome of mate", GroupOption.MATE_CHROMOSOME);
            mappings.put("pair orientation", GroupOption.PAIR_ORIENTATION);
            mappings.put("supplementary flag", GroupOption.SUPPLEMENTARY);


            for (Map.Entry<String, GroupOption> el : mappings.entrySet()) {
                JCheckBoxMenuItem mi = getGroupMenuItem(el.getKey(), el.getValue());
                groupMenu.add(mi);
                group.add(mi);
            }

            JCheckBoxMenuItem tagOption = new JCheckBoxMenuItem("tag");
            tagOption.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    String tag = MessageUtils.showInputDialog("Enter tag", renderOptions.getGroupByTag());
                    if (tag != null && tag.trim().length() > 0) {
                        IGV.getInstance().groupAlignmentTracks(GroupOption.TAG, tag, null);
                    }

                }
            });
            tagOption.setSelected(renderOptions.groupByOption == GroupOption.TAG);
            groupMenu.add(tagOption);
            group.add(tagOption);

            Range oldGroupByPos = renderOptions.getGroupByPos();
            if (renderOptions.groupByOption == GroupOption.BASE_AT_POS) { // already sorted by the base at a position
                JCheckBoxMenuItem oldGroupByPosOption = new JCheckBoxMenuItem("base at " + oldGroupByPos.getChr() +
                        ":" + Globals.DECIMAL_FORMAT.format(1 + oldGroupByPos.getStart()));
                groupMenu.add(oldGroupByPosOption);
                oldGroupByPosOption.setSelected(true);
            }

            if (renderOptions.groupByOption != GroupOption.BASE_AT_POS || oldGroupByPos == null ||
                    !oldGroupByPos.getChr().equals(chrom) || oldGroupByPos.getStart() != chromStart) { // not already sorted by this position
                JCheckBoxMenuItem newGroupByPosOption = new JCheckBoxMenuItem("base at " + chrom +
                        ":" + Globals.DECIMAL_FORMAT.format(1 + chromStart));
                newGroupByPosOption.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent aEvt) {
                        Range groupByPos = new Range(chrom, chromStart, chromStart + 1);
                        IGV.getInstance().groupAlignmentTracks(GroupOption.BASE_AT_POS, null, groupByPos);
                    }
                });
                groupMenu.add(newGroupByPosOption);
                group.add(newGroupByPosOption);
            }

            add(groupMenu);
        }

        private JMenuItem getSortMenuItem(String label, final SortOption option) {
            JMenuItem mi = new JMenuItem(label);
            mi.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    sortAlignmentTracks(option, null);
                }
            });

            return mi;
        }

        /**
         * Sort menu
         */
        public void addSortMenuItem() {


            JMenu sortMenu = new JMenu("Sort alignments by");
            //LinkedHashMap is supposed to preserve order of insertion for iteration
            Map<String, SortOption> mappings = new LinkedHashMap<String, SortOption>();

            mappings.put("start location", SortOption.START);
            mappings.put("read strand", SortOption.STRAND);
            mappings.put("first-of-pair strand", SortOption.FIRST_OF_PAIR_STRAND);
            mappings.put("base", SortOption.NUCLEOTIDE);
            mappings.put("mapping quality", SortOption.QUALITY);
            mappings.put("sample", SortOption.SAMPLE);
            mappings.put("read group", SortOption.READ_GROUP);

            if (dataManager.isPairedEnd()) {
                mappings.put("insert size", SortOption.INSERT_SIZE);
                mappings.put("chromosome of mate", SortOption.MATE_CHR);
            }
            // mappings.put("supplementary flag", SortOption.SUPPLEMENTARY);

            for (Map.Entry<String, SortOption> el : mappings.entrySet()) {
                sortMenu.add(getSortMenuItem(el.getKey(), el.getValue()));
            }


            JMenuItem tagOption = new JMenuItem("tag");
            tagOption.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    String tag = MessageUtils.showInputDialog("Enter tag", renderOptions.getSortByTag());
                    if (tag != null && tag.trim().length() > 0) {
                        renderOptions.setSortByTag(tag);
                        sortAlignmentTracks(SortOption.TAG, tag);
                    }
                }
            });
            sortMenu.add(tagOption);


            add(sortMenu);
        }

        private void setBisulfiteContext(BisulfiteContext option) {
            renderOptions.bisulfiteContext = option;
            getPreferences().put(SAM_BISULFITE_CONTEXT, option.toString());
        }

        private void setColorOption(ColorOption option) {
            renderOptions.colorOption = option;
            getPreferences().put(SAM_COLOR_BY, option.toString());
        }

        private JRadioButtonMenuItem getColorMenuItem(String label, final ColorOption option) {
            JRadioButtonMenuItem mi = new JRadioButtonMenuItem(label);
            mi.setSelected(renderOptions.colorOption == option);
            mi.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    setColorOption(option);
                    refresh();
                }
            });

            return mi;
        }

        public void addColorByMenuItem() {
            // Change track height by attribute
            JMenu colorMenu = new JMenu("Color alignments by");

            ButtonGroup group = new ButtonGroup();

            Map<String, ColorOption> mappings = new LinkedHashMap<String, ColorOption>();

            mappings.put("no color", ColorOption.NONE);

            if (dataManager.isPairedEnd()) {

                mappings.put("insert size", ColorOption.INSERT_SIZE);
                mappings.put("pair orientation", ColorOption.PAIR_ORIENTATION);
                mappings.put("insert size and pair orientation", ColorOption.UNEXPECTED_PAIR);

            }

            mappings.put("read strand", ColorOption.READ_STRAND);

            if (dataManager.isPairedEnd()) {
                mappings.put("first-of-pair strand", ColorOption.FIRST_OF_PAIR_STRAND);
            }

            mappings.put("read group", ColorOption.READ_GROUP);
            mappings.put("sample", ColorOption.SAMPLE);
            mappings.put("library", ColorOption.LIBRARY);

            for (Map.Entry<String, ColorOption> el : mappings.entrySet()) {
                JRadioButtonMenuItem mi = getColorMenuItem(el.getKey(), el.getValue());
                colorMenu.add(mi);
                group.add(mi);
            }

            JRadioButtonMenuItem tagOption = new JRadioButtonMenuItem("tag");
            tagOption.setSelected(renderOptions.colorOption == ColorOption.TAG);
            tagOption.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    setColorOption(ColorOption.TAG);
                    String tag = MessageUtils.showInputDialog("Enter tag", renderOptions.getColorByTag());
                    if (tag != null && tag.trim().length() > 0) {
                        renderOptions.setColorByTag(tag);
                        refresh();
                    }
                }
            });
            colorMenu.add(tagOption);
            group.add(tagOption);


            colorMenu.add(getBisulfiteContextMenuItem(group));


            add(colorMenu);

        }

        public void addPackMenuItem() {
            // Change track height by attribute
            JMenuItem item = new JMenuItem("Re-pack alignments");
            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    UIUtilities.invokeOnEventThread(new Runnable() {

                        public void run() {
                            IGV.getInstance().packAlignmentTracks();
                            refresh();
                        }
                    });
                }
            });

            add(item);
        }

        public void addCopyToClipboardItem(final TrackClickEvent te) {

            final MouseEvent me = te.getMouseEvent();
            JMenuItem item = new JMenuItem("Copy read details to clipboard");

            final ReferenceFrame frame = te.getFrame();
            if (frame == null) {
                item.setEnabled(false);
            } else {
                final double location = frame.getChromosomePosition(me.getX());
                final Alignment alignment = getAlignmentAt(location, me.getY(), frame);

                // Change track height by attribute
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent aEvt) {
                        copyToClipboard(te, alignment, location, me.getX());

                    }
                });
                if (alignment == null) {
                    item.setEnabled(false);
                }
            }

            add(item);
        }


        public void addViewAsPairsMenuItem() {
            final JMenuItem item = new JCheckBoxMenuItem("View as pairs");
            item.setSelected(renderOptions.isViewPairs());
            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    boolean viewAsPairs = item.isSelected();
                    setViewAsPairs(viewAsPairs);
                }
            });
            item.setEnabled(dataManager.isPairedEnd());
            add(item);
        }

        public void addGoToMate(final TrackClickEvent te) {
            // Change track height by attribute
            JMenuItem item = new JMenuItem("Go to mate");
            MouseEvent e = te.getMouseEvent();

            final ReferenceFrame frame = te.getFrame();
            if (frame == null) {
                item.setEnabled(false);
            } else {
                double location = frame.getChromosomePosition(e.getX());
                final Alignment alignment = getAlignmentAt(location, e.getY(), frame);
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent aEvt) {
                        gotoMate(te, alignment);
                    }
                });
                if (alignment == null || !alignment.isPaired() || !alignment.getMate().isMapped()) {
                    item.setEnabled(false);
                }
            }
            add(item);
        }

        public void showMateRegion(final TrackClickEvent te) {
            // Change track height by attribute
            JMenuItem item = new JMenuItem("View mate region in split screen");
            MouseEvent e = te.getMouseEvent();

            final ReferenceFrame frame = te.getFrame();
            if (frame == null) {
                item.setEnabled(false);
            } else {
                double location = frame.getChromosomePosition(e.getX());

                Alignment clickedAlignment = getAlignmentAt(location, e.getY(), frame);

                if (clickedAlignment instanceof PairedAlignment) {
                    Alignment first = ((PairedAlignment) clickedAlignment).getFirstAlignment();
                    Alignment second = ((PairedAlignment) clickedAlignment).getSecondAlignment();
                    if (first.contains(location)) {
                        clickedAlignment = first;

                    } else if (second.contains(location)) {
                        clickedAlignment = second;

                    } else {
                        clickedAlignment = null;

                    }
                }

                final Alignment alignment = clickedAlignment;
                item.addActionListener(aEvt -> splitScreenMate(te, alignment));
                if (alignment == null || !alignment.isPaired() || !alignment.getMate().isMapped()) {
                    item.setEnabled(false);
                }
            }
            add(item);
        }

        public void addClearSelectionsMenuItem() {
            // Change track height by attribute
            JMenuItem item = new JMenuItem("Clear selections");
            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    selectedReadNames.clear();
                    refresh();
                }
            });
            add(item);
        }

        public JMenuItem addShowAllBasesMenuItem() {
            // Change track height by attribute
            final JMenuItem item = new JCheckBoxMenuItem("Show all bases");

            if (renderOptions.colorOption == ColorOption.BISULFITE || renderOptions.colorOption == ColorOption.NOMESEQ) {
                //    item.setEnabled(false);
            } else {
                item.setSelected(renderOptions.showAllBases);
            }
            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    renderOptions.setShowAllBases(item.isSelected());
                    refresh();
                }
            });
            add(item);
            return item;
        }

        public JMenuItem addQuickConsensusModeItem() {
            // Change track height by attribute
            final JMenuItem item = new JCheckBoxMenuItem("Quick consensus mode");
            item.setSelected(renderOptions.quickConsensusMode);

            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    renderOptions.setQuickConsensusMode(item.isSelected());
                    refresh();
                }
            });
            add(item);
            return item;
        }

        public JMenuItem addShowMismatchesMenuItem() {
            // Change track height by attribute
            final JMenuItem item = new JCheckBoxMenuItem("Show mismatched bases");


            item.setSelected(renderOptions.showMismatches);
            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    renderOptions.setShowMismatches(item.isSelected());
                    refresh();
                }
            });
            add(item);
            return item;
        }


        public void addInsertSizeMenuItem() {
            // Change track height by attribute
            final JMenuItem item = new JCheckBoxMenuItem("Set insert size options ...");
            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {

                    InsertSizeSettingsDialog dlg = new InsertSizeSettingsDialog(IGV.getMainFrame(), renderOptions);
                    dlg.setModal(true);
                    dlg.setVisible(true);
                    if (!dlg.isCanceled()) {
                        renderOptions.setComputeIsizes(dlg.isComputeIsize());
                        renderOptions.setMinInsertSizePercentile(dlg.getMinPercentile());
                        renderOptions.setMaxInsertSizePercentile(dlg.getMaxPercentile());
                        if (renderOptions.isComputeIsizes()) {
                            dataManager.updatePEStats(renderOptions);
                        }

                        renderOptions.setMinInsertSize(dlg.getMinThreshold());
                        renderOptions.setMaxInsertSize(dlg.getMaxThreshold());
                        refresh();
                    }
                }
            });


            item.setEnabled(dataManager.isPairedEnd());
            add(item);
        }

        public void addShadeBaseByMenuItem() {

            final JMenuItem item = new JCheckBoxMenuItem("Shade base by quality");
            item.setSelected(renderOptions.shadeBasesOption == ShadeBasesOption.QUALITY);
            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent aEvt) {
                    UIUtilities.invokeOnEventThread(new Runnable() {

                        public void run() {
                            if (item.isSelected()) {
                                renderOptions.shadeBasesOption = ShadeBasesOption.QUALITY;
                            } else {
                                renderOptions.shadeBasesOption = ShadeBasesOption.NONE;
                            }
                            refresh();
                        }
                    });
                }
            });

            add(item);


        }

        public void addShowItems() {

            if (AlignmentTrack.this.coverageTrack != null) {
                final JMenuItem item = new JCheckBoxMenuItem("Show Coverage Track");
                item.setSelected(AlignmentTrack.this.coverageTrack.isVisible());
                item.setEnabled(!AlignmentTrack.this.coverageTrack.isRemoved());
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent aEvt) {
                        UIUtilities.invokeOnEventThread(new Runnable() {

                            public void run() {
                                if (getCoverageTrack() != null) {
                                    getCoverageTrack().setVisible(item.isSelected());
                                    IGV.getInstance().getMainPanel().revalidate();
                                }
                            }
                        });
                    }
                });
                add(item);
            }

            if (AlignmentTrack.this.spliceJunctionTrack != null) {
                final JMenuItem item = new JCheckBoxMenuItem("Show Splice Junction Track");
                item.setSelected(AlignmentTrack.this.spliceJunctionTrack.isVisible());
                item.setEnabled(!AlignmentTrack.this.spliceJunctionTrack.isRemoved());
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent aEvt) {
                        UIUtilities.invokeOnEventThread(new Runnable() {

                            public void run() {
                                if (AlignmentTrack.this.spliceJunctionTrack != null) {
                                    AlignmentTrack.this.spliceJunctionTrack.setVisible(item.isSelected());
                                }
                            }
                        });
                    }
                });
                add(item);
            }

            final JMenuItem alignmentItem = new JMenuItem("Hide Track");
            alignmentItem.setEnabled(!AlignmentTrack.this.isRemoved());
            alignmentItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AlignmentTrack.this.setVisible(false);
                }
            });
            add(alignmentItem);
        }


        public void addCopySequenceItem(final TrackClickEvent te) {
            // Change track height by attribute
            final JMenuItem item = new JMenuItem("Copy read sequence");
            add(item);

            final Alignment alignment = getAlignment(te);
            if (alignment == null) {
                item.setEnabled(false);
                return;
            }

            final String seq = alignment.getReadSequence();
            if (seq == null) {
                item.setEnabled(false);
                return;

            }

            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent aEvt) {
                    StringUtils.copyTextToClipboard(seq);
                }
            });

        }


        public void addBlatItem(final TrackClickEvent te) {
            // Change track height by attribute
            final JMenuItem item = new JMenuItem("Blat read sequence");
            add(item);

            final Alignment alignment = getSpecficAlignment(te);
            if (alignment == null) {
                item.setEnabled(false);
                return;
            }

            final String seq = alignment.getReadSequence();
            if (seq == null) {
                item.setEnabled(false);
                return;

            }

            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent aEvt) {
                    String blatSeq = alignment.getReadStrand() == Strand.NEGATIVE ?
                            SequenceTrack.getReverseComplement(seq) : seq;
                    BlatClient.doBlatQuery(blatSeq);
                }
            });

        }

        public void addExtViewItem(final TrackClickEvent te) {
            // Change track height by attribute
            final JMenuItem item = new JMenuItem("ExtView");
            add(item);

            final Alignment alignment = getAlignment(te);
            if (alignment == null) {
                item.setEnabled(false);
                return;
            }

            final String seq = alignment.getReadSequence();
            if (seq == null) {
                item.setEnabled(false);
                return;

            }

            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent aEvt) {
                    ExtendViewClient.postExtendView(alignment);
                }
            });

        }

        public void addTenXItems() {

            addSeparator();

            final JMenuItem bxItem = new JCheckBoxMenuItem("View linked reads (BX)");
            final JMenuItem miItem = new JCheckBoxMenuItem("View linked reads (MI)");

            if (isLinkedReads()) {
                bxItem.setSelected("BX".equals(renderOptions.linkByTag));
                miItem.setSelected("MI".equals(renderOptions.linkByTag));
            } else {
                bxItem.setSelected(false);
                miItem.setSelected(false);
            }

            bxItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent aEvt) {
                    boolean linkedReads = bxItem.isSelected();
                    setLinkedReads(linkedReads, "BX");
                }
            });
            add(bxItem);

            miItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent aEvt) {
                    boolean linkedReads = miItem.isSelected();
                    setLinkedReads(linkedReads, "MI");
                }
            });
            add(miItem);

        }

        public void addSupplItems() {


            addSeparator();

            final JMenuItem bxItem = new JCheckBoxMenuItem("Link supplementary alignments");

            if (isLinkedReads()) {
                bxItem.setSelected("READNAME".equals(renderOptions.linkByTag));
            } else {
                bxItem.setSelected(false);
            }

            bxItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent aEvt) {
                    boolean linkedReads = bxItem.isSelected();
                    setLinkedReads(linkedReads, "READNAME");
                }
            });
            add(bxItem);
        }
    }


    static class InsertionMenu extends IGVPopupMenu {

        AlignmentBlock insertion;

        InsertionMenu(AlignmentBlock insertion) {

            this.insertion = insertion;

            addCopySequenceItem();

            if (insertion.getBases() != null && insertion.getBases().length > 10) {
                addBlatItem();
            }
        }


        public void addCopySequenceItem() {
            // Change track height by attribute
            final JMenuItem item = new JMenuItem("Copy insert sequence");
            add(item);
            item.addActionListener(aEvt -> StringUtils.copyTextToClipboard(new String(insertion.getBases())));
        }


        public void addBlatItem() {
            // Change track height by attribute
            final JMenuItem item = new JMenuItem("Blat insert sequence");
            add(item);


            item.addActionListener(aEvt -> {
                String blatSeq = new String(insertion.getBases());
                BlatClient.doBlatQuery(blatSeq);
            });
        }

        @Override
        public boolean includeStandardItems() {
            return false;
        }
    }


}
