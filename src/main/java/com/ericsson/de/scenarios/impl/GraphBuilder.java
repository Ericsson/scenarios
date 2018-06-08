package com.ericsson.de.scenarios.impl;
/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

import static com.ericsson.de.scenarios.impl.RxDataSource.endTime;
import static com.ericsson.de.scenarios.impl.RxDataSource.startTime;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newArrayList;

import java.util.Iterator;
import java.util.List;

import com.ericsson.de.scenarios.impl.graph.GraphNodeFactory;
import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph;

/**
 * List<Internals.FlowExecutionResult> → ScenarioExecutionGraph
 */
class GraphBuilder {

    static ScenarioExecutionGraph build(List<Internals.FlowExecutionResult> results) {
        checkArgument(!results.isEmpty(), "No RxFlow Execution Results supplied");
        ScenarioExecutionGraph graph = new ScenarioExecutionGraph();

        long startTime = startTime(results.get(0).executions);
        long endTime = endTime(getLast(results).executions);

        ScenarioExecutionGraph.GraphNode root = GraphNodeFactory.createRootNode(startTime, endTime);
        graph.addVertex(root);

        Iterator<Internals.FlowExecutionResult> resultIterator = results.iterator();
        List<ScenarioExecutionGraph.GraphNode> preJoinNodes = addExpandedFork(graph, root, resultIterator.next());

        while (resultIterator.hasNext()) {
            ScenarioExecutionGraph.GraphNode forkNode = preJoinNodes.iterator().next();
            Internals.FlowExecutionResult result = resultIterator.next();
            preJoinNodes = addExpandedFork(graph, forkNode, result);
        }

        ScenarioExecutionGraph.GraphNode finish = GraphNodeFactory.createScenarioFinishedNode(startTime, endTime);
        graph.addVertex(finish);

        joinNodes(graph, preJoinNodes, finish);

        return graph;
    }

    private static List<ScenarioExecutionGraph.GraphNode> addExpandedFork(ScenarioExecutionGraph graph, ScenarioExecutionGraph.GraphNode forkNode,
            Internals.FlowExecutionResult result) {
        List<ScenarioExecutionGraph.GraphNode> preJoinNodes = newArrayList();
        for (Internals.Exec execution : result.executions) {
            preJoinNodes.add(addExecution(graph, forkNode, execution));
        }
        return preJoinNodes;
    }

    private static ScenarioExecutionGraph.GraphNode addExecution(ScenarioExecutionGraph graph, ScenarioExecutionGraph.GraphNode firstNode,
            Internals.Exec execution) {
        ScenarioExecutionGraph.GraphNode lastNode = firstNode;
        for (Internals.TestStepResult testStepResult : execution.getExecutedTestSteps()) {
            ScenarioExecutionGraph.GraphNode newNode;
            if (testStepResult instanceof Internals.FlowExecutionResult) {
                Internals.FlowExecutionResult result = (Internals.FlowExecutionResult) testStepResult;
                if (skipForkNodeCreation(execution)) {
                    newNode = lastNode;
                } else {
                    newNode = createForkNode(execution, result);
                    graph.addVertex(newNode);
                    graph.addEdge(lastNode, newNode);
                }
                lastNode = addJoinedFork(graph, newNode, result);
            } else {
                newNode = createStepNode(execution, testStepResult);
                graph.addVertex(newNode);
                addEdge(graph, execution, lastNode, newNode);
                lastNode = newNode;
            }
        }
        return lastNode;
    }

    private static boolean skipForkNodeCreation(Internals.Exec execution) {
        return execution.vUser.isScenarioLevel();
    }

    private static ScenarioExecutionGraph.GraphNode addJoinedFork(ScenarioExecutionGraph graph, ScenarioExecutionGraph.GraphNode forkNode,
            Internals.FlowExecutionResult result) {
        List<ScenarioExecutionGraph.GraphNode> preJoinNodes = addExpandedFork(graph, forkNode, result);
        ScenarioExecutionGraph.GraphNode joinNode = createJoinNode(forkNode, result);
        graph.addVertex(joinNode);
        joinNodes(graph, preJoinNodes, joinNode);
        return joinNode;
    }

    private static ScenarioExecutionGraph.GraphNode createForkNode(Internals.Exec execution, Internals.FlowExecutionResult result) {
        return GraphNodeFactory.createSubFlowNode(result.id, result.name, execution.dataRecord.toString(), execution.vUser.getId(), "", result.error);
    }

    private static ScenarioExecutionGraph.TestStepNode createStepNode(Internals.Exec execution, Internals.TestStepResult result) {
        return GraphNodeFactory.createTestStepNode(result.id, result.name, result.startTime, result.endTime, execution.dataRecord.toString(),
                execution.vUser.getId(), "", result.status.name(), result.error);
    }

    private static ScenarioExecutionGraph.GraphNode createJoinNode(ScenarioExecutionGraph.GraphNode forkNode, Internals.FlowExecutionResult result) {
        return GraphNodeFactory.createFlowEndedNode(result.id, "join", forkNode.getDataRecord(), forkNode.getVUser(), "", forkNode.getException());
    }

    private static void joinNodes(ScenarioExecutionGraph graph, List<ScenarioExecutionGraph.GraphNode> preJoinNodes,
            ScenarioExecutionGraph.GraphNode joinNode) {
        for (ScenarioExecutionGraph.GraphNode preJoinNode : preJoinNodes) {
            graph.addEdge(preJoinNode, joinNode);
        }
    }

    private static void addEdge(ScenarioExecutionGraph graph, Internals.Exec execution, ScenarioExecutionGraph.GraphNode from,
            ScenarioExecutionGraph.GraphNode to) {
        if (shouldLabelEdge(execution.vUser.getId(), from)) {
            graph.addEdge(from, to, createLabeledEdge(execution));
        } else {
            graph.addEdge(from, to);
        }
    }

    private static boolean shouldLabelEdge(String vUserId, ScenarioExecutionGraph.GraphNode lastTestStep) {
        return !lastTestStep.getVUser().equals(vUserId) || lastTestStep instanceof ScenarioExecutionGraph.RootNode
                || lastTestStep instanceof ScenarioExecutionGraph.FlowEndedNode;
    }

    private static ScenarioExecutionGraph.LabeledEdge createLabeledEdge(Internals.Exec execution) {
        return new ScenarioExecutionGraph.LabeledEdge("vUser " + execution.vUser);
    }
}
