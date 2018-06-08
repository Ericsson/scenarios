package com.ericsson.de.scenarios.api;

import com.ericsson.de.scenarios.impl.RxApi;

/**
 * Utility class that provides access to all API necessary for RxScenario creation and execution.
 * Class can be extended to provide custom RxScenario functionality.
 */
public abstract class Api extends RxApi {

    /**
     * Creates Builder for flow
     *
     * @return builder
     */
    public static FlowBuilderInterfaces.FlowStart<Flow> flow() {
        return FlowBuilder.flow();
    }

    /**
     * Creates Builder for flow with given name
     *
     * @return builder
     */
    public static FlowBuilderInterfaces.FlowStart<Flow> flow(String name) {
        return FlowBuilder.flow(name);
    }

    /**
     * Creates builder for RxScenario
     *
     * @return builder
     */
    public static ScenarioBuilderInterfaces.ScenarioStart<Scenario, Flow> scenario() {
        return ScenarioBuilder.scenario();
    }

    /**
     * Creates builder for RxScenario with given name
     *
     * @return builder
     */
    public static ScenarioBuilderInterfaces.ScenarioStart<Scenario, Flow> scenario(String name) {
        return ScenarioBuilder.scenario(name);
    }

    /**
     * Creates Data Source that could be populated from Test Step return values
     *
     * @param name
     *         name of the Data Source
     *
     * @return definition
     * @see TestStep#collectResultsToDataSource(ContextDataSource)
     */
    public static <T> ContextDataSource<T> contextDataSource(final String name, Class<T> type) {
        return new ContextDataSource<>(name, type);
    }

    /**
     * Creates builder for RxScenario Runner
     **/
    public static ScenarioRunnerBuilder runner() {
        return new ScenarioRunnerBuilder();
    }

    /**
     * Run given scenario.
     * In case you need to configure, use {@link #runner()}
     */
    public static void run(Scenario scenario) {
        runner().build().run(scenario);
    }

}
