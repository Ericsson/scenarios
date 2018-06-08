/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.de.scenarios.impl.graph.export;

import static java.util.Arrays.asList;

import static com.ericsson.de.scenarios.impl.graph.VertexTypeResolver.resolveVertexTypeName;

import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.jgrapht.ext.IntegerEdgeNameProvider;
import org.jgrapht.ext.IntegerNameProvider;

import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph;
import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph.GraphNode;
import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph.LabeledEdge;
import com.ericsson.de.scenarios.impl.graph.jgrapht.AttributeProvider;
import com.ericsson.de.scenarios.impl.graph.jgrapht.SuperGraphMLExporter;

public class GraphMlExporter extends AbstractGraphExporter {

    private static final String EXTENSION = ".graphml";
    private static final String VERTEX_TOOLTIP = "Vertex Tooltip";

    private SuperGraphMLExporter<GraphNode, LabeledEdge> exporter;

    public GraphMlExporter() {
        super(EXTENSION);
    }

    @Override
    protected void init() {
        List<AttributeProvider<GraphNode>> vertexAttributeProviders = asList(new AttributeProvider<GraphNode>(GraphNode.Attributes.NAME) {
                    @Override
                    public String getAttributeValue(GraphNode graphNode) {
                        return graphNode.getName();
                    }
                }, new AttributeProvider<GraphNode>(GraphNode.Attributes.VERTEX_ID) {
                    @Override
                    public String getAttributeValue(GraphNode graphNode) {
                        return graphNode.getVertexId();
                    }
                }, new AttributeProvider<GraphNode>(GraphNode.Attributes.VERTEX_TYPE) {
                    @Override
                    public String getAttributeValue(GraphNode graphNode) {
                        return resolveVertexTypeName(graphNode.getClass());
                    }
                },

                new AttributeProvider<GraphNode>(GraphNode.Attributes.ID) {
                    @Override
                    public String getAttributeValue(GraphNode graphNode) {
                        return graphNode.getId();
                    }
                }, new AttributeProvider<GraphNode>(GraphNode.Attributes.DATA_RECORDS) {
                    @Override
                    public String getAttributeValue(GraphNode graphNode) {
                        return graphNode.getDataRecord();
                    }
                }, new AttributeProvider<GraphNode>(GraphNode.Attributes.V_USER) {
                    @Override
                    public String getAttributeValue(GraphNode graphNode) {
                        return graphNode.getVUser();
                    }
                }, new AttributeProvider<GraphNode>(GraphNode.Attributes.META) {
                    @Override
                    public String getAttributeValue(GraphNode graphNode) {
                        return graphNode.getMeta();
                    }
                }, new AttributeProvider<GraphNode>(GraphNode.Attributes.VERTEX_FAILED) {
                    @Override
                    public String getAttributeValue(GraphNode graphNode) {
                        return graphNode.isFailed().toString();
                    }
                }, new AttributeProvider<GraphNode>(GraphNode.Attributes.EXCEPTION_NAME) {
                    @Override
                    public String getAttributeValue(GraphNode graphNode) {
                        return graphNode.getExceptionName();
                    }
                }, new AttributeProvider<GraphNode>(GraphNode.Attributes.STATUS) {
                    @Override
                    public String getAttributeValue(GraphNode graphNode) {
                        return graphNode.getStatus();
                    }
                }, new AttributeProvider<GraphNode>(VERTEX_TOOLTIP) {
                    @Override
                    public String getAttributeValue(GraphNode graphNode) {
                        return tooltip(graphNode);
                    }
                }, new AttributeProvider<GraphNode>(GraphNode.Attributes.START_TIME) {
                    @Override
                    public String getAttributeValue(GraphNode graphNode) {
                        return "" + graphNode.getStartTime();
                    }
                }, new AttributeProvider<GraphNode>(GraphNode.Attributes.END_TIME) {
                    @Override
                    public String getAttributeValue(GraphNode graphNode) {
                        return "" + graphNode.getEndTime();
                    }
                });

        List<AttributeProvider<LabeledEdge>> edgeAttributeProviders = Arrays.<AttributeProvider<LabeledEdge>>asList(
                new AttributeProvider<LabeledEdge>(LabeledEdge.EDGE_LABEL) {
                    @Override
                    public String getAttributeValue(LabeledEdge edge) {
                        return edge.toString();
                    }
                });

        exporter = new SuperGraphMLExporter<>(new IntegerNameProvider<GraphNode>(), vertexAttributeProviders,
                new IntegerEdgeNameProvider<LabeledEdge>(), edgeAttributeProviders);
    }

    @Override
    public void export(ScenarioExecutionGraph graph, Writer writer) throws Exception {
        exporter.export(writer, graph);
    }
}
