package com.ericsson.de.scenarios.impl.graph;/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

import static com.google.common.collect.Maps.newHashMap;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.google.common.collect.Maps;

public class GraphMlImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphMlImporter.class);

    private static ScenarioExecutionGraph parseGraph(URL graphText) throws IOException {
        ObjectMapper mapper = new XmlMapper();

        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Doc doc = mapper.readValue(graphText, Doc.class);
        Map<String, String> replacements = parseReplacements(doc.keys);

        ScenarioExecutionGraph result = new ScenarioExecutionGraph();

        Map<String, ScenarioExecutionGraph.GraphNode> nodes = newHashMap();

        for (RawNode rawNode : doc.graph.nodes) {
            Map<String, String> attrMap = parseAttrMap(rawNode.data, replacements);
            ScenarioExecutionGraph.GraphNode node = GraphNodeFactory.createGraphNode(attrMap);

            result.addVertex(node);
            nodes.put(rawNode.id, node);
        }

        for (Edge edge : doc.graph.edges) {
            ScenarioExecutionGraph.GraphNode sourceVertex = nodes.get(edge.source);
            ScenarioExecutionGraph.GraphNode targetVertex = nodes.get(edge.target);

            Map<String, String> attrMap = parseAttrMap(edge.data, replacements);
            String edgeLabel = attrMap.get(ScenarioExecutionGraph.LabeledEdge.EDGE_LABEL);

            if (edgeLabel != null) {
                result.addEdge(sourceVertex, targetVertex, new ScenarioExecutionGraph.LabeledEdge(edgeLabel));
            } else {
                result.addEdge(sourceVertex, targetVertex);
            }
        }

        return result;
    }

    private static Map<String, String> parseReplacements(List<Key> keys) {
        HashMap<String, String> replacements = Maps.newHashMap();

        for (Key key : keys) {
            if (key.replace != null) {
                replacements.put(key.replace, key.to);
            }
        }

        return replacements;
    }

    private static Map<String, String> parseAttrMap(List<Map> data, Map<String, String> keyMap) {
        Map<String, String> attrMap = newHashMap();

        for (Map dataElement : data) {
            Object rawKey = dataElement.get("key");
            Object rawValue = dataElement.get("");
            if (rawKey == null || rawValue == null) {
                continue;
            }
            String key = keyMap.get(rawKey.toString());
            String value = rawValue.toString();
            attrMap.put(key, value);
        }

        return attrMap;
    }

    public ScenarioExecutionGraph importFile(URL path) {
        try {
            return parseGraph(path);
        } catch (Exception e) {
            LOGGER.error("Unable to parse graph", e);
        }
        return null;
    }

    public static class Doc {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JsonProperty("key")
        List<Key> keys;

        Graph graph;
    }

    public static class Key {
        @JsonProperty("id")
        String replace;

        @JsonProperty("attr.name")
        String to;
    }

    public static class Edge {
        String source;
        String target;
        @JacksonXmlElementWrapper(useWrapping = false)
        @JsonProperty("data")
        List<Map> data;
    }

    public static class Graph {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JsonProperty("node")
        List<RawNode> nodes;

        @JacksonXmlElementWrapper(useWrapping = false)
        @JsonProperty("edge")
        List<Edge> edges;
    }

    public static class RawNode {
        String id;
        @JacksonXmlElementWrapper(useWrapping = false)
        @JsonProperty("data")
        List<Map> data;
    }
}
