::

    .=====================================================.
       _  _   __   ____   __   __ _        ___  __  ____ 
      / )( \ / _\ (  _ \ / _\ (  ( \ ___  / __)(  )(  __)
      \ \/ //    \ )   //    \/    /(___)( (_ \ )(  ) _) 
       \__/ \_/\_/(__\_)\_/\_/\_)__)      \___/(__)(____)                                                                               
    .=====================================================.
    
    VARAN GIE: An IGV extension for the annotation of genomic intervals

Introduction
============

VARAN-GIE (VARAN) is an editing extension to the popular `IGV`_ genome browser. 
It extends IGV by adding **functionality to create, edit and annotate sets of genomic intervals**.
By this, VARAN supports an integrative approach to viewing and annotating large genomic data sets. 

Application scenarios for VARAN include

* Manual curation of genomic annotations, e.g., gene lists, CNV or SV calls, novel transcripts
* Merging and curation of multiple BED files
* De-novo authoring of *multi-layer* interval sets 

and many more.

.. _IGV: http://software.broadinstitute.org/software/igv/

Installation
============

1. Install java 1.8  (see `here`_ for a discussion on how to install java 8 on MacOS)
2. Download and unzip the `release ZIP file`_
3. Start VARAN-GIE with the respective shell script or by directly running the (executable) JAR file

Prior installation of IGV is not required as the VARAN-GIE release ZIP will contain IGV + the extension. 
When first started, VARAN-GIE will create a subdirectory "gie" in your IGV home directory (usually ~/igv) where it 
will store all GIE-related files. If IGV+VARAN-GIE is slow or runs out of memory, consider updating the -Xmx parameter 
in the startup shell script to reserve more memory for VARAN-GIE. Please note that VARAN-GIE is currently not 
automatically updated when IGV is updated as it was branched from the main IGV development tree.

.. _release ZIP file: https://github.com/popitsch/varan-gie/tags
.. _here: https://stackoverflow.com/questions/24342886/how-to-install-java-8-on-mac

User Guide
==========

* watch our introductory `videos`_ on youtube 
* a detailed user guide that also describes VARAN's data model is provided in our `wiki`_  


.. _videos: https://www.youtube.com/watch?v=aBHKEviy9g4&list=PLvayEaZ7ZDgwyUiv5h0ygUTdGVGj_U061
.. _wiki: https://github.com/popitsch/varan-gie/wiki/Home


Citation
========

Please cite our paper:

    Niko Popitsch, VARAN-GIE: curation of genomic interval sets, 
    Bioinformatics, Volume 35, Issue 5, 01 March 2019, 
    Pages 868–870, https://doi.org/10.1093/bioinformatics/bty723
 