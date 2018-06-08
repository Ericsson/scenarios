package com.ericsson.de.scenarios.impl;

import java.util.List;

import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.ericsson.de.scenarios.api.Flow;
import com.ericsson.de.scenarios.api.TestStep;
import com.google.common.base.Predicate;

public class PerformanceFlowBuilder extends RxFlowBuilder<Flow> {
    private RxRampUp.StrategyProvider rampUp = RxRampUp.allAtOnce();

    protected PerformanceFlowBuilder(String name) {
        super(name);
    }

    @Override
    protected Flow[] createFlowArray(int length) {
        return new Flow[0];
    }

    @Override
    protected Flow createFlow(String name, DataSourceStrategy dataSource, List<Invocation> testSteps, List<TestStep> beforeInvocations,
            List<TestStep> afterInvocations, RxExceptionHandler exceptionHandler, Predicate<DataRecordWrapper> predicate) {
        return new Flow(name, dataSource, testSteps, beforeInvocations, afterInvocations, exceptionHandler, predicate);
    }

    public RxRampUp.StrategyProvider getRampUp() {
        return rampUp;
    }

    public void withRampUp(RxRampUp.StrategyProvider rampUp) {
        this.rampUp = rampUp;
    }
}
