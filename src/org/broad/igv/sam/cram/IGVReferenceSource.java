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

package org.broad.igv.sam.cram;


import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import htsjdk.samtools.cram.ref.ReferenceSource;
import org.broad.igv.feature.Chromosome;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.util.ObjectCache;

/**
 * Provide a reference sequence for CRAM decompression.  Note the rule for MD5 calculation.
 *
 * M5 (sequence MD5 checksum) field of @SQ sequence record in the BAM header is required and UR (URI
 for the sequence fasta optionally gzipped file) field is strongly advised. The rule for calculating MD5 is
 to remove any non-base symbols (like \n, sequence name or length and spaces) and upper case the rest.
 */

public class IGVReferenceSource implements CRAMReferenceSource {

    ObjectCache<String, byte[]> cachedSequences = new ObjectCache<String, byte[]>(2);

    @Override
    public synchronized byte[] getReferenceBases(SAMSequenceRecord record, boolean tryNameVariants) {

        final String name = record.getSequenceName();

        String igvName = GenomeManager.getInstance().getCurrentGenome().getCanonicalChrName(name);

        byte[] bases = cachedSequences.get(igvName);

        if (bases == null) {

            Chromosome chromosome = GenomeManager.getInstance().getCurrentGenome().getChromosome(igvName);

            bases = GenomeManager.getInstance().getCurrentGenome().getSequence(igvName, 0, chromosome.getLength());

            // CRAM spec requires upper case
            for(int i=0; i<bases.length; i++) {
                if(bases[i] >= 97) bases[i] -= 32;
            }

            cachedSequences.put(igvName, bases);
        }

        return bases;



    }
}
