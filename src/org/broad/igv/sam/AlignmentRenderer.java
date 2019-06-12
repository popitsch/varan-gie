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
import org.broad.igv.feature.Strand;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.prefs.IGVPreferences;
import org.broad.igv.prefs.PreferencesManager;
import org.broad.igv.renderer.ContinuousColorScale;
import org.broad.igv.renderer.GraphicUtils;
import org.broad.igv.renderer.SequenceRenderer;
import org.broad.igv.sam.AlignmentTrack.ColorOption;
import org.broad.igv.sam.AlignmentTrack.RenderOptions;
import org.broad.igv.sam.AlignmentTrack.ShadeBasesOption;
import org.broad.igv.sam.BisulfiteBaseInfo.DisplayStatus;
import org.broad.igv.track.RenderContext;
import org.broad.igv.ui.FontManager;
import org.broad.igv.ui.color.*;
import org.broad.igv.util.ChromosomeColors;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broad.igv.prefs.Constants.*;

/**
 * @author jrobinso
 */
public class AlignmentRenderer {

    private static Logger log = Logger.getLogger(AlignmentRenderer.class);


    // Alignment colors
    private static Color DEFAULT_ALIGNMENT_COLOR = new Color(185, 185, 185); //200, 200, 200);
    private static final Color negStrandColor = new Color(150, 150, 230);
    private static final Color posStrandColor = new Color(230, 150, 150);
    private static final Color LR_COLOR = DEFAULT_ALIGNMENT_COLOR; // "Normal" alignment color
    private static final Color RL_COLOR = new Color(0, 150, 0);
    private static final Color RR_COLOR = new Color(20, 50, 200);
    private static final Color LL_COLOR = new Color(0, 150, 150);
    private static Color smallISizeColor = new Color(0, 0, 150);
    private static Color largeISizeColor = new Color(150, 0, 0);
    private static final Color OUTLINE_COLOR = new Color(185, 185, 185);

    // Clipping colors
    private static Color clippedColor = new Color(255, 20, 147);

    // Indel colors
    public static Color purple = new Color(118, 24, 220);
    private static Color deletionColor = Color.black;
    private static Color skippedColor = new Color(150, 184, 200);
    private static Color unknownGapColor = new Color(0, 150, 0);

    // Bisulfite colors
    private static final Color bisulfiteColorFw1 = new Color(195, 195, 195);
    private static final Color bisulfiteColorRev1 = new Color(195, 210, 195);
    private static final Color nomeseqColor = new Color(195, 195, 195);

    private static Map<String, AlignmentTrack.OrientationType> frOrientationTypes;
    private static Map<String, AlignmentTrack.OrientationType> f1f2OrientationTypes;
    private static Map<String, AlignmentTrack.OrientationType> f2f1OrientationTypes;
    private static Map<String, AlignmentTrack.OrientationType> rfOrientationTypes;
    private static Map<AlignmentTrack.OrientationType, Color> typeToColorMap;
    public static HashMap<Character, Color> nucleotideColors;

    public static final HSLColorTable tenXColorTable1 = new HSLColorTable(30);
    public static final HSLColorTable tenXColorTable2 = new HSLColorTable(270);
    public static final GreyscaleColorTable tenXColorTable3 = new GreyscaleColorTable();

    public static final Color GROUP_DIVIDER_COLOR = new Color(200, 200, 200);
    // A "dummy" reference for soft-clipped reads.
    private static byte[] softClippedReference = new byte[1000];

    private static ColorTable readGroupColors;
    private static ColorTable sampleColors;
    private static Map<String, ColorTable> tagValueColors;
    private static ColorTable defaultTagColors;

    private static void setNucleotideColors() {

        IGVPreferences prefs = PreferencesManager.getPreferences();

        nucleotideColors = new HashMap();

        Color a = ColorUtilities.stringToColor(prefs.get(SAM_COLOR_A), Color.green);
        Color c = ColorUtilities.stringToColor(prefs.get(SAM_COLOR_C), Color.blue);
        Color t = ColorUtilities.stringToColor(prefs.get(SAM_COLOR_T), Color.red);
        Color g = ColorUtilities.stringToColor(prefs.get(SAM_COLOR_G), Color.gray);
        Color n = ColorUtilities.stringToColor(prefs.get(SAM_COLOR_N), Color.gray);

        nucleotideColors.put('A', a);
        nucleotideColors.put('a', a);
        nucleotideColors.put('C', c);
        nucleotideColors.put('c', c);
        nucleotideColors.put('T', t);
        nucleotideColors.put('t', t);
        nucleotideColors.put('G', g);
        nucleotideColors.put('g', g);
        nucleotideColors.put('N', n);
        nucleotideColors.put('n', n);
        nucleotideColors.put('-', Color.lightGray);

    }

    private static void initializeTagTypes() {
        // pre-seed from orientation colors

        // fr Orientations (e.g. Illumina paired-end libraries)
        frOrientationTypes = new HashMap();
        //LR
        frOrientationTypes.put("F1R2", AlignmentTrack.OrientationType.LR);
        frOrientationTypes.put("F2R1", AlignmentTrack.OrientationType.LR);
        frOrientationTypes.put("F R ", AlignmentTrack.OrientationType.LR);
        frOrientationTypes.put("FR", AlignmentTrack.OrientationType.LR);
        //LL
        frOrientationTypes.put("F1F2", AlignmentTrack.OrientationType.LL);
        frOrientationTypes.put("F2F1", AlignmentTrack.OrientationType.LL);
        frOrientationTypes.put("F F ", AlignmentTrack.OrientationType.LL);
        frOrientationTypes.put("FF", AlignmentTrack.OrientationType.LL);
        //RR
        frOrientationTypes.put("R1R2", AlignmentTrack.OrientationType.RR);
        frOrientationTypes.put("R2R1", AlignmentTrack.OrientationType.RR);
        frOrientationTypes.put("R R ", AlignmentTrack.OrientationType.RR);
        frOrientationTypes.put("RR", AlignmentTrack.OrientationType.RR);
        //RL
        frOrientationTypes.put("R1F2", AlignmentTrack.OrientationType.RL);
        frOrientationTypes.put("R2F1", AlignmentTrack.OrientationType.RL);
        frOrientationTypes.put("R F ", AlignmentTrack.OrientationType.RL);
        frOrientationTypes.put("RF", AlignmentTrack.OrientationType.RL);

        // rf orienation  (e.g. Illumina mate-pair libraries)
        rfOrientationTypes = new HashMap();
        //LR
        rfOrientationTypes.put("R1F2", AlignmentTrack.OrientationType.LR);
        rfOrientationTypes.put("R2F1", AlignmentTrack.OrientationType.LR);
        //rfOrientationTypes.put("R F ", AlignmentTrack.OrientationType.LR);
        //rfOrientationTypes.put("RF", AlignmentTrack.OrientationType.LR);
        //LL
        rfOrientationTypes.put("R1R2", AlignmentTrack.OrientationType.LL);
        rfOrientationTypes.put("R2R1", AlignmentTrack.OrientationType.LL);
        rfOrientationTypes.put("R R ", AlignmentTrack.OrientationType.LL);
        rfOrientationTypes.put("RR ", AlignmentTrack.OrientationType.LL);

        rfOrientationTypes.put("F1F2", AlignmentTrack.OrientationType.RR);
        rfOrientationTypes.put("F2F1", AlignmentTrack.OrientationType.RR);
        rfOrientationTypes.put("F F ", AlignmentTrack.OrientationType.RR);
        rfOrientationTypes.put("FF", AlignmentTrack.OrientationType.RR);
        //RL
        rfOrientationTypes.put("F1R2", AlignmentTrack.OrientationType.RL);
        rfOrientationTypes.put("F2R1", AlignmentTrack.OrientationType.RL);
        rfOrientationTypes.put("F R ", AlignmentTrack.OrientationType.RL);
        rfOrientationTypes.put("FR", AlignmentTrack.OrientationType.RL);

        // f1f2 orienation  (e.g. SOLID libraries, AlignmentTrack.OrientationType.second read appears first on + strand (leftmost))
        f2f1OrientationTypes = new HashMap();
        //LR
        f2f1OrientationTypes.put("F2F1", AlignmentTrack.OrientationType.LR);
        f2f1OrientationTypes.put("R1R2", AlignmentTrack.OrientationType.LR);

        //LL
        f2f1OrientationTypes.put("F2R1", AlignmentTrack.OrientationType.LL);
        f2f1OrientationTypes.put("R1F2", AlignmentTrack.OrientationType.LL);

        //RR
        f2f1OrientationTypes.put("R2F1", AlignmentTrack.OrientationType.RR);
        f2f1OrientationTypes.put("F1R2", AlignmentTrack.OrientationType.RR);

        //RL
        f2f1OrientationTypes.put("R2R1", AlignmentTrack.OrientationType.RL);
        f2f1OrientationTypes.put("F1F2", AlignmentTrack.OrientationType.RL);

        // f1f2 orienation  (e.g. SOLID libraries, AlignmentTrack.OrientationType.actually is this one even possible?)
        f1f2OrientationTypes = new HashMap();
        //LR
        f1f2OrientationTypes.put("F1F2", AlignmentTrack.OrientationType.LR);
        f1f2OrientationTypes.put("R2R1", AlignmentTrack.OrientationType.LR);
        //LL
        f1f2OrientationTypes.put("F1R2", AlignmentTrack.OrientationType.LL);
        f1f2OrientationTypes.put("R2F1", AlignmentTrack.OrientationType.LL);
        //RR
        f1f2OrientationTypes.put("R1F2", AlignmentTrack.OrientationType.RR);
        f1f2OrientationTypes.put("F2R1", AlignmentTrack.OrientationType.RR);
        //RL
        f1f2OrientationTypes.put("R1R2", AlignmentTrack.OrientationType.RL);
        f1f2OrientationTypes.put("F2F1", AlignmentTrack.OrientationType.RL);
    }

    private static void initializeTagColors() {
        ColorPalette palette = ColorUtilities.getPalette("Pastel 1");  // TODO let user choose
        readGroupColors = new PaletteColorTable(palette);
        sampleColors = new PaletteColorTable(palette);
        defaultTagColors = new PaletteColorTable(palette);
        tagValueColors = new HashMap();

        typeToColorMap = new HashMap<>(5);
        typeToColorMap.put(AlignmentTrack.OrientationType.LL, LL_COLOR);
        typeToColorMap.put(AlignmentTrack.OrientationType.LR, LR_COLOR);
        typeToColorMap.put(AlignmentTrack.OrientationType.RL, RL_COLOR);
        typeToColorMap.put(AlignmentTrack.OrientationType.RR, RR_COLOR);
        typeToColorMap.put(null, DEFAULT_ALIGNMENT_COLOR);
    }


    static {
        initializeTagTypes();
        setNucleotideColors();
        initializeTagColors();
    }


    AlignmentTrack track;

    public AlignmentRenderer(AlignmentTrack track) {
        this.track = track;
    }

    private void initializeGraphics(RenderContext context) {

        Font font = FontManager.getFont(10);
        Graphics2D g = context.getGraphics2D("ALIGNMENT");

        float alpha = 0.75f;
        int type = AlphaComposite.SRC_OVER;
        Composite alignmentAlphaComposite = AlphaComposite.getInstance(type, alpha);
        g.setComposite(alignmentAlphaComposite);
        g.setFont(font);

        g = context.getGraphics2D("LINK_LINE");
        alpha = 0.3f;
        type = AlphaComposite.SRC_OVER;
        alignmentAlphaComposite = AlphaComposite.getInstance(type, alpha);
        g.setComposite(alignmentAlphaComposite);


        g = context.getGraphics2D("THICK_STROKE");
        g.setStroke(new BasicStroke(2.0f));

        g = context.getGraphics2D("OUTLINE");

        g = context.getGraphics2D("INDEL_LABEL");

        g = context.getGraphics2D("BASE");

        g = context.getGraphics2D("STRAND");
        g.setColor(Color.DARK_GRAY);
    }

    /**
     * Render a row of alignments in the given rectangle.
     */
    public void renderAlignments(List<Alignment> alignments,
                                 RenderContext context,
                                 Rectangle rowRect,
                                 Rectangle trackRect,
                                 RenderOptions renderOptions,
                                 boolean leaveMargin,
                                 Map<String, Color> selectedReadNames,
                                 AlignmentCounts alignmentCounts,
                                 IGVPreferences prefs) {

        initializeGraphics(context);
        double origin = context.getOrigin();
        double locScale = context.getScale();
        boolean completeReadsOnly = prefs.getAsBoolean(SAM_COMPLETE_READS_ONLY);

        if ((alignments != null) && (alignments.size() > 0)) {

            int lastPixelDrawn = -1;

            for (Alignment alignment : alignments) {
                // Compute the start and dend of the alignment in pixels
                double pixelStart = ((alignment.getStart() - origin) / locScale);
                double pixelEnd = ((alignment.getEnd() - origin) / locScale);

                // If any any part of the feature fits in the track rectangle draw  it
                if (pixelEnd < rowRect.x || pixelStart > rowRect.getMaxX()) {
                    continue;
                }

                // Optionally only draw alignments that are completely in view
                if (completeReadsOnly) {
                    if (pixelStart < rowRect.x || pixelEnd > rowRect.getMaxX()) {
                        continue;
                    }
                }

                // If the alignment is 3 pixels or less,  draw alignment as a single block,
                // further detail would not be seen and just add to drawing overhead
                // Does the change for Bisulfite kill some machines?
                double pixelWidth = pixelEnd - pixelStart;

                Color alignmentColor = getAlignmentColor(alignment, renderOptions);

                if ((pixelWidth < 2) && !(AlignmentTrack.isBisulfiteColorType(renderOptions.getColorOption()) && (pixelWidth >= 1))) {

                    // Optimization for really zoomed out views.  If this alignment occupies screen space already taken,
                    // and it is the default color, skip drawing.
                    if (pixelEnd <= lastPixelDrawn && alignmentColor == DEFAULT_ALIGNMENT_COLOR) {
                        continue;
                    }

                    Graphics2D g = context.getGraphics2D("ALIGNMENT");
                    g.setColor(alignmentColor);
                    int w = Math.max(1, (int) (pixelWidth));
                    int h = (int) Math.max(1, rowRect.getHeight() - 2);
                    int y = (int) (rowRect.getY() + (rowRect.getHeight() - h) / 2);
                    g.fillRect((int) pixelStart, y, w, h);
                    lastPixelDrawn = (int) pixelStart + w;
                } else if (alignment instanceof PairedAlignment) {
                    drawPairedAlignment((PairedAlignment) alignment, rowRect, context, renderOptions, leaveMargin, selectedReadNames, alignmentCounts, prefs);
                } else if (alignment instanceof LinkedAlignment) {
                    drawLinkedAlignment((LinkedAlignment) alignment, rowRect, context, renderOptions, leaveMargin, selectedReadNames, alignmentCounts, prefs);
                } else {
                    drawAlignment(alignment, rowRect, context, alignmentColor, renderOptions, leaveMargin, selectedReadNames, alignmentCounts, false, prefs);
                }
            }

            // Optionally draw a border around the center base
            boolean showCenterLine = prefs.getAsBoolean(SAM_SHOW_CENTER_LINE);
            final int bottom = rowRect.y + rowRect.height;
            if (showCenterLine) {
                // Calculate center lines
                double center = (int) (context.getReferenceFrame().getCenter() - origin);
                int centerLeftP = (int) (center / locScale);
                int centerRightP = (int) ((center + 1) / locScale);
                //float transparency = Math.max(0.5f, (float) Math.round(10 * (1 - .75 * locScale)) / 10);
                Graphics2D g = context.getGraphics();
                g.setColor(Color.black);
                GraphicUtils.drawDottedDashLine(g, centerLeftP, rowRect.y, centerLeftP, bottom);
                if ((centerRightP - centerLeftP > 2)) {
                    GraphicUtils.drawDottedDashLine(g, centerRightP, rowRect.y, centerRightP, bottom);
                }
            }
        }
    }


    public void renderExpandedInsertion(InsertionMarker i,
                                        List<Alignment> alignments,
                                        RenderContext context,
                                        Rectangle rect,
                                        boolean leaveMargin) {

        double origin = context.getOrigin();
        double locScale = context.getScale();

        if ((alignments != null) && (alignments.size() > 0)) {

            Graphics2D g = context.getGraphics2D("INSERTIONS");
            //dhmay adding check for adequate track height
            double dX = 1 / context.getScale();
            int fontSize = (int) Math.min(dX, 12);
            if (fontSize >= 8) {
                Font f = FontManager.getFont(Font.BOLD, fontSize);
                g.setFont(f);
            }

            for (Alignment alignment : alignments) {

                if (alignment.getEnd() < i.position) continue;
                if (alignment.getStart() > i.position) break;

                AlignmentBlock aBlock = alignment.getInsertionAt(i.position);

                if (aBlock != null) {

                    // Compute the start and dend of the alignment in pixels
                    double pixelStart = (aBlock.getStart() - origin) / locScale;
                    double pixelEnd = (aBlock.getEnd() - origin) / locScale;
                    int x = (int) pixelStart;

                    // If any any part of the feature fits in the track rectangle draw  it
                    if (pixelEnd < rect.x || pixelStart > rect.getMaxX()) {
                        continue;
                    }

                    int bpWidth = aBlock.getBases().length;
                    double pxWidthExact = ((double) bpWidth) / locScale;
                    int h = (int) Math.max(1, rect.getHeight() - 2);
                    int y = (int) (rect.getY() + (rect.getHeight() - h) / 2) - 1;


                    if (aBlock.getBases() == null) {
                        g.setColor(purple);
                        g.fillRect(x, y, (int) pxWidthExact, h);

                    } else {
                        drawExpandedInsertionBases(x, context, rect, aBlock, leaveMargin);
                    }
                }


            }
        }
    }


    private void drawLinkedAlignment(LinkedAlignment alignment, Rectangle rowRect, RenderContext context,
                                     RenderOptions renderOptions, boolean leaveMargin,
                                     Map<String, Color> selectedReadNames, AlignmentCounts alignmentCounts,
                                     IGVPreferences prefs) {

        double origin = context.getOrigin();
        double locScale = context.getScale();

        Color alignmentColor = getAlignmentColor(alignment, renderOptions);
     //   Graphics2D g = context.getGraphics2D("ALIGNMENT");
     //   g.setColor(alignmentColor);


        List<Alignment> barcodedAlignments = alignment.alignments;

        if (barcodedAlignments.size() > 0) {

            boolean mixedStrand =  (alignment instanceof LinkedAlignment && alignment.getStrand() == Strand.NONE);
            Alignment firstAlignment = barcodedAlignments.get(0);

            if (barcodedAlignments.size() > 1) {
                Graphics2D gline = context.getGraphics2D("LINK_LINE");
                gline.setColor(alignmentColor);
                int startX = (int) ((firstAlignment.getEnd() - origin) / locScale);
                int endX = (int) ((barcodedAlignments.get(barcodedAlignments.size() - 1).getStart() - origin) / locScale);
                int h = (int) Math.max(1, rowRect.getHeight() - (leaveMargin ? 2 : 0));
                int y = (int) (rowRect.getY());
                startX = Math.max(rowRect.x, startX);
                endX = Math.min(rowRect.x + rowRect.width, endX);
                gline.drawLine(startX, y + h / 2, endX, y + h / 2);
            }
            for (int i = 0; i < barcodedAlignments.size(); i++) {
                boolean overlapped = false;
                Alignment al = barcodedAlignments.get(i);
                if (al.isNegativeStrand()) {
                    if(mixedStrand) alignmentColor = negStrandColor;
                    overlapped = (i > 0 && barcodedAlignments.get(i - 1).getAlignmentEnd() > al.getAlignmentStart());
                } else {
                    if(mixedStrand) alignmentColor = posStrandColor;
                    overlapped = i < barcodedAlignments.size() - 1 && al.getAlignmentEnd() > barcodedAlignments.get(i + 1).getAlignmentStart();
                }
                drawAlignment(al, rowRect, context, alignmentColor, renderOptions, leaveMargin, selectedReadNames, alignmentCounts, overlapped, prefs);
            }
        }
    }


    /**
     * Method for drawing alignments without "blocks" (e.g. DotAlignedAlignment)
     */
    private void drawSimpleAlignment(Alignment alignment,
                                     Rectangle rect,
                                     Graphics2D g,
                                     RenderContext context,
                                     boolean flagUnmappedPair) {

        double origin = context.getOrigin();
        double locScale = context.getScale();
        int x = (int) ((alignment.getStart() - origin) / locScale);
        int length = alignment.getEnd() - alignment.getStart();
        int w = (int) Math.ceil(length / locScale);
        int h = (int) Math.max(1, rect.getHeight() - 2);
        int y = (int) (rect.getY() + (rect.getHeight() - h) / 2);
        int arrowLength = Math.min(5, w / 6);

        int d = Math.max(0, (int) (arrowLength + 2 - AlignmentPacker.MIN_ALIGNMENT_SPACING / context.getScale()));
        if (alignment.isNegativeStrand()) x += d;
        if (!alignment.isNegativeStrand()) w -= d;


        int[] xPoly = null;
        int[] yPoly = {y, y, y + h / 2, y + h, y + h};


        // Don't draw off edge of clipping rect
        if (x < rect.x && (x + w) > (rect.x + rect.width)) {
            x = rect.x;
            w = rect.width;
            arrowLength = 0;
        } else if (x < rect.x) {
            int delta = rect.x - x;
            x = rect.x;
            w -= delta;
            if (alignment.isNegativeStrand()) {
                arrowLength = 0;
            }
        } else if ((x + w) > (rect.x + rect.width)) {
            w -= ((x + w) - (rect.x + rect.width));
            if (!alignment.isNegativeStrand()) {
                arrowLength = 0;
            }
        }


        if (alignment.isNegativeStrand()) {
            //     2     1
            //   3
            //     5     5
            xPoly = new int[]{x + w, x, x - arrowLength, x, x + w};
        } else {
            //     1     2
            //             3
            //     5     4
            xPoly = new int[]{x, x + w, x + w + arrowLength, x + w, x};
        }
        g.fillPolygon(xPoly, yPoly, xPoly.length);

        if (flagUnmappedPair && alignment.isPaired() && !alignment.getMate().isMapped()) {
            g.setColor(Color.red);
            g.drawPolygon(xPoly, yPoly, xPoly.length);
        }
    }

    /**
     * Draw a pair of alignments as a single "template".
     *
     * @param pair
     * @param rowRect
     * @param context
     * @param renderOptions
     * @param leaveMargin
     * @param selectedReadNames
     * @param alignmentCounts
     */
    private void drawPairedAlignment(
            PairedAlignment pair,
            Rectangle rowRect,
            RenderContext context,
            RenderOptions renderOptions,
            boolean leaveMargin,
            Map<String, Color> selectedReadNames,
            AlignmentCounts alignmentCounts,
            IGVPreferences prefs) {

        double locScale = context.getScale();

        Color alignmentColor1 = getAlignmentColor(pair.firstAlignment, renderOptions);
        Color alignmentColor2 = null;

        boolean overlapped = pair.secondAlignment != null && (pair.firstAlignment.getChr().equals(pair.secondAlignment.getChr())) &&
                pair.firstAlignment.getAlignmentEnd() > pair.secondAlignment.getAlignmentStart();

        Graphics2D g = context.getGraphics2D("ALIGNMENT");
        g.setColor(alignmentColor1);

        drawAlignment(pair.firstAlignment, rowRect, context, alignmentColor1, renderOptions, leaveMargin, selectedReadNames, alignmentCounts, overlapped, prefs);

        //If the paired alignment is in memory, we draw it.
        //However, we get the coordinates from the first alignment
        if (pair.secondAlignment != null) {

            if (alignmentColor2 == null) {
                alignmentColor2 = getAlignmentColor(pair.secondAlignment, renderOptions);
            }
            g.setColor(alignmentColor2);

            drawAlignment(pair.secondAlignment, rowRect, context, alignmentColor2, renderOptions, leaveMargin, selectedReadNames, alignmentCounts, overlapped, prefs);
        } else {
            return;
        }

        Color lineColor = DEFAULT_ALIGNMENT_COLOR;

        if (alignmentColor1.equals(alignmentColor2) || pair.secondAlignment == null) {
            lineColor = alignmentColor1;
        }
        g.setColor(lineColor);

        double origin = context.getOrigin();
        int startX = (int) ((pair.firstAlignment.getEnd() - origin) / locScale);
        int endX = (int) ((pair.firstAlignment.getMate().getStart() - origin) / locScale);

        int h = (int) Math.max(1, rowRect.getHeight() - (leaveMargin ? 2 : 0));
        int y = (int) (rowRect.getY());

        startX = Math.max(rowRect.x, startX);
        endX = Math.min(rowRect.x + rowRect.width, endX);
        g.drawLine(startX, y + h / 2, endX, y + h / 2);


    }

    /**
     * Draw a single ungapped block in an alignment.
     */
    private void drawAlignmentBlock(Graphics2D blockGraphics, Graphics2D outlineGraphics,
                                    Graphics2D clippedGraphics, Graphics2D strandGraphics,
                                    boolean isNegativeStrand, int alignmentChromStart,
                                    int alignmentChromEnd, double blockChromStart, int blockChromEnd,
                                    int blockPxStart, int blockPxWidth, int y, int h, double locSale,
                                    boolean overlapped, boolean leftClipped, boolean rightClipped) {

        if (blockPxWidth == 0) {
            return;
        } // skip blocks too small to render

        int blockPxEnd = blockPxStart + blockPxWidth;

        boolean leftmost = (blockChromStart == alignmentChromStart),
                rightmost = (blockChromEnd == alignmentChromEnd),
                tallEnoughForArrow = h > 6;

        if (h == 1) {
            blockGraphics.drawLine(blockPxStart, y, blockPxEnd, y);
        } else {
            Shape blockShape;
            int arrowPxWidth = Math.min(Math.min(5, h / 2), blockPxWidth / 6);

            if (!overlapped) {
                int pixelGap = (int) (AlignmentPacker.MIN_ALIGNMENT_SPACING / locSale);
                if (pixelGap < arrowPxWidth) {
                    arrowPxWidth = Math.max(0, arrowPxWidth - pixelGap);
                }
            }

            // Draw block as a rectangle; use a pointed hexagon in terminal block to indicate strand.
            int[] xPoly = {blockPxStart - (leftmost && isNegativeStrand && tallEnoughForArrow ? arrowPxWidth : 0),
                    blockPxStart,
                    blockPxEnd,
                    blockPxEnd + (rightmost && !isNegativeStrand && tallEnoughForArrow ? arrowPxWidth : 0),
                    blockPxEnd,
                    blockPxStart},
                    yPoly = {y + h / 2,
                            y,
                            y,
                            y + h / 2,
                            y + h,
                            y + h};
            blockShape = new Polygon(xPoly, yPoly, xPoly.length);

            blockGraphics.fill(blockShape);
            if (outlineGraphics != null) {
                outlineGraphics.draw(blockShape);
            }
            if (leftmost && leftClipped) {
                clippedGraphics.drawLine(xPoly[0], yPoly[0], xPoly[1], yPoly[1]);
                clippedGraphics.drawLine(xPoly[5], yPoly[5] - 1, xPoly[0], yPoly[0]);
            }
            if (rightmost && rightClipped) {
                clippedGraphics.drawLine(xPoly[2], yPoly[2], xPoly[3], yPoly[3]);
                clippedGraphics.drawLine(xPoly[3], yPoly[3], xPoly[4], yPoly[4] - 1);
            }
        }

    }

    /**
     * Draw a (possibly gapped) alignment
     *
     * @param alignment
     * @param rowRect
     * @param context
     * @param alignmentColor
     * @param renderOptions
     * @param leaveMargin
     * @param selectedReadNames
     * @param alignmentCounts
     */
    private void drawAlignment(
            Alignment alignment,
            Rectangle rowRect,
            RenderContext context,
            Color alignmentColor,
            AlignmentTrack.RenderOptions renderOptions,
            boolean leaveMargin,
            Map<String, Color> selectedReadNames,
            AlignmentCounts alignmentCounts,
            boolean overlapped,
            IGVPreferences prefs) {

        AlignmentBlock[] blocks = alignment.getAlignmentBlocks();

        Graphics2D g = context.getGraphics2D("ALIGNMENT");
        g.setColor(alignmentColor);

        // No blocks.  Note: SAM/BAM alignments always have at least 1 block
        if (blocks == null || blocks.length == 0) {
            drawSimpleAlignment(alignment, rowRect, g, context, renderOptions.flagUnmappedPairs);
            return;
        }

        boolean flagLargeIndels = prefs.getAsBoolean(SAM_FLAG_LARGE_INDELS);
        int largeInsertionsThreshold = prefs.getAsInt(SAM_LARGE_INDELS_THRESHOLD);
        boolean hideSmallIndelsBP = prefs.getAsBoolean(SAM_HIDE_SMALL_INDEL);
        int indelThresholdBP = prefs.getAsInt(SAM_SMALL_INDEL_BP_THRESHOLD);
        boolean quickConsensus = renderOptions.quickConsensusMode;
        final float snpThreshold = prefs.getAsFloat(SAM_ALLELE_THRESHOLD);

        // Scale and position of the alignment rendering.
        double locScale = context.getScale();
        int h = (int) Math.max(1, rowRect.getHeight() - (leaveMargin ? 2 : 0));
        int y = (int) (rowRect.getY());

        // Get a graphics context for outlining alignment blocks.
        Graphics2D outlineGraphics = null;
        if (selectedReadNames.containsKey(alignment.getReadName())) {
            Color c = selectedReadNames.get(alignment.getReadName());
            c = (c == null) ? Color.blue : c;
            outlineGraphics = context.getGraphics2D("THICK_STROKE");
            g.setColor(c);
        } else if (renderOptions.flagUnmappedPairs && alignment.isPaired() && !alignment.getMate().isMapped()) {
            outlineGraphics = context.getGraphics2D("OUTLINE");
            outlineGraphics.setColor(Color.red);
        } else if (alignment.getMappingQuality() == 0 && renderOptions.flagZeroQualityAlignments) {
            outlineGraphics = context.getGraphic2DForColor(OUTLINE_COLOR);
        }

        // Get a graphics context for drawing clipping indicators.
        Graphics2D clippedGraphics = context.getGraphic2DForColor(clippedColor);
        if (h > 5) {
            clippedGraphics.setStroke(new BasicStroke(1.2f));
        }

        // Get a graphics context for drawing strand indicators.
        Graphics2D strandGraphics = context.getGraphics2D("STRAND");

        // Define a graphics context for indel labels.
        Graphics2D largeIndelGraphics = context.getGraphics2D("INDEL_LABEL");
        largeIndelGraphics.setFont(FontManager.getFont(Font.BOLD, h - 2));

        // Get a graphics context for drawing individual basepairs.
        Graphics2D bpGraphics = context.getGraphics2D("BASE");
        int dX = (int) Math.max(1, (1.0 / locScale));
        if (dX >= 8) {
            Font f = FontManager.getFont(Font.BOLD, Math.min(dX, h));
            bpGraphics.setFont(f);
        }

        /* Process the alignment. */
        AlignmentBlock firstBlock = blocks[0], lastBlock = blocks[blocks.length - 1];
        int alignmentChromStart = (int) firstBlock.getStart(),
                alignmentChromEnd = (int) (lastBlock.getStart() + lastBlock.getLength());

        /* Clipping */
        boolean flagClipping = prefs.getAsBoolean(SAM_FLAG_CLIPPING);
        int clippingThreshold = prefs.getAsInt(SAM_CLIPPING_THRESHOLD);
        int[] clipping = SAMAlignment.getClipping(alignment.getCigarString());
        boolean leftClipped = flagClipping && ((clipping[0] + clipping[1]) > clippingThreshold);
        boolean rightClipped = flagClipping && ((clipping[2] + clipping[3]) > clippingThreshold);

        // BED-style coordinate for the visible context.  Do not draw outside the context.
        double contextChromStart = context.getOrigin(),
                contextChromEnd = Math.ceil(context.getEndLocation());

        // BED-style start coordinate for the next alignment block to draw.
        double blockChromStart =   Math.max(alignmentChromStart, contextChromStart);

        // Draw aligment blocks separated by gaps.  Define the blocks by walking through the gap list,
        // skipping over gaps that are too small to show at the curren resolution.
        java.util.List<Gap> gaps = alignment.getGaps();
        if (gaps != null) {

            for (Gap gap : gaps) {
                int gapChromStart = gap.getStart(),
                        gapChromWidth = gap.getnBases(),
                        gapChromEnd = gapChromStart + gapChromWidth,
                        gapPxEnd = (int) ((Math.min(contextChromEnd, gapChromEnd) - contextChromStart) / locScale);

                if (gapChromEnd <= contextChromStart) { // gap ends before the visible context
                    continue; // move to next gap
                } else if (gapChromStart >= contextChromEnd) { // gap starts after the visible context
                    break; // done examining gaps
                }

                // Draw the gap if it is sufficiently large
                boolean drawGap = (!hideSmallIndelsBP || gapChromWidth >= indelThresholdBP);

                //Check deletion consistency  -- TODO this needs work, disabled for now
                //if (drawGap && quickConsensus) {
                //    drawGap = alignmentCounts.isConsensusDeletion(gapChromStart, gap.getnBases(), snpThreshold);
                //}
                if (!drawGap) {
                    continue;
                }

                // Draw the preceding alignment block.
                int blockPxStart = (int) ((blockChromStart - contextChromStart) / locScale),
                        blockChromEnd = gapChromStart,
                        blockPxWidth = (int) Math.max(1, (blockChromEnd - blockChromStart) / locScale - 1),
                        blockPxEnd = blockPxStart + blockPxWidth;

                drawAlignmentBlock(g, outlineGraphics, clippedGraphics, strandGraphics, alignment.isNegativeStrand(),
                        alignmentChromStart, alignmentChromEnd, blockChromStart, blockChromEnd,
                        blockPxStart, blockPxWidth, y, h, locScale, overlapped, leftClipped, rightClipped);

                // Draw the gap line.
                Graphics2D gapGraphics = context.getGraphics2D("GAP");
                int ggOffset = 0;
                if (gap.getType() == SAMAlignment.UNKNOWN) {
                    gapGraphics.setColor(unknownGapColor);
                } else if (gap.getType() == SAMAlignment.SKIPPED_REGION) {
                    gapGraphics.setColor(skippedColor);
                } else if (h > 5) {
                    gapGraphics = context.getGraphics2D("THICK_STROKE");
                    gapGraphics.setColor(deletionColor);
                    ggOffset = 0;
                }

                gapGraphics.drawLine(blockPxEnd + ggOffset, y + h / 2, gapPxEnd - ggOffset, y + h / 2);
                // Label the size of the deletion if it is "large" and the label fits.
                if (flagLargeIndels && (gap.getType() == SAMAlignment.DELETION) && gapChromWidth > largeInsertionsThreshold) {
                    drawLargeIndelLabel(largeIndelGraphics,
                            false,
                            Globals.DECIMAL_FORMAT.format(gapChromWidth),
                            ((blockPxEnd + gapPxEnd) / 2),
                            y,
                            h,
                            gapPxEnd - blockPxEnd - 2,
                            context.translateX,
                            null);
                }

                // Start the next alignment block after the gap.
                blockChromStart = gapChromEnd;
            }
        }

        // Draw the final block after the last gap.
        int blockPxStart = (int) ((blockChromStart - contextChromStart) / locScale),
                blockChromEnd = (int) Math.min(contextChromEnd, alignmentChromEnd),
                blockPxWidth = (int) Math.max(1, (blockChromEnd - blockChromStart) / locScale - 1);
        drawAlignmentBlock(g, outlineGraphics, clippedGraphics, strandGraphics, alignment.isNegativeStrand(),
                alignmentChromStart, alignmentChromEnd, blockChromStart, blockChromEnd,
                blockPxStart, blockPxWidth, y, h, locScale, overlapped, leftClipped, rightClipped);

        // Draw insertions.
        drawInsertions(rowRect, alignment, context, renderOptions, alignmentCounts, leaveMargin, prefs);

        // Draw basepairs / mismatches.
        if (locScale < 100) {
            if (renderOptions.showMismatches || renderOptions.showAllBases) {
                for (AlignmentBlock aBlock : alignment.getAlignmentBlocks()) {
                    int aBlockChromStart = (int) aBlock.getStart(),
                            aBlockChromEnd = (int) (aBlock.getStart() + aBlock.getLength());

                    if (aBlockChromEnd <= contextChromStart) { // block ends before the visible context
                        continue; // move to next block
                    } else if (aBlockChromStart >= contextChromEnd) { // block starts after the visible context
                        break; // done examining blocks
                    }

                    drawBases(context, bpGraphics, rowRect, alignment, aBlock, alignmentCounts, alignmentColor, leaveMargin, renderOptions, prefs);
                }
            }
        }
    }


    /**
     * Draw bases for an alignment block.  The bases are "overlaid" on the block with a transparency value (alpha)
     * that is proportional to the base quality score, or flow signal deviation, whichever is selected.
     *
     * @param context
     * @param g
     * @param rect
     * @param baseAlignment
     * @param block
     * @param alignmentCounts
     * @param alignmentColor
     * @param leaveMargin
     * @param renderOptions
     */
    private void drawBases(RenderContext context,
                           Graphics2D g,
                           Rectangle rect,
                           Alignment baseAlignment,
                           AlignmentBlock block,
                           AlignmentCounts alignmentCounts,
                           Color alignmentColor,
                           boolean leaveMargin,
                           RenderOptions renderOptions,
    IGVPreferences prefs) {

        boolean isSoftClipped = block.isSoftClipped();


        // Get the base qualities, start/end,  and reference sequence
        String chr = context.getChr();
        final int start = block.getStart();
        final int end = block.getEnd();
        Genome genome = GenomeManager.getInstance().getCurrentGenome();

        final byte[] reference = isSoftClipped ? softClippedReference : genome.getSequence(chr, start, end);

        boolean haveBases = (block.hasBases() && block.getLength() > 0);

        ShadeBasesOption shadeBasesOption = renderOptions.shadeBasesOption;
        ColorOption colorOption = renderOptions.getColorOption();
        final boolean quickConsensus = renderOptions.quickConsensusMode;
        final float snpThreshold = prefs.getAsFloat(SAM_ALLELE_THRESHOLD);


        // Disable showAllBases in bisulfite mode
        boolean showAllBases = renderOptions.showAllBases &&
                !(colorOption == ColorOption.BISULFITE || colorOption == ColorOption.NOMESEQ);

        if (!showAllBases && (!haveBases || reference == null)) {
            return;
        }

        byte[] read;
        if (haveBases) {
            read = block.getBases();
        } else {
            read = reference;
        }


        double locScale = context.getScale();
        double origin = context.getOrigin();

        // Compute bounds
        int pY = (int) rect.getY();
        int dY = (int) rect.getHeight();
        int dX = (int) Math.max(1, (1.0 / locScale));

        BisulfiteBaseInfo bisinfo = null;
        boolean nomeseqMode = (renderOptions.getColorOption().equals(AlignmentTrack.ColorOption.NOMESEQ));
        boolean bisulfiteMode = AlignmentTrack.isBisulfiteColorType(renderOptions.getColorOption());
        if (nomeseqMode) {
            bisinfo = new BisulfiteBaseInfoNOMeseq(reference, baseAlignment, block, renderOptions.bisulfiteContext);
        } else if (bisulfiteMode) {
            bisinfo = new BisulfiteBaseInfo(reference, baseAlignment, block, renderOptions.bisulfiteContext);
        }

        for (int loc = start; loc < end; loc++) {
            int idx = loc - start;

            boolean misMatch = haveBases && AlignmentUtils.isMisMatch(reference, read, isSoftClipped, idx);

            if (showAllBases || (!bisulfiteMode && misMatch) ||
                    (bisulfiteMode && (!DisplayStatus.NOTHING.equals(bisinfo.getDisplayStatus(idx))))) {
                char c = (char) read[idx];

                Color color = nucleotideColors.get(c);
                if (bisulfiteMode) color = bisinfo.getDisplayColor(idx);
                if (color == null) {
                    color = Color.black;
                }

                if (ShadeBasesOption.QUALITY == shadeBasesOption) {
                    byte qual = block.getQuality(loc - start);
                    color = getShadedColor(qual, color, alignmentColor, prefs);
                }

                double bisulfiteXaxisShift = (bisulfiteMode) ? bisinfo.getXaxisShift(idx) : 0;

                // If there is room for text draw the character, otherwise
                // just draw a rectangle to represent the
                int pX = (int) (((double) loc + bisulfiteXaxisShift - origin) / locScale);

                // Don't draw out of clipping rect
                if (pX > rect.getMaxX()) {
                    break;
                } else if (pX + dX < rect.getX()) {
                    continue;
                }

                BisulfiteBaseInfo.DisplayStatus bisstatus = (bisinfo == null) ? null : bisinfo.getDisplayStatus(idx);

                if (isSoftClipped || bisulfiteMode ||
                        // In "quick consensus" mode, only show mismatches at positions with a consistent alternative basepair.
                        (!quickConsensus || alignmentCounts.isConsensusMismatch(loc, reference[idx], chr, snpThreshold))
                        ) {
                    drawBase(g, color, c, pX, pY, dX, dY - (leaveMargin ? 2 : 0), bisulfiteMode, bisstatus);
                }
            }
        }

    }

    /**
     * Draw the base using either a letter or character, using the given color,
     * depending on size and bisulfite status
     *
     * @param g
     * @param color
     * @param c
     * @param pX
     * @param pY
     * @param dX
     * @param dY
     * @param bisulfiteMode
     * @param bisstatus
     */
    private void drawBase(Graphics2D g, Color color, char c, int pX, int pY, int dX, int dY, boolean bisulfiteMode,
                          DisplayStatus bisstatus) {
        if (((dY >= 12) && (dX >= dY)) && (!bisulfiteMode || (bisulfiteMode && bisstatus.equals(DisplayStatus.CHARACTER)))) {
            g.setColor(color);
            GraphicUtils.drawCenteredText(new char[]{c}, pX, pY, dX, dY, g);
        } else {

            int pX0i = pX, dXi = dX;

            // If bisulfite mode, we expand the rectangle to make it more visible
            if (bisulfiteMode && bisstatus.equals(DisplayStatus.COLOR)) {
                if (dXi < 3) {
                    int expansion = dXi;
                    pX0i -= expansion;
                    dXi += (2 * expansion);
                }
            }

            int dW = (dXi > 4 ? dXi - 1 : dXi);

            if (color != null) {
                g.setColor(color);
                g.fillRect(pX0i, pY, dXi, dY);
            }
        }
    }

    private Color getShadedColor(byte qual, Color foregroundColor, Color backgroundColor, IGVPreferences prefs) {
        float alpha = 0;
        int minQ = prefs.getAsInt(SAM_BASE_QUALITY_MIN);
        if (qual < minQ) {
            alpha = 0.1f;
        } else {
            int maxQ = prefs.getAsInt(SAM_BASE_QUALITY_MAX);
            alpha = Math.max(0.1f, Math.min(1.0f, 0.1f + 0.9f * (qual - minQ) / (maxQ - minQ)));
        }
        // Round alpha to nearest 0.1
        alpha = ((int) (alpha * 10 + 0.5f)) / 10.0f;

        if (alpha >= 1) {
            return foregroundColor;
        }
        Color color = ColorUtilities.getCompositeColor(backgroundColor, foregroundColor, alpha);
        return color;
    }

    private void drawLargeIndelLabel(Graphics2D g, boolean isInsertion, String labelText, int pxCenter,
                                     int pxTop, int pxH, int pxWmax, int translateX, AlignmentBlock insertionBlock) {

        final int pxPad = 2;   // text padding in the label
        final int pxWing = (pxH > 10 ? 2 : 1);  // width of the cursor "wing"
        final int minTextHeight = 8; // min height to draw text

        // Calculate the width required to draw the label
        Rectangle2D textBounds = g.getFontMetrics().getStringBounds(labelText, g);
        int pxTextW = 2 * pxPad + (int) textBounds.getWidth();
        boolean doesTextFit = (pxH >= minTextHeight) && (pxTextW < pxWmax);

        if (!doesTextFit && !isInsertion) {
            return;
        } // only label deletions when the text fits

        // Calculate the pixel bounds of the label
        int pxW = (int) Math.max(2, Math.min(pxTextW, pxWmax)),
                pxLeft = pxCenter - (int) Math.ceil(pxW / 2),
                pxRight = pxLeft + pxW;

        // Draw the label
        g.setColor(isInsertion ? purple : Color.white);
        g.fillRect(pxLeft, pxTop, pxRight - pxLeft, pxH);

        if (isInsertion && pxH > 5) {
            g.fillRect(pxLeft - pxWing, pxTop, pxRight - pxLeft + 2 * pxWing, 2);
            g.fillRect(pxLeft - pxWing, pxTop + pxH - 2, pxRight - pxLeft + 2 * pxWing, 2);
        } // draw "wings" For insertions

        if (doesTextFit) {
            g.setColor(isInsertion ? Color.white : purple);
            GraphicUtils.drawCenteredText(labelText, pxLeft, pxTop, pxW, pxH, g);
        } // draw the text if it fits

        if (insertionBlock != null) {
            insertionBlock.setPixelRange(pxLeft + translateX, pxRight + translateX);
        }
    }

    private void drawInsertions(Rectangle rect, Alignment alignment, RenderContext context, RenderOptions renderOptions,
                                AlignmentCounts alignmentCounts, boolean leaveMargin, IGVPreferences prefs) {

        AlignmentBlock[] insertions = alignment.getInsertions();
        double origin = context.getOrigin();
        double locScale = context.getScale();
        boolean flagLargeIndels = prefs.getAsBoolean(SAM_FLAG_LARGE_INDELS);
        int largeInsertionsThreshold = prefs.getAsInt(SAM_LARGE_INDELS_THRESHOLD);
        final float snpThreshold = prefs.getAsFloat(SAM_ALLELE_THRESHOLD);

        // TODO Quick consensus for insertions needs worked -- disabled for now
        boolean quickConsensus = false;//renderOptions.quickConsensusMode;


        if (insertions != null) {

            InsertionMarker expandedInsertion = InsertionManager.getInstance().getSelectedInsertion(context.getReferenceFrame().getChrName());
            int expandedPosition = expandedInsertion == null ? -1 : expandedInsertion.position;

            boolean hideSmallIndelsBP = prefs.getAsBoolean(SAM_HIDE_SMALL_INDEL);
            int indelThresholdBP = prefs.getAsInt(SAM_SMALL_INDEL_BP_THRESHOLD);

            for (AlignmentBlock aBlock : insertions) {

                if(aBlock.getStart() == expandedPosition) continue;   // Skip, will be drawn expanded

                int x = (int) ((aBlock.getStart() - origin) / locScale);
                int bpWidth = aBlock.getBases().length;
                double pxWidthExact = ((double) bpWidth) / locScale;
                int h = (int) Math.max(1, rect.getHeight() - (leaveMargin ? 2 : 0));
                int y = (int) (rect.getY() + (rect.getHeight() - h) / 2) - (leaveMargin ? 1 : 0);

                // Don't draw out of clipping rect
                if (x > rect.getMaxX()) {
                    break;
                } else if (x < rect.getX()) {
                    continue;
                }

                if ((!hideSmallIndelsBP || bpWidth >= indelThresholdBP)) {
                       // && (!quickConsensus || alignmentCounts.isConsensusInsertion(aBlock.getStart(), snpThreshold))) {
                    if (flagLargeIndels && bpWidth > largeInsertionsThreshold) {
                        drawLargeIndelLabel(context.getGraphics2D("INDEL_LABEL"),
                                true,
                                Globals.DECIMAL_FORMAT.format(bpWidth),
                                x - 1,
                                y,
                                h,
                                (int) pxWidthExact,
                                context.translateX,
                                aBlock);
                    } else {
                        int pxWing = (h > 10 ? 2 : (h > 5) ? 1 : 0);
                        Graphics2D g = context.getGraphics();
                        g.setColor(purple);
                        g.fillRect(x, y, 2, h);
                        g.fillRect(x - pxWing, y, 2 + 2 * pxWing, 2);
                        g.fillRect(x - pxWing, y + h - 2, 2 + 2 * pxWing, 2);

                        x += context.translateX;
                        aBlock.setPixelRange(x - pxWing, x + 2 + pxWing);
                    }
                }
            }
        }
    }


    private void drawExpandedInsertionBases(int pixelPosition,
                                            RenderContext context,
                                            Rectangle rect,
                                            AlignmentBlock block,
                                            boolean leaveMargin) {

        Graphics2D g = context.getGraphics2D("INSERTIONS");
        byte[] bases = block.getBases();
        int padding = block.getPadding();

        double locScale = context.getScale();
        double origin = context.getOrigin();

        // Compute bounds
        int pY = (int) rect.getY();
        int dY = (int) rect.getHeight();
        int dX = (int) Math.max(1, (1.0 / locScale));

        final int size = bases.length + padding;
        for (int p = 0; p < size; p++) {

            char c = p < padding ? '-' : (char) bases[p - padding];

            Color color = SequenceRenderer.nucleotideColors.get(c);
            if (color == null) {
                color = Color.black;
            }

            // If there is room for text draw the character, otherwise
            // just draw a rectangle to represent the
            int pX = (int) (pixelPosition + (p / locScale));

            // Don't draw out of clipping rect
            if (pX > rect.getMaxX()) {
                break;
            } else if (pX + dX < rect.getX()) {
                continue;
            }

            drawBase(g, color, c, pX, pY, dX, dY - (leaveMargin ? 2 : 0), false, null);
        }

        int leftX = pixelPosition + context.translateX;
        int rightX = leftX + rect.width;
        block.setPixelRange(leftX, rightX);

    }


    private Color getAlignmentColor(Alignment alignment, RenderOptions renderOptions) {

        // Set color used to draw the feature.  Highlight features that intersect the
        // center line.  Also restorePersistentState row "score" if alignment intersects center line


        Color color = alignment.getColor();
        if (color != null) return color;   // Color has been explicitly set

        Color c = DEFAULT_ALIGNMENT_COLOR;

        ColorOption colorOption = renderOptions.getColorOption();
        switch (colorOption) {
            case BISULFITE:
                // Just a simple forward/reverse strand color scheme that won't clash with the
                // methylation rectangles.
                c = (alignment.getFirstOfPairStrand() == Strand.POSITIVE) ? bisulfiteColorFw1 : bisulfiteColorRev1;

//                if (alignment.isNegativeStrand()) {
//                    c = (alignment.isSecondOfPair()) ? bisulfiteColorRev2 : bisulfiteColorRev1;
//                } else {
//                    c = (alignment.isSecondOfPair()) ? bisulfiteColorFw2 : bisulfiteColorFw1;
//                }
                break;
            case NOMESEQ:
                c = nomeseqColor;
                break;

            case UNEXPECTED_PAIR:
            case PAIR_ORIENTATION:
                c = getOrientationColor(alignment, getPEStats(alignment, renderOptions));
                if (colorOption == ColorOption.PAIR_ORIENTATION) {
                    break;
                }
            case INSERT_SIZE:
                boolean isPairedAlignment = alignment instanceof PairedAlignment;
                if ((alignment.isPaired() && alignment.getMate().isMapped()) || isPairedAlignment) {
                    boolean sameChr = isPairedAlignment || alignment.getMate().getChr().equals(alignment.getChr());
                    if (sameChr) {
                        int readDistance = Math.abs(alignment.getInferredInsertSize());
                        if (readDistance != 0) {

                            int minThreshold = renderOptions.getMinInsertSize();
                            int maxThreshold = renderOptions.getMaxInsertSize();
                            PEStats peStats = getPEStats(alignment, renderOptions);
                            if (renderOptions.isComputeIsizes() && peStats != null) {
                                minThreshold = peStats.getMinThreshold();
                                maxThreshold = peStats.getMaxThreshold();
                            }

                            if (readDistance < minThreshold) {
                                c = smallISizeColor;
                            } else if (readDistance > maxThreshold) {
                                c = largeISizeColor;
                            }
                        }
                        //return renderOptions.insertSizeColorScale.getColor(readDistance);
                    } else {
                        c = ChromosomeColors.getColor(alignment.getMate().getChr());
                        if (c == null) {
                            c = Color.black;
                        }
                    }
                }


                break;
            case READ_STRAND:
                if (alignment.isNegativeStrand()) {
                    c = negStrandColor;
                } else {
                    c = posStrandColor;
                }
                break;
            case FIRST_OF_PAIR_STRAND:
                final Strand fragmentStrand = alignment.getFirstOfPairStrand();
                if (fragmentStrand == Strand.NEGATIVE) {
                    c = negStrandColor;
                } else if (fragmentStrand == Strand.POSITIVE) {
                    c = posStrandColor;
                }
                break;
            case READ_GROUP:
                String rg = alignment.getReadGroup();
                if (rg != null) {
                    c = readGroupColors.get(rg);
                }
                break;
            case SAMPLE:
                String sample = alignment.getSample();
                if (sample != null) {
                    c = sampleColors.get(sample);
                }
                break;
            case LIBRARY:
                String library = alignment.getLibrary();
                if (library != null) {
                    c = sampleColors.get(library);
                }
                break;
            case TAG:
                final String tag = renderOptions.getColorByTag();
                if (tag != null) {
                    Object tagValue = alignment.getAttribute(tag);
                    if (tagValue != null) {

                        ColorTable ctable;
                        String ctableKey;

                        String groupByTag = renderOptions.getGroupByTag();
                        if (groupByTag == null) {
                            ctable = defaultTagColors;

                        } else {
                            Object g = alignment.getAttribute(groupByTag);
                            String group = g == null ? "" : g.toString();
                            ctableKey = groupByTag + ":" + group;
                            ctable = tagValueColors.get(ctableKey);
                            if (ctable == null) {

                                if (groupByTag.equals("HP")) {
                                    ctable = getTenXColorTable(group);
                                } else {
                                    ctable = defaultTagColors;
                                }

                                tagValueColors.put(group, ctable);
                            }
                        }

                        c = ctable.get(tagValue.toString());

                    }
                }
                break;
       //     case LINK_STRAND:
       //         if (alignment instanceof LinkedAlignment && ((LinkedAlignment) alignment).getStrand() == Strand.NONE) {
       //             c = LL_COLOR;
       //         }
       //         break;


            default:
//                if (renderOptions.shadeCenters && center >= alignment.getStart() && center <= alignment.getEnd()) {
//                    if (locScale < 1) {
//                        c = grey2;
//                    }
//                }

        }
        if (c == null) c = DEFAULT_ALIGNMENT_COLOR;

        if (alignment.getMappingQuality() == 0 && renderOptions.flagZeroQualityAlignments) {
            // Maping Q = 0
            float alpha = 0.15f;
            // Assuming white background TODO -- this should probably be passed in
            return ColorUtilities.getCompositeColor(Color.white, c, alpha);
        }

        return c;

    }

    private ColorTable getTenXColorTable(String group) {
        ColorTable ctable;
        if (group.equals("1")) {
            ctable = tenXColorTable1;

        } else if (group.equals("2")) {
            ctable = tenXColorTable2;
        } else {
            ctable = tenXColorTable3;
        }
        return ctable;
    }

    public static PEStats getPEStats(Alignment alignment, RenderOptions renderOptions) {
        String lb = alignment.getLibrary();
        if (lb == null) lb = "null";
        PEStats peStats = null;
        if (renderOptions.peStats != null) {
            peStats = renderOptions.peStats.get(lb);
        }
        return peStats;
    }

    /**
     * Determine if alignment insert size is outside max or min
     * range for outliers.
     *
     * @param alignment
     * @param renderOptions
     * @return -1 if unknown (stats not computed), 0 if not
     * an outlier, 1 if outlier
     */
    private int getOutlierStatus(Alignment alignment, RenderOptions renderOptions) {
        PEStats peStats = getPEStats(alignment, renderOptions);
        if (renderOptions.isComputeIsizes() && peStats != null) {
            int minThreshold = peStats.getMinOutlierInsertSize();
            int maxThreshold = peStats.getMaxOutlierInsertSize();
            int dist = Math.abs(alignment.getInferredInsertSize());
            if (dist >= minThreshold || dist <= maxThreshold) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return -1;
        }
    }

    /**
     * Returns -1 if alignment distance is less than minimum,
     * 0 if within bounds, and +1 if above maximum.
     *
     * @param alignment
     * @return
     */
    private int compareToBounds(Alignment alignment, RenderOptions renderOptions) {
        int minThreshold = renderOptions.getMinInsertSize();
        int maxThreshold = renderOptions.getMaxInsertSize();
        PEStats peStats = getPEStats(alignment, renderOptions);
        if (renderOptions.isComputeIsizes() && peStats != null) {
            minThreshold = peStats.getMinThreshold();
            maxThreshold = peStats.getMaxThreshold();
        }

        int dist = Math.abs(alignment.getInferredInsertSize());

        if (dist < minThreshold) return -1;
        if (dist > maxThreshold) return +1;
        return 0;
    }

    /**
     * Assuming we want to color a pair of alignments based on their distance,
     * this returns an appropriate color
     *
     * @param pair
     * @return
     */
    private static Color getColorRelDistance(PairedAlignment pair) {
        if (pair.secondAlignment == null) {
            return DEFAULT_ALIGNMENT_COLOR;
        }

        int dist = Math.abs(pair.getInferredInsertSize());
        double logDist = Math.log(dist);
        Color minColor = smallISizeColor;
        Color maxColor = largeISizeColor;
        ContinuousColorScale colorScale = new ContinuousColorScale(0, 20, minColor, maxColor);
        return colorScale.getColor((float) logDist);
    }


    /**
     * Returns a color to flag unexpected pair orientations.  Expected orientations (e.g. FR for Illumina) get the
     * neutral grey color
     *
     * @param alignment
     * @param peStats
     * @return
     */
    private Color getOrientationColor(Alignment alignment, PEStats peStats) {
        AlignmentTrack.OrientationType type = getOrientationType(alignment, peStats);
        Color c = typeToColorMap.get(type);
        return c == null ? DEFAULT_ALIGNMENT_COLOR : c;

    }

    static AlignmentTrack.OrientationType getOrientationType(Alignment alignment, PEStats peStats) {
        AlignmentTrack.OrientationType type = null;
        if (alignment.isPaired()) { // && !alignment.isProperPair()) {
            final String pairOrientation = alignment.getPairOrientation();
            if (peStats != null) {
                PEStats.Orientation libraryOrientation = peStats.getOrientation();
                switch (libraryOrientation) {
                    case FR:
                        //if (!alignment.isSmallInsert()) {
                        // if the isize < read length the reads might overlap, invalidating this test
                        type = frOrientationTypes.get(pairOrientation);
                        //}
                        break;
                    case RF:
                        type = rfOrientationTypes.get(pairOrientation);
                        break;
                    case F1F2:
                        type = f1f2OrientationTypes.get(pairOrientation);
                        break;
                    case F2F1:
                        type = f2f1OrientationTypes.get(pairOrientation);
                        break;
                }

            } else {
                // No peStats for this library, just guess
                if (alignment.getAttribute("CS") != null) {
                    type = f2f1OrientationTypes.get(pairOrientation);
                } else {
                    type = frOrientationTypes.get(pairOrientation);
                }
            }
        }

        return type;
    }

}
