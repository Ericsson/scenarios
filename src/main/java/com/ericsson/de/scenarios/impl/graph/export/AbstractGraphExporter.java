package com.ericsson.de.scenarios.impl.graph.export;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph;

public abstract class AbstractGraphExporter implements GraphExporter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractGraphExporter.class);

    private static final String EXPORT_LOCATION = "target/scenario-visualization/";
    private static final String LEADING_WHITESPACE = "^\\s+";
    private static final String TRAILING_WHITESPACE = "\\s+$";
    private static final String WHITESPACE = "\\s+";

    private final String extension;
    private final TooltipGenerator tooltipGenerator = new TooltipGenerator();

    AbstractGraphExporter(String extension) {
        this.extension = extension;
        init();
    }

    protected abstract void init();

    @Override
    public final void export(ScenarioExecutionGraph graph, String graphName) {
        final String filteredGraphName = graphName.replaceAll(LEADING_WHITESPACE, "").replaceAll(TRAILING_WHITESPACE, "").replaceAll(WHITESPACE, "_");
        String pathname = EXPORT_LOCATION + filteredGraphName + extension;
        File file = graphFile(pathname);
        saveGraph(graph, file);
    }

    private File graphFile(String pathname) {
        File file = new File(pathname);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file;
    }

    private void saveGraph(ScenarioExecutionGraph graph, File file) {
        try {
            export(graph, new FileWriter(file));
            logger.info("Graph: " + file.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String tooltip(ScenarioExecutionGraph.GraphNode node) {
        return tooltipGenerator.export(node);
    }

    protected abstract void export(ScenarioExecutionGraph graph, Writer writer) throws Exception;
}
