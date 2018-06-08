package com.ericsson.de.scenarios.api;

import com.ericsson.de.scenarios.impl.RxFlow;
import com.ericsson.de.scenarios.impl.RxScenario;

/**
 * Determines the order in which RxScenario builder methods can be called.
 */
@SuppressWarnings("WeakerAccess")
public final class ScenarioBuilderInterfaces {

    private ScenarioBuilderInterfaces() {
    }

    public interface ScenarioBuilderStates<S extends RxScenario, F extends RxFlow>
            extends ScenarioStart<S, F>, Flows<S, F>, AlwaysRun<S, F>, ExceptionHandler<S> {

    }

    /*---------------- STATE INTERFACES ----------------*/

    public interface ScenarioStart<S extends RxScenario, F extends RxFlow> extends ToFlows<S, F> {

        ScenarioStart<S, F> withParameter(String key, Object value);

    }

    public interface Flows<S extends RxScenario, F extends RxFlow> extends ToFlows<S, F>, ToExceptionHandler<S>, Builder<S> {

        AlwaysRun<S, F> alwaysRun();

    }

    public interface AlwaysRun<S extends RxScenario, F extends RxFlow> extends ToFlows<S, F>, ToExceptionHandler<S>, Builder<S> {

    }

    public interface ExceptionHandler<S extends RxScenario> extends Builder<S> {

    }

    /*---------------- DESTINATION INTERFACES ----------------*/

    private interface ToFlows<S extends RxScenario, F extends RxFlow> {

        Flows<S, F> addFlow(Builder<F> flowBuilder);

        Flows<S, F> addFlow(F flow);

        Flows<S, F> split(Builder<F>... subFlows);

        Flows<S, F> split(F... subFlows);

    }

    private interface ToExceptionHandler<S extends RxScenario> {

        ExceptionHandler<S> withExceptionHandler(com.ericsson.de.scenarios.api.ExceptionHandler exceptionHandler);

    }
}
