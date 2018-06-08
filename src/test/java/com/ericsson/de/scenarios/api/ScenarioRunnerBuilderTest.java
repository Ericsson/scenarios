package com.ericsson.de.scenarios.api;

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.de.scenarios.api.BasicDataRecord.fromValues;
import static com.ericsson.de.scenarios.api.DebugGraphMode.GRAPH_ML;
import static com.ericsson.de.scenarios.api.DebugGraphMode.NONE;
import static com.ericsson.de.scenarios.api.DebugGraphMode.SVG;
import static com.ericsson.de.scenarios.api.RxApiImpl.runner;
import static com.ericsson.de.scenarios.api.ScenarioRunnerBuilder.DEBUG_GRAPH_MODE;
import static com.ericsson.de.scenarios.api.ScenarioRunnerBuilder.DEBUG_LOG_ENABLED;
import static com.ericsson.de.scenarios.api.ScenarioRunnerBuilder.ERROR_LISTENER_DUPLICATE;
import static com.ericsson.de.scenarios.api.TestStep.ERROR_PARAMETER_ALREADY_SET;
import static com.ericsson.de.scenarios.api.TestStep.ERROR_PARAMETER_NULL;

import java.util.Map;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ericsson.de.scenarios.impl.DebugLogScenarioListener;
import com.ericsson.de.scenarios.impl.RxFlowBuilder;

public class ScenarioRunnerBuilderTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ScenarioRunnerBuilder builder = runner();

    @After
    public void tearDown() throws Exception {
        System.clearProperty(DEBUG_LOG_ENABLED);
        System.clearProperty(DEBUG_GRAPH_MODE);
    }

    @Test
    public void withDebugLogging_exception_whenCalledTwice() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(format(ERROR_PARAMETER_ALREADY_SET, "debugLogEnabled"));

        builder.withDebugLogEnabled().withDebugLogEnabled();
    }

    @Test
    public void withGraphExportMode_exception_whenNull() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_PARAMETER_NULL);

        builder.withGraphExportMode(null);
    }

    @Test
    public void withGraphExportMode_exception_whenCalledTwice() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(format(ERROR_PARAMETER_ALREADY_SET, "debugGraphMode"));

        builder.withGraphExportMode(GRAPH_ML).withGraphExportMode(SVG);
    }

    @Test
    public void addListener_exception_whenDuplicate() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(ERROR_LISTENER_DUPLICATE);

        ScenarioListener listener = new ScenarioListener() {
        };

        builder.addListener(listener).addListener(listener);
    }

    @Test
    public void withExceptionHandler_exception_whenExceptionHandler_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(RxFlowBuilder.ERROR_EXCEPTION_HANDLER_NULL);

        builder.withDefaultExceptionHandler(null);
    }

    @Test
    public void withExceptionHandler_exception_whenCalledMoreThanOnce() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(RxFlowBuilder.ERROR_EXCEPTION_HANDLER_NOT_ONCE);
        thrown.expectMessage(RxFlowBuilder.HINT_EXCEPTION_HANDLER);

        builder.withDefaultExceptionHandler(ExceptionHandler.PROPAGATE).withDefaultExceptionHandler(ExceptionHandler.IGNORE);
    }

    @Test
    public void build_exception_whenIncorrectDebugLogEnabled() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(format(ScenarioRunnerBuilder.ERROR_DEBUG_LOG_ENABLED, "foo"));

        setSystemProperties(DEBUG_LOG_ENABLED, "foo");

        builder.build();
    }

    @Test
    public void build_exception_whenIncorrectDebugGraphMode() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(format(ScenarioRunnerBuilder.ERROR_DEBUG_GRAPH_MODE, "bar"));

        setSystemProperties(DEBUG_GRAPH_MODE, "bar");

        builder.build();
    }

    @Test
    public void build_defaultValues() throws Exception {
        builder.build();

        assertThat(builder.debugLogEnabled).isFalse();
        assertThat(builder.debugGraphMode).isSameAs(NONE);
    }

    @Test
    public void build_builderParams() throws Exception {
        builder.withDebugLogEnabled().withGraphExportMode(SVG);

        builder.build();

        assertThat(builder.debugLogEnabled).isTrue();
        assertThat(builder.debugGraphMode).isSameAs(SVG);
    }

    @Test
    public void build_systemProperties() throws Exception {
        setSystemProperties(DEBUG_LOG_ENABLED, "true", DEBUG_GRAPH_MODE, "svg");

        builder.build();

        assertThat(builder.debugLogEnabled).isTrue();
        assertThat(builder.debugGraphMode).isSameAs(SVG);
    }

    @Test
    public void build_builderParams_andSystemProperties_disjoint_1() throws Exception {
        setSystemProperties(DEBUG_LOG_ENABLED, "true");
        builder.withGraphExportMode(SVG);

        builder.build();

        assertThat(builder.debugLogEnabled).isTrue();
        assertThat(builder.debugGraphMode).isSameAs(SVG);
    }

    @Test
    public void build_builderParams_andSystemProperties_disjoint_2() throws Exception {
        setSystemProperties(DEBUG_GRAPH_MODE, "svg");
        builder.withDebugLogEnabled();

        builder.build();

        assertThat(builder.debugLogEnabled).isTrue();
        assertThat(builder.debugGraphMode).isSameAs(SVG);
    }

    @Test
    public void build_systemProperties_overrideDebugLogEnabled() throws Exception {
        setSystemProperties(DEBUG_LOG_ENABLED, "true");

        builder.build();

        assertThat(builder.debugLogEnabled).isTrue();
    }

    @Test
    public void build_whenSystemProperties_overrideDebugGraphMode() throws Exception {
        setSystemProperties(DEBUG_GRAPH_MODE, "svg");
        builder.withGraphExportMode(GRAPH_ML);

        builder.build();

        assertThat(builder.debugGraphMode).isSameAs(SVG);
    }

    @Test
    public void build_whenSystemProperties_overrideAllBuildParams() throws Exception {
        setSystemProperties(DEBUG_LOG_ENABLED, "true", DEBUG_GRAPH_MODE, "svg");
        builder.withGraphExportMode(GRAPH_ML);

        builder.build();

        assertThat(builder.debugLogEnabled).isTrue();
        assertThat(builder.debugGraphMode).isSameAs(SVG);
    }

    @Test
    public void build_shouldAddDebugLogScenarioListener_whenDebugLogEnabled() throws Exception {
        builder.withDebugLogEnabled();
        assertThat(builder.listeners).isEmpty();

        builder.build();

        assertThat(builder.listeners).containsExactly(DebugLogScenarioListener.INSTANCE);
    }

    private void setSystemProperties(String... keyValues) {
        Map<String, Object> parameters = fromValues((Object[]) keyValues).getAllFields();
        for (String key : parameters.keySet()) {
            Object property = parameters.get(key);
            System.setProperty(key, property.toString());
        }
    }
}
