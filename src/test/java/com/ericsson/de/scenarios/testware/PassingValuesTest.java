package com.ericsson.de.scenarios.testware;

/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

import static java.util.Arrays.asList;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.de.scenarios.impl.RxApi.fromIterable;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Stack;
import javax.inject.Named;

import org.junit.Test;

import com.ericsson.de.scenarios.Node;
import com.ericsson.de.scenarios.api.Api;
import com.ericsson.de.scenarios.api.BasicDataRecord;
import com.ericsson.de.scenarios.api.ContextDataSource;
import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.api.DataSource;
import com.ericsson.de.scenarios.api.RxApiImpl;
import com.ericsson.de.scenarios.api.Scenario;
import com.ericsson.de.scenarios.api.ScenarioContext;
import com.ericsson.de.scenarios.api.TestStep;
import com.ericsson.de.scenarios.impl.ScenarioTest;

public class PassingValuesTest extends ScenarioTest {

    private static final String DATA_SOURCE = "dataSource";
    private static final String FIELD = "field";
    private static final String DIFFERENT_NAME = "differentName";

    @Test
    public void passValuesBetweenSteps() throws Exception {
        Counter counter = new Counter();

        final TestStep stringProducer = stringProducer();

        Scenario scenario = Api.scenario().addFlow(Api.flow().addTestStep(stringProducer).addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumer1(@Named(DATA_SOURCE) Integer fromDataSource, @Named(ScenarioContext.CONTEXT_RECORD_NAME) ScenarioContext context,
                    @Named("stringProducer") String fromProducer) {
                assertThat(fromProducer).isEqualTo("return" + fromDataSource);
                assertThat(context.getFieldValue(stringProducer.getName())).isEqualTo("return" + fromDataSource);
            }
        }).addSubFlow(Api.flow().addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumer2(@Named(DATA_SOURCE) Integer fromDataSource, @Named(ScenarioContext.CONTEXT_RECORD_NAME) ScenarioContext context,
                    @Named("stringProducer") String fromProducer) {
                assertThat(fromProducer).isEqualTo("return" + fromDataSource);
                assertThat(context.getFieldValue(stringProducer.getName())).isEqualTo("return" + fromDataSource);
            }
        })).addTestStep(counter).withVUsers(2).withDataSources(fromIterable(DATA_SOURCE, asList(1, 2)).shared())).build();

        RxApiImpl.run(scenario);

        counter.assertEqualTo(2);
    }

    @Test
    public void passValuesBetweenStepsDifferentName() throws Exception {
        Stack<String> stack = new Stack<>();

        final TestStep stringProducer = stringProducer().withParameter(DATA_SOURCE).value(6);

        Scenario scenario = Api.scenario().addFlow(
                Api.flow().addTestStep(stringProducer).addTestStep(consumerToStack(stack).withParameter(FIELD).fromTestStepResult(stringProducer)))
                .build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("return6");
    }

    @Test
    public void passDataRecordBetweenStepsDifferentName() throws Exception {
        Stack<String> stack = new Stack<>();

        final TestStep producer = dataRecordProducer();

        Scenario scenario = Api.scenario().addFlow(Api.flow().addTestStep(producer)
                .addTestStep(consumerToStackDifferentName(stack).withParameter(DIFFERENT_NAME).fromTestStepResult(producer, FIELD))).build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("value");
    }

    @Test
    public void testScopeOfContext() throws Exception {
        Counter counter = new Counter();

        Scenario scenario = Api.scenario().withParameter("scenarioParam", "scenarioParam").addFlow(Api.flow().addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            DataRecord producer() {
                return BasicDataRecord.builder().setField("flowParam", "beforeSubflowCreated").setField("scenarioParam", "flowOverride").build();
            }
        }).addSubFlow(Api.flow().addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            DataRecord subFlowProducer() {
                return BasicDataRecord.builder().setField("subFlowParam1", "subFlowParam1").setField("subFlowParam2", "subFlowParam2")
                        .setField("flowParam", "override").build();
            }
        }).addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            void subFlowConsumer(@Named("scenarioParam") String scenarioParam, @Named("flowParam") String flowParam,
                    @Named("subFlowParam1") String subFlowParam1, @Named("subFlowParam2") String subFlowParam2) {

                assertThat(scenarioParam).isEqualTo("flowOverride");
                assertThat(flowParam).isEqualTo("override");
                assertThat(subFlowParam1).isEqualTo("subFlowParam1");
                assertThat(subFlowParam2).isEqualTo("subFlowParam2");
            }
        })).addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            DataRecord flowOverride() {
                return BasicDataRecord.builder().setField("flowParam", "afterSubflowCreated").build();
            }
        }).addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            void flowConsumer(@Named("scenarioParam") String scenarioParam, @Named("flowParam") String flowParam) {

                assertThat(scenarioParam).isEqualTo("flowOverride");
                assertThat(flowParam).isEqualTo("afterSubflowCreated");
            }
        }).addTestStep(counter)).build();

        RxApiImpl.run(scenario);

        counter.assertEqualTo(1);
    }

    @Test
    public void testScopeOfContextDataSource() throws Exception {
        Counter counter = new Counter();

        Scenario scenario = Api.scenario().addFlow(Api.flow().addTestStep(producerFromDataSource()).addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumer(@Named(DATA_SOURCE) Integer fromDataSource, @Named(ScenarioContext.CONTEXT_RECORD_NAME) ScenarioContext context) {

                assertThat(context.getFieldValue(FIELD)).isEqualTo("value" + fromDataSource);
            }
        }).addTestStep(counter).withDataSources(fromIterable(DATA_SOURCE, asList(1, 2)))).build();

        RxApiImpl.run(scenario);

        counter.assertEqualTo(2);
    }

    @Test
    public void returnMultipleDataRecords() throws Exception {
        Counter counter = new Counter();

        Scenario scenario = Api.scenario().addFlow(Api.flow().addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            Object producer() {
                return asList(BasicDataRecord.builder().setField("name1", "value1").build(),
                        BasicDataRecord.builder().setField("name2", "value2").build(),
                        BasicDataRecord.builder().setField("name2", "override").build());
            }
        }).addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumer1(@Named("name1") String name1, @Named("name2") String name2) {

                assertThat(name1).isEqualTo("value1");
                assertThat(name2).isEqualTo("override");
            }
        }).addTestStep(counter)).build();

        RxApiImpl.run(scenario);

        counter.assertEqualTo(1);
    }

    @Test
    public void passDataRecordBetweenFlows() throws Exception {
        final Stack<String> stack = new Stack<>();

        ContextDataSource<DataRecord> contextDataSource = Api.contextDataSource("name", DataRecord.class);

        Scenario scenario = Api.scenario().addFlow(
                Api.flow("producerFromDataSource").addTestStep(producerFromDataSource().collectResultsToDataSource(contextDataSource)).withVUsers(2)
                        .withDataSources(fromIterable(DATA_SOURCE, asList(1, 2)).shared())

        ).addFlow(Api.flow("consumer").addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumer1(@Named(FIELD) String field) {
                stack.add(field);
            }
        }).withVUsers(2).withDataSources(contextDataSource.shared())).build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("value1", "value2");
    }

    @Test
    public void passMultipleDataRecordsBetweenFlows() throws Exception {
        final Stack<String> stack = new Stack<>();

        ContextDataSource<DataRecord> contextDataSource = Api.contextDataSource("name", DataRecord.class);

        Scenario scenario = Api.scenario().addFlow(Api.flow("producerFromDataSource").addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            List<DataRecord> multiProducer(@Named(DATA_SOURCE) Integer fromDataSource) {
                List<DataRecord> hostsWithVUsers = newArrayList();
                for (int i = fromDataSource * 3; i < (fromDataSource + 1) * 3; i++) {
                    hostsWithVUsers.add(BasicDataRecord.builder().setField(FIELD, "value" + i).build());
                }

                return hostsWithVUsers;
            }
        }.collectResultsToDataSource(contextDataSource)).withVUsers(2).withDataSources(fromIterable(DATA_SOURCE, asList(0, 1)).shared()))
                .addFlow(Api.flow("consumer").addTestStep(consumerToStack(stack)).withVUsers(3).withDataSources(contextDataSource.shared())).build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("value0", "value1", "value2", "value3", "value4", "value5");
    }

    @Test
    public void passObjectBetweenFlows() throws Exception {
        final Stack<String> stack = new Stack<>();

        ContextDataSource<DataRecord> contextDataSource = Api.contextDataSource("name", DataRecord.class);

        TestStep stringProducer = stringProducer().collectResultsToDataSource(contextDataSource);

        Scenario scenario = Api.scenario().addFlow(Api.flow("producerFromDataSource").addTestStep(stringProducer).withVUsers(2)
                .withDataSources(fromIterable(DATA_SOURCE, asList(1, 2)).shared())

        ).addFlow(Api.flow("consumer").addTestStep(consumerToStack(stack).withParameter(FIELD).fromTestStepResult(stringProducer)).withVUsers(2)
                .withDataSources(contextDataSource.shared())).build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("return1", "return2");
    }

    @Test(expected = IllegalStateException.class)
    public void passDataBetweenFlows_oneFlowEmpty() throws Exception {
        ContextDataSource<DataRecord> contextDataSource = Api.contextDataSource("name", DataRecord.class);

        Scenario scenario = Api.scenario().addFlow(Api.flow("consumer").addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumer1(@Named(FIELD) String field) {
                throw new IllegalStateException("Should not be executed.");
            }
        }).withDataSources(contextDataSource)).build();

        RxApiImpl.run(scenario);
    }

    @Test
    public void passToDifferentName() throws Exception {
        final Stack<String> stack = new Stack<>();

        Scenario scenario = Api.scenario().addFlow(Api.flow("producerFromDataSource").addTestStep(dataRecordProducer())
                .addTestStep(consumerToStackDifferentName(stack).withParameter(DIFFERENT_NAME).bindTo(FIELD))).build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("value");
    }

    @Test
    public void overrideTest() throws Exception {
        final Stack<String> stack = new Stack<>();

        Scenario scenario = Api.scenario().addFlow(Api.flow("producerFromDataSource").addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            DataRecord producer1() {
                return BasicDataRecord.builder().setField(FIELD, "value1").build();
            }
        }).addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            DataRecord producer2() {
                return BasicDataRecord.builder().setField(FIELD, "value2").build();
            }
        }).addTestStep(consumerToStack(stack)).addTestStep(consumerToStack(stack).withParameter(FIELD).bindTo("producer1.field"))).build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("value2", "value1");
    }

    @Test
    public void bindInputToDifferentName() throws Exception {
        Stack<String> stack1 = new Stack<>();
        Stack<String> stack2 = new Stack<>();
        Stack<String> stack3 = new Stack<>();

        DataSource<Node> nodeDataSource = getNodeDataSource();
        DataSource<String> iterableDataSource = fromIterable("numbers", asList("a", "b"));

        final String PARAM_NAME = "param";
        final String PARAM_VALUE = "pampam";

        Scenario scenario = Api.scenario().withParameter(PARAM_NAME, PARAM_VALUE).addFlow(
                Api.flow().addTestStep(consumerToStack(stack1).withParameter(FIELD).fromDataSource(nodeDataSource, Node.NETWORK_ELEMENT_ID))
                        .withDataSources(nodeDataSource)

        ).addFlow(Api.flow().addTestStep(consumerToStack(stack2).withParameter(FIELD).fromDataSource(iterableDataSource))
                .withDataSources(iterableDataSource)

        ).addFlow(Api.flow().addTestStep(consumerToStack(stack3).withParameter(FIELD).fromContext(PARAM_NAME))).build();

        RxApiImpl.run(scenario);

        assertThat(stack1).containsExactlyInAnyOrder("SGSN-MME", "SGSN-14B");
        assertThat(stack2).containsExactlyInAnyOrder("a", "b");
        assertThat(stack3).containsExactlyInAnyOrder(PARAM_VALUE);
    }

    @Test
    public void bindDataSource() throws Exception {
        Stack<String> stack1 = new Stack<>();
        Stack<String> stack2 = new Stack<>();
        Stack<String> stack3 = new Stack<>();

        DataSource<Node> nodeDataSource = getNodeDataSource();
        DataSource<String> iterableDataSource = fromIterable("iterable", asList("a", "b"));

        Scenario scenario = Api.scenario()
                .addFlow(Api.flow().addTestStep(dataRecordToStackDifferentName(stack1)).withDataSources(nodeDataSource.rename(DIFFERENT_NAME))

                ).addFlow(Api.flow().addTestStep(dataRecordFieldToStackDifferentName(stack2)).withDataSources(nodeDataSource.rename(DIFFERENT_NAME))

                ).addFlow(Api.flow().addTestStep(consumerToStackDifferentName(stack3)).withDataSources(iterableDataSource.rename(DIFFERENT_NAME))

                ).build();

        RxApiImpl.run(scenario);

        assertThat(stack1).containsExactlyInAnyOrder("SGSN-MME", "SGSN-14B");
        assertThat(stack2).containsExactlyInAnyOrder("SGSN-MME", "SGSN-14B");
        assertThat(stack3).containsExactlyInAnyOrder("a", "b");
    }

    private InlineInvocation stringProducer() {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            Object stringProducer(@Named(DATA_SOURCE) Integer fromDataSource) {
                return "return" + fromDataSource;
            }
        };
    }

    private TestStep dataRecordProducer() {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            DataRecord producer() {
                return BasicDataRecord.builder().setField(FIELD, "value").build();
            }
        };
    }

    private TestStep producerFromDataSource() {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            DataRecord producer(@Named(DATA_SOURCE) Integer fromDataSource, @Named(ScenarioContext.CONTEXT_RECORD_NAME) ScenarioContext context) {

                assertThat(context.getFieldValue(FIELD)).isNull();

                return BasicDataRecord.builder().setField(FIELD, "value" + fromDataSource).build();
            }
        };
    }

    private TestStep consumerToStack(final Stack<String> stack) {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumerToStack(@Named(FIELD) String value) {
                stack.add(value);
            }
        };
    }

    private TestStep consumerToStackDifferentName(final Stack<String> stack) {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumerToStack(@Named(DIFFERENT_NAME) String value) {
                stack.add(value);
            }
        };
    }

    private TestStep dataRecordToStackDifferentName(final Stack<String> stack) {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumerToStack(@Named(DIFFERENT_NAME) Node node) {
                stack.add(node.getNetworkElementId());
            }
        };
    }

    private TestStep dataRecordFieldToStackDifferentName(final Stack<String> stack) {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumerToStack(@Named(DIFFERENT_NAME + "." + Node.NETWORK_ELEMENT_ID) String nodeId) {
                stack.add(nodeId);
            }
        };
    }
}
