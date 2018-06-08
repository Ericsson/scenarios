package com.ericsson.de.scenarios.impl;

/*

 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

import static java.util.Arrays.copyOf;
import static java.util.concurrent.TimeUnit.SECONDS;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import com.ericsson.de.scenarios.api.BasicDataRecord;
import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.ericsson.de.scenarios.api.ScenarioContext;
import com.ericsson.de.scenarios.api.TestStep;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.primitives.Ints;

import rx.Scheduler;

class Internals {

    static final String STORE_V_USERS_IN_CONTEXT = "scenario.debug.context.vusers.enabled";

    static class Chunk {
        String flowName;
        final List<TestStep> testSteps = newArrayList();
        Fork fork;

        @Override
        public String toString() {
            return toStringHelper(this).add("flowName", flowName).add("testSteps", testSteps).add("fork", fork).toString();
        }
    }

    static class RunnableTestStep extends TestStep {

        private final Runnable runnable;

        RunnableTestStep(String name, Runnable runnable) {
            super(name);
            this.runnable = runnable;
        }

        @Override
        public Optional<Object> doRun(DataRecordWrapper dataRecord) {
            runnable.run();
            return Optional.absent();
        }

        @Override
        protected TestStep copySelf() {
            return new RunnableTestStep(name, runnable);
        }
    }

    static abstract class ControlTestStep extends TestStep {

        ControlTestStep(String name) {
            super(name);
        }

        @Override
        protected TestStep copySelf() {
            throw new IllegalStateException();
        }

    }

    static class BeforeTestStepEvent extends ControlTestStep {

        private FlowExecutionContext context;

        BeforeTestStepEvent(FlowExecutionContext context) {
            super("Before Started Test Step ");
            this.context = context;
        }

        @Override
        protected Optional<Object> doRun(DataRecordWrapper dataRecord) throws Exception {
            if (context.rxFlow != context.scenario.rxFlow) {
                context.eventBus.flowStarted(context.rxFlow, context.dataSource);
            }
            return Optional.absent();
        }
    }

    static class AfterTestStepEvent extends ControlTestStep {

        private FlowExecutionContext context;

        AfterTestStepEvent(FlowExecutionContext context) {
            super("After Started Test Step ");
            this.context = context;
        }

        @Override
        protected Optional<Object> doRun(DataRecordWrapper dataRecord) throws Exception {
            if (context.rxFlow != context.scenario.rxFlow) {
                context.eventBus.flowFinished(context.rxFlow, context.dataSource);
            }
            return Optional.absent();
        }
    }

    static class Fork extends Invocation {
        final List<? extends RxFlow> flows;
        boolean alwaysRun;

        Fork(List<? extends RxFlow> flows) {
            this.flows = flows;
        }

        @Override
        public Fork alwaysRun() {
            alwaysRun = true;
            return this;
        }
    }

    /**
     * Represents Execution of one vUser / one DataRecord in scope of one {@link Chunk} rxFlow
     */
    static class Exec {
        final InternalScenarioContext context;
        final String flowPath;
        final VUser vUser;
        final Exec parent;
        final DataRecordWrapper dataRecord;

        final List<Throwable> errors = newArrayList();
        private final List<TestStepResult> executedTestSteps = newArrayList();
        private long delay = 0;

        Exec(String flowPath, VUser vUser, InternalScenarioContext context, Exec parent, DataRecordWrapper dataRecord) {
            this.flowPath = flowPath;
            this.vUser = vUser;
            this.context = context;
            this.dataRecord = dataRecord;
            this.parent = parent;

            if (context.storeVUsers()) {
                context.setFieldValue("vUser", vUser);
            }
        }

        static Exec rootExec(Map<String, Object> parameters) {
            InternalScenarioContext context = new InternalScenarioContext(parameters);
            return new Exec("", VUser.ROOT, context, null, null);
        }

        Exec child(String flowName, int childNo, DataRecordWrapper dataRecord) {
            String subFlowName = flowPath.isEmpty() ? flowName : flowPath + "." + flowName;
            VUser childVUser = vUser.child(childNo);
            InternalScenarioContext childContext = context.child();
            return new Exec(subFlowName, childVUser, childContext, this, dataRecord);
        }

        Exec copy() {
            return new Exec(flowPath, vUser, context, parent, dataRecord);
        }

        boolean isFailed() {
            return !errors.isEmpty();
        }

        @Override
        public String toString() {
            return toStringHelper(this).add("context", context).add("errors", errors).add("vUser", vUser).add("dataRecord", dataRecord).toString();
        }

        void addExecutedTestStep(TestStepResult testStepResult) {
            executedTestSteps.add(testStepResult);

            if (testStepResult.getReturnedValue().isPresent()) {
                context.parseValues(testStepResult.name, testStepResult.getReturnedValue().get());
            }

            if (testStepResult.isFailed()) {
                errors.add(testStepResult.error);
            }
        }

        List<TestStepResult> getExecutedTestSteps() {
            return executedTestSteps;
        }

        DataRecordWrapper getDataRecordAndContext() {
            return context.wrapDataRecord(dataRecord);
        }

        public void setDelay(long delay) {
            this.delay = delay;
        }

        public long getDelay() {
            return delay;
        }
    }

    static class InternalScenarioContext {
        final Map<String, Object> values = newHashMap();

        InternalScenarioContext(Map<String, Object> values) {
            this.values.putAll(values);
        }

        DataRecordWrapper wrapDataRecord(DataRecordWrapper dataRecord) {
            DataRecords.Single context = new DataRecords.Single(ScenarioContext.CONTEXT_RECORD_NAME, new DefaultDataRecordTransformer(), 0,
                    BasicDataRecord.fromMap(values));

            return new DataRecords.Parent(context, dataRecord);
        }

        InternalScenarioContext child() {
            return new InternalScenarioContext(values);
        }

        void setFieldValue(String name, Object value) {
            values.put(name, value);
        }

        void setFieldValues(DataRecord dataRecord) {
            values.putAll(dataRecord.getAllFields());
        }

        void parseValues(String name, Object values) {
            if (values instanceof DataRecord) {
                setFieldValue(name, values);
                setFieldValues(DataRecord.class.cast(values));
            } else if (Bridge.isCollectionOfDataRecords(values)) {
                for (Object dataRecord : Collection.class.cast(values)) {
                    setFieldValues(DataRecord.class.cast(dataRecord));
                }
            } else {
                setFieldValue(name, values);
            }
        }

        boolean storeVUsers() {
            return hasKey(STORE_V_USERS_IN_CONTEXT);
        }

        private boolean hasKey(String key) {
            return Boolean.valueOf("" + values.get(key));
        }
    }

    static class TestStepResult {
        final String id;
        final String name;
        final long startTime;
        final long endTime;
        final private Object returnedValue;
        final Throwable error;
        final TestStep.Status status;

        static TestStepResult success(TestStep testStep, long startTime, Object returnedValue, VUser vUser, String iteration) {
            return new TestStepResult(id("step", testStep.getId(), vUser, iteration), testStep.getName(), startTime, System.currentTimeMillis(),
                    returnedValue, null, TestStep.Status.SUCCESS);
        }

        static TestStepResult failure(TestStep testStep, long startTime, Throwable error, VUser vUser, String iteration) {
            return new TestStepResult(id("step", testStep.getId(), vUser, iteration), testStep.getName(), startTime, System.currentTimeMillis(), null,
                    error, TestStep.Status.FAILED);
        }

        static TestStepResult skipped(TestStep testStep, long startTime, VUser vUser, String iteration) {
            return new TestStepResult(id("step", testStep.getId(), vUser, iteration), testStep.getName(), startTime, System.currentTimeMillis(), null,
                    null, TestStep.Status.SKIPPED);
        }

        TestStepResult(String id, String name, long startTime, long endTime, Object returnedValue, Throwable error, TestStep.Status status) {
            this.id = id;
            this.name = name;
            this.startTime = startTime;
            this.endTime = endTime;
            this.returnedValue = returnedValue;
            this.error = error;
            this.status = status;
        }

        static String id(String type, Long id, VUser vUser, String iteration) {
            return String.format("%s%d-%s-%s", type, id, vUser, iteration);
        }

        boolean isFailed() {
            return error != null;
        }

        Optional<Object> getReturnedValue() {
            return fromNullable(returnedValue);
        }

    }

    static class FlowExecutionResult extends TestStepResult {
        final RxFlow rxFlow;
        final List<Exec> executions = newArrayList();

        FlowExecutionResult(Collection<Exec> executions) {
            this(null, "split", null, executions);
        }

        FlowExecutionResult(RxFlow rxFlow, Collection<Exec> executions) {
            this(rxFlow.id, rxFlow.getName(), rxFlow, executions);
        }

        private FlowExecutionResult(Long id, String name, RxFlow rxFlow, Collection<Exec> executions) {
            super(flowId(id, executions), name, RxDataSource.startTime(executions), RxDataSource.endTime(executions), null, getError(executions),
                    TestStep.Status.SUCCESS);
            this.rxFlow = rxFlow;
            this.executions.addAll(executions);
        }

        private static String flowId(Long id, Collection<Exec> executions) {
            StringBuilder result = new StringBuilder("fork");
            for (Exec execution : executions) {
                result.append(id("", id, execution.vUser, execution.dataRecord.getIteration()));
            }
            return result.toString();

        }

        private static Throwable getError(Collection<Exec> executions) {
            Set<Throwable> errors = newLinkedHashSet();
            for (Exec execution : executions) {
                errors.addAll(execution.errors);
            }
            return RxDataSource.compose(errors);
        }

        @Override
        public String toString() {
            return toStringHelper(this).add("id", id).add("name", name).add("rxFlow", rxFlow).toString();
        }
    }

    static class VUser {

        static final VUser ROOT = new VUser();

        private final int[] tokens;

        private VUser(int... numbers) {
            tokens = numbers;
        }

        VUser child(int number) {
            int[] childId = copyOf(tokens, tokens.length + 1);
            childId[tokens.length] = number;
            return new VUser(childId);
        }

        boolean isScenarioLevel() {
            return tokens.length == 1;
        }

        public String getId() {
            return Joiner.on(".").join(Ints.asList(tokens));
        }

        @Override
        public String toString() {
            return getId();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof VUser))
                return false;
            VUser vUser = (VUser) o;
            return Arrays.equals(tokens, vUser.tokens);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(tokens);
        }
    }

    static class ClosableScheduler implements AutoCloseable {
        final Scheduler scheduler;

        ClosableScheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public void close() {
            // do nothing
        }
    }

    static class ThreadPoolScheduler extends ClosableScheduler {
        private ExecutorService executor;

        ThreadPoolScheduler(Scheduler from, ExecutorService executor) {
            super(from);
            this.executor = executor;
        }

        @Override
        public void close() {
            try {
                executor.shutdown();

                boolean successful = executor.awaitTermination(30, SECONDS);
                if (!successful) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
