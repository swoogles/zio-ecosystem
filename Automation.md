# How to Automate Production of ZIO 2.0 Ecosystem Artifacts
This document _merely suggests_ one plan for automating the production of the interactive SVG document and Excel spreadsheet (alternatively, Google Sheet).

1. **Maintain project-specific notes in JSON files in this git repo.** There is usually important metadata that developers should know about regarding specific projects. For example, a project's maintainer, Github website, links to any relevant Github issues, or miscellaneous notes. Each project in the ZIO ecosystem should have one of these files in this repo.

2. **Automate collection of library dependencies:** The program should collect information for building the ZIO ecosystem dependency graph based on the freshest information available. This could be done by crawling Github repos (for more up-to-date), or possibly through Maven/Sonatype/etc. The latter may not be a good fit due to staleness, but who knows.

3. **Artifact Generation:**
    - **Produce Excel artifact:** Use the Apache POI library to produce an Excel spreadsheet dump of the dependencies and project metadata. This should look pretty much identical to Bill's amazing Google Sheet, but with fresher data in it.

    - **Produce Google Sheets artifact:** Use a Scala Google Sheets API library such as [this one](https://github.com/BenFradet/gsheets4s) to dump the dependencies and project metadata into Google Sheets.

    - **Produce SVG Graph Artifact:** Use a Java Graphics2D-compatible SVG library (e.g., [batik](https://xmlgraphics.apache.org/batik/) or [JFreeSVG](https://github.com/jfree/jfreesvg)) to render the dependency graph and metadata as an SVG file. **NOTE:** In general, graph layout is tricky, and sometimes it is impossible to make the output look good. I'd recommend the [Compressed Adjacency Matrices](https://ieeexplore.ieee.org/document/6327251) algorithm, but it would have to be implemented from scratch. I may have an old implementation sitting around somewhere - will look. There are some java graph layout implementations that may work. If you decide to use `graphviz` for computing layouts, [this online editor](https://dreampuf.github.io/GraphvizOnline/) is awesome for experimentation.

4. **Automate execution:** Configure this program to run as a cron job. The program may dump the artifacts directly into Github as commits. Alternatively, consider putting the artifacts somewhere such as S3 (not ideal because of $).

5. **Send execution status through Slack API:** When the job runs, send its exit status to a Ziverge Slack channel using the Slack API.
