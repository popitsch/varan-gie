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
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.broad.igv.ui.IGV;
import org.broad.igv.ui.action.MenuAction;
import org.broad.igv.util.BrowserLauncher;

import at.ccri.varan.GIE;

/**
 * GIE about dialog.
 * 
 * @author Niko
 */
public class GIEAboutMenuAction extends MenuAction {
    private static final long serialVersionUID = 1L;

    IGV mainFrame;
    final static String GIE_ABOUT_URL = "http://science.ccri.at/";
    final static String GIE_HELP_URL = "https://github.com/popitsch/varan-gie/blob/master/README.GIE.md";

    final String[] keys = new String[] {

	    "CTRL-ALT-A", "Add region (hold CTRL to merge new region with all overlapping)",

	    "CTRL-ALT-C", "Clip region",

	    "CTRL-ALT-M", "Merge regions",

	    "CTRL-ALT-D", "Delete visible region",

	    "CTRL-ALT-R", "Save and update BED track",

	    "CTRL-F", "Next (downstream) region",

	    "CTRL-B", "Previous (upstream) region",

	    "CTRL-Z", "Undo last action",

	    "CTRL-C", "Copy region coordinates",

	    "CTRL-H", "Show/hide refernce lines",

	    "CTRL-#", "Switch to layer (#=1..9)" };

    public GIEAboutMenuAction(String label, IGV mainFrame) {
	super(label, null);
	this.mainFrame = mainFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

	ImageIcon icon = new ImageIcon(getClass().getResource("/images/ccri.gif"));
	String text = "<b>Variant Annotation Genomic Interval Editor (VARAN-GIE)</b><br/>Version " + GIE.VERSION
		+ "<br/><br/>VARAN-GIE was developed at the " + "<a href=\"" + GIE_ABOUT_URL
		+ "\">Children's Cancer Research institute</a><br/>" + "by " + GIE.AUTHORS + "<br/>"
		+ "<hr/><table style=\"font-size:8px;\"><tr><th>Key Shortcut</th><th>Mapping</th></tr>";
	for (int i = 0; i < keys.length / 2; i++)
	    text += "<tr><td>" + keys[i * 2] + "</td><td>" + keys[i * 2 + 1] + "</td></tr>";
	text += "</table><hr/>";
	text += "<br/><b>Click <a href=\"" + GIE_HELP_URL + "\">here to access more online help</a></b>.<br/><br/>";

	JEditorPane ep = new JEditorPane("text/html", "<html><body>" + text + "</body></html>");

	ep.addHyperlinkListener(new HyperlinkListener() {
	    @Override
	    public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
		    try {
			BrowserLauncher.openURL(e.getURL().toString());
		    } catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		    }
	    }
	});
	ep.setEditable(false);

	JOptionPane.showMessageDialog(null, ep, "About GIE", JOptionPane.INFORMATION_MESSAGE, icon);

    }

}
