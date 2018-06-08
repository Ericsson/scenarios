package com.ericsson.de.scenarios.api.events;

import com.ericsson.de.scenarios.api.Scenario;
import com.ericsson.de.scenarios.impl.RxScenario;

public class ScenarioEvent extends Event {

    private final Scenario scenario;

    private ScenarioEvent(RxScenario scenario) {
        this.scenario = (Scenario) scenario;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public static class ScenarioStartedEvent extends ScenarioEvent {

        public ScenarioStartedEvent(RxScenario scenario) {
            super(scenario);
        }
    }

    public static class ScenarioFinishedEvent extends ScenarioEvent {

        public ScenarioFinishedEvent(RxScenario scenario) {
            super(scenario);
        }
    }
}
