package com.ericsson.de.scenarios.impl;

import static java.util.Collections.singletonList;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;

import java.util.Iterator;
import java.util.List;

import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.google.common.base.Predicate;

import rx.Observable;
import rx.functions.Func1;

public class FlowExecutionContext {

    final RxScenario scenario;
    final ScenarioEventBus eventBus;
    private final RxExceptionHandler defaultExceptionHandler;

    final RxFlow rxFlow;
    final int vUsers;
    final Observable<DataRecordWrapper> dataSource;
    final DataRecordsToExecutions toExecutions;
    protected final Predicate<DataRecordWrapper> predicate;

    private FlowExecutionContext(RxScenario scenario, ScenarioEventBus eventBus, RxExceptionHandler defaultExceptionHandler, RxFlow rxFlow,
            int vUsers, final Observable<DataRecordWrapper> dataSource, DataRecordsToExecutions toExecutions,
            Predicate<DataRecordWrapper> predicate) {
        this.scenario = scenario;
        this.eventBus = eventBus;
        this.defaultExceptionHandler = defaultExceptionHandler;

        this.rxFlow = rxFlow;
        this.vUsers = vUsers;
        this.dataSource = dataSource;
        this.toExecutions = toExecutions;

        this.predicate = predicate;
    }

    /**
     * Initial rxFlow which is created for scenario
     */
    static FlowExecutionContext createScenarioFlowContext(RxScenario scenario, ScenarioEventBus eventBus,
            RxExceptionHandler defaultExceptionHandler) {
        RxFlow rxFlow = scenario.rxFlow;
        int vUsers = rxFlow.dataSource.vUsers;
        Observable<DataRecordWrapper> dataSource = rxFlow.dataSource.provide();
        List<Internals.Exec> executions = singletonList(Internals.Exec.rootExec(scenario.parameters));
        DataRecordsToExecutions toExecutions = new DataRecordsToExecutions(rxFlow, executions, 0);

        final Predicate<DataRecordWrapper> rxDataRecordWrapperPredicate = predicateOrDefault(rxFlow, dataSource);

        return new FlowExecutionContext(scenario, eventBus, defaultExceptionHandler, rxFlow, vUsers, dataSource, toExecutions,
                rxDataRecordWrapperPredicate);
    }

    /**
     * Forks subRxFlow from rxFlow
     */
    public FlowExecutionContext subFlow(RxFlow subRxFlow, List<Internals.Exec> executions, int vUserOffset) {
        int vUsers = executions.size() * subRxFlow.dataSource.vUsers;
        Observable<DataRecordWrapper> subFlowDataSource = subRxFlow.dataSource.forkFrom(getDataRecords(executions));
        DataRecordsToExecutions dataRecordsToExecutions = new DataRecordsToExecutions(subRxFlow, executions, vUserOffset);
        final Predicate<DataRecordWrapper> rxDataRecordWrapperPredicate = predicateOrDefault(subRxFlow, subFlowDataSource);
        return new FlowExecutionContext(scenario, eventBus, defaultExceptionHandler, subRxFlow, vUsers, subFlowDataSource, dataRecordsToExecutions,
                rxDataRecordWrapperPredicate);
    }

    private static Predicate predicateOrDefault(RxFlow rxFlow, final Observable<DataRecordWrapper> dataSource) {
        if (rxFlow.predicate == null) {
            return new Predicate<DataRecordWrapper>() {
                int dataIteration = 0;
                final Integer dataRecordCount = dataSource.count().toBlocking().single();

                @Override
                public boolean apply(DataRecordWrapper input) {
                    dataIteration++;
                    return dataIteration <= dataRecordCount;
                }
            };
        } else {
            return rxFlow.predicate;
        }

    }

    RxExceptionHandler exceptionHandler() {
        return firstNonNull(rxFlow.exceptionHandler, defaultExceptionHandler);
    }

    private static Observable<DataRecordWrapper> getDataRecords(List<Internals.Exec> executions) {
        return Observable.from(executions).map(new Func1<Internals.Exec, DataRecordWrapper>() {
            @Override
            public DataRecordWrapper call(Internals.Exec exec) {
                return exec.dataRecord;
            }
        });
    }

    /**
     * Function Wraps {@code dataRecords} into {@link Internals.Exec} allocating vUsers
     */
    static class DataRecordsToExecutions implements Func1<List<DataRecordWrapper>, List<Internals.Exec>> {

        final private RxFlow rxFlow;
        final private int forkCount;
        final private List<Internals.Exec> parentExecutions;
        final private int vUserOffset;

        DataRecordsToExecutions(RxFlow rxFlow, List<Internals.Exec> parentExecutions, int vUserOffset) {
            this.rxFlow = rxFlow;
            this.forkCount = rxFlow.dataSource.vUsers;
            this.parentExecutions = parentExecutions;
            this.vUserOffset = vUserOffset;
        }

        @Override
        public List<Internals.Exec> call(List<DataRecordWrapper> dataRecords) {
            checkArgument(parentExecutions.size() * forkCount >= dataRecords.size());

            Iterator<Internals.Exec> parentIterator = parentExecutions.iterator();
            Internals.Exec parent = parentIterator.next();
            int childNo = 1;

            List<Internals.Exec> executions = newArrayList();
            for (Iterator<DataRecordWrapper> iterator = dataRecords.iterator(); iterator.hasNext(); childNo++) {
                if (childNo == forkCount + 1) {
                    parent = parentIterator.next();
                    childNo = 1;
                }

                executions.add(parent.child(rxFlow.getName(), childNo + vUserOffset, iterator.next()));
            }

            return executions;
        }
    }
}
