package com.ericsson.de.scenarios.api;

import com.ericsson.de.scenarios.impl.graph.export.GraphExporter;

/**
 * In which format to save RxScenario Graph
 */
public enum DebugGraphMode {
    NONE(), /**
     * Save RxScenario Graph in Graph Ml format for tools like <a href="http://www.yworks.com/products/yed?">Yed</a>
     */
    GRAPH_ML(ScenarioRunner.graphMlExporter), /**
     * Save RxScenario Graph as svg image
     */
    SVG(ScenarioRunner.svgExporter), ALL(ScenarioRunner.graphMlExporter, ScenarioRunner.svgExporter);

    private GraphExporter[] exporters;

    DebugGraphMode(GraphExporter... exporters) {
        this.exporters = exporters;
    }

    public GraphExporter[] getExporters() {
        return exporters;
    }
}
