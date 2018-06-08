/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.de.scenarios.impl.graph;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;

public class ScenarioExecutionGraph extends DefaultDirectedGraph<ScenarioExecutionGraph.GraphNode, ScenarioExecutionGraph.LabeledEdge> {
    public ScenarioExecutionGraph() {
        super(LabeledEdge.class);
    }

    @Override
    public boolean addVertex(GraphNode graphNode) {
        if (vertexSet().contains(graphNode)) {
            System.out.println();
        }

        Preconditions.checkArgument(!vertexSet().contains(graphNode),
                "Attempt to add node that equals node already existing in graph.\n" + "Probably something wrong with .equals()/.hashCode().\n"
                        + "Node: " + graphNode);

        return super.addVertex(graphNode);
    }

    public static class LabeledEdge extends DefaultEdge {

        public static final String EDGE_LABEL = "Edge Label";

        private final String name;

        @SuppressWarnings("unused")
        public LabeledEdge() {
            this("");
        }

        public LabeledEdge(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof LabeledEdge))
                return false;
            LabeledEdge that = (LabeledEdge) o;
            return Objects.equals(name, that.name) && Objects.equals(getSource(), that.getSource()) && Objects.equals(getTarget(), that.getTarget());
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, getSource(), getTarget());
        }
    }

    public static class RootNode extends GraphNode {

        private static final String SCENARIO_STARTED = "RxScenario started";

        RootNode(long startTime, long endTime) {
            super(SCENARIO_STARTED);
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    private abstract static class FlowNode extends GraphNode {
        protected FlowNode(String id, String name, String dataRecord, String vUser, String meta, String exceptionName, Throwable exception) {
            super(id, name, vUser, meta, dataRecord, "SUCCESS", exceptionName, exception);
        }
    }

    public static class SubFlowNode extends FlowNode {

        SubFlowNode(String id, String name, String dataRecord, String vUser, String meta, String exceptionName, Throwable exception) {
            super(id, name, dataRecord, vUser, meta, exceptionName, exception);
        }
    }

    public static class FlowEndedNode extends FlowNode {

        FlowEndedNode(String id, String name, String dataRecord, String vUser, String meta, String exceptionName, Throwable exception) {
            super(id, name, dataRecord, vUser, meta, exceptionName, exception);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof FlowEndedNode && super.equals(o);
        }
    }

    public static class TestStepNode extends GraphNode {
        TestStepNode(String id, String name, Long startTime, Long endTime, String dataRecord, String vUser, String meta, String status,
                String exceptionName, Throwable exception) {
            super(id, name, vUser, meta, dataRecord, status, exceptionName, exception);
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public String getVertexId() {
            return id;
        }
    }

    public static class ScenarioFinishedNode extends GraphNode {

        private static final String SCENARIO_FINISHED = "RxScenario finished";

        public ScenarioFinishedNode(long startTime, long endTime) {
            super(SCENARIO_FINISHED);
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public static abstract class GraphNode {

        public interface Attributes {

            String VERTEX_ID = "Vertex ID";
            String VERTEX_TYPE = "Vertex Type";

            String ID = "Invocation ID";
            String NAME = "Vertex Label";
            String V_USER = "vUser";
            String DATA_RECORDS = "Data Records";
            String META = "Meta";

            String STATUS = "Vertex Status";
            String EXCEPTION_NAME = "Vertex Exception";
            String VERTEX_FAILED = "Vertex Failed";

            String START_TIME = "Start Time";
            String END_TIME = "End Time";
        }

        static final AtomicLong idGenerator = new AtomicLong();

        private final Long vertexId = idGenerator.incrementAndGet();

        final String id;
        final String name;
        final String vUser;
        final String meta;
        final String dataRecord;
        final String status;
        final String exceptionName;
        final Throwable exception;
        Long startTime = 0L;
        Long endTime = 0L;

        GraphNode(String name) {
            this("", name, "", "", "", "SUCCESS", "", null);
        }

        GraphNode(String id, String name, String vUser, String meta, String dataRecord, String status, String exceptionName, Throwable exception) {
            this.id = id;
            this.name = name;
            this.vUser = vUser;
            this.meta = meta;
            this.dataRecord = dataRecord;
            this.exception = exception;
            this.exceptionName = exceptionName;
            this.status = status;
        }

        public String getVertexId() {
            return "" + vertexId;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getVUser() {
            return vUser;
        }

        public String getMeta() {
            return meta;
        }

        public String getDataRecord() {
            return dataRecord;
        }

        public boolean hasDataRecord() {
            return !isNullOrEmpty(dataRecord);
        }

        public String getStatus() {
            return status;
        }

        public boolean isSkipped() {
            return "SKIPPED".equals(status);
        }

        public Throwable getException() {
            return exception;
        }

        public String getExceptionName() {
            return exceptionName;
        }

        public Boolean isFailed() {
            return !isNullOrEmpty(exceptionName);
        }

        public Long getExecutionTime() {
            return endTime - startTime;
        }

        public Long getStartTime() {
            return startTime;
        }

        public Long getEndTime() {
            return endTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(this.getClass().equals(o.getClass())))
                return false;
            GraphNode graphNode = (GraphNode) o;
            return Objects.equals(id, graphNode.id) && Objects.equals(dataRecord, graphNode.dataRecord) && Objects.equals(vUser, graphNode.vUser)
                    && Objects.equals(meta, graphNode.meta) && Objects.equals(exceptionName, graphNode.exceptionName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, dataRecord, vUser, exceptionName);
        }

        @Override
        public String toString() {
            return appendBaseString(toStringHelper(getClass())).toString();
        }

        ToStringHelper appendBaseString(ToStringHelper toStringHelper) {
            return toStringHelper.add("id", id).add("name", name).add("dataRecord", dataRecord).add("vUser", vUser).add("meta", meta)
                    .add("exceptionName", exceptionName);
        }
    }
}
