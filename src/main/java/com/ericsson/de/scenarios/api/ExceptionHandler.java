package com.ericsson.de.scenarios.api;

import com.ericsson.de.scenarios.impl.RxExceptionHandler;

/**
 * Exception handler determines whether flow propagates or ignores the exception.
 */
public abstract class ExceptionHandler extends RxExceptionHandler {

    public enum Outcome {
        /**
         * Stop RxFlow execution and propagate exception to next level exception handler (i.e. Sub RxFlow → RxFlow → RxScenario).
         * If there are no more handlers defined, exception will be propagated to main thread and test will fail.
         */
        PROPAGATE_EXCEPTION,

        /**
         * If handler handles exception and returns this constant, RxFlow execution will continue, and no other handlers will be called.
         */
        CONTINUE_FLOW,
    }

    /**
     * @see Outcome#PROPAGATE_EXCEPTION
     */
    public static final ExceptionHandler PROPAGATE = new ExceptionHandler() {
        @Override
        public Outcome onException(Throwable e) {
            return Outcome.PROPAGATE_EXCEPTION;
        }
    };

    /**
     * @see Outcome#CONTINUE_FLOW
     */
    public static final ExceptionHandler IGNORE = new ExceptionHandler() {
        @Override
        public Outcome onException(Throwable e) {
            return Outcome.CONTINUE_FLOW;
        }
    };

    @Override
    public abstract Outcome onException(Throwable e);

}
