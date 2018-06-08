package com.ericsson.de.scenarios.api;

import java.util.List;

import com.ericsson.de.scenarios.impl.RxScenario;
import com.ericsson.de.scenarios.impl.RxScenarioListener;
import com.ericsson.de.scenarios.impl.RxScenarioRunner;
import com.ericsson.de.scenarios.impl.graph.export.GraphExporter;
import com.ericsson.de.scenarios.impl.graph.export.GraphMlExporter;
import com.ericsson.de.scenarios.impl.graph.export.SvgExporter;

public class ScenarioRunner extends RxScenarioRunner {

    static GraphExporter graphMlExporter = new GraphMlExporter();
    static GraphExporter svgExporter = new SvgExporter();

    ScenarioRunner(DebugGraphMode debugGraphMode, List<RxScenarioListener> listeners, ExceptionHandler defaultExceptionHandler) {
        super(debugGraphMode, listeners, defaultExceptionHandler);
    }

    @Override
    public void run(RxScenario scenario) {
        super.run(scenario);
    }

    /**
     * @deprecated use {@link #run(RxScenario)} instead.
     */
    @Deprecated
    public void start(RxScenario scenario) {
        super.run(scenario);
    }
}
