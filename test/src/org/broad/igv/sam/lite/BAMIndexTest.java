package org.broad.igv.sam.lite;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by jrobinso on 3/10/17.
 */
public class BAMIndexTest {
    @Test
    public void chunksForRange() throws Exception {
        int refID = 14;
        int beg = 24375199;
        int end = 24378544;
        String url = "https://data.broadinstitute.org/igvdata/test/data/bam/gstt1_sample.bam.bai";

        BAMIndex bamIndex = BAMIndex.loadIndex(url, null);

        List<BAMIndex.Chunk> chunks = bamIndex.chunksForRange(refID, beg, end);
        assertNotNull(chunks);
        assertEquals(1, chunks.size());

        BAMIndex.Chunk chunk = chunks.get(0);
        assertEquals(0, chunk.end.offset);
        assertEquals(60872, chunk.end.block);

    }

}
