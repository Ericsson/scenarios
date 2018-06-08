package com.ericsson.de.scenarios.impl;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.ericsson.de.scenarios.api.Identifiable;
import com.ericsson.de.scenarios.api.TestStep;
import com.google.common.base.Predicate;

import rx.Observable;

/**
 * Sequence of Test Steps
 */
public abstract class RxFlow implements Identifiable<Long> {

    Long id = null;
    private final String name;
    final DataSourceStrategy dataSource;
    final List<Invocation> testSteps;

    private final List<TestStep> beforeInvocations;
    private final List<TestStep> afterInvocations;
    final Predicate predicate;

    final RxExceptionHandler exceptionHandler;

    protected RxFlow(String name, DataSourceStrategy dataSource, List<Invocation> testSteps, List<TestStep> beforeInvocations,
            List<TestStep> afterInvocations, RxExceptionHandler exceptionHandler, Predicate<DataRecordWrapper> predicate) {
        this.name = name;
        this.dataSource = dataSource;
        this.testSteps = testSteps;

        this.beforeInvocations = beforeInvocations;
        this.afterInvocations = afterInvocations;

        this.exceptionHandler = exceptionHandler;
        this.predicate = predicate;
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
     * @return name of the RxFlow.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Chunks of scenario that is safe to execute in parallel
     */
    Observable<Internals.Chunk> chunks() {
        List<Internals.Chunk> chunks = newArrayList();
        Internals.Chunk chunk = new Internals.Chunk();
        for (Invocation testStep : testSteps) {
            if (testStep instanceof TestStep) {
                chunk.testSteps.add(TestStep.class.cast(testStep));
            } else if (testStep instanceof Internals.Fork) {
                chunk.fork = Internals.Fork.class.cast(testStep);
                chunks.add(chunk);
                chunk = new Internals.Chunk();
            }
        }
        chunks.add(chunk);

        return Observable.from(chunks);
    }

    List<TestStep> getBefore() {
        return beforeInvocations;
    }

    List<TestStep> getAfter() {
        return afterInvocations;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).add("name", name).add("testSteps", testSteps).add("dataSource", dataSource).toString();
    }
}
