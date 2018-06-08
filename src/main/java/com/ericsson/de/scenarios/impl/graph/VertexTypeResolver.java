package com.ericsson.de.scenarios.impl.graph;

import static java.util.Collections.unmodifiableMap;

import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;

public final class VertexTypeResolver {

    enum VertexType {

        ROOT(ScenarioExecutionGraph.RootNode.class), SUB_FLOW(ScenarioExecutionGraph.SubFlowNode.class), TEST_STEP(
                ScenarioExecutionGraph.TestStepNode.class), FLOW_ENDED(ScenarioExecutionGraph.FlowEndedNode.class), SCENARIO_FINISHED(
                ScenarioExecutionGraph.ScenarioFinishedNode.class);

        private Class<? extends ScenarioExecutionGraph.GraphNode> nodeClass;

        VertexType(Class<? extends ScenarioExecutionGraph.GraphNode> nodeClass) {
            this.nodeClass = nodeClass;
        }
    }

    private final static Map<String, VertexType> vertexTypeMap;

    static {
        Map<String, VertexType> typeMap = newHashMap();
        for (VertexType vertexType : VertexType.values()) {
            typeMap.put(resolveVertexTypeName(vertexType.nodeClass), vertexType);
        }
        vertexTypeMap = unmodifiableMap(typeMap);
    }

    private VertexTypeResolver() {
    }

    public static String resolveVertexTypeName(Class<? extends ScenarioExecutionGraph.GraphNode> aClass) {
        return aClass.getSimpleName();
    }

    public static VertexType resolveVertexType(String vertexType) {
        return vertexTypeMap.get(vertexType);
    }
}
