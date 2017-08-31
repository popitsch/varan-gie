/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 University of California San Diego
 * Author: Jim Robinson
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

package org.broad.igv.feature.basepair;

import htsjdk.tribble.Feature;

import java.awt.*;

/**
 * Created by jrobinson on 3/1/16.
 */
public class BasePairFeature implements Feature{

    String chr;
    int startLeft;
    int startRight;
    int endLeft;
    int endRight;
    Color color;

    public BasePairFeature(String chr,  int startLeft, int startRight, int endLeft, int endRight,  Color color) {
        this.chr = chr;
        this.color = color;
        this.endLeft = endLeft;
        this.endRight = endRight;
        this.startLeft = startLeft;
        this.startRight = startRight;
    }

    @Override
    public String getChr() {
        return chr;
    }

    @Override
    public String getContig() {
        return chr;
    }

    @Override
    public int getStart() {
        return startLeft;
    }

    @Override
    public int getEnd() {
        return endRight;
    }

    public int getStartLeft() { return startLeft; }

    public int getStartRight() { return startRight; }

    public int getEndLeft() { return endLeft; }

    public int getEndRight() { return endRight; }

    public Color getColor() { return color; }


    public String toStringNoColor() {
        return getChr() + "\t" + startLeft + "\t" + startRight + "\t" + endLeft + "\t" + endRight;
    }

    public String toString() {
        return toStringNoColor() + "\t" + color;
    }
}
