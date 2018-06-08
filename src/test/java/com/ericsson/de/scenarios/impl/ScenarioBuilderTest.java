package com.ericsson.de.scenarios.impl;

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.de.scenarios.api.RxApiImpl.flow;
import static com.ericsson.de.scenarios.api.RxApiImpl.scenario;
import static com.ericsson.de.scenarios.impl.RxScenarioBuilder.ERROR_FLOW_UNIQUENESS;
import static com.ericsson.de.scenarios.impl.RxScenarioBuilder.ERROR_TEST_STEP_UNIQUENESS;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ericsson.de.scenarios.api.ExceptionHandler;
import com.ericsson.de.scenarios.api.Flow;
import com.ericsson.de.scenarios.api.FlowBuilder;
import com.ericsson.de.scenarios.api.FlowBuilderInterfaces;
import com.ericsson.de.scenarios.api.ScenarioRunnerBuilder;
import com.ericsson.de.scenarios.api.TestStep;

public class ScenarioBuilderTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private TestStep nopStep = ScenarioTest.nop();
    private FlowBuilderInterfaces.Steps<Flow> nopFlow = flow().addTestStep(nopStep);

    @Test
    public void addFlow_shouldThrowNullPointerException_whenFlowBuilder_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(RxFlowBuilder.ERROR_SUBFLOW_NULL);

        scenario().addFlow((FlowBuilder) null).build();
    }

    @Test
    public void addFlow_shouldThrowNullPointerException_whenFlow_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(RxFlowBuilder.ERROR_SUBFLOW_NULL);

        scenario().addFlow((Flow) null).build();
    }

    @Test
    public void split_shouldThrowNullPointerException_whenSubFlowsBuilders_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(RxFlowBuilder.ERROR_SUBFLOWS_NULL);

        scenario().split((FlowBuilder[]) null).build();
    }

    @Test
    public void split_shouldThrowNullPointerException_whenOneOfSubFlowsBuilders_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(RxFlowBuilder.ERROR_SUBFLOW_NULL);

        scenario().split(nopFlow, null, nopFlow).build();
    }

    @Test
    public void split_shouldThrowNullPointerException_whenSubFlows_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(RxFlowBuilder.ERROR_SUBFLOWS_NULL);

        scenario().split((Flow[]) null).build();
    }

    @Test
    public void split_shouldThrowNullPointerException_whenOneOfSubFlows_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(RxFlowBuilder.ERROR_SUBFLOW_NULL);

        Flow flow = nopFlow.build();

        scenario().split(flow, null, flow).build();
    }

    @Test
    public void withParameter_shouldThrowNullPointerException_whenParameterKey_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(TestStep.ERROR_PARAMETER_NULL);

        scenario().withParameter(null, 13);
    }

    @Test
    public void withParameter_shouldThrowIllegalArgumentException_whenSameParameterSetTwice() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(format(TestStep.ERROR_PARAMETER_ALREADY_SET, "param"));

        scenario().withParameter("param", 42).withParameter("param", 13);
    }

    @Test
    public void withParameter_shouldThrowIllegalArgumentException_whenDebugLogEnabled() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(RxScenarioBuilder.ERROR_DEBUG_LOG_ENABLED);

        scenario().withParameter(ScenarioRunnerBuilder.DEBUG_LOG_ENABLED, null);
    }

    @Test
    public void withParameter_shouldThrowIllegalArgumentException_whenDebugGraphMode() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(RxScenarioBuilder.ERROR_DEBUG_GRAPH_MODE);

        scenario().withParameter(ScenarioRunnerBuilder.DEBUG_GRAPH_MODE, null);
    }

    @Test
    public void withExceptionHandler_shouldThrowNullPointerException_whenExceptionHandler_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(RxFlowBuilder.ERROR_EXCEPTION_HANDLER_NULL);

        scenario().addFlow(nopFlow).withExceptionHandler(null);
    }

    @Test
    public void withExceptionHandler_shouldThrowIllegalStateException_whenCalledMoreThanOnce() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(RxFlowBuilder.ERROR_EXCEPTION_HANDLER_NOT_ONCE);
        thrown.expectMessage(RxFlowBuilder.HINT_EXCEPTION_HANDLER);

        ((RxScenarioBuilder) scenario().addFlow(nopFlow).withExceptionHandler(ExceptionHandler.PROPAGATE))
                .withExceptionHandler(ExceptionHandler.IGNORE);
    }

    @Test
    public void build_exceptionHandler_null_byDefault() throws Exception {
        RxScenario scenario = scenario().addFlow(nopFlow).build();

        assertThat(scenario.rxFlow.exceptionHandler).isNull();
    }

    @Test
    public void build_shouldAssignUniqueScenarioIds_toAllTestSteps() throws Exception {
        TestStep testStepA = ScenarioTest.print("Test Step A");
        TestStep testStepB = ScenarioTest.print("Test Step B");
        TestStep testStepC = ScenarioTest.print("Test Step C");
        TestStep testStepD = ScenarioTest.print("Test Step D");
        TestStep testStepE = ScenarioTest.print("Test Step E");

        scenario() // 1
                .addFlow(flow() // 2
                        .addTestStep(testStepA) // 3
                        .addSubFlow(flow() // 4
                                .addTestStep(testStepB) // 5
                        ).split(flow() // 6
                                .addTestStep(testStepC) // 7
                        )).split(flow() // 8
                        .addTestStep(testStepD), // 9
                flow() // 10
                        .split(flow() // 11
                                .addTestStep(testStepE) // 12
                        )).build();

        assertThat(testStepA.getId()).isEqualTo(3);
        assertThat(testStepB.getId()).isEqualTo(5);
        assertThat(testStepC.getId()).isEqualTo(7);
        assertThat(testStepD.getId()).isEqualTo(9);
        assertThat(testStepE.getId()).isEqualTo(12);
    }

    /*-------- Test Step Uniqueness Tests --------*/

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_inSameFlow() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario().addFlow(flow().addTestStep(nopStep).addTestStep(nopStep)).build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_inDifferentFlows() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario().addFlow(flow().addTestStep(nopStep)).addFlow(flow().addTestStep(nopStep)).build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_inDifferentSubFlows() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario().addFlow(flow().addSubFlow(flow().addTestStep(nopStep)).addSubFlow(flow().addTestStep(nopStep))).build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_onDifferentLevels() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario().addFlow(flow().addTestStep(nopStep).addSubFlow(flow().addTestStep(nopStep))).build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_inDifferentSplitFlows() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario().addFlow(flow().split(flow().addTestStep(nopStep), flow().addTestStep(nopStep))).build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_inDifferentSplits() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario().addFlow(flow().split(flow().addTestStep(nopStep)).split(flow().addTestStep(nopStep))).build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameTestStepAddedTwice_inDifferentSplitsSubFlows() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_TEST_STEP_UNIQUENESS);

        scenario().addFlow(flow().split(flow().addSubFlow(flow().addTestStep(nopStep))).split(flow().addSubFlow(flow().addTestStep(nopStep))))
                .build();
    }

    /*-------- RxFlow Uniqueness Tests --------*/

    @Test
    public void shouldThrowIllegalArgumentException_whenSameFlowAddedTwice_inSameScenario() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_FLOW_UNIQUENESS);

        Flow flow = nopFlow.build();

        scenario().addFlow(flow).addFlow(flow).build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameFlowAddedTwice_inDifferentForks() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_FLOW_UNIQUENESS);

        Flow flow = nopFlow.build();

        scenario().addFlow(flow).split(flow).build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameFlowAddedTwice_onDifferentLevels() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_FLOW_UNIQUENESS);

        Flow flow = nopFlow.build();

        scenario().addFlow(flow).addFlow(flow().addSubFlow(flow)).build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameFlowAddedTwice_inSameSplit() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_FLOW_UNIQUENESS);

        Flow flow = nopFlow.build();

        scenario().split(flow, flow).build();
    }

    @Test
    public void shouldThrowIllegalArgumentException_whenSameFlowAddedTwice_inDifferentSplits() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR_FLOW_UNIQUENESS);

        Flow flow = nopFlow.build();

        scenario().split(flow).split(flow).build();
    }
}
