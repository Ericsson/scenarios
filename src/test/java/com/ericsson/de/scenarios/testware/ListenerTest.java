package com.ericsson.de.scenarios.testware;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.de.scenarios.impl.RxApi.fromIterable;
import static com.google.common.base.Strings.repeat;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Stack;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.de.scenarios.api.Api;
import com.ericsson.de.scenarios.api.Scenario;
import com.ericsson.de.scenarios.api.ScenarioListener;
import com.ericsson.de.scenarios.api.ScenarioRunner;
import com.ericsson.de.scenarios.api.events.FlowEvent.FlowFinishedEvent;
import com.ericsson.de.scenarios.api.events.FlowEvent.FlowStartedEvent;
import com.ericsson.de.scenarios.api.events.ScenarioEvent.ScenarioFinishedEvent;
import com.ericsson.de.scenarios.api.events.ScenarioEvent.ScenarioStartedEvent;
import com.ericsson.de.scenarios.api.events.TestStepEvent.TestStepFinishedEvent;
import com.ericsson.de.scenarios.api.events.TestStepEvent.TestStepStartedEvent;
import com.ericsson.de.scenarios.impl.ScenarioTest;

public class ListenerTest {

    private Stack<String> stack;
    private ScenarioRunner runner;

    @Before
    public void setUp() throws Exception {
        stack = new Stack<>();
        runner = Api.runner().addListener(new StackListener(stack)).build();
    }

    @Test
    public void empty() throws Exception {
        Scenario scenario = Api.scenario("foo").addFlow(Api.flow("bar").addTestStep(ScenarioTest.named("baz"))).build();

        runner.run(scenario);

        assertThat(stack)
                .containsExactly("RxScenario started: foo", "  RxFlow started: bar", "    Test Step started: baz", "    Test Step finished: baz",
                        "  RxFlow finished: bar", "RxScenario finished: foo");
    }

    @Test
    public void steps() throws Exception {
        Scenario scenario = Api.scenario("foo").addFlow(
                Api.flow("bar1").addTestStep(ScenarioTest.named("baz1")).addTestStep(ScenarioTest.named("baz2"))
                        .addTestStep(ScenarioTest.named("baz3"))).addFlow(
                Api.flow("bar2").addTestStep(ScenarioTest.named("qux1")).addTestStep(ScenarioTest.named("qux2"))
                        .addTestStep(ScenarioTest.named("qux3"))).build();

        runner.run(scenario);

        assertThat(stack)
                .containsExactly("RxScenario started: foo", "  RxFlow started: bar1", "    Test Step started: baz1", "    Test Step finished: baz1",
                        "    Test Step started: baz2", "    Test Step finished: baz2", "    Test Step started: baz3", "    Test Step finished: baz3",
                        "  RxFlow finished: bar1", "  RxFlow started: bar2", "    Test Step started: qux1", "    Test Step finished: qux1",
                        "    Test Step started: qux2", "    Test Step finished: qux2", "    Test Step started: qux3", "    Test Step finished: qux3",
                        "  RxFlow finished: bar2", "RxScenario finished: foo");
    }

    @Test
    public void subflows() throws Exception {
        Scenario scenario = Api.scenario("foo").addFlow(Api.flow("bar").addTestStep(ScenarioTest.named("bar")).addSubFlow(
                Api.flow("baz").addTestStep(ScenarioTest.named("baz")).addSubFlow(Api.flow("qux").addTestStep(ScenarioTest.named("qux"))))).build();

        runner.run(scenario);

        assertThat(stack)
                .containsExactly("RxScenario started: foo", "  RxFlow started: bar", "    Test Step started: bar", "    Test Step finished: bar",
                        "    RxFlow started: baz", "      Test Step started: baz", "      Test Step finished: baz", "      RxFlow started: qux",
                        "        Test Step started: qux", "        Test Step finished: qux", "      RxFlow finished: qux", "    RxFlow finished: baz",
                        "  RxFlow finished: bar", "RxScenario finished: foo");
    }

    @Test
    public void vUsers() throws Exception {
        List<Integer> numbers = newArrayList(1, 2, 3);

        Scenario scenario = Api.scenario("foo").addFlow(
                Api.flow("bar").addTestStep(ScenarioTest.named("baz")).withDataSources(fromIterable("numbers", numbers).shared()).withVUsersAuto())
                .build();

        runner.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("RxScenario started: foo", "  RxFlow started: bar", "    Test Step started: baz",
                "    Test Step finished: baz", "    Test Step started: baz", "    Test Step finished: baz", "    Test Step started: baz",
                "    Test Step finished: baz", "  RxFlow finished: bar", "RxScenario finished: foo");
    }

    @Test
    public void dataSource() throws Exception {
        List<Integer> numbers = newArrayList(1, 2, 3);

        Scenario scenario = Api.scenario("foo")
                .addFlow(Api.flow("bar").addTestStep(ScenarioTest.named("baz")).withDataSources(fromIterable("numbers", numbers))).build();

        runner.run(scenario);

        assertThat(stack)
                .containsExactly("RxScenario started: foo", "  RxFlow started: bar", "    Test Step started: baz", "    Test Step finished: baz",
                        "    Test Step started: baz", "    Test Step finished: baz", "    Test Step started: baz", "    Test Step finished: baz",
                        "  RxFlow finished: bar", "RxScenario finished: foo");
    }

    @Test
    public void exception() throws Exception {
        Scenario scenario = Api.scenario("foo").addFlow(Api.flow("bar").addTestStep(ScenarioTest.named("baz"))).build();

        runner = Api.runner().addListener(new ScenarioListener() {
            @Override
            public void onScenarioStarted(ScenarioStartedEvent event) {
                throw new ScenarioTest.VeryExpectedException();
            }
        }).addListener(new StackListener(stack)).build();

        try {
            runner.run(scenario);
        } catch (Exception ignored) {
        }

        assertThat(stack)
                .containsExactly("RxScenario started: foo", "  RxFlow started: bar", "    Test Step started: baz", "    Test Step finished: baz",
                        "  RxFlow finished: bar", "RxScenario finished: foo");
    }

    private static class StackListener extends ScenarioListener {

        private Stack<String> stack;
        private int indent = 0;

        StackListener(Stack<String> stack) {
            this.stack = stack;
        }

        @Override
        public void onScenarioStarted(ScenarioStartedEvent event) {
            indent++;
            stack.push("RxScenario started: " + event.getScenario().getName());
        }

        @Override
        public void onScenarioFinished(ScenarioFinishedEvent event) {
            indent--;
            stack.push("RxScenario finished: " + event.getScenario().getName());
        }

        @Override
        public void onFlowStarted(FlowStartedEvent event) {
            stack.push(repeat(" ", 2 * indent++) + "RxFlow started: " + event.getFlow().getName());
        }

        @Override
        public void onFlowFinished(FlowFinishedEvent event) {
            stack.push(repeat(" ", 2 * --indent) + "RxFlow finished: " + event.getFlow().getName());
        }

        @Override
        public void onTestStepStarted(TestStepStartedEvent event) {
            stack.push(repeat(" ", 2 * indent) + "Test Step started: " + event.getName());
        }

        @Override
        public void onTestStepFinished(TestStepFinishedEvent event) {
            stack.push(repeat(" ", 2 * indent) + "Test Step finished: " + event.getName());
        }
    }
}
