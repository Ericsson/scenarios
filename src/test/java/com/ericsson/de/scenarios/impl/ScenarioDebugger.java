package com.ericsson.de.scenarios.impl;

import static org.mockito.Mockito.mock;

import static com.ericsson.de.scenarios.impl.FlowExecutionContext.createScenarioFlowContext;
import static com.ericsson.de.scenarios.impl.Implementation.runFlow;
import static com.google.common.collect.Iterators.forArray;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.de.scenarios.api.DebugGraphMode;
import com.ericsson.de.scenarios.api.ExceptionHandler;
import com.ericsson.de.scenarios.impl.Internals.FlowExecutionResult;
import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph;

public class ScenarioDebugger extends RxScenarioRunner {

    private static final Logger logger = LoggerFactory.getLogger(RxScenarioRunner.class);

    protected ScenarioDebugger() {
        super(DebugGraphMode.NONE, Collections.<RxScenarioListener>emptyList(), ExceptionHandler.PROPAGATE);
    }

    /**
     * Debug given scenario.
     * Will not fail Test if RxScenario produces exception.
     * Used for creating graph for debugging purposes.
     */
    public static ScenarioExecutionGraph debug(RxScenario scenario) {
        return new ScenarioDebugger().doDebug(scenario);
    }

    private ScenarioExecutionGraph doDebug(RxScenario scenario) {
        ScenarioEventBus eventBus = mock(ScenarioEventBus.class);
        FlowExecutionContext context = createScenarioFlowContext(scenario, eventBus, ExceptionHandler.PROPAGATE);
        List<FlowExecutionResult> results = runFlow(context).toList().toBlocking().single();

        for (FlowExecutionResult result : results) {
            if (result.isFailed()) {
                logger.info("RxScenario produced errors in debug mode: ", result.error);
            }
        }

        return createGraph(results, DebugGraphMode.ALL, graphName());
    }

    private String graphName() {
        Iterator<StackTraceElement> elements = forArray(new Exception().getStackTrace());
        StackTraceElement element;
        do {
            element = elements.next();
        } while (element.getClassName().equals(ScenarioDebugger.class.getName()));
        return element.getMethodName();
    }
}
