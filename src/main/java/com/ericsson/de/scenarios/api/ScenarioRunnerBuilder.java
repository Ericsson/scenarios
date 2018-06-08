package com.ericsson.de.scenarios.api;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import com.ericsson.de.scenarios.impl.DebugLogScenarioListener;
import com.ericsson.de.scenarios.impl.RxFlowBuilder;
import com.ericsson.de.scenarios.impl.RxScenarioListener;
import com.google.common.base.Preconditions;

public class ScenarioRunnerBuilder implements Builder<ScenarioRunner> {

    public static final String DEBUG_LOG_ENABLED = "scenario.debug.log.enabled";
    public static final String DEBUG_GRAPH_MODE = "scenario.debug.graph.mode";

    static final String ERROR_DEBUG_LOG_ENABLED =
            "System property '-D" + DEBUG_LOG_ENABLED + "' " + "must have either 'true' or 'false' value, but got: %s";
    static final String ERROR_DEBUG_GRAPH_MODE =
            "System property '-D" + DEBUG_GRAPH_MODE + "' " + "must have one of the 'none'|'graphml'|'svg'|'all' values, but got: %s";
    static final String ERROR_LISTENER_DUPLICATE = "Duplicate listeners can't be registered";

    Boolean debugLogEnabled = null;
    DebugGraphMode debugGraphMode = null;
    List<RxScenarioListener> listeners = newArrayList();
    ExceptionHandler defaultExceptionHandler;

    ScenarioRunnerBuilder() {
    }

    /**
     * Turns on debug logging for Flows and Test Steps,
     * including Data Sources metadata and Data Records
     */
    public ScenarioRunnerBuilder withDebugLogEnabled() {
        checkState(debugLogEnabled == null, TestStep.ERROR_PARAMETER_ALREADY_SET, "debugLogEnabled");
        debugLogEnabled = true;
        return this;
    }

    /**
     * Turns on RxScenario execution graph exporting in .svg and/or .graphml formats
     */
    public ScenarioRunnerBuilder withGraphExportMode(DebugGraphMode debugGraphMode) {
        checkArgument(debugGraphMode != null, TestStep.ERROR_PARAMETER_NULL, "debugGraphMode");
        checkState(this.debugGraphMode == null, TestStep.ERROR_PARAMETER_ALREADY_SET, "debugGraphMode");
        this.debugGraphMode = debugGraphMode;
        return this;
    }

    /**
     * Subscribes custom listener for getting notifications about RxScenario execution events
     */
    public ScenarioRunnerBuilder addListener(ScenarioListener listener) {
        checkState(!listeners.contains(listener), ERROR_LISTENER_DUPLICATE);
        listeners.add(listener);
        return this;
    }

    /**
     * Sets a default exception handler for all Scenarios which will be started with this Runner.
     * In case if an exception gets thrown in a RxFlow without any exception handlers,
     * this default exception handler will handle that exception.
     *
     * @see ScenarioRunnerBuilder#withDefaultExceptionHandler(ExceptionHandler)
     * @see FlowBuilder#withExceptionHandler(ExceptionHandler)
     */
    public ScenarioRunnerBuilder withDefaultExceptionHandler(ExceptionHandler defaultExceptionHandler) {
        Preconditions.checkNotNull(defaultExceptionHandler, RxFlowBuilder.ERROR_EXCEPTION_HANDLER_NULL);
        Preconditions.checkState(this.defaultExceptionHandler == null,
                RxFlowBuilder.ERROR_EXCEPTION_HANDLER_NOT_ONCE + RxFlowBuilder.HINT_EXCEPTION_HANDLER);

        this.defaultExceptionHandler = defaultExceptionHandler;
        return this;
    }

    @Override
    public ScenarioRunner build() {
        Boolean scenarioDebugLogEnabled = toDebugLogEnabled(System.getProperty(DEBUG_LOG_ENABLED));
        DebugGraphMode scenarioDebugGraphMode = toGraphExportMode(System.getProperty(DEBUG_GRAPH_MODE));

        this.debugLogEnabled = selectParameter(scenarioDebugLogEnabled, this.debugLogEnabled, false);
        this.debugGraphMode = selectParameter(scenarioDebugGraphMode, this.debugGraphMode, DebugGraphMode.NONE);

        if (this.debugLogEnabled) {
            listeners.add(DebugLogScenarioListener.INSTANCE);
        }

        defaultExceptionHandler = firstNonNull(defaultExceptionHandler, ExceptionHandler.PROPAGATE);

        return new ScenarioRunner(this.debugGraphMode, listeners, defaultExceptionHandler);
    }

    private Boolean toDebugLogEnabled(String property) {
        boolean isBoolean = "false".equalsIgnoreCase(property) || "true".equalsIgnoreCase(property);
        checkArgument(property == null || isBoolean, ERROR_DEBUG_LOG_ENABLED, property);
        return isBoolean ? parseBoolean(property) : null;
    }

    private DebugGraphMode toGraphExportMode(String property) {
        if (property == null) {
            return null;
        } else
            try {
                return DebugGraphMode.valueOf(property.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(format(ERROR_DEBUG_GRAPH_MODE, property));
            }
    }

    private <T> T selectParameter(T systemProperty, T builderParam, T defaultValue) {
        return firstNonNull(systemProperty, firstNonNull(builderParam, defaultValue));
    }
}
