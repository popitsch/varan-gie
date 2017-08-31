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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.data;

/**
 * Class is public to permit unit testing
 */
public class DataTile {

    private int[] startLocations;
    private int[] endLocations;
    private float[] values;
    private String[] featureNames;

    public DataTile(int[] startLocations, int[] endLocations, float[] values, String[] features) {
        this.startLocations = startLocations;
        this.endLocations = endLocations;
        this.values = values;
        this.featureNames = features;
    }

    public boolean isEmpty() {
        return startLocations == null || startLocations.length == 0;
    }

    public int[] getStartLocations() {
        return startLocations;
    }

    public int[] getEndLocations() {
        return endLocations;
    }

    public float[] getValues() {
        return values;
    }

    /**
     * @return the featureNames
     */
    public String[] getFeatureNames() {
        return featureNames;
    }
}
