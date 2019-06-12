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

package org.broad.igv.ui;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.broad.igv.batch.CommandListener;
import org.broad.igv.dev.db.DBManager;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.track.Track;

import at.ccri.varan.GIE;

/**
 * This thread is registered upon startup and will get executed upon exit.
 */
public class ShutdownThread extends Thread {

    private static Logger log = Logger.getLogger(ShutdownThread.class);
    private static long oneDayMS = 24 * 60 * 60 * 1000;

    public static void runS() {
	log.info("Shutting down");
	DBManager.shutdown();
	CommandListener.halt();
	// close GIE - will autosave current session and has to be done before tracks are disposed
	GIE.getInstance().close();
	if (IGV.hasInstance()) {
	    IGV.getInstance().saveStateForExit();
	    for (Track t : IGV.getInstance().getAllTracks()) {
		t.dispose();
	    }
	}
    }

    @Override
    public void run() {
	runS();
    }

    private static void writeRegionsOfInterestFile(File roiFile) {

	if (roiFile == null) {
	    log.info("A blank Region of Interest export file was supplied!");
	    return;
	}
	try {
	    Collection<RegionOfInterest> regions = IGV.getInstance().getSession().getAllRegionsOfInterest();

	    if (regions == null || regions.isEmpty()) {
		return;
	    }

	    // Create export file
	    roiFile.createNewFile();
	    PrintWriter writer = null;
	    try {
		writer = new PrintWriter(roiFile);
		for (RegionOfInterest regionOfInterest : regions) {
		    Integer regionStart = regionOfInterest.getStart();
		    if (regionStart == null) {
			// skip - null starts are bad regions of interest
			continue;
		    }
		    Integer regionEnd = regionOfInterest.getEnd();
		    if (regionEnd == null) {
			regionEnd = regionStart;
		    }

		    // Write info in BED format
		    writer.print(regionOfInterest.getChr());
		    writer.print("\t");
		    writer.print(regionStart);
		    writer.print("\t");
		    writer.print(regionEnd);

		    if (regionOfInterest.getDescription() != null) {
			writer.print("\t");
			writer.println(regionOfInterest.getDescription());
		    } else {
			writer.println();
		    }
		}
	    } finally {

		if (writer != null) {
		    writer.close();
		}
	    }
	} catch (Exception e) {
	    log.error("Failed to write Region of Interest export file!", e);
	}
    }
}
