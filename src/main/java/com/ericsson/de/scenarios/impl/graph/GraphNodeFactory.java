package com.ericsson.de.scenarios.impl.graph;

import static com.ericsson.de.scenarios.impl.graph.VertexTypeResolver.resolveVertexType;
import static com.google.common.base.Strings.nullToEmpty;

import java.util.Map;

import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph.FlowEndedNode;
import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph.GraphNode;
import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph.RootNode;
import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph.ScenarioFinishedNode;
import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph.SubFlowNode;
import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph.TestStepNode;
import com.ericsson.de.scenarios.impl.graph.VertexTypeResolver.VertexType;

@SuppressWarnings("WeakerAccess")
public final class GraphNodeFactory {

    private GraphNodeFactory() {
    }

    static GraphNode createGraphNode(Map<String, String> attrMap) {
        String vertexTypeName = getAttribute(attrMap, GraphNode.Attributes.VERTEX_TYPE);

        String id = getAttribute(attrMap, GraphNode.Attributes.ID);
        String name = getAttribute(attrMap, GraphNode.Attributes.NAME);
        String dataRecord = getAttribute(attrMap, GraphNode.Attributes.DATA_RECORDS);
        String vUser = getAttribute(attrMap, GraphNode.Attributes.V_USER);
        String exceptionName = getAttribute(attrMap, GraphNode.Attributes.EXCEPTION_NAME);
        String meta = getAttribute(attrMap, GraphNode.Attributes.META);
        String status = getAttribute(attrMap, GraphNode.Attributes.STATUS);
        String startTimeAttr = getAttribute(attrMap, GraphNode.Attributes.START_TIME);
        Long startTime = startTimeAttr.isEmpty() ? 0 : Long.valueOf(startTimeAttr);
        String endTimeAttr = getAttribute(attrMap, GraphNode.Attributes.END_TIME);
        Long endTime = endTimeAttr.isEmpty() ? 0 : Long.valueOf(startTimeAttr);

        VertexType vertexType = resolveVertexType(vertexTypeName);
        switch (vertexType) {
            case ROOT:
                return createRootNode(startTime, endTime);
            case SUB_FLOW:
                return createSubFlowNode(id, name, dataRecord, vUser, meta, exceptionName);
            case TEST_STEP:
                return createTestStepNode(id, name, startTime, endTime, dataRecord, vUser, meta, status, exceptionName);
            case FLOW_ENDED:
                return createFlowEndedNode(id, name, dataRecord, vUser, meta, exceptionName);
            case SCENARIO_FINISHED:
                return createScenarioFinishedNode(startTime, endTime);
            default:
                throw new RuntimeException("Unknown vertex type: " + vertexTypeName);
        }
    }

    private static String getAttribute(Map<String, String> attrMap, String id) {
        return nullToEmpty(attrMap.get(id));
    }

    public static GraphNode createRootNode(long startTime, long endTime) {
        return new RootNode(startTime, endTime);
    }

    /*---------------- Exception name ----------------*/

    public static SubFlowNode createSubFlowNode(String id, String name, String dataRecord, String vUser, String meta, String exceptionName) {
        return createSubFlowNode(id, name, dataRecord, vUser, meta, exceptionName, null);
    }

    public static TestStepNode createTestStepNode(String id, String name, Long startTime, Long endTime, String dataRecord, String vUser, String meta,
            String status, String exceptionName) {
        return createTestStepNode(id, name, startTime, endTime, dataRecord, vUser, meta, status, exceptionName, null);
    }

    public static FlowEndedNode createFlowEndedNode(String id, String name, String dataRecord, String vUser, String meta, String exceptionName) {
        return createFlowEndedNode(id, name, dataRecord, vUser, meta, exceptionName, null);
    }

    /*---------------- Exception ----------------*/

    public static SubFlowNode createSubFlowNode(String id, String name, String dataRecord, String vUser, String meta, Throwable exception) {
        return createSubFlowNode(id, name, dataRecord, vUser, meta, errorName(exception), exception);
    }

    public static TestStepNode createTestStepNode(String id, String name, Long startTime, Long endTime, String dataRecord, String vUser, String meta,
            String status, Throwable exception) {
        return createTestStepNode(id, name, startTime, endTime, dataRecord, vUser, meta, status, errorName(exception), exception);
    }

    public static FlowEndedNode createFlowEndedNode(String id, String name, String dataRecord, String vUser, String meta, Throwable exception) {
        return createFlowEndedNode(id, name, dataRecord, vUser, meta, errorName(exception), exception);
    }

    private static String errorName(Throwable error) {
        return error == null ? "" : error.getClass().getName();
    }

    /*---------------- Full constructors ----------------*/

    public static SubFlowNode createSubFlowNode(String id, String name, String dataRecord, String vUser, String meta, String exceptionName,
            Throwable exception) {
        return new SubFlowNode(id, name, dataRecord, vUser, meta, exceptionName, exception);
    }

    public static TestStepNode createTestStepNode(String id, String name, Long startTime, Long endTime, String dataRecord, String vUser, String meta,
            String status, String exceptionName, Throwable exception) {
        return new TestStepNode(id, name, startTime, endTime, dataRecord, vUser, meta, status, exceptionName, exception);
    }

    public static FlowEndedNode createFlowEndedNode(String id, String name, String dataRecord, String vUser, String meta, String exceptionName,
            Throwable exception) {
        return new FlowEndedNode(id, name, dataRecord, vUser, meta, exceptionName, exception);
    }

    public static ScenarioFinishedNode createScenarioFinishedNode(long startTime, long endTime) {
        return new ScenarioFinishedNode(startTime, endTime);
    }
}
