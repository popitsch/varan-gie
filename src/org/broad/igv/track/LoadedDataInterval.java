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

package org.broad.igv.track;

import org.broad.igv.feature.Locus;
import org.broad.igv.feature.LocusScore;
import org.broad.igv.ui.panel.ReferenceFrame;

import java.util.List;

/**
 * @author jrobinso
 * @date Sep 28, 2010
 */
public class LoadedDataInterval<T> {


    public Locus range;
    private T scores;
    int zoom = -1;

    public LoadedDataInterval(String chr, int start, int end, T scores) {

        range = new Locus(chr, start, end);
        this.scores = scores;
    }

    public LoadedDataInterval(String chr, int start, int end, int zoom, T scores) {

        range = new Locus(chr, start, end);
        this.zoom = zoom;
        this.scores = scores;
    }
    public boolean contains(String chr, int start, int end) {
        return range.contains(chr, start, end);
    }

    public boolean contains(String chr, int start, int end, int zoom) {
        return (this.zoom == -1 || this.zoom == zoom) && range.contains(chr, start, end);
    }

    public boolean contains(ReferenceFrame frame) {
        String chr = frame.getChrName();
        int start = (int) frame.getOrigin();
        int end = (int) frame.getEnd();
        int zoom = frame.getZoom();
        return this.contains(chr, start, end, zoom);
    }

    public T getFeatures() {
        return scores;
    }
}
