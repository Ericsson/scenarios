package com.ericsson.de.scenarios.impl;

import com.ericsson.de.scenarios.api.ExceptionHandler;

public abstract class RxExceptionHandler {

    public static final String ERROR_EXCEPTION_HANDLER_RETURN_NULL =
            "ExceptionHandler can't return null, " + "please use either Outcome.PROPAGATE_EXCEPTION or Outcome.CONTINUE_FLOW";

    final boolean cannotHandle(Throwable e) {
        return ExceptionHandler.Outcome.PROPAGATE_EXCEPTION.equals(tryHandle(e));
    }

    final boolean canHandle(Throwable e) {
        return ExceptionHandler.Outcome.CONTINUE_FLOW.equals(tryHandle(e));
    }

    boolean continueOnNextDataRecord() {
        return false;
    }

    private ExceptionHandler.Outcome tryHandle(Throwable e) {
        return Bridge.checkRxNotNull(onException(e), ERROR_EXCEPTION_HANDLER_RETURN_NULL);
    }

    public abstract ExceptionHandler.Outcome onException(Throwable e);

}
