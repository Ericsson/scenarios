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

import static com.google.common.base.Throwables.getStackTraceAsString;

import java.util.Map;

import com.ericsson.de.scenarios.api.DataRecord;
import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph;
import com.google.common.base.Strings;
import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;

class TooltipGenerator {

    private final DataRecordsJsonParser parser = new DataRecordsJsonParser();

    public String export(ScenarioExecutionGraph.GraphNode node) {
        HtmlBuilder builder = new HtmlBuilder();
        addCommonAttributes(node, builder);
        addDataRecords(node, builder);
        addException(node, builder);
        return builder.toString();
    }

    private void addCommonAttributes(ScenarioExecutionGraph.GraphNode node, HtmlBuilder builder) {
        builder.html("<table>");
        addAttributeRow(builder, "Name:", node.getName());
        addAttributeRow(builder, "Type:", node.getClass().getSimpleName());
        if (!Strings.isNullOrEmpty(node.getVUser())) {
            addAttributeRow(builder, "vUsers:", node.getVUser());
        }
        if (node.getExecutionTime() != null) {
            addAttributeRow(builder, "Time:", "" + node.getExecutionTime() + "ms");
        }
        builder.html("</table>");
    }

    private void addAttributeRow(HtmlBuilder builder, String key, String value) {
        builder.html("<tr>").html("<td>").strong(key).html("</td>").html("<td>").text(value).html("</td>").html("</tr>");
    }

    private void addDataRecords(ScenarioExecutionGraph.GraphNode node, HtmlBuilder builder) {
        if (node.hasDataRecord()) {
            Map<String, DataRecord> dataRecords = parser.parseDataRecords(node.getDataRecord());
            builder.html("<table><tr><th>Data Source</th><th>Data Record</th><th>Value</th></tr>");
            for (Map.Entry<String, DataRecord> entry : dataRecords.entrySet()) {
                String dataSourceName = entry.getKey();
                DataRecord dataRecord1 = entry.getValue();
                boolean first = true;
                for (Map.Entry<String, Object> fields : dataRecord1.getAllFields().entrySet()) {
                    builder.html("<tr>");
                    builder.html("<td>");
                    if (first) {
                        builder.text(dataSourceName);
                        first = false;
                    }
                    builder.html("</td>");
                    builder.html("<td>").text(fields.getKey()).html("</td>");
                    builder.html("<td>").text(fields.getValue()).html("</td>");
                    builder.html("</tr>");
                }
            }
            builder.html("</table>");
        }
    }

    private void addException(ScenarioExecutionGraph.GraphNode node, HtmlBuilder builder) {
        if (node.isFailed()) {
            String stackTrace = getStackTraceAsString(node.getException());
            builder.strong("Exception:").newLine().code(stackTrace);
        }
    }

    private class HtmlBuilder {

        private StringBuilder builder = new StringBuilder("<body xmlns=\"http://www.w3.org/1999/xhtml\">");
        private Escaper escaper = HtmlEscapers.htmlEscaper();

        HtmlBuilder strong(String text) {
            return html("<strong>").text(text).html("</strong>");
        }

        HtmlBuilder code(String code) {
            return html("<code>").text(code).html("</code>");
        }

        HtmlBuilder newLine() {
            return html("<br/>");
        }

        HtmlBuilder text(Object text) {
            return html(escaper.escape(text.toString()));
        }

        HtmlBuilder html(Object html) {
            builder.append(html);
            return this;
        }

        @Override
        public String toString() {
            return builder.toString() + "</body>";
        }
    }
}
