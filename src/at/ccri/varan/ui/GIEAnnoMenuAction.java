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
package at.ccri.varan.ui;

import java.awt.event.ActionEvent;

import org.apache.log4j.Logger;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.action.MenuAction;
import org.broad.igv.ui.util.UIUtilities;

import at.ccri.varan.GIE;

/**
 * @author Niko
 */
public class GIEAnnoMenuAction extends MenuAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    static Logger log = Logger.getLogger(GIEAnnoMenuAction.class);
    IGV igv;

    public GIEAnnoMenuAction(String label, IGV igv) {
	super(label, null);
	this.igv = igv;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

	UIUtilities.invokeOnEventThread(new Runnable() {

	    public void run() {

		if (GIE.getInstance() != null) {

		    // show main dialog
		    GIEAnnoDialog diag = GIEAnnoDialog.getInstance(IGV.getMainFrame());
		    if (diag.isVisible()) {
			diag.pack();
		    } else {
			diag.setVisible(true);
			diag.pack();
		    }

		}

	    }

	});
    }
}
