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

package org.broad.igv.event;

/**
 * @author Jim Robinson
 * @date 12/2/11
 */
public class AlignmentTrackEvent {

    public enum Type {ALLELE_THRESHOLD, RELOAD, REFRESH}

    private Object source;
    private Type type;
    private boolean booleanValue;

    public AlignmentTrackEvent(Object source, Type type) {
        this.source = source;
        this.type = type;
    }

    public AlignmentTrackEvent(Object source, Type type, boolean booleanValue) {
        this.source = source;
        this.type = type;
        this.booleanValue = booleanValue;
    }

    public Type getType() {
        return type;
    }

    public boolean getBooleanValue() {
        return booleanValue;
    }
}
