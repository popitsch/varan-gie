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

package org.broad.igv.plugin;

import org.broad.igv.dev.api.IGVPlugin;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.util.UIUtilities;

import javax.swing.*;

/**
 * This is probably a case of over-design.
 * This plugin exists solely to structure the tools menu to our liking, it
 * just adds a separator to the menu.
 *
 * @author jacob
 * @date 2013-Apr-12
 */
public class AddMenuSeparator implements IGVPlugin {
    @Override
    public void init() {
        UIUtilities.invokeAndWaitOnEventThread(() -> IGV.getInstance().addOtherToolMenu(new JSeparator()));
    }
}
