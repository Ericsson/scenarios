package com.ericsson.de.scenarios.api;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.Map;

import com.ericsson.de.scenarios.impl.Bridge;
import com.ericsson.de.scenarios.impl.Invocation;
import com.ericsson.de.scenarios.impl.RxFlowBuilder;
import com.google.common.base.Optional;

/**
 * Definition of the Test Step with base functionality
 */
public abstract class TestStep extends Invocation {

    public enum Status {
        SUCCESS, FAILED, SKIPPED
    }

    public static final String ERROR_PARAMETER_NULL = "Parameter cannot be null";
    public static final String ERROR_PARAMETER_ALREADY_SET = "Parameter '%s' already set";

    protected final String name;

    private final Map<String, Object> parameters = new HashMap<>();
    private final Map<String, String> bindings = new HashMap<>();
    private boolean alwaysRun;
    private ContextDataSource resultingDataSource;

    public TestStep(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isAlwaysRun() {
        return alwaysRun;
    }

    /**
     * Overrides input field named <code>key</code> of Test Step
     *
     * @param key
     *         name of input
     *
     * @return parameter
     */
    public ParameterBuilder withParameter(String key) {
        validateParameter(key);
        return new ParameterBuilder(key);
    }

    protected void validateParameter(String key) {
        checkArgument(key != null, ERROR_PARAMETER_NULL);
        checkState(!parameters.containsKey(key) && !bindings.containsKey(key), ERROR_PARAMETER_ALREADY_SET, key);
    }

    /**
     * @deprecated use {@link #withParameter(String).value(Object)} instead.
     */
    @Deprecated
    public TestStep withParameter(String key, Object value) {
        return withParameter(key).value(value);
    }

    /**
     * @see RxFlowBuilder#alwaysRun()
     */
    public TestStep alwaysRun() {
        TestStep copy = copy();
        copy.alwaysRun = true;
        return copy;
    }

    /**
     * If Test Step returns value, it's possible to collect resulting values from all executed Test Steps to Data Source
     * for further reuse of this Data Source in following flows.
     *
     * @param dataSource
     *         target Data Source
     *
     * @return builder
     */
    public TestStep collectResultsToDataSource(ContextDataSource dataSource) {
        TestStep copy = copy();
        copy.resultingDataSource = dataSource;
        return copy;
    }

    public Optional<Object> run(DataRecordWrapper dataRecord) throws Exception {
        DataRecordWrapper wrappedDataRecord = Bridge.wrapWithParameters(parameters, bindings, dataRecord);
        Optional<Object> result = doRun(wrappedDataRecord);
        parseResult(result);

        return result;
    }

    private void parseResult(Optional<Object> result) {
        if (resultingDataSource != null && result.isPresent()) {
            resultingDataSource.collectFromResult(getName(), result.get());
        }
    }

    protected abstract Optional<Object> doRun(DataRecordWrapper dataRecord) throws Exception;

    /**
     * Method for overrides
     *
     * @return copy of implementation with all implementation specific params
     */
    protected abstract TestStep copySelf();

    private TestStep copy() {
        TestStep copy = copySelf();
        copy.parameters.putAll(parameters);
        copy.bindings.putAll(bindings);
        copy.alwaysRun = alwaysRun;
        copy.resultingDataSource = resultingDataSource;

        return copy;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", getId()).add("name", getName()).toString();
    }

    public class ParameterBuilder {
        private final String key;

        ParameterBuilder(String key) {
            this.key = key;
        }

        /**
         * Override parameter with constant <code>value</code>
         *
         * @return builder
         */
        public TestStep value(Object value) {
            TestStep copy = copy();
            copy.parameters.put(key, value);
            return copy;
        }

        /**
         * Override parameter with field value of previously executed <code>testStep</code> of same RxFlow
         * If Test Step returns {@link DataRecord}, this will allow to get its fields.
         *
         * @return builder
         */
        public TestStep fromTestStepResult(TestStep testStep, String field) {
            TestStep copy = copy();
            copy.bindings.put(key, testStep.getName() + "." + field);
            return copy;
        }

        /**
         * Override parameter with result of previously executed <code>testStep</code> of same RxFlow
         * If Test Step returns {@link DataRecord} parameter should extend {@link DataRecord}
         * If Test Step returns object parameter should be type of return object.
         *
         * @return builder
         */
        public TestStep fromTestStepResult(TestStep testStep) {
            TestStep copy = copy();
            copy.bindings.put(key, testStep.getName());
            return copy;
        }

        /**
         * Override parameter with custom value
         *
         * @return builder
         */
        public TestStep bindTo(String field) {
            TestStep copy = copy();
            copy.bindings.put(key, field);
            return copy;
        }

        /**
         * Override parameter with field value from Data Source
         *
         * @return builder
         */
        public TestStep fromDataSource(DataSource dataSource, String field) {
            TestStep copy = copy();
            copy.bindings.put(key, dataSource.getName() + "." + field);
            return copy;
        }

        /**
         * Override parameter with {@link DataRecord} from Data Source
         *
         * @return builder
         */
        public TestStep fromDataSource(DataSource dataSource) {
            TestStep copy = copy();
            copy.bindings.put(key, dataSource.getName());
            return copy;
        }

        /**
         * Override parameter with field value from RxScenario Context
         *
         * @return builder
         */
        public TestStep fromContext(String field) {
            TestStep copy = copy();
            copy.bindings.put(key, ScenarioContext.CONTEXT_RECORD_NAME + "." + field);
            return copy;
        }
    }
}
