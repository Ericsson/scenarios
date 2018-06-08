package com.ericsson.de.scenarios.impl;

import static java.lang.String.format;

import static com.ericsson.de.scenarios.api.ScenarioRunnerBuilder.DEBUG_GRAPH_MODE;
import static com.ericsson.de.scenarios.api.ScenarioRunnerBuilder.DEBUG_LOG_ENABLED;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.List;
import java.util.Map;

import com.ericsson.de.scenarios.api.Builder;
import com.ericsson.de.scenarios.api.ExceptionHandler;
import com.ericsson.de.scenarios.api.FlowBuilder;
import com.ericsson.de.scenarios.api.ScenarioBuilderInterfaces;
import com.ericsson.de.scenarios.api.ScenarioRunnerBuilder;
import com.ericsson.de.scenarios.api.TestStep;
import com.ericsson.de.scenarios.impl.Internals.Fork;
import com.google.common.collect.Lists;

public abstract class RxScenarioBuilder<S extends RxScenario, F extends RxFlow>
        implements ScenarioBuilderInterfaces.ScenarioBuilderStates<S, F>, Builder<S> {

    protected static final String DEFAULT_NAME = "RxScenario";

    static final String ERROR_DEBUG_LOG_ENABLED = "Parameter '" + DEBUG_LOG_ENABLED + "' is reserved, use Api.runner().withDebugLogEnabled() instead";
    static final String ERROR_DEBUG_GRAPH_MODE =
            "Parameter '" + DEBUG_GRAPH_MODE + "' is reserved, use Api.runner().withGraphExportMode(...) instead";

    private static final String ERROR_UNIQUENESS_TEMPLATE = "%1$ss can not be reused within one scenario. "
            + "Please create new %1$s. You may extract %1$s creation to method if you need identical %1$ss";
    static final String ERROR_TEST_STEP_UNIQUENESS = format(ERROR_UNIQUENESS_TEMPLATE, "Test Step");
    static final String ERROR_FLOW_UNIQUENESS = format(ERROR_UNIQUENESS_TEMPLATE, "RxFlow");

    private final String name;
    protected final RxFlowBuilder<F> scenarioRxFlowBuilder;
    private final Map<String, Object> parameters = newLinkedHashMap();
    protected final List<RxScenarioListener> listeners = Lists.newArrayList();

    protected RxScenarioBuilder(String name, RxFlowBuilder<F> scenarioRxFlowBuilder) {
        this.name = name;
        this.scenarioRxFlowBuilder = scenarioRxFlowBuilder;
    }

    /**
     * @see #addFlow(RxFlow)
     */
    @Override
    public ScenarioBuilderInterfaces.Flows<S, F> addFlow(Builder<F> flowBuilder) {
        scenarioRxFlowBuilder.addSubFlow(flowBuilder);
        return this;
    }

    /**
     * Adds test step rxFlow to the current scenario.
     *
     * @param flow
     *         rxFlow to add
     *
     * @return builder
     */
    @Override
    public ScenarioBuilderInterfaces.Flows<S, F> addFlow(F flow) {
        scenarioRxFlowBuilder.addSubFlow(flow);
        return this;
    }

    /**
     * @see #split(RxFlow...)
     */
    @Override
    public ScenarioBuilderInterfaces.Flows<S, F> split(Builder<F>... subFlows) {
        scenarioRxFlowBuilder.split(subFlows);
        return this;
    }

    /**
     * Execute flows passed in param in parallel
     *
     * @param subFlows
     *         to execute in parallel
     *
     * @return builder
     */
    @Override
    public ScenarioBuilderInterfaces.Flows<S, F> split(F... subFlows) {
        scenarioRxFlowBuilder.split(subFlows);
        return this;
    }

    /**
     * @see RxFlowBuilder#alwaysRun()
     */
    @Override
    public ScenarioBuilderInterfaces.AlwaysRun<S, F> alwaysRun() {
        scenarioRxFlowBuilder.alwaysRun();
        return this;
    }

    /**
     * Add parameter which will be available to all Test Steps of RxScenario if not overridden
     * by {@link TestStep#withParameter(String)}
     *
     * @return builder
     */
    @Override
    public ScenarioBuilderInterfaces.ScenarioStart<S, F> withParameter(String key, Object value) {
        checkNotNull(key, TestStep.ERROR_PARAMETER_NULL);
        checkState(!parameters.containsKey(key), TestStep.ERROR_PARAMETER_ALREADY_SET, key);
        checkArgument(!key.equals(DEBUG_LOG_ENABLED), ERROR_DEBUG_LOG_ENABLED);
        checkArgument(!key.equals(DEBUG_GRAPH_MODE), ERROR_DEBUG_GRAPH_MODE);
        parameters.put(key, value);
        return this;
    }

    /**
     * Set an exception handler for the RxScenario, which will be called on exceptions in Test Steps.
     * If the exception handler does not propagate Exception, scenario rxFlow will continue.
     *
     * @see ScenarioRunnerBuilder#withDefaultExceptionHandler(ExceptionHandler)
     * @see FlowBuilder#withExceptionHandler(ExceptionHandler)
     */
    @Override
    public ScenarioBuilderInterfaces.ExceptionHandler<S> withExceptionHandler(ExceptionHandler exceptionHandler) {
        scenarioRxFlowBuilder.withExceptionHandler(exceptionHandler);
        return this;
    }

    /**
     * @return RxScenario
     */
    @Override
    public S build() {
        F scenarioFlow = scenarioRxFlowBuilder.build();
        assignUniqueScenarioIds(scenarioFlow, 1);
        //noinspection unchecked
        return createScenario(name, parameters, scenarioFlow);
    }

    protected abstract S createScenario(String name, Map<String, Object> parameters, F flow);

    private long assignUniqueScenarioIds(RxFlow rxFlow, long startScenarioId) {
        checkArgument(rxFlow.getId() == null, ERROR_FLOW_UNIQUENESS);
        long scenarioId = startScenarioId;
        rxFlow.id = scenarioId++;

        for (Invocation invocation : concat(rxFlow.getBefore(), rxFlow.testSteps, rxFlow.getAfter())) {
            if (invocation instanceof TestStep) {
                checkArgument(invocation.getId() == null, ERROR_TEST_STEP_UNIQUENESS);
                invocation.id = scenarioId++;
            } else if (invocation instanceof Fork) {
                Fork fork = (Fork) invocation;
                for (RxFlow subRxFlow : fork.flows) {
                    scenarioId = assignUniqueScenarioIds(subRxFlow, scenarioId);
                }
            }
        }
        return scenarioId;
    }
}
