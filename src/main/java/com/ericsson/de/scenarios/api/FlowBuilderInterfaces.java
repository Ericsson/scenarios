package com.ericsson.de.scenarios.api;

import com.ericsson.de.scenarios.impl.RxFlow;
import com.google.common.base.Predicate;

/**
 * Determines the order in which RxFlow builder methods can be called.
 */
@SuppressWarnings("WeakerAccess")
public final class FlowBuilderInterfaces {

    private FlowBuilderInterfaces() {
    }

    public interface FlowBuilderStates<T extends RxFlow> extends FlowStart<T>, Before<T>, Steps<T>, AlwaysRun<T>, After<T>, Options<T> {

    }

    /*---------------- STATE INTERFACES ----------------*/

    public interface FlowStart<T extends RxFlow> extends ToBefore<T>, ToSteps<T> {

    }

    public interface Before<T extends RxFlow> extends ToSteps<T> {

    }

    public interface Steps<T extends RxFlow> extends ToSteps<T>, ToAfter<T>, ToOptions<T>, Builder<T> {

        AlwaysRun<T> alwaysRun();

    }

    public interface AlwaysRun<T extends RxFlow> extends ToSteps<T>, ToAfter<T>, ToOptions<T>, Builder<T> {

    }

    public interface After<T extends RxFlow> extends ToOptions<T>, Builder<T> {

    }

    public interface Options<T extends RxFlow> extends ToOptions<T>, Builder<T> {

    }

    /*---------------- DESTINATION INTERFACES ----------------*/

    private interface ToBefore<T extends RxFlow> {

        Before<T> beforeFlow(Runnable... runnables);

        Before<T> withBefore(TestStep... testStep);

    }

    private interface ToSteps<T extends RxFlow> {

        Steps<T> addTestStep(TestStep testStep);

        Steps<T> addSubFlow(Builder<T> subFlow);

        Steps<T> addSubFlow(T flow);

        Steps<T> split(Builder<T>... builders);

        Steps<T> split(T... flows);

    }

    private interface ToAfter<T extends RxFlow> {

        After<T> afterFlow(Runnable... runnables);

        After<T> withAfter(TestStep... testStep);

    }

    private interface ToOptions<T extends RxFlow> {

        Options<T> withVUsers(int vUsers);

        Options<T> withVUsersAuto();

        Options<T> withDataSources(DataSource... dataSources);

        Options<T> withExceptionHandler(ExceptionHandler exceptionHandler);

        Options<T> runWhile(Predicate<DataRecordWrapper> predicate);
    }
}
