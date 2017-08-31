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

import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.track.TrackType;

import java.awt.*;

/**
 * @author jrobinso
 */
public class UIConstants {

    private static Logger log = Logger.getLogger(UIConstants.class);

    final public static String APPLICATION_NAME = "IGV";
    final public static int groupGap = 10;
    final public static Dimension preferredSize = new Dimension(1000, 750);

    // To support mutation track overlay.  Generalize later.  Cancer specific option.
    public static TrackType overlayTrackType = TrackType.MUTATION;

    private static int doubleClickInterval = -1;

    // Menu tooltips
    static final public String LOAD_TRACKS_TOOLTIP = "Load tracks or sample information";
    static final public String LOAD_SERVER_DATA_TOOLTIP = "Load tracks or sample information from a server";
    static final public String SAVE_IMAGE_TOOLTIP = "Capture and save an image";
    static final public String NEW_SESSION_TOOLTIP = "Create a new session";
    static final public String SAVE_SESSION_TOOLTIP = "Save the current session";
    static final public String RESTORE_SESSION_TOOLTIP = "Reload the session";
    static final public String EXIT_TOOLTIP = "Exit the application";
    static final public String IMPORT_GENOME_TOOLTIP = "Create a .genome file";
    static final public String REMOVE_IMPORTED_GENOME_TOOLTIP = "Removes user-defined genomes from the drop-down list";
    static final public String CLEAR_GENOME_CACHE_TOOLTIP = "Clears locally cached versions of IGV hosted genomes";
    static final public String SELECT_DISPLAYABLE_ATTRIBUTES_TOOLTIP =
            "Customize attribute display to show only checked attributes";

    static final public String SORT_TRACKS_TOOLTIP = "Sort tracks by attribute value";
    static final public String GROUP_TRACKS_TOOLTIP = "Group tracks by attribute value";
    static final public String FILTER_TRACKS_TOOLTIP = "Filter tracks by attribute value";
    static final public String SET_DEFAULT_TRACK_HEIGHT_TOOLTIP = "Set the height for all tracks";
    static final public String FIT_DATA_TO_WINDOW_TOOLTIP =
            "Resize track heights to make best use of vertical space without scrolling";

    static final public String EXPORT_REGION_TOOLTIP = "Save all defined regions to a file";
    static final public String IMPORT_REGION_TOOLTIP = "Load regions from a file";

    static final public String HELP_TOOLTIP = "Open web help page";
    static final public String ABOUT_TOOLTIP = "Display application information";

    static final public String RESET_FACTORY_TOOLTIP = "Restores all user preferences to their default settings.";
    public static final String REGION_NAVIGATOR_TOOLTIP = "Navigate regions";


    static final public String CLICK_ITEM_TO_EDIT_TOOLTIP = "Click this item bring up its editor";
    static final public String CHANGE_GENOME_TOOLTIP = "Switch the current genome";

    static final public String PREFERENCE_TOOLTIP = "Set user specific preferences";
    static final public String SHOW_HEATMAP_LEGEND_TOOLTIP = "Edit color legends and scales";

    public static Font boldFont = FontManager.getFont(Font.BOLD, 12);


    final public static String OVERWRITE_SESSION_MESSAGE =
            "<html>Do you want to unload all current data?";
    final public static String CANNOT_ACCESS_SERVER_GENOME_LIST = "The Genome server is currently inaccessible.";
    final public static int NUMBER_OF_RECENT_SESSIONS_TO_LIST = 3;
    final public static String DEFAULT_SESSION_FILE = "igv_session" + Globals.SESSION_FILE_EXTENSION;
    static final public String SERVER_BASE_URL = "http://www.broadinstitute.org/";
    static final public Color LIGHT_YELLOW = new Color(255, 244, 201);

    final public static Color LIGHT_GREY = new Color(238, 239, 240);

    final public static Color TRACK_BORDER_GRAY = new Color(200, 200, 210);

    public static Color NO_DATA_COLOR = new Color(200, 200, 200, 150);
    static final public String REMOVE_GENOME_LIST_MENU_ITEM = "Remove Imported Genomes...";
    static final public String GENOME_LIST_SEPARATOR = "--SEPARATOR--";
    static final public int DEFAULT_DOUBLE_CLICK_INTERVAL = 400;


    public static int getDoubleClickInterval() {

        if (doubleClickInterval < 0) {

            Number obj = (Number) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
            if (obj != null) {
                doubleClickInterval = obj.intValue();
            } else {
                doubleClickInterval = DEFAULT_DOUBLE_CLICK_INTERVAL;
            }

        }
        return doubleClickInterval;
    }

}
