package com.ericsson.de.scenarios.api;

import java.util.List;
import java.util.Map;

import com.ericsson.de.scenarios.impl.RxScenario;
import com.ericsson.de.scenarios.impl.RxScenarioListener;

public class Scenario extends RxScenario {

    public Scenario(String name, Map<String, Object> parameters, Flow flow, List<RxScenarioListener> listeners) {
        super(name, parameters, flow, listeners);
    }
}
