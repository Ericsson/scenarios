package com.ericsson.de.scenarios.api;

import java.util.Map;

import com.ericsson.de.scenarios.impl.RxFlowBuilder;
import com.ericsson.de.scenarios.impl.RxScenarioBuilder;

public class ScenarioBuilder extends RxScenarioBuilder<Scenario, Flow> {

    private ScenarioBuilder(String name) {
        super(name, (RxFlowBuilder<Flow>) FlowBuilder.flow(name));
    }

    static ScenarioBuilderInterfaces.ScenarioStart<Scenario, Flow> scenario() {
        return scenario(DEFAULT_NAME);
    }

    static ScenarioBuilderInterfaces.ScenarioStart<Scenario, Flow> scenario(String name) {
        return new ScenarioBuilder(name);
    }

    @Override
    protected Scenario createScenario(String name, Map<String, Object> parameters, Flow flow) {
        return new Scenario(name, parameters, flow, listeners);
    }
}
