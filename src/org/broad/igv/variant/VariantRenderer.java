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

package org.broad.igv.variant;

import org.apache.log4j.Logger;
import org.broad.igv.prefs.IGVPreferences;
import org.broad.igv.prefs.PreferencesManager;
import org.broad.igv.track.RenderContext;
import org.broad.igv.track.Track;
import org.broad.igv.ui.FontManager;
import org.broad.igv.ui.color.ColorUtilities;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static org.broad.igv.prefs.Constants.*;

/**
 * @author Jesse Whitworth
 * @api
 * @since Jul 16, 2010
 */
public class VariantRenderer { //extends FeatureRenderer {

    private static Logger log = Logger.getLogger(VariantRenderer.class);

    private static final int BOTTOM_MARGIN = 0;
    private static final int TOP_MARGIN = 3;
    private static float alphaValue = 0.2f;
    private static final Color colorAlleleRef = Color.gray;
    private static Color colorAlleleRefAlpha = ColorUtilities.getCompositeColor(colorAlleleRef, alphaValue);

    static Map<Character, Color> nucleotideColors = new HashMap<Character, Color>();

    static {
        nucleotideColors.put('A', Color.GREEN);
        nucleotideColors.put('a', Color.GREEN);
        nucleotideColors.put('C', Color.BLUE);
        nucleotideColors.put('c', Color.BLUE);
        nucleotideColors.put('T', Color.RED);
        nucleotideColors.put('t', Color.RED);
        nucleotideColors.put('G', new Color(242, 182, 65));
        nucleotideColors.put('g', new Color(242, 182, 65));
        nucleotideColors.put('N', colorAlleleRef);
        nucleotideColors.put('n', colorAlleleRef);
        nucleotideColors.put('.', colorAlleleRef);
        nucleotideColors.put(null, Color.BLACK);
    }


    private VariantTrack track;

    private Color colorAlleleBandVar;
    private Color colorAlleleBandVarAlpha;
    private Color colorAlleleBandRef;
    private Color colorAlleleBandRefAlpha;
    private Color colorHomRef;
    private Color colorHomRefAlpha;
    private Color colorHomVar;
    private Color colorHomVarAlpha;
    private Color colorHet;
    private Color colorHetAlpha;
    private Color colorNoCall;
    private Color colorNoCallAlpha;

    public VariantRenderer(VariantTrack track) {
        this.track = track;
        updateColors();
    }

    /**
     * Check colors against user prefs and update if neccessary
     */
    private void updateColors() {

        final IGVPreferences prefMgr = PreferencesManager.getPreferences();

        if (!prefMgr.getAsColor(AF_VAR_COLOR).equals(colorAlleleBandVar)) {
            colorAlleleBandVar = prefMgr.getAsColor(AF_VAR_COLOR);
            colorAlleleBandVarAlpha = ColorUtilities.getCompositeColor(colorAlleleBandVar, alphaValue);

        }
        if (!prefMgr.getAsColor(AF_REF_COLOR).equals(colorAlleleBandRef)) {
            colorAlleleBandRef = prefMgr.getAsColor(AF_REF_COLOR);
            colorAlleleBandRefAlpha = ColorUtilities.getCompositeColor(colorAlleleBandRef, alphaValue);

        }
        if (!prefMgr.getAsColor(HOMREF_COLOR).equals(colorHomRef)) {
            colorHomRef = prefMgr.getAsColor(HOMREF_COLOR);
            colorHomRefAlpha = ColorUtilities.getCompositeColor(colorHomRef, alphaValue);
        }
        if (!prefMgr.getAsColor(HOMVAR_COLOR).equals(colorHomVar)) {
            colorHomVar = prefMgr.getAsColor(HOMVAR_COLOR);
            colorHomVarAlpha = ColorUtilities.getCompositeColor(colorHomVar, alphaValue);
        }
        if (!prefMgr.getAsColor(HETVAR_COLOR).equals(colorHet)) {
            colorHet = prefMgr.getAsColor(HETVAR_COLOR);
            colorHetAlpha = ColorUtilities.getCompositeColor(colorHet, alphaValue);
        }
        if (!prefMgr.getAsColor(NOCALL_COLOR).equals(colorNoCall)) {
            colorNoCall = prefMgr.getAsColor(NOCALL_COLOR);
            colorNoCallAlpha = ColorUtilities.getCompositeColor(colorNoCall, alphaValue);
        }

    }

    /**
     * Render the site track (the top, summary view of the site).
     *
     * @param variant
     * @param bandRectangle
     * @param pixelX        Location of the variant in pixels
     * @param xWidth        Width of the variant in pixels
     * @param context
     * @api
     */
    public void renderSiteBand(Variant variant,
                               Rectangle bandRectangle,
                               int pixelX, int xWidth,
                               RenderContext context) {

        updateColors();

        final boolean useAlpha = variant.isFiltered();
        final Color alleleColor;
        final Color refColor;
        double percent = 0;
        if (track.getColorMode() == VariantTrack.ColorMode.METHYLATION_RATE) {
            alleleColor = this.convertMethylationRateToColor((float) variant.getMethlationRate() / 100);
            percent = variant.getCoveredSampleFraction();
            refColor = useAlpha ? colorAlleleRefAlpha : colorAlleleRef;   // Gray
        } else {
            alleleColor = useAlpha ? colorAlleleBandVarAlpha : colorAlleleBandVar; // Red

            double af = track.getSiteColorMode() == VariantTrack.ColorMode.ALLELE_FREQUENCY ?
                    variant.getAlternateAlleleFrequency() :
                    variant.getAlleleFraction();
            percent = Math.min(1, af);

            if (percent <= 0) {
                percent = 0;
                refColor = useAlpha ? colorAlleleRefAlpha : colorAlleleRef;   // Gray
            } else {
                refColor = useAlpha ? colorAlleleBandRefAlpha : colorAlleleBandRef;                      // Blue
            }

        }

        final int bottomY = calculateBottomYSiteBand(bandRectangle);
        final int barHeight = calculateBarHeightSiteBand(bandRectangle);

        final int alleleBarHeight = (int) (percent * barHeight);
        final int remainderHeight = barHeight - alleleBarHeight;

        if (remainderHeight > 0) {
            Graphics2D g = context.getGraphic2DForColor(refColor);
            g.fillRect(pixelX, bottomY - alleleBarHeight - remainderHeight, xWidth, remainderHeight);
        }

        if (alleleBarHeight > 0) {
            Graphics2D g = context.getGraphic2DForColor(alleleColor);
            g.fillRect(pixelX, bottomY - alleleBarHeight, xWidth, alleleBarHeight);
        }
    }

    protected int calculateBottomYSiteBand(Rectangle bandRectangle) {
        return bandRectangle.y + bandRectangle.height - BOTTOM_MARGIN;
    }

    protected int calculateBarHeightSiteBand(Rectangle bandRectangle) {
        return bandRectangle.height - TOP_MARGIN - BOTTOM_MARGIN;
    }


    public void renderGenotypeBandSNP(Variant variant, RenderContext context, Rectangle bandRectangle, int pX0, int dX,
                                      String sampleName, VariantTrack.ColorMode coloring, boolean hideFiltered) {

        updateColors();

        int pY = (int) bandRectangle.getY();
        int dY = (int) bandRectangle.getHeight();

        int tOffset = 6;
        int bOffset = 8;
        Graphics2D g = (Graphics2D) context.getGraphics2D("GENOTYPE");

        if (dX >= 10) {
            if (dY > 24) {
                Font f = FontManager.getFont(Font.BOLD, Math.min(dX, 12));
                g.setFont(f);
            } else if (dY > 18) {
                Font f = FontManager.getFont(Font.BOLD, Math.min(dX, 8));
                tOffset = 4;
                bOffset = 5;
                g.setFont(f);
            }
        }

        boolean isFiltered = variant.isFiltered() && hideFiltered;

        Genotype genotype = variant.getGenotype(sampleName);

        if (genotype == null) {
            log.error("Genotype not found for sample " + sampleName);
        } else {
            Color b1Color = Color.gray;
            Color b2Color = Color.gray;
            char b1 = ' ';
            char b2 = ' ';
            //Assign proper coloring
            switch (coloring) {
                case GENOTYPE:

                    b1Color = getGenotypeColor(genotype, isFiltered);
                    b2Color = b1Color;
                    break;

                case METHYLATION_RATE:

                    final double goodBaseCount = genotype.getAttributeAsDouble("GB");
                    b1Color = colorNoCall;
                    b2Color = b1Color;
                    final double value = genotype.getAttributeAsDouble("MR");
                    if (!Double.isNaN(goodBaseCount) && !Double.isNaN(value)) {
                        if (goodBaseCount < VariantTrack.METHYLATION_MIN_BASE_COUNT || Double.isNaN(value)) {
                            b1Color = colorNoCall;
                            b2Color = b1Color;
                        } else {
                            float mr = (float) value;
                            mr /= 100f;
                            b1Color = convertMethylationRateToColor(mr);
                            b2Color = b1Color;
                        }
                    } else {
                        log.error("GB and MR fields must be defined for all records in a VCF methylation file.");
                    }
                    break;

                default:
                    b1Color = colorNoCall;
                    b2Color = b1Color;
            }


            int y0 = track.getDisplayMode() == Track.DisplayMode.EXPANDED ? pY + 1 : pY;
            int h = Math.max(1, track.getDisplayMode() == Track.DisplayMode.EXPANDED ? dY - 2 : dY);

            if (coloring == VariantTrack.ColorMode.GENOTYPE) {

                g.setColor(b1Color);
                g.fillRect(pX0, y0, dX, h);
            } else {
                // Color by allele
                g.setColor(b1Color);
                g.fillRect(pX0, y0, (dX / 2), h);
                g.setColor(b2Color);
                g.fillRect(pX0 + (dX / 2), y0, (dX / 2), h);

            }


            if ((dX >= 10) && (dY >= 18)) {
                if (b1Color == Color.blue) {
                    g.setColor(Color.white);
                } else {
                    g.setColor(Color.black);
                }
                drawCenteredText(g, new char[]{b1}, pX0, pY - tOffset, dX, dY);
                drawCenteredText(g, new char[]{b2}, pX0, pY + (dY / 2) - bOffset, dX, dY);
            }
        }
    }

    private Color convertMethylationRateToColor(float mr) {
        Color color;
        /*
        if (mr >= .25) {
            return Color.getHSBColor((mr - .25f) * (1f / 0.75f), 1, 1);
        } else {
            // use a light grey between 0 and 0.25 brightness to indicate moderate methylation at the site.
            return new Color(1f - mr, 1f - mr, 1f - mr);
        }
          */
        float v = 1.5f + (mr / 2); //add one to have a floor. getHSBColor removes the floor to yield a fraction betweeen 0 and 1.
        return Color.getHSBColor(v, .75f, 1);
    }

    public char getFirstBase(Allele allele) {
        byte[] bases = allele.getBases();
        if (bases.length > 0) {
            return (char) bases[0];
        } else {
            return '.';
        }
    }

    public Color getGenotypeColor(Genotype genotype, boolean isFiltered) {
        if (genotype.isNoCall()) {
            return isFiltered ? colorNoCallAlpha : colorNoCall;
        } else if (genotype.isHomRef()) {
            return isFiltered ? colorHomRefAlpha : colorHomRef;
        } else if (genotype.isHomVar()) {
            return isFiltered ? colorHomVarAlpha : colorHomVar;
        } else if (genotype.isHet()) {
            return isFiltered ? colorHetAlpha : colorHet;
        }
        return Color.white;
    }

    private void drawCenteredText(Graphics2D g, char[] chars, int x, int y,
                                  int w, int h) {

        // Get measures needed to center the message
        FontMetrics fm = g.getFontMetrics();

        // How many pixels wide is the string
        int msg_width = fm.charsWidth(chars, 0, 1);

        // How far above the baseline can the font go?
        int ascent = fm.getMaxAscent();

        // How far below the baseline?
        int descent = fm.getMaxDescent();

        // Use the string width to find the starting point
        int msgX = x + w / 2 - msg_width / 2;

        // Use the vertical height of this font to find
        // the vertical starting coordinate
        int msgY = y + h / 2 - descent / 2 + ascent / 2;

        g.drawChars(chars, 0, 1, msgX, msgY);

    }

}
