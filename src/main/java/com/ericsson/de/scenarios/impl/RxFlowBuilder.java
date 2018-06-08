package com.ericsson.de.scenarios.impl;

import static java.lang.Math.min;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ericsson.de.scenarios.api.Builder;
import com.ericsson.de.scenarios.api.DataRecordWrapper;
import com.ericsson.de.scenarios.api.DataSource;
import com.ericsson.de.scenarios.api.ExceptionHandler;
import com.ericsson.de.scenarios.api.FlowBuilderInterfaces;
import com.ericsson.de.scenarios.api.ScenarioBuilder;
import com.ericsson.de.scenarios.api.ScenarioRunnerBuilder;
import com.ericsson.de.scenarios.api.TestStep;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

/**
 * Builder for {@link RxFlow}
 */
@SuppressWarnings("WeakerAccess")
public abstract class RxFlowBuilder<T extends RxFlow> implements FlowBuilderInterfaces.FlowBuilderStates<T> {

    protected static final String DEFAULT_NAME = "fork";

    private static final int V_USERS_PER_PROCESSOR = 8;

    static final String ERROR_V_USERS_NOT_ONCE = "Methods withVUsers()/withVUsersAuto() can be called just once per rxFlow";
    static final String ERROR_V_USERS_NEGATIVE = "vUser value should be greater than zero";
    static final String ERROR_V_USERS_AUTO_NO_DATA_SOURCES = "Method withVUsersAuto() can't be used without any data sources";
    static final String ERROR_V_USERS_AUTO_NON_SHARED = "Method withVUsersAuto() must be used with shared Data Sources only. ";
    static final String HINT_NON_SHARED = "The following Data Sources are not shared: ";

    static final String ERROR_DATA_SOURCES_NOT_ONCE = "Method withDataSources() can be called just once per rxFlow";
    static final String ERROR_DATA_SOURCES_NULL = "Data Sources can't be null";
    static final String ERROR_DATA_SOURCES_EMPTY = "Data Sources can't be empty";
    static final String ERROR_DATA_SOURCE_NULL = "Data Source can't be null";
    static final String ERROR_DATA_SOURCE_TOO_CYCLIC = "At least one of data sources defined on one rxFlow should be not cyclic to avoid forever "
            + "loop";

    static final String ERROR_TEST_STEP_NULL = "Test step can't be null";
    static final String ERROR_WITH_BEFORE_NOT_ONCE = "Method withBefore() can be called just once per rxFlow. ";
    static final String ERROR_WITH_AFTER_NOT_ONCE = "Method withAfter() can be called just once per rxFlow. ";
    static final String HINT_SINGLE_CALL = "Please pass (several test steps if required) into single method call.";

    static final String ERROR_SUBFLOWS_NULL = "Sub Flows can't be null";
    static final String ERROR_SUBFLOW_NULL = "Sub RxFlow can't be null";

    public static final String ERROR_EXCEPTION_HANDLER_NULL = "ExceptionHandler can't be null";
    public static final String ERROR_EXCEPTION_HANDLER_NOT_ONCE = "ExceptionHandler can't be set twice. ";
    public static final String HINT_EXCEPTION_HANDLER = "In case you need multiple exception handlers use Api.compositeExceptionHandler()";
    public static final String ERROR_PREDICATE_NULL = "Predicate defined can not be null";
    protected final String name;

    private Integer vUsers = null;
    private boolean vUsersAuto = false;
    private DataSource[] dataSources = null;
    private final List<Invocation> testSteps = newArrayList();
    private Predicate<DataRecordWrapper> predicate = null;

    private List<TestStep> beforeInvocation = new ArrayList<>();
    private List<TestStep> afterInvocation = new ArrayList<>();

    protected RxExceptionHandler exceptionHandler;

    protected RxFlowBuilder(String name) {
        this.name = name;
    }

    /**
     * Continuously executes a rxFlow. Terminates once predicate is no longer satisfied.<br/>
     * <b>Note:</b> If Predicate returns false on first run of rxFlow, rxFlow execution will be skipped.
     *
     * @param predicate
     *         User defined predicate
     *
     * @return builder
     * @see RxApi#during(Integer, TimeUnit)
     */
    @Override
    public FlowBuilderInterfaces.Options<T> runWhile(Predicate<DataRecordWrapper> predicate) {
        checkNotNull(predicate, ERROR_PREDICATE_NULL);
        this.predicate = predicate;
        return this;
    }

    /**
     * Sets concurrency level for current RxFlow. RxFlow will be executed in parallel for each vUser
     *
     * @param vUsers
     *         number of vUsers
     *
     * @return builder
     */
    @Override
    public FlowBuilderInterfaces.Options<T> withVUsers(int vUsers) {
        checkVUsersNotSet();
        checkArgument(vUsers > 0, ERROR_V_USERS_NEGATIVE);
        this.vUsers = vUsers;
        return this;
    }

    @Override
    public FlowBuilderInterfaces.Options<T> withVUsersAuto() {
        checkVUsersNotSet();
        vUsersAuto = true;
        return this;
    }

    private void checkVUsersNotSet() {
        checkState(this.vUsers == null, ERROR_V_USERS_NOT_ONCE);
        checkState(!vUsersAuto, ERROR_V_USERS_NOT_ONCE);
    }

    /**
     * Add Data Sources to RxFlow. RxFlow will repeated with each Data Record
     *
     * @param dataSources
     *         Data Sources
     *
     * @return builder
     */
    @Override
    public FlowBuilderInterfaces.Options<T> withDataSources(DataSource... dataSources) {
        checkDataSources(dataSources);
        this.dataSources = dataSources;
        return this;
    }

    private void checkDataSources(DataSource[] dataSources) {
        checkState(this.dataSources == null, ERROR_DATA_SOURCES_NOT_ONCE);
        checkNotNull(dataSources, ERROR_DATA_SOURCES_NULL);
        checkArgument(dataSources.length > 0, ERROR_DATA_SOURCES_EMPTY);
        boolean atLeastOneNotCyclic = false;
        for (DataSource dataSource : dataSources) {
            checkNotNull(dataSource, ERROR_DATA_SOURCE_NULL);
            if (!dataSource.isCyclic()) {
                atLeastOneNotCyclic = true;
            }
        }
        checkArgument(atLeastOneNotCyclic, ERROR_DATA_SOURCE_TOO_CYCLIC);
    }

    /**
     * Adds a test step as the last one in this rxFlow.
     *
     * @param testStep
     *         test step to add
     *
     * @return builder
     */
    @Override
    public FlowBuilderInterfaces.Steps<T> addTestStep(TestStep testStep) {
        checkNotNull(testStep, ERROR_TEST_STEP_NULL);
        return addInvocation(testStep);
    }

    /**
     * @deprecated For migration simplification only. Please use {@link #withBefore(TestStep...)}
     */
    @Override
    @Deprecated
    public FlowBuilderInterfaces.Before<T> beforeFlow(Runnable... runnables) {
        return withBefore(toTestSteps(runnables));
    }

    /**
     * Run given steps before rxFlow. Will be run once not depending on vUser count or Data Sources
     *
     * @param testStep
     *         to run
     *
     * @return builder
     */
    @Override
    public FlowBuilderInterfaces.Before<T> withBefore(TestStep... testStep) {
        checkState(beforeInvocation.isEmpty(), ERROR_WITH_BEFORE_NOT_ONCE + HINT_SINGLE_CALL);
        beforeInvocation = asList(testStep);
        return this;
    }

    /**
     * @deprecated For migration simplification only. Please use {@link #withAfter(TestStep...)}
     */
    @Override
    @Deprecated
    public FlowBuilderInterfaces.After<T> afterFlow(Runnable... runnables) {
        return withAfter(toTestSteps(runnables));
    }

    /**
     * Run given steps after rxFlow. Will be run once not depending on vUser count or Data Sources
     *
     * @param testStep
     *         to run
     *
     * @return builder
     */
    @Override
    public FlowBuilderInterfaces.After<T> withAfter(TestStep... testStep) {
        checkState(afterInvocation.isEmpty(), ERROR_WITH_AFTER_NOT_ONCE + HINT_SINGLE_CALL);
        afterInvocation = asList(testStep);
        return this;
    }

    private TestStep[] toTestSteps(Runnable[] runnables) {
        TestStep[] testSteps = new TestStep[runnables.length];
        int i = 0;
        for (Runnable runnable : runnables) {
            testSteps[i++] = RxApi.runnable(runnable);
        }
        return testSteps;
    }

    /**
     * @see #addSubFlow(RxFlow subFlow)
     */
    @Override
    public FlowBuilderInterfaces.Steps<T> addSubFlow(Builder<T> subFlow) {
        checkNotNull(subFlow, ERROR_SUBFLOW_NULL);
        return addSubFlow(subFlow.build());
    }

    /**
     * Adds a subFlow as a subflow to this RxFlow.
     *
     * @param subFlow
     *         to add
     *
     * @return builder
     */
    @Override
    public FlowBuilderInterfaces.Steps<T> addSubFlow(T subFlow) {
        checkNotNull(subFlow, ERROR_SUBFLOW_NULL);
        return split(subFlow);
    }

    /**
     * @return builder
     * @see #split(RxFlow...)
     */
    @Override
    public FlowBuilderInterfaces.Steps<T> split(Builder<T>... builders) {
        checkNotNull(builders, ERROR_SUBFLOWS_NULL);
        T[] flows = createFlowArray(builders.length);
        for (int i = 0; i < builders.length; i++) {
            checkNotNull(builders[i], ERROR_SUBFLOW_NULL);
            flows[i] = builders[i].build();
        }
        return split(flows);
    }

    protected abstract T[] createFlowArray(int length);

    /**
     * Run given flows in parallel
     *
     * @param parallelRxFlows
     *         rxFlow to run in parallel
     *
     * @return builder
     */
    @Override
    public FlowBuilderInterfaces.Steps<T> split(RxFlow... parallelRxFlows) {
        checkNotNull(parallelRxFlows, ERROR_SUBFLOWS_NULL);
        for (RxFlow rxFlow : parallelRxFlows) {
            checkNotNull(rxFlow, ERROR_SUBFLOW_NULL);
        }
        Internals.Fork fork = new Internals.Fork(newArrayList(parallelRxFlows));
        return addInvocation(fork);
    }

    private RxFlowBuilder<T> addInvocation(Invocation invocation) {
        testSteps.add(invocation);
        return this;
    }

    /**
     * Test Step added rxFlow will be run even if previous Test Step threw an un-handled exception.
     *
     * @return builder
     */
    @Override
    public FlowBuilderInterfaces.AlwaysRun<T> alwaysRun() {
        Invocation lastInvocation = testSteps.remove(testSteps.size() - 1);
        testSteps.add(lastInvocation.alwaysRun());
        return this;
    }

    /**
     * Set an exception handler for the RxFlow, which will be called on exceptions in Test Steps.
     * If the exception handler does not propagate Exception, scenario rxFlow will continue.
     *
     * @see ScenarioRunnerBuilder#withDefaultExceptionHandler(ExceptionHandler)
     * @see ScenarioBuilder#withExceptionHandler(ExceptionHandler)
     */
    public FlowBuilderInterfaces.Options<T> withExceptionHandler(ExceptionHandler exceptionHandler) {
        checkNotNull(exceptionHandler, ERROR_EXCEPTION_HANDLER_NULL);
        checkState(this.exceptionHandler == null, ERROR_EXCEPTION_HANDLER_NOT_ONCE + HINT_EXCEPTION_HANDLER);
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    /**
     * @return RxFlow
     */
    @Override
    public T build() {
        vUsers = calculateVUsers();
        DataSourceStrategy dataSource = (dataSources == null) ?
                DataSourceStrategy.empty(name, vUsers) :
                DataSourceStrategy.fromDefinitions(dataSources, vUsers);

        return createFlow(name, dataSource, testSteps, beforeInvocation, afterInvocation, exceptionHandler, predicate);
    }

    protected abstract T createFlow(String name, DataSourceStrategy dataSource, List<Invocation> testSteps, List<TestStep> beforeInvocations,
            List<TestStep> afterInvocations, RxExceptionHandler exceptionHandler, Predicate<DataRecordWrapper> predicate);

    private int calculateVUsers() {
        if (vUsersAuto) {
            int minDataRecords = minDataRecords();
            int vUserThreshold = vUserThreshold();
            return min(minDataRecords, vUserThreshold);
        } else if (vUsers == null) {
            return 1;
        } else {
            return vUsers;
        }
    }

    private int minDataRecords() {
        checkState(dataSources != null, ERROR_V_USERS_AUTO_NO_DATA_SOURCES);

        int minDataRecords = Integer.MAX_VALUE;
        List<String> nonSharedDSNames = newArrayList();
        for (DataSource dataSource : dataSources) {
            if (!dataSource.isShared()) {
                nonSharedDSNames.add(dataSource.getName());
            } else if (!dataSource.isCyclic()) {
                Preconditions.checkArgument(dataSource.getSize() > 0, format(DataSourceStrategy.ERROR_DATA_SOURCE_EMPTY, dataSource.getName()));
                minDataRecords = min(minDataRecords, dataSource.getSize());
            }
        }

        checkState(nonSharedDSNames.isEmpty(), ERROR_V_USERS_AUTO_NON_SHARED + HINT_NON_SHARED + Joiner.on(',').join(nonSharedDSNames));

        return minDataRecords;
    }

    @VisibleForTesting
    int vUserThreshold() {
        int numProcessors = getRuntime().availableProcessors();
        return V_USERS_PER_PROCESSOR * numProcessors;
    }
}
