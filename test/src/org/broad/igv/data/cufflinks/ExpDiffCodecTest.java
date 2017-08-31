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

package org.broad.igv.data.cufflinks;

import org.broad.igv.feature.Range;
import org.broad.igv.util.TestUtils;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author jacob
 * @date 2013-Apr-18
 */
public class ExpDiffCodecTest {

    @Test
    public void testsamplegene_expdiff() throws Exception{
        String path = TestUtils.DATA_DIR + "cufflinks/sample_gene_exp.diff";

        List<? extends Range> values = CufflinksParser.parse(path);

        String[] expGenes = new String[]{"TSPAN6", "TNMD", "DPM1", "SCYL3"};
        int index = 0;

        for(String expGene: expGenes){
            Range value = values.get(index++);
            assertTrue(value instanceof ExpDiffValue);
            assertEquals(expGene, ((ExpDiffValue) value).getGene());
        }
    }
}
