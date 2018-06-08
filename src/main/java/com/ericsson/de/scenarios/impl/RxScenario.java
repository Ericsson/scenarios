package com.ericsson.de.scenarios.impl;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * Representation of individual Test RxScenario.
 */
public abstract class RxScenario {

    final String name;
    final Map<String, Object> parameters;
    final RxFlow rxFlow;
    final List<RxScenarioListener> listeners;

    protected RxScenario(String name, Map<String, Object> parameters, RxFlow rxFlow, List<RxScenarioListener> listeners) {
        this.name = name;
        this.parameters = parameters;
        this.rxFlow = rxFlow;
        this.listeners = listeners;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getParameters() {
        return ImmutableMap.copyOf(parameters);
    }
}
