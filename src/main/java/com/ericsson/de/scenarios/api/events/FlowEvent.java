package com.ericsson.de.scenarios.api.events;

import com.ericsson.de.scenarios.api.Flow;
import com.ericsson.de.scenarios.impl.RxFlow;

public class FlowEvent extends Event {

    private final Flow flow;

    private FlowEvent(RxFlow rxFlow) {
        this.flow = (Flow) rxFlow;
    }

    public Flow getFlow() {
        return flow;
    }

    public static class FlowStartedEvent extends FlowEvent {

        public FlowStartedEvent(RxFlow rxFlow) {
            super(rxFlow);
        }
    }

    public static class FlowFinishedEvent extends FlowEvent {

        public FlowFinishedEvent(RxFlow rxFlow) {
            super(rxFlow);
        }
    }
}
