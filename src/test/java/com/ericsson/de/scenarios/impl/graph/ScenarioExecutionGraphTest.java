package com.ericsson.de.scenarios.impl.graph;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.de.scenarios.impl.graph.GraphNodeFactory.createFlowEndedNode;
import static com.ericsson.de.scenarios.impl.graph.GraphNodeFactory.createRootNode;
import static com.ericsson.de.scenarios.impl.graph.GraphNodeFactory.createScenarioFinishedNode;
import static com.ericsson.de.scenarios.impl.graph.GraphNodeFactory.createSubFlowNode;
import static com.ericsson.de.scenarios.impl.graph.GraphNodeFactory.createTestStepNode;
import static com.google.common.collect.Sets.newHashSet;

import org.junit.Test;

import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph.GraphNode;

public class ScenarioExecutionGraphTest {

    @Test
    public void verifyNodeEquality_RootNode() throws Exception {
        GraphNode node1 = createRootNode(0L, 0L);
        GraphNode node2 = createRootNode(0L, 0L);

        assertThat(node1).isEqualTo(node2);
    }

    @Test
    public void verifyNodeEquality_SubFlowNode_byId_dataRecord_vUser_meta() throws Exception {
        GraphNode node1 = createSubFlowNode("ID", "foo", "DATA_RECORD", "V_USER", "META", "ERROR");
        GraphNode node2 = createSubFlowNode("ID", "bar", "DATA_RECORD", "V_USER", "META", "ERROR");

        assertThat(node1).isEqualTo(node2);
    }

    @Test
    public void verifyNodeEquality_TestStepNode_byId_dataRecord_vUser() throws Exception {
        GraphNode node1 = createTestStepNode("ID", "foo", 0L, 0L, "DATA_RECORD", "V_USER", "1", "SUCCESS", "ERROR");
        GraphNode node2 = createTestStepNode("ID", "bar", 0L, 0L, "DATA_RECORD", "V_USER", "1", "SUCCESS", "ERROR");

        assertThat(node1).isEqualTo(node2);
    }

    @Test
    public void verifyNodeEquality_FlowEndedNode_byId_dataRecord_vUser_meta() throws Exception {
        GraphNode node1 = createFlowEndedNode("ID", "foo", "DATA_RECORD", "V_USER", "META", "ERROR");
        GraphNode node2 = createFlowEndedNode("ID", "bar", "DATA_RECORD", "V_USER", "META", "ERROR");

        assertThat(node1).isEqualTo(node2);
    }

    @Test
    public void verifyNodeEquality_ScenarioFinishedNode() throws Exception {
        GraphNode node1 = createScenarioFinishedNode(0L, 0L);
        GraphNode node2 = createScenarioFinishedNode(0L, 0L);

        assertThat(node1).isEqualTo(node2);
    }

    @Test
    public void verifyNodeInequality_SubFlowNode() throws Exception {
        GraphNode node = createSubFlowNode("ID", "NAME", "DATA_RECORD", "V_USER", "META", "ERROR");
        GraphNode node1 = createSubFlowNode("foo", "NAME", "DATA_RECORD", "V_USER", "META", "ERROR");
        GraphNode node2 = createSubFlowNode("ID", "NAME", "bar", "V_USER", "META", "ERROR");
        GraphNode node3 = createSubFlowNode("ID", "NAME", "DATA_RECORD", "baz", "META", "ERROR");
        GraphNode node4 = createSubFlowNode("ID", "NAME", "DATA_RECORD", "V_USER", "qux", "ERROR");
        GraphNode node5 = createSubFlowNode("ID", "NAME", "DATA_RECORD", "V_USER", "META", "foobar");

        assertDifferentNodes(node, node1, node2, node3, node4, node5);
    }

    @Test
    public void verifyNodeInequality_TestStepNode() throws Exception {
        GraphNode node = createTestStepNode("ID", "NAME", 0L, 0L, "DATA_RECORD", "V_USER", "1", "SUCCESS", "ERROR");
        GraphNode node1 = createTestStepNode("foo", "NAME", 0L, 0L, "DATA_RECORD", "V_USER", "1", "SUCCESS", "ERROR");
        GraphNode node2 = createTestStepNode("ID", "NAME", 0L, 0L, "bar", "V_USER", "SUCCESS", "1", "ERROR");
        GraphNode node3 = createTestStepNode("ID", "NAME", 0L, 0L, "DATA_RECORD", "baz", "SUCCESS", "1", "ERROR");
        GraphNode node4 = createTestStepNode("ID", "NAME", 0L, 0L, "DATA_RECORD", "V_USER", "1", "SUCCESS", "qux");

        assertDifferentNodes(node, node1, node2, node3, node4);
    }

    @Test
    public void verifyNodeInequality_FlowEndedNode() throws Exception {
        GraphNode node = createFlowEndedNode("ID", "NAME", "DATA_RECORD", "V_USER", "META", "ERROR");
        GraphNode node1 = createFlowEndedNode("foo", "NAME", "DATA_RECORD", "V_USER", "META", "ERROR");
        GraphNode node2 = createFlowEndedNode("ID", "NAME", "bar", "V_USER", "META", "ERROR");
        GraphNode node3 = createFlowEndedNode("ID", "NAME", "DATA_RECORD", "baz", "META", "ERROR");
        GraphNode node4 = createFlowEndedNode("ID", "NAME", "DATA_RECORD", "V_USER", "qux", "ERROR");
        GraphNode node5 = createFlowEndedNode("ID", "NAME", "DATA_RECORD", "V_USER", "META", "foobar");

        assertDifferentNodes(node, node1, node2, node3, node4, node5);
    }

    @Test
    public void verifyNodesInequality_differentNodeTypes() throws Exception {
        GraphNode root = createRootNode(0L, 0L);
        GraphNode subFlow = createSubFlowNode("ID", "NAME", "DATA_RECORD", "V_USER", "META", "ERROR");
        GraphNode testStep = createTestStepNode("ID", "NAME", 0L, 0L, "DATA_RECORD", "V_USER", "1", "SUCCESS", "ERROR");
        GraphNode flowEnded = createFlowEndedNode("ID", "NAME", "DATA_RECORD", "V_USER", "META", "ERROR");
        GraphNode finished = createScenarioFinishedNode(0L, 0L);

        assertDifferentNodes(root, subFlow, testStep, flowEnded, finished);
    }

    private void assertDifferentNodes(GraphNode... nodes) {
        assertThat(newHashSet(nodes)).hasSize(nodes.length);
    }
}
