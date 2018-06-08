package com.ericsson.de.scenarios.impl;

import static com.ericsson.de.scenarios.impl.StackTraceFilter.filterListenerStackTrace;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.ericsson.de.scenarios.api.TestStep;
import com.ericsson.de.scenarios.api.events.Event;
import com.ericsson.de.scenarios.api.events.FlowEvent;
import com.ericsson.de.scenarios.api.events.ScenarioEvent;
import com.ericsson.de.scenarios.api.events.TestStepEvent;
import com.ericsson.de.scenarios.impl.Internals.Exec;
import com.ericsson.de.scenarios.impl.Internals.TestStepResult;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

import rx.Observable;

/**
 * Currently only used for {@link RxScenarioListener}
 */
class ScenarioEventBus {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioEventBus.class);

    private EventBus eventBus;

    ScenarioEventBus(Iterable<RxScenarioListener> listeners) {
        eventBus = new EventBus(ScenarioLoggingHandler.INSTANCE);
        for (RxScenarioListener listener : listeners) {
            eventBus.register(listener);
        }
    }

    void scenarioStarted(RxScenario scenario) {
        post(new ScenarioEvent.ScenarioStartedEvent(scenario));
    }

    void scenarioFinished(RxScenario scenario, List<Internals.FlowExecutionResult> results) {
        post(new InternalScenarioFinishedEvent(scenario, results));
    }

    void flowStarted(RxFlow rxFlow, Observable<DataRecordWrapper> dataSource) {
        post(new InternalFlowStartedEvent(rxFlow, dataSource));
    }

    void flowFinished(RxFlow rxFlow, Observable<DataRecordWrapper> dataSource) {
        post(new InternalFlowFinishedEvent(rxFlow, dataSource));
    }

    void testStepStarted(TestStep testStep, Exec execution) {
        post(new InternalTestStepStartedEvent(testStep, execution));
    }

    void testStepFinished(TestStep testStep, Exec execution, TestStepResult result) {
        post(new InternalTestStepFinishedEvent(testStep, execution, result));
    }

    private void post(Event event) {
        eventBus.post(event);
    }

    /*---------------- Internal Events ----------------*/

    static class InternalFlowStartedEvent extends FlowEvent.FlowStartedEvent {

        private Observable<DataRecordWrapper> dataSource;

        private InternalFlowStartedEvent(RxFlow rxFlow, Observable<DataRecordWrapper> dataSource) {
            super(rxFlow);
            this.dataSource = dataSource;
        }

        public Observable<DataRecordWrapper> getDataSource() {
            return dataSource;
        }
    }

    static class InternalFlowFinishedEvent extends FlowEvent.FlowFinishedEvent {

        private Observable<DataRecordWrapper> dataSource;

        private InternalFlowFinishedEvent(RxFlow rxFlow, Observable<DataRecordWrapper> dataSource) {
            super(rxFlow);
            this.dataSource = dataSource;
        }

        public Observable<DataRecordWrapper> getDataSource() {
            return dataSource;
        }
    }

    static class InternalScenarioFinishedEvent extends ScenarioEvent.ScenarioFinishedEvent {
        protected List<Internals.FlowExecutionResult> results;

        public InternalScenarioFinishedEvent(RxScenario scenario, List<Internals.FlowExecutionResult> results) {
            super(scenario);
            this.results = results;
        }
    }

    static class InternalTestStepStartedEvent extends TestStepEvent.TestStepStartedEvent {

        private Exec execution;

        private InternalTestStepStartedEvent(TestStep testStep, Exec execution) {
            super(testStep, execution.dataRecord);
            this.execution = execution;
        }

        public Exec getExecution() {
            return execution;
        }
    }

    static class InternalTestStepFinishedEvent extends TestStepEvent.TestStepFinishedEvent {

        private Exec execution;
        private TestStepResult result;

        private InternalTestStepFinishedEvent(TestStep testStep, Exec execution, TestStepResult result) {
            super(testStep, result.status, result.error);
            this.execution = execution;
            this.result = result;
        }

        public TestStepResult getResult() {
            return result;
        }

        public Exec getExecution() {
            return execution;
        }
    }

    /*-------- end --------*/

    private static final class ScenarioLoggingHandler implements SubscriberExceptionHandler {

        static final ScenarioLoggingHandler INSTANCE = new ScenarioLoggingHandler();

        @Override
        public void handleException(Throwable exception, SubscriberExceptionContext context) {
            logger.error("Exception thrown by ScenarioListener:", filterListenerStackTrace(exception));
        }
    }
}
