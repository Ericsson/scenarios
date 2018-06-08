package com.ericsson.de.scenarios.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import static com.ericsson.de.scenarios.impl.StackTraceFilter.RX_API;
import static com.ericsson.de.scenarios.impl.StackTraceFilter.RX_SCENARIO_RUNNER;
import static com.ericsson.de.scenarios.impl.StackTraceFilter.filterFrameworkStackTrace;
import static com.ericsson.de.scenarios.impl.StackTraceFilter.filterListenerStackTrace;
import static com.ericsson.de.scenarios.impl.StackTraceFilter.filterTestwareStackTrace;
import static com.ericsson.de.scenarios.impl.StackTraceFilter.getStackTraceLimit;

import org.assertj.core.api.AbstractIntegerAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class StackTraceFilterTest {

    private static final String RX_JAVA_CLASS = Observable.class.getName();

    @Mock
    private Throwable e;

    @Captor
    private ArgumentCaptor<StackTraceElement[]> stackTraceCaptor;

    @Test
    public void clearStackTrace() throws Exception {
        RuntimeException e = new RuntimeException();

        StackTraceFilter.clearStackTrace(e);

        assertThat(e.getStackTrace()).isEmpty();
    }

    @Test
    public void filterFrameworkStackTrace_shouldRemove_rxJava() throws Exception {
        prepareStackTrace("foo", "rx.bar", RX_JAVA_CLASS, "baz", "rx.qux");

        filterFrameworkStackTrace(e);

        assertThatStackTraceContainsExactly("foo", "baz");
    }

    @Test
    public void filterTestwareStackTrace_shouldRemove_rxJava() throws Exception {
        prepareStackTrace("foo", "rx.bar", RX_JAVA_CLASS, "baz", "rx.qux");

        filterTestwareStackTrace(e);

        assertThatStackTraceContainsExactly("foo", "baz");
    }

    @Test
    public void filterTestwareStackTrace_shouldRemove_reflections() throws Exception {
        prepareStackTrace("foo", "sun.reflect.bar", "baz", "java.lang.reflect.qux");

        filterTestwareStackTrace(e);

        assertThatStackTraceContainsExactly("foo", "baz");
    }

    @Test
    public void filterTestwareStackTrace_shouldRemove_rxScenario() throws Exception {
        prepareStackTrace(RxApi.class.getName(), "foo", "com.ericsson.de.scenarios.impl.Foo", "bar", Implementation.class.getName(), "baz",
                "com.ericsson.de.scenarios.impl.Bar", "qux");

        filterTestwareStackTrace(e);

        assertThatStackTraceContainsExactly("foo", "bar", "baz", "qux");
    }

    @Test
    public void filterListenerStackTrace_shouldRemove_guava() throws Exception {
        prepareStackTrace("foo", "com.google.bar", "baz", "com.google.common.qux");

        filterListenerStackTrace(e);

        assertThatStackTraceContainsExactly("foo", "baz");
    }

    @Test
    public void filterListenerStackTrace_shouldRemove_rxJava() throws Exception {
        prepareStackTrace("foo", "rx.bar", RX_JAVA_CLASS, "baz", "rx.qux");

        filterListenerStackTrace(e);

        assertThatStackTraceContainsExactly("foo", "baz");
    }

    @Test
    public void filterListenerStackTrace_shouldRemove_reflections() throws Exception {
        prepareStackTrace("foo", "sun.reflect.bar", "baz", "java.lang.reflect.qux");

        filterListenerStackTrace(e);

        assertThatStackTraceContainsExactly("foo", "baz");
    }

    @Test
    public void filterListenerStackTrace_shouldRemove_rxScenario() throws Exception {
        prepareStackTrace(RxApi.class.getName(), "foo", "com.ericsson.de.scenarios.impl.Foo", "bar", Implementation.class.getName(), "baz",
                "com.ericsson.de.scenarios.impl.Bar", "qux");

        filterListenerStackTrace(e);

        assertThatStackTraceContainsExactly("foo", "bar", "baz", "qux");
    }

    @Test
    public void getStackTraceLimit_withoutRunner() throws Exception {
        assertThatLimitForStackTrace("foo", "bar", "baz").isEqualTo(3);
    }

    @Test
    public void getStackTraceLimit_withRunner() throws Exception {
        assertThatLimitForStackTrace("foo", RX_SCENARIO_RUNNER).isEqualTo(2);
        assertThatLimitForStackTrace("foo", RX_SCENARIO_RUNNER, "com.testware.Test").isEqualTo(3);
        assertThatLimitForStackTrace("foo", RX_SCENARIO_RUNNER, "com.testware.Test", "com.runner.TestRunner").isEqualTo(3);
        assertThatLimitForStackTrace("foo", RX_SCENARIO_RUNNER, "com.testware.Test1", "com.testware.Test2").isEqualTo(4);
        assertThatLimitForStackTrace("foo", RX_SCENARIO_RUNNER, "com.testware.Test1", "com.testware.Test2", "com.runner.TestRunner").isEqualTo(4);
    }

    @Test
    public void getStackTraceLimit_withRunnerAndRxApiImpl() throws Exception {
        assertThatLimitForStackTrace("foo", RX_SCENARIO_RUNNER, RX_API).isEqualTo(3);
        assertThatLimitForStackTrace("foo", RX_SCENARIO_RUNNER, RX_API).isEqualTo(3);
        assertThatLimitForStackTrace("foo", RX_SCENARIO_RUNNER, RX_API, "com.testware.Test").isEqualTo(4);
        assertThatLimitForStackTrace("foo", RX_SCENARIO_RUNNER, RX_API, "com.testware.Test", "com.runner.TestRunner").isEqualTo(4);
        assertThatLimitForStackTrace("foo", RX_SCENARIO_RUNNER, RX_API, "com.testware.Test1", "com.testware.Test2").isEqualTo(5);
        assertThatLimitForStackTrace("foo", RX_SCENARIO_RUNNER, RX_API, "com.testware.Test1", "com.testware.Test2", "com.runner.TestRunner")
                .isEqualTo(5);
    }

    private AbstractIntegerAssert<?> assertThatLimitForStackTrace(String... classNames) {
        StackTraceElement[] stackTrace = stackTraceElements(classNames);

        int limit = getStackTraceLimit(stackTrace);

        return assertThat(limit);
    }

    private void prepareStackTrace(String... classNames) {
        doReturn(stackTraceElements(classNames)).when(e).getStackTrace();
    }

    private void assertThatStackTraceContainsExactly(String... classNames) {
        verify(e).setStackTrace(stackTraceCaptor.capture());
        assertThat(stackTraceCaptor.getValue()).containsExactly(stackTraceElements(classNames));
    }

    private StackTraceElement[] stackTraceElements(String... classNames) {
        StackTraceElement[] stackTraceElements = new StackTraceElement[classNames.length];
        for (int i = 0; i < classNames.length; i++) {
            stackTraceElements[i] = stackTraceElement(classNames[i]);
        }
        return stackTraceElements;
    }

    private StackTraceElement stackTraceElement(String className) {
        return new StackTraceElement(className, "method", null, 0);
    }
}
