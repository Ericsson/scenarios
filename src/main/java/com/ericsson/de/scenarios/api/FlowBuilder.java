package com.ericsson.de.scenarios.api;

import java.util.List;

import com.ericsson.de.scenarios.impl.DataSourceStrategy;
import com.ericsson.de.scenarios.impl.Invocation;
import com.ericsson.de.scenarios.impl.RxExceptionHandler;
import com.ericsson.de.scenarios.impl.RxFlowBuilder;
import com.google.common.base.Predicate;

public final class FlowBuilder extends RxFlowBuilder<Flow> {

    private FlowBuilder(String name) {
        super(name);
    }

    static FlowBuilderInterfaces.FlowStart<Flow> flow() {
        return flow(DEFAULT_NAME);
    }

    static FlowBuilderInterfaces.FlowStart<Flow> flow(String name) {
        return new FlowBuilder(name);
    }

    @Override
    protected Flow[] createFlowArray(int length) {
        return new Flow[length];
    }

    @Override
    protected Flow createFlow(String name, DataSourceStrategy dataSource, List<Invocation> testSteps, List<TestStep> beforeInvocations,
            List<TestStep> afterInvocations, RxExceptionHandler exceptionHandler, Predicate<DataRecordWrapper> predicate) {
        return new Flow(name, dataSource, testSteps, beforeInvocations, afterInvocations, exceptionHandler, predicate);
    }
}
