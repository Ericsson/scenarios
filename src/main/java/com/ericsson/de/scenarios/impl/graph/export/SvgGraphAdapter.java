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

import static com.google.common.collect.Maps.newHashMap;

import java.util.HashMap;
import java.util.Map;

import org.jgrapht.ext.JGraphXAdapter;

import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxConstants;

public class SvgGraphAdapter extends JGraphXAdapter<ScenarioExecutionGraph.GraphNode, ScenarioExecutionGraph.LabeledEdge> {

    SvgGraphAdapter(ScenarioExecutionGraph graph) {
        super(graph);
    }

    @Override
    public String getLabel(Object cell) {
        mxCell mxCell = (mxCell) cell;
        Object value = mxCell.getValue();
        if (value instanceof ScenarioExecutionGraph.GraphNode) {
            ScenarioExecutionGraph.GraphNode node = (ScenarioExecutionGraph.GraphNode) value;
            return node.getName();
        }

        return super.getLabel(cell);
    }

    @Override
    protected String getLinkForCell(Object cell) {
        mxCell mxCell = (mxCell) cell;
        Object value = mxCell.getValue();
        if (value instanceof ScenarioExecutionGraph.GraphNode) {
            ScenarioExecutionGraph.GraphNode node = (ScenarioExecutionGraph.GraphNode) value;
            return "#" + node.getVertexId();
        }

        return super.getLinkForCell(cell);
    }

    @Override
    public String getToolTipForCell(Object cell) {
        return "Click to see details...";
    }

    @Override
    public Map<String, Object> getCellStyle(Object cell) {
        mxCell mxCell = (mxCell) cell;
        Object value = mxCell.getValue();
        HashMap<String, Object> style = newHashMap();

        if (value instanceof ScenarioExecutionGraph.RootNode) {
            style.put(mxConstants.STYLE_FILLCOLOR, "#188079");
            style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
        } else if (value instanceof ScenarioExecutionGraph.SubFlowNode) {
            style.put(mxConstants.STYLE_FILLCOLOR, "#4fb889");
            style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CYLINDER);
        } else if (value instanceof ScenarioExecutionGraph.FlowEndedNode) {
            style.put(mxConstants.STYLE_FILLCOLOR, "#baffe0");
            style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CYLINDER);
        } else if (value instanceof ScenarioExecutionGraph.TestStepNode) {
            style.put(mxConstants.STYLE_FILLCOLOR, "#ffcc6e");
        } else if (value instanceof ScenarioExecutionGraph.ScenarioFinishedNode) {
            style.put(mxConstants.STYLE_FILLCOLOR, "#188079");
            style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
        }

        if (value instanceof ScenarioExecutionGraph.GraphNode) {
            ScenarioExecutionGraph.GraphNode node = (ScenarioExecutionGraph.GraphNode) value;
            if (node.isFailed()) {
                style.put(mxConstants.STYLE_FILLCOLOR, "#f74043");
            } else if (node.isSkipped()) {
                style.put(mxConstants.STYLE_FILLCOLOR, "#c0c0c0");
            }
        }

        return style.isEmpty() ? super.getCellStyle(cell) : style;
    }
}
