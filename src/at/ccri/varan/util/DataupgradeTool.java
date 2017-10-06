package at.ccri.varan.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;


public class DataupgradeTool {

    /**
     * Upgrade wrt. exon boundary bug
     * @param inDir
     * @param outDir
     * @throws IOException
     */
    public static void updgradeFromAlpha3ToAlpha4(File inDir, File outDir) throws IOException {
	if (!new File(inDir, "gie.conf.json").exists())
	    throw new IOException("No conf file found - is this a GIE dir?");
	// mkdir
	outDir.mkdirs();
	for ( File f : inDir.listFiles() ) {
	    if ( ! f.isFile()) continue;
	    File outf =  new File(outDir, f.getName());
	    if ( f.getName().endsWith(".bed")) {
		TabIterator ti = new TabIterator(f, null);
		PrintWriter out = new PrintWriter(outf);
		boolean migrated = false;
		while ( ti.hasNext()) {
		    String[] t = ti.next();
		    if ( t.length > 9 && !t[9].equals("1")) {
			String[] x = new String[t.length+3];
			for ( int i = 0; i < 9; i++)
			    x[i]=t[i];
			x[9]="1"; // blockCount
			x[10]=(Integer.parseInt( t[2])-Integer.parseInt( t[1]))+"";
			x[11]="0";
			for ( int i = 12; i < t.length+3; i++ )
			    x[i] = t[i-3];
			t=x;
			migrated = true;
		    }
		    
		    out.println(StringUtils.concat(t, "\t"));
		}
		out.close();
		if ( migrated) 
		System.out.println("Migrated file " + f);
	    } else
		FileUtils.copyFile(f,outf);
	}
	System.err.println("Done migrating");
    }
    
    public static void main(String[] args) throws IOException {
	
	
	updgradeFromAlpha3ToAlpha4(new File("C:/Users/niko.popitsch/igv/gie"), 
		new File("C:/Users/niko.popitsch/igv/gie/upgrade"));
	

    }

}
