package com.ericsson.de.scenarios.testware;

import static java.util.Objects.requireNonNull;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.de.scenarios.api.Api.flow;
import static com.ericsson.de.scenarios.api.Api.runner;
import static com.ericsson.de.scenarios.api.Api.scenario;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.ericsson.de.scenarios.api.DebugGraphMode;
import com.ericsson.de.scenarios.api.Scenario;
import com.ericsson.de.scenarios.impl.ScenarioTest;

public class GraphGenerationTest extends ScenarioTest {

    private static final String DESIRED_GRAPH_LOCATION = "target/scenario-visualization/";
    private static final String SVG_EXTENSION = ".svg";
    private static final String GRAPHML_EXTENSION = ".graphml";

    @Test
    public void testSavedGraphLocation() throws Exception {
        final String scenarioName = "GraphLocationVerification";
        Scenario scenario = scenario(scenarioName).addFlow(flow("print flow").addTestStep(print("testStep1"))).build();

        runScenarioAllGraphs(scenario);
        verifyGraphDirectoryContents(scenarioName);
    }

    @Test
    public void testGraphNameSpaceFiltering() throws Exception {
        final String scenarioNameUnfiltered = "   Graph     name   filtered      ";
        final String scenarioNameFiltered = "Graph_name_filtered";
        Scenario scenario = scenario(scenarioNameUnfiltered).addFlow(flow("print flow").addTestStep(print("testStep1"))).build();

        runScenarioAllGraphs(scenario);
        verifyGraphDirectoryContents(scenarioNameFiltered);
    }

    private void verifyGraphDirectoryContents(String scenarioName) {
        File graphFolder = new File(DESIRED_GRAPH_LOCATION);
        assertThat(graphFolder.isDirectory());
        List<String> graphDirectoryContents = Arrays.asList(requireNonNull(graphFolder.list()));
        assertThat(graphDirectoryContents).contains(scenarioName + GRAPHML_EXTENSION, scenarioName + SVG_EXTENSION);
    }

    private void runScenarioAllGraphs(Scenario scenario) {
        runner().withGraphExportMode(DebugGraphMode.ALL).build().run(scenario);
    }
}
