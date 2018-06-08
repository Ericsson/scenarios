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

import static com.ericsson.de.scenarios.api.RxApiImpl.contextDataSource;
import static com.ericsson.de.scenarios.api.RxApiImpl.flow;
import static com.ericsson.de.scenarios.api.RxApiImpl.runner;
import static com.ericsson.de.scenarios.api.RxApiImpl.scenario;
import static com.ericsson.de.scenarios.impl.RxApi.fromDataRecords;
import static com.ericsson.de.scenarios.impl.RxApi.fromIterable;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Stack;
import javax.inject.Named;

import org.junit.Test;

import com.ericsson.de.scenarios.Node;
import com.ericsson.de.scenarios.api.BasicDataRecord;
import com.ericsson.de.scenarios.api.ContextDataSource;
import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.api.DataSource;
import com.ericsson.de.scenarios.api.RxApiImpl;
import com.ericsson.de.scenarios.api.Scenario;
import com.ericsson.de.scenarios.impl.ScenarioDebugger;
import com.ericsson.de.scenarios.impl.ScenarioTest;
import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph;

public class DataSourceTest extends ScenarioTest {

    @Test
    public void scenarioWithSharedDataSources() throws Exception {
        List<String> dataSource = newArrayList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");
        List<String> subFlowDataSource = newArrayList("a", "b", "c", "d", "e", "f");
        List<String> subSubFlowDataSource = newArrayList("x", "o", "+");

        Scenario scenario = scenario().addFlow(flow().addTestStep(print("flow1")).addTestStep(print("flow2")).addSubFlow(
                flow().addTestStep(print("subFlow1")).addSubFlow(flow().addTestStep(print("subSubFlow1")).withVUsers(2)
                        .withDataSources(fromIterable("subSubFlowDataSource", subSubFlowDataSource).shared())).withVUsers(2)
                        .withDataSources(fromIterable("subFlowDataSource", subFlowDataSource).shared())).addTestStep(print("flow3")).withVUsers(3)
                .withDataSources(fromIterable("dataSource", dataSource).shared())).build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "scenarioWithSharedDataSources.graphml");
    }

    @Test
    public void scenarioWithRegularDataSources() throws Exception {
        List dataSource = newArrayList("1", "2");
        List subFlowDataSource = newArrayList("a", "b");
        List subSubFlowDataSource = newArrayList("x", "o");

        Scenario scenario = scenario().addFlow(flow().addTestStep(print("flow1")).addTestStep(print("flow2")).addSubFlow(
                flow().addTestStep(print("subFlow1")).addSubFlow(flow().addTestStep(print("subSubFlow1")).withVUsers(2)
                        .withDataSources(fromIterable("subSubFlowDataSource", subSubFlowDataSource))).withVUsers(2)
                        .withDataSources(fromIterable("subFlowDataSource", subFlowDataSource))).addTestStep(print("flow3")).withVUsers(2)
                .withDataSources(fromIterable("dataSource", dataSource))).build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "scenarioWithRegularDataSources.graphml");
    }

    @Test
    public void sharedDataSourcesTest_ChildIsSmaller() throws Exception {
        List<String> dataSource = newArrayList("1", "2", "3", "4");
        List<String> subFlowDataSource = newArrayList("a", "b", "c");

        Scenario scenario = scenario().addFlow(flow().addSubFlow(
                flow().addTestStep(print("subFlow1")).withVUsers(2).withDataSources(fromIterable("subFlowDataSource", subFlowDataSource).shared()))
                .withVUsers(2).withDataSources(fromIterable("dataSource", dataSource).shared())).build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "sharedDataSourcesTest_ChildIsSmaller.graphml");
    }

    @Test
    public void sharedDataSourcesTest_ChildIsLarger() throws Exception {
        List<String> dataSource = newArrayList("1", "2", "3", "4");
        List<String> subFlowDataSource = newArrayList("a", "b", "c", "d", "e", "f");

        Scenario scenario = scenario().addFlow(flow().addSubFlow(
                flow().addTestStep(print("subFlow1")).withVUsers(2).withDataSources(fromIterable("subFlowDataSource", subFlowDataSource).shared()))
                .withVUsers(2).withDataSources(fromIterable("dataSource", dataSource).shared())).build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "sharedDataSourcesTest_ChildIsLarger.graphml");
    }

    @Test
    public void combinationOfTwoDataSources() throws Exception {
        List<String> dataSource1 = newArrayList("1", "2", "3", "4");
        List<String> dataSource2 = newArrayList("a", "b", "c");

        List<String> dataSource3 = newArrayList("w", "x", "y", "z");
        List<String> dataSource4 = newArrayList("+", "o", "/");

        Scenario scenario = scenario().addFlow(flow().addTestStep(print("flow1")).addSubFlow(flow().addTestStep(print("subFlow1")).withVUsers(2)
                .withDataSources(fromIterable("dataSource3", dataSource3), fromIterable("dataSource4", dataSource4))).withVUsers(2)
                .withDataSources(fromIterable("dataSource1", dataSource1), fromIterable("dataSource2", dataSource2))).build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "combinationOfTwoDataSources.graphml");
    }

    @Test
    public void sharedAndNotSharedDataSource_Flow() throws Exception {
        List<String> sharedDataSource1 = newArrayList("1", "2", "3", "4");
        List<String> copiedDataSource2 = newArrayList("a", "b", "c");

        Scenario scenario = scenario().addFlow(flow().addTestStep(print("subFlow1")).withVUsers(2)
                .withDataSources(fromIterable("sharedDataSource1", sharedDataSource1).shared(), fromIterable("copiedDataSource2", copiedDataSource2)))
                .build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "sharedAndNotSharedDataSource_Flow.graphml");
    }

    @Test
    public void sharedAndNotSharedDataSource_SubFlow() throws Exception {
        List<String> dataSource1 = newArrayList("1", "2", "3", "4");
        List<String> dataSource2 = newArrayList("a", "b", "c", "d");

        Scenario scenario = scenario().addFlow(flow().addTestStep(print("subFlow1")).withVUsers(2)
                .withDataSources(fromIterable("dataSource1", dataSource1).shared(), fromIterable("dataSource2", dataSource2))).build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "sharedAndNotSharedDataSource_SubFlow.graphml");
    }

    @Test
    public void moreDataSources() throws Exception {
        List<String> dataSource1 = newArrayList("1", "2", "3", "4");
        List<String> dataSource2 = newArrayList("a", "b", "c");
        List<String> dataSource3 = newArrayList("w", "x", "y", "z");

        Scenario scenario = scenario().addFlow(flow().addTestStep(print("flow1"))
                .withDataSources(fromIterable("dataSource1", dataSource1), fromIterable("dataSource2", dataSource2),
                        fromIterable("dataSource3", dataSource3)).withVUsers(2)).build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "moreDataSources.graphml");
    }

    @Test(timeout = 10000L)
    public void moreDataSourcesCyclic() throws Exception {
        List<String> dataSource1 = newArrayList("1", "2", "3", "4");
        List<String> dataSource2 = newArrayList("a", "b", "c");
        List<String> dataSource3 = newArrayList("w", "x", "y", "z");

        Scenario scenario = scenario().addFlow(flow().addTestStep(print("flow1"))
                .withDataSources(fromIterable("dataSource1", dataSource1), fromIterable("dataSource2", dataSource2).cyclic(),
                        fromIterable("dataSource3", dataSource3)).withVUsers(2)).build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "moreDataSourcesCyclic.graphml");
    }

    @Test
    public void moreDataSourcesShared() throws Exception {
        List<String> dataSource1 = newArrayList("1", "2", "3", "4");
        List<String> dataSource2 = newArrayList("a", "b", "c");
        List<String> dataSource3 = newArrayList("w", "x", "y", "z");

        Scenario scenario = scenario().addFlow(flow().addTestStep(print("flow1"))
                .withDataSources(fromIterable("dataSource1", dataSource1), fromIterable("dataSource2", dataSource2).shared(),
                        fromIterable("dataSource3", dataSource3)).withVUsers(2)).build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "moreDataSourcesShared.graphml");
    }

    @Test
    public void moreDataSourcesAllShared() throws Exception {
        List<String> dataSource1 = newArrayList("1", "2", "3", "4");
        List<String> dataSource2 = newArrayList("a", "b", "c");
        List<String> dataSource3 = newArrayList("w", "x", "y", "z");

        Scenario scenario = scenario().addFlow(flow().addTestStep(print("flow1"))
                .withDataSources(fromIterable("dataSource1", dataSource1).shared(), fromIterable("dataSource2", dataSource2).shared(),
                        fromIterable("dataSource3", dataSource3).shared()).withVUsers(2)).build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "moreDataSourcesAllShared.graphml");
    }

    @Test
    public void testDataRecords() throws Exception {
        final String ds = "DS";
        final String subFlowDS1 = "subFlowDS1";
        final String subFlowDS2 = "subFlowDS2";
        final String subSubFlowDS = "subSubFlowDS";

        Scenario scenario = scenario().addFlow(flow().addSubFlow(flow().addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            public void validateSubFlow1(@Named("key") String akey, @Named(subFlowDS1 + ".key") String bkey, @Named(subFlowDS2 + ".key") String ckey,
                    @Named(ds + ".key") String dkey) {

                assertThat(akey).isEqualTo(subFlowDS1);
                assertThat(bkey).isEqualTo(subFlowDS1);
                assertThat(ckey).isEqualTo(subFlowDS2);
                assertThat(dkey).isEqualTo(ds);
            }
        }).addSubFlow(flow().addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            public void validateSubSubFlow1(@Named("key") String akey, @Named(subSubFlowDS + ".key") String bkey,
                    @Named(subFlowDS1 + ".key") String ckey, @Named(subFlowDS2 + ".key") String dkey, @Named(ds + ".key") String ekey) {

                assertThat(akey).isEqualTo(subSubFlowDS);
                assertThat(bkey).isEqualTo(subSubFlowDS);
                assertThat(ckey).isEqualTo(subFlowDS1);
                assertThat(dkey).isEqualTo(subFlowDS2);
                assertThat(ekey).isEqualTo(ds);
            }
        }).withDataSources(fromDataRecords(subSubFlowDS, BasicDataRecord.fromValues("key", subSubFlowDS))))
                .withDataSources(fromDataRecords(subFlowDS1, BasicDataRecord.fromValues("key", subFlowDS1)),
                        fromDataRecords(subFlowDS2, BasicDataRecord.fromValues("key", subFlowDS2))))
                .withDataSources(fromDataRecords(ds, BasicDataRecord.fromValues("key", ds)))).build();

        RxApiImpl.run(scenario);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionOnNonExistingField() throws Exception {
        DataSource<Node> dataSource = getNodeDataSource();

        Scenario scenario = scenario()
                .addFlow(flow().addTestStep(print("subFlow1")).withDataSources(dataSource.filterField("not_existing_field").equalTo("nope"))).build();

        RxApiImpl.run(scenario);
    }

    @Test
    public void filterDataSource() throws Exception {
        final Stack<String> ids = new Stack<>();

        String targetId = "AWSM-MEE";
        DataSource<Node> dataSource = fromDataRecords("nodes", getNode("SGSN-14B", "LTE01ERB", 20), getNode("SGSN-MME", "ERBS", 20),
                getNode(targetId, "ERBS", 20));

        Scenario scenario = scenario().addFlow(flow().addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            void idToStack(@Named(Node.NETWORK_ELEMENT_ID) String id) {
                ids.add(id);
            }
        }).withDataSources(
                dataSource.filterField(Node.PORT).equalTo(20).filterField(Node.NETWORK_ELEMENT_ID).contains("MEE").filterField(Node.NODE_TYPE)
                        .equalToIgnoreCase("eRbS"))).build();

        RxApiImpl.run(scenario);

        assertThat(ids).containsExactly(targetId);
    }

    @Test
    public void filterSharedDataSourcePredictability() throws Exception {
        DataSource<Node> dataSource = fromDataRecords("nodes", getNode("SGSN-14B", "LTE01ERB", 20), getNode("SGSN-MME", "ERBS", 20),
                getNode("AWSM-MEE", "ERBS", 20), getNode("DESP-MME", "ERBS", 20), getNode("MEEE-MEE", "ERBS", 20));

        Scenario scenario = scenario().addFlow(flow("parent").addSubFlow(flow("subFlow1").addTestStep(print("subFlow1")).withVUsers(3)
                .withDataSources(dataSource.filterField(Node.NODE_TYPE).equalTo("ERBS").shared()))
                .withDataSources(fromIterable("parentDs", asList("1", "2")))).build();

        ScenarioExecutionGraph debug = ScenarioDebugger.debug(scenario);

        compareGraphs(debug, "filterSharedDataSourcePredictability.graphml");
    }

    @Test
    public void filterContextDataSource() throws Exception {
        final Stack<String> ids = new Stack<>();

        ContextDataSource<Node> contextDataSource = contextDataSource("name", Node.class);

        final String targetId = "AWSM-MEE";
        Scenario scenario = scenario().addFlow(flow().addTestStep(new InlineInvocation() {
            DataRecord producer(@Named("nodesToCreateIds") String id) {
                return getNode(id, "type", 80);
            }
        }.collectResultsToDataSource(contextDataSource))
                .withDataSources(fromIterable("nodesToCreateIds", asList(targetId, targetId, "SGSN-14B", "SGSN-MME"))))
                .addFlow(flow("ds").addTestStep(new InlineInvocation() {
                    void idToStack(@Named(Node.NETWORK_ELEMENT_ID) String id) {
                        ids.add(id);
                    }
                }).withVUsers(2).withDataSources(contextDataSource.shared().filterField(Node.NETWORK_ELEMENT_ID).equalTo(targetId))).build();

        ScenarioDebugger.debug(scenario);

        assertThat(ids).containsExactly(targetId, targetId);
    }

    @Test(timeout = 10000L)
    public void cyclicDataSource() throws Exception {
        final Stack<String> usernodes = new Stack<>();

        DataSource<Node> nodes = fromDataRecords("nodes", getNode("node1", "LTE01ERB", 20), getNode("node2", "ERBS", 20),
                getNode("node3", "ERBS", 20), getNode("node4", "ERBS", 20), getNode("node5", "ERBS", 20));

        DataSource<String> users = fromIterable("user", asList("user1", "user2", "user3"));

        Scenario scenario = scenario().addFlow(flow().addTestStep(new InlineInvocation() {
            @SuppressWarnings("unused")
            void idToStack(@Named(Node.NETWORK_ELEMENT_ID) String id, @Named("user") String user) {
                usernodes.add(user + "-" + id);
            }
        }).withVUsers(3).withDataSources(nodes.shared(), users.shared().cyclic())).build();

        RxApiImpl.run(scenario);

        assertThat(usernodes).containsExactlyInAnyOrder("user1-node1", "user2-node2", "user3-node3", "user1-node4", "user2-node5");
    }

    @Test
    public void filteredDataSourceSize() throws Exception {
        String DS_NAME = "users";

        DataSource<String> users = fromIterable(DS_NAME, asList("user1", "user2", "user3", "admin1", "admin2")).filterField(DS_NAME).contains("user");

        assertThat(users.getSize()).isEqualTo(3);
        assertThat(users).hasSize(3);
    }

    @Test(timeout = 10000L)
    public void cyclicDataSourceSize() throws Exception {
        DataSource<String> users = fromIterable("user", asList("user1", "user2", "user3"));

        assertThat(users.cyclic().getSize()).isEqualTo(users.getSize());
    }

    @Test
    public void dataRecordTest() throws Exception {
        final Stack<String> ids = new Stack<>();

        Scenario scenario = scenario().addFlow(flow().addTestStep(new InlineInvocation() {
            void producer(@Named("nodes") Node node) {
                ids.push(node.getNetworkElementId());
                ids.push(node.getAllFields().get(Node.NETWORK_ELEMENT_ID).toString());
                ids.push(node.getFieldValue(Node.NETWORK_ELEMENT_ID).toString());
            }
        }).withDataSources(getNodeDataSource())).build();

        runner().build().run(scenario);

        assertThat(ids).containsExactly("SGSN-14B", "SGSN-14B", "SGSN-14B", "SGSN-MME", "SGSN-MME", "SGSN-MME");
    }
}
