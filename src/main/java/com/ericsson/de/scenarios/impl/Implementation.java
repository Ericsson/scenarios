package com.ericsson.de.scenarios.impl;

import static java.util.Collections.singletonList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static com.google.common.collect.Lists.newArrayList;

import static rx.functions.Actions.empty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.ericsson.de.scenarios.api.TestStep;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Table;

import rx.Observable;
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

class Implementation {

    static Observable<Internals.FlowExecutionResult> runFlow(FlowExecutionContext context) {
        BehaviorSubject<Object> noErrors = BehaviorSubject.create();
        return context.dataSource.repeat().takeWhile(predicateIsTrue(context)).takeUntil(noErrors).buffer(context.vUsers).map(context.toExecutions)
                .doOnNext(runBefore(context)).doOnNext(runChunksOfFlow(context)).lift(runAfter(context))
                .doOnNext(processErrors(noErrors, context.exceptionHandler())).map(executorsToResult(context.rxFlow));
    }

    static List<Internals.TestStepResult> runFlowPerformance(final FlowExecutionContext context, RxRampUp.StrategyProvider rampupProvider) {
        final int vUsers = context.vUsers;
        try (Internals.ClosableScheduler scheduler = getScheduler(vUsers)) {

            final Internals.Exec exec = Internals.Exec.rootExec(Maps.<String, Object>newHashMap());

            BehaviorSubject<Object> noErrors = BehaviorSubject.create();

            return context.dataSource.repeat().takeWhile(predicateIsTrue(context)).takeUntil(noErrors)
                    .map(toExecution(context, vUsers, exec, rampupProvider))
                    .flatMap(runTestSteps(context, singleChunk(context.rxFlow), scheduler.scheduler), vUsers).toList().toBlocking().single();
        }
    }

    private static Func1<DataRecordWrapper, Internals.Exec> toExecution(final FlowExecutionContext context, final int vUsers,
            final Internals.Exec exec, final RxRampUp.StrategyProvider rampupProvider) {
        return new Func1<DataRecordWrapper, Internals.Exec>() {
            final RxRampUp.Strategy rampUp = rampupProvider.provideFor(vUsers);
            final Iterator<Integer> vUserIterator = getVUserIterator(context);
            final AtomicLong startDelay = new AtomicLong();
            final AtomicInteger currentUser = new AtomicInteger();

            @Override
            public Internals.Exec call(DataRecordWrapper dataRecordWrapper) {
                Internals.Exec child = exec.child(context.rxFlow.getName(), vUserIterator.next(), dataRecordWrapper);

                if (currentUser.getAndIncrement() < vUsers) {
                    long delay = startDelay.getAndAdd(rampUp.nextVUserDelayDelta());
                    child.setDelay(delay);
                }

                return child;
            }
        };
    }

    private static List<TestStep> singleChunk(RxFlow rxFlow) {
        ArrayList<TestStep> objects = newArrayList();

        for (Invocation testStep : rxFlow.testSteps) {
            Preconditions.checkArgument(testStep instanceof TestStep, "RxFlow should not contain subflows!");
            objects.add(TestStep.class.cast(testStep));
        }

        return objects;
    }

    private static Iterator<Integer> getVUserIterator(FlowExecutionContext context) {
        return Observable.range(1, context.vUsers).repeat().toBlocking().getIterator();
    }

    private static Func1<DataRecordWrapper, Boolean> predicateIsTrue(final FlowExecutionContext context) {
        return new Func1<DataRecordWrapper, Boolean>() {
            @Override
            public Boolean call(DataRecordWrapper dataRecordWrapper) {
                return context.predicate.apply(dataRecordWrapper);
            }
        };
    }

    private static Action1<List<Internals.Exec>> runBefore(final FlowExecutionContext context) {
        final ArrayList<TestStep> flowEventList = Lists.newArrayList((TestStep) new Internals.BeforeTestStepEvent(context));
        flowEventList.addAll(context.rxFlow.getBefore());
        return runBeforeOrAfter(context, flowEventList);
    }

    private static AfterOperator runAfter(final FlowExecutionContext context) {
        final ArrayList<TestStep> flowEventList = Lists.newArrayList((TestStep) new Internals.AfterTestStepEvent(context));
        flowEventList.addAll(context.rxFlow.getAfter());
        return new AfterOperator(runBeforeOrAfter(context, flowEventList));
    }

    private static Action1<List<Internals.Exec>> runBeforeOrAfter(final FlowExecutionContext context, final List<TestStep> beforeAfterTestSteps) {
        if (beforeAfterTestSteps.isEmpty()) {
            return empty();
        }

        return new Action1<List<Internals.Exec>>() {

            private boolean called = false;

            @Override
            public void call(List<Internals.Exec> executions) {
                // for before ONLY
                if (called) {
                    return;
                }
                called = true;

                for (TestStep testStep : beforeAfterTestSteps) {
                    DataRecords.Forbidden dataRecord = new DataRecords.Forbidden("Before or After Data Source");
                    Internals.TestStepResult beforeResult = runTestStep(context, testStep, Internals.VUser.ROOT, dataRecord);
                    if (!(testStep instanceof Internals.ControlTestStep)) {
                        addTestStepToGraph(executions, testStep, dataRecord, beforeResult);
                    }

                }
            }
        };
    }

    private static void addTestStepToGraph(List<Internals.Exec> executions, TestStep testStep, DataRecords.Forbidden dataRecord,
            Internals.TestStepResult beforeResult) {
        boolean first = true;
        for (Internals.Exec exec : executions) {
            if (!first) {
                beforeResult = Internals.TestStepResult
                        .skipped(testStep, System.currentTimeMillis(), Internals.VUser.ROOT, dataRecord.getIteration());
            }
            exec.addExecutedTestStep(beforeResult);
            first = false;
        }
    }

    private static Action1<List<Internals.Exec>> runChunksOfFlow(final FlowExecutionContext context) {
        return new Action1<List<Internals.Exec>>() {
            @Override
            public void call(List<Internals.Exec> executions) {
                try (ExceptionAccumulator exceptionAccumulator = new ExceptionAccumulator()) {
                    context.rxFlow.chunks().doOnNext(runInParallel(context, executions)).doOnNext(fork(context, executions))
                            .subscribe(exceptionAccumulator);
                }
            }
        };
    }

    /**
     * Runs Test Steps of {@code chunk} in parallel by multiple {@code executions} (vUsers)
     * Waits until execution is finished
     * Because each Test Step runner has its own executor, it will run in parallel
     */
    static Action1<Internals.Chunk> runInParallel(final FlowExecutionContext context, final List<Internals.Exec> executions) {
        return new Action1<Internals.Chunk>() {
            @Override
            public void call(Internals.Chunk chunk) {
                int vUsers = executions.size();
                try (Internals.ClosableScheduler scheduler = getScheduler(vUsers);
                        ExceptionAccumulator exceptionAccumulator = new ExceptionAccumulator()) {
                    Observable.from(executions).flatMap(runTestSteps(context, chunk.testSteps, scheduler.scheduler), vUsers).toBlocking()
                            .subscribe(exceptionAccumulator);
                }
            }
        };
    }

    /**
     * Runs {@code testSteps} in thread provided by {@code scheduler} for given vUser ({@code execution})
     * If {@code execution} is broken will execute only alwaysRun Test Steps
     */
    private static Func1<Internals.Exec, Observable<Internals.TestStepResult>> runTestSteps(final FlowExecutionContext context,
            final List<TestStep> testSteps, final Scheduler scheduler) {
        return new Func1<Internals.Exec, Observable<Internals.TestStepResult>>() {
            @Override
            public Observable<Internals.TestStepResult> call(final Internals.Exec execution) {
                return Observable.from(testSteps).filter(new Func1<TestStep, Boolean>() {
                    @Override
                    public Boolean call(TestStep testStep) {
                        return !execution.isFailed() || testStep.isAlwaysRun();
                    }
                }).doOnNext(sleep(execution.getDelay())).map(runTestStep(context, execution)).doOnNext(new Action1<Internals.TestStepResult>() {
                    @Override
                    public void call(Internals.TestStepResult result) {
                        execution.addExecutedTestStep(result);
                    }
                }).subscribeOn(scheduler);
            }
        };
    }

    /**
     * {@link Observable#delay(long, java.util.concurrent.TimeUnit)} runs stuff on different scheduler
     * {@link Observable#delay(long, java.util.concurrent.TimeUnit, Scheduler scheduler)} do non blocking schedule
     * To actually delay execution use this method
     */
    private static Action1<? super Object> sleep(final long delay) {
        return new Action1<Object>() {
            @Override
            public void call(Object o) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Run single testStep
     */
    private static Func1<TestStep, Internals.TestStepResult> runTestStep(final FlowExecutionContext context, final Internals.Exec execution) {
        return new Func1<TestStep, Internals.TestStepResult>() {
            @Override
            public Internals.TestStepResult call(TestStep testStep) {
                context.eventBus.testStepStarted(testStep, execution);
                Internals.TestStepResult result = runTestStep(context, testStep, execution.vUser, execution.getDataRecordAndContext());
                context.eventBus.testStepFinished(testStep, execution, result);
                return result;
            }
        };
    }

    private static Internals.TestStepResult runTestStep(FlowExecutionContext context, TestStep testStep, Internals.VUser vUser,
            DataRecordWrapper dataRecord) {
        long startTime = System.currentTimeMillis();
        try {
            Optional<Object> returnedValue = testStep.run(dataRecord);
            return Internals.TestStepResult.success(testStep, startTime, returnedValue.orNull(), vUser, dataRecord.getIteration());
        } catch (Throwable e) {
            return context.exceptionHandler().canHandle(e) ?
                    Internals.TestStepResult.skipped(testStep, startTime, vUser, dataRecord.getIteration()) :
                    Internals.TestStepResult
                            .failure(testStep, startTime, StackTraceFilter.filterTestwareStackTrace(e), vUser, dataRecord.getIteration());
        }
    }

    /**
     * If {@code chunk} contains subflow create and subscribe to observable that will execute subflow
     * If there are multiple flows in fork (i.e. main rxFlow splits to two different subflow1 and subflow2) run them
     * in parallel
     */
    private static Action1<Internals.Chunk> fork(final FlowExecutionContext context, final List<Internals.Exec> executions) {
        return new Action1<Internals.Chunk>() {
            @Override
            public void call(Internals.Chunk chunk) {
                List<Internals.Exec> validExecutions = notFailedExecs(executions);

                if (chunk.fork != null && (chunk.fork.alwaysRun || !validExecutions.isEmpty())) {
                    List<Internals.Exec> executionsToRun = chunk.fork.alwaysRun ? executions : validExecutions;
                    try (Internals.ClosableScheduler scheduler = getScheduler(chunk.fork.flows.size());
                            ExceptionAccumulator exceptionAccumulator = new ExceptionAccumulator(attachResultsToParents(context))) {
                        AtomicInteger vUserOffset = new AtomicInteger();

                        Observable.from(chunk.fork.flows).flatMap(subflowToObservable(context, executionsToRun, scheduler.scheduler, vUserOffset))
                                .toList().toBlocking().subscribe(exceptionAccumulator);
                    }
                }
            }
        };
    }

    /**
     * Creates observable from {@code subFlow}, based on parent executions validExecutions
     * Will execute on Scheduler so rxFlow will run in parallel
     *
     * @return function to create Observable
     */
    private static Func1<RxFlow, Observable<Internals.FlowExecutionResult>> subflowToObservable(final FlowExecutionContext context,
            final List<Internals.Exec> validExecutions, final Scheduler scheduler, final AtomicInteger vUserOffset) {
        return new Func1<RxFlow, Observable<Internals.FlowExecutionResult>>() {
            @Override
            public Observable<Internals.FlowExecutionResult> call(RxFlow subRxFlow) {
                int vUserOffsetInt = vUserOffset.getAndAdd(subRxFlow.dataSource.vUsers);
                FlowExecutionContext subFlowContext = context.subFlow(subRxFlow, validExecutions, vUserOffsetInt);
                return runFlow(subFlowContext).subscribeOn(scheduler);
            }
        };
    }

    /**
     * Attaches results of subFlow execution to parent {@link Internals.Exec#executedTestSteps}
     */
    private static Action1<List<Internals.FlowExecutionResult>> attachResultsToParents(final FlowExecutionContext context) {
        return new Action1<List<Internals.FlowExecutionResult>>() {
            @Override
            public void call(List<Internals.FlowExecutionResult> allResults) {
                Table<Internals.Exec, Long, List<Internals.FlowExecutionResult>> parentsFlowsExecution = groupByParentsAndFlows(allResults);
                for (Map.Entry<Internals.Exec, Map<Long, List<Internals.FlowExecutionResult>>> parentToFlows : parentsFlowsExecution.rowMap()
                        .entrySet()) {
                    Internals.Exec parent = parentToFlows.getKey();
                    Map<Long, List<Internals.FlowExecutionResult>> flowsToResults = parentToFlows.getValue();
                    List<Internals.FlowExecutionResult> resultsToAttach = resultsToAttach(flowsToResults);
                    attachResultsToParent(context, parent, resultsToAttach);
                }
            }
        };
    }

    private static Table<Internals.Exec, Long, List<Internals.FlowExecutionResult>> groupByParentsAndFlows(
            List<Internals.FlowExecutionResult> results) {
        Table<Internals.Exec, Long, List<Internals.FlowExecutionResult>> parentsFlowsExecution = HashBasedTable.create();

        for (Internals.FlowExecutionResult result : results) {
            ImmutableListMultimap<Internals.Exec, Internals.Exec> groupByParents = Multimaps
                    .index(result.executions, new Function<Internals.Exec, Internals.Exec>() {
                        @Override
                        public Internals.Exec apply(Internals.Exec e) {
                            return e.parent;
                        }
                    });
            for (Map.Entry<Internals.Exec, Collection<Internals.Exec>> parentToExecutions : groupByParents.asMap().entrySet()) {
                Internals.Exec parent = parentToExecutions.getKey();
                Collection<Internals.Exec> execs = parentToExecutions.getValue();

                if (!parentsFlowsExecution.contains(parent, result.rxFlow.id)) {
                    parentsFlowsExecution.put(parent, result.rxFlow.id, Lists.<Internals.FlowExecutionResult>newArrayList());
                }

                parentsFlowsExecution.get(parent, result.rxFlow.id).add(new Internals.FlowExecutionResult(result.rxFlow, execs));
            }
        }
        return parentsFlowsExecution;
    }

    private static List<Internals.FlowExecutionResult> resultsToAttach(Map<Long, List<Internals.FlowExecutionResult>> flowsToResults) {
        return (flowsToResults.size() == 1) ? first(flowsToResults.values()) : getSplitResults(flowsToResults);
    }

    private static List<Internals.FlowExecutionResult> getSplitResults(Map<Long, List<Internals.FlowExecutionResult>> flowsToResults) {
        List<Internals.Exec> execs = newArrayList();
        TreeSet<Long> flowIds = new TreeSet<>(flowsToResults.keySet());
        for (Long flowId : flowIds) {
            List<Internals.FlowExecutionResult> results = flowsToResults.get(flowId);
            Internals.Exec firstExec = first(results).executions.get(0);
            Internals.Exec exec = firstExec.copy();
            for (Internals.FlowExecutionResult result : results) {
                exec.addExecutedTestStep(result);
            }
            execs.add(exec);
        }
        return singletonList(new Internals.FlowExecutionResult(execs));
    }

    private static void attachResultsToParent(FlowExecutionContext context, Internals.Exec parent, List<Internals.FlowExecutionResult> results) {
        for (Internals.FlowExecutionResult result : results) {
            parent.addExecutedTestStep(result);
            if (result.isFailed() && context.exceptionHandler().canHandle(result.error)) {
                parent.errors.remove(result.error);
            }
        }
    }

    /**
     * If there are any errors, push them to observable
     */
    private static Action1<List<Internals.Exec>> processErrors(final BehaviorSubject<Object> observableToPutErrorsInto,
            final RxExceptionHandler exceptionHandler) {
        return new Action1<List<Internals.Exec>>() {
            @Override
            public void call(List<Internals.Exec> execs) {
                if (!exceptionHandler.continueOnNextDataRecord()) {
                    for (Internals.Exec exec : execs) {
                        if (exec.isFailed()) {
                            for (Throwable ignored : exec.errors) {
                                observableToPutErrorsInto.onNext(exec.errors);
                            }
                        }
                    }
                }
            }
        };
    }

    private static Func1<List<Internals.Exec>, Internals.FlowExecutionResult> executorsToResult(final RxFlow rxFlow) {
        return new Func1<List<Internals.Exec>, Internals.FlowExecutionResult>() {
            @Override
            public Internals.FlowExecutionResult call(List<Internals.Exec> executions) {
                return new Internals.FlowExecutionResult(rxFlow, executions);
            }
        };
    }

    private static ArrayList<Internals.Exec> notFailedExecs(List<Internals.Exec> executions) {
        return Lists.newArrayList(Iterables.filter(executions, new Predicate<Internals.Exec>() {
            @Override
            public boolean apply(Internals.Exec exec) {
                return !exec.isFailed();
            }
        }));
    }

    private static Internals.ClosableScheduler getScheduler(int size) {
        if (size == 1) {
            return new Internals.ClosableScheduler(Schedulers.immediate());
        } else {
            ExecutorService executor = Executors.newFixedThreadPool(size);
            return new Internals.ThreadPoolScheduler(Schedulers.from(executor), executor);
        }
    }

    private static <T> T first(Iterable<T> iterable) {
        Iterator<T> iterator = iterable.iterator();
        checkArgument(iterator.hasNext());
        return iterator.next();
    }

    static class ExceptionAccumulator extends Subscriber implements AutoCloseable {
        Set<Throwable> throwables = new HashSet<>();
        private Action1 doOnNext;

        ExceptionAccumulator() {
            this(Actions.empty());
        }

        ExceptionAccumulator(Action1 doOnNext) {
            this.doOnNext = doOnNext;
        }

        @Override
        public void onCompleted() {
            // do nothing
        }

        @Override
        public void onError(Throwable e) {
            throwables.add(e);
        }

        @Override
        public void onNext(Object o) {
            doOnNext.call(o);
        }

        @Override
        public void close() {
            if (!throwables.isEmpty()) {
                Throwable compose = RxDataSource.compose(throwables);
                throwIfUnchecked(StackTraceFilter.filterFrameworkStackTrace(compose));
                throw StackTraceFilter.filterFrameworkStackTrace(new RuntimeException(compose));
            }
        }
    }

    static class AfterOperator implements Operator<List<Internals.Exec>, List<Internals.Exec>> {

        private final Action1<List<Internals.Exec>> afterAction;

        AfterOperator(Action1<List<Internals.Exec>> afterAction) {
            this.afterAction = afterAction;
        }

        @Override
        public Subscriber<? super List<Internals.Exec>> call(final Subscriber<? super List<Internals.Exec>> delegate) {
            return new Subscriber<List<Internals.Exec>>(delegate) {
                private List<Internals.Exec> execs;

                @Override
                public void onNext(List<Internals.Exec> execs) {
                    // cache execs
                    this.execs = execs;

                    // default
                    if (!delegate.isUnsubscribed()) {
                        delegate.onNext(execs);
                    }
                }

                @Override
                public void onCompleted() {
                    // as last step execute after method(-s)
                    afterAction.call(execs);

                    // default
                    if (!delegate.isUnsubscribed()) {
                        delegate.onCompleted();
                    }
                }

                @Override
                public void onError(Throwable t) {
                    if (!delegate.isUnsubscribed()) {
                        delegate.onError(t);
                    }
                }
            };
        }
    }
}
