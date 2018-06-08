package com.ericsson.de.scenarios.impl;

import static java.lang.String.format;

import static com.ericsson.de.scenarios.impl.StackTraceFilter.filterFrameworkStackTrace;

import java.util.Collection;
import java.util.Map;

import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.api.DataRecordWrapper;

/**
 * Provide access to some internal functionality located in impl to api package
 */
public final class Bridge {
    private Bridge() {
    }

    public static DataRecordWrapper wrapWithParameters(Map<String, Object> parameters, Map<String, String> bindings, DataRecordWrapper dataRecord) {
        dataRecord = parameters.isEmpty() ? dataRecord : new DataRecords.Parameter(dataRecord, parameters);
        dataRecord = bindings.isEmpty() ? dataRecord : new DataRecords.Binding(dataRecord, bindings);

        return dataRecord;
    }

    public static boolean isCollectionOfDataRecords(Object returnedValue) {
        return returnedValue instanceof Collection && Collection.class.cast(returnedValue).iterator().hasNext() && DataRecord.class
                .isInstance(Collection.class.cast(returnedValue).iterator().next());
    }

    /**
     * Acts as {@link com.google.common.base.Preconditions#checkState(boolean)} but filters stack trace
     * Should only be used to validate User error
     */
    public static void checkRxState(boolean expression, String errorMessageTemplate, Object... errorMessageArgs) {
        if (!expression) {
            IllegalStateException illegalStateException = new IllegalStateException(format(errorMessageTemplate, errorMessageArgs));
            throw filterFrameworkStackTrace(illegalStateException);
        }
    }

    /**
     * Acts as {@link com.google.common.base.Preconditions#checkNotNull(Object)}} but filters stack trace
     * Should only be used to validate User error
     */
    public static <T> T checkRxNotNull(T reference, String msg) {
        if (reference == null) {
            NullPointerException nullPointerException = new NullPointerException(msg);
            throw filterFrameworkStackTrace(nullPointerException);
        } else {
            return reference;
        }
    }
}
