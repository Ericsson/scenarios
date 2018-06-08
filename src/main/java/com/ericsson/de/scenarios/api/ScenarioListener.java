package com.ericsson.de.scenarios.api;

import com.ericsson.de.scenarios.api.events.FlowEvent;
import com.ericsson.de.scenarios.api.events.ScenarioEvent;
import com.ericsson.de.scenarios.api.events.TestStepEvent;
import com.ericsson.de.scenarios.impl.RxScenarioListener;
import com.google.common.eventbus.Subscribe;

/**
 * Base class for extending custom RxScenario listeners from
 *
 * @see ScenarioRunnerBuilder#addListener(ScenarioListener)
 */
@SuppressWarnings("unused")
public abstract class ScenarioListener implements RxScenarioListener {

    @Subscribe
    public void onScenarioStarted(ScenarioEvent.ScenarioStartedEvent event) {
        // intentionally do nothing
    }

    @Subscribe
    public void onScenarioFinished(ScenarioEvent.ScenarioFinishedEvent event) {
        // intentionally do nothing
    }

    @Subscribe
    public void onFlowStarted(FlowEvent.FlowStartedEvent event) {
        // intentionally do nothing
    }

    @Subscribe
    public void onFlowFinished(FlowEvent.FlowFinishedEvent event) {
        // intentionally do nothing
    }

    @Subscribe
    public void onTestStepStarted(TestStepEvent.TestStepStartedEvent event) {
        // intentionally do nothing
    }

    @Subscribe
    public void onTestStepFinished(TestStepEvent.TestStepFinishedEvent event) {
        // intentionally do nothing
    }
}
