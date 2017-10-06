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
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.action.MenuAction;
import org.broad.igv.ui.util.UIUtilities;

import at.ccri.varan.GIE;

/**
 * @author Niko
 */
public class GIECreateBackupMenuAction extends MenuAction {

    private static final long serialVersionUID = 1L;

    private transient SimpleDateFormat backupSF = new SimpleDateFormat("'GIEManualBackup'_yyyy.MM.dd");

    static Logger log = Logger.getLogger(GIECreateBackupMenuAction.class);
    IGV igv;

    public GIECreateBackupMenuAction(String label, IGV igv) {
	super(label, null);
	this.igv = igv;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

	UIUtilities.invokeOnEventThread(new Runnable() {

	    public void run() {

		if (GIE.getInstance() != null) {
		    // ask for parent directory
		    JFileChooser chooser = new JFileChooser();
		    chooser.setCurrentDirectory(GIE.getInstance().getHomeDir());
		    chooser.setDialogTitle("Select Backup Parent directory");
		    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		    chooser.setAcceptAllFileFilterUsed(false);
		    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			File parentDir = chooser.getSelectedFile();
			String name = JOptionPane.showInputDialog(null, "BACKUP name: ", backupSF.format(new Date()));
			if (name != null && !name.trim().equals("")) {
			    File backupDir = new File(parentDir, name);
			    if (backupDir.exists())
				JOptionPane.showMessageDialog(null, "Backup directory already exists.", "Error",
					JOptionPane.ERROR_MESSAGE);
			    else {
				GIE.getInstance().createManualBackup(backupDir);
			    }
			}
		    }
		}

	    }

	});
    }
}
