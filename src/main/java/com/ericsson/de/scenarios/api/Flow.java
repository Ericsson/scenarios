package com.ericsson.de.scenarios.api;

import java.util.List;

import com.ericsson.de.scenarios.impl.DataSourceStrategy;
import com.ericsson.de.scenarios.impl.Invocation;
import com.ericsson.de.scenarios.impl.RxExceptionHandler;
import com.ericsson.de.scenarios.impl.RxFlow;
import com.google.common.base.Predicate;

public class Flow extends RxFlow {

    public Flow(String name, DataSourceStrategy dataSource, List<Invocation> testSteps, List<TestStep> beforeInvocations,
            List<TestStep> afterInvocations, RxExceptionHandler exceptionHandler, Predicate<DataRecordWrapper> predicate) {
        super(name, dataSource, testSteps, beforeInvocations, afterInvocations, exceptionHandler, predicate);
    }
}
