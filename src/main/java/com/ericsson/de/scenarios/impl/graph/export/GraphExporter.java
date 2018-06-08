package com.ericsson.de.scenarios.impl.graph.export;

import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph;

public interface GraphExporter {

    void export(ScenarioExecutionGraph graph, String pathname);

}
