package com.ericsson.de.scenarios.impl.graph.jgrapht;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.GraphMLExporter;
import org.jgrapht.ext.VertexNameProvider;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;

/**
 * Reasons why this class was created:
 * 1. org.jgrapht.ext.GraphMLExporter  does not support custom data fields for nodes.
 * 2. It loads SAXTransformerFactory via SPI. Because JCAT uses Saxon as SAX Transformer, it would be used for GraphML
 * export too. Saxon is unusable and generates not valid XML, so this breaks export completely
 */
public class SuperGraphMLExporter<V, E> extends GraphMLExporter<V, E> {
    private VertexNameProvider<V> vertexIDProvider;
    private Collection<AttributeProvider<V>> vertexAttributeProviders;
    private EdgeNameProvider<E> edgeIDProvider;
    private Collection<AttributeProvider<E>> edgeAttributeProviders;
    private static final String CDATA = "CDATA";

    public SuperGraphMLExporter(VertexNameProvider<V> vertexIDProvider, Collection<AttributeProvider<V>> vertexAttributeProviders,
            EdgeNameProvider<E> edgeIDProvider, Collection<AttributeProvider<E>> edgeAttributeProviders) {
        this.vertexIDProvider = vertexIDProvider;
        this.vertexAttributeProviders = vertexAttributeProviders;
        this.edgeIDProvider = edgeIDProvider;
        this.edgeAttributeProviders = edgeAttributeProviders;
    }

    /**
     * Exports a graph into a plain text file in GraphML format.
     *
     * @param writer
     *         the writer to which the graph to be exported
     * @param g
     *         the graph to be exported
     */
    public void export(Writer writer, Graph<V, E> g) throws SAXException, TransformerConfigurationException {
        // Prepare an XML file to receive the GraphML data
        PrintWriter out = new PrintWriter(writer);
        StreamResult streamResult = new StreamResult(out);

        SAXTransformerFactory factory = hackInTransformerFactory();

        TransformerHandler handler = factory.newTransformerHandler();
        Transformer serializer = handler.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        handler.setResult(streamResult);
        handler.startDocument();
        AttributesImpl attr = new AttributesImpl();

        // <graphml>
        handler.startPrefixMapping("xsi", "http://www.w3.org/2001/XMLSchema-instance");

        attr.addAttribute("", "", "xsi:schemaLocation", CDATA,
                "http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd");
        handler.startElement("http://graphml.graphdrawing.org/xmlns", "", "graphml", attr);
        handler.endPrefixMapping("xsi");

        for (AttributeProvider<V> provider : vertexAttributeProviders) {
            addDataAttribute(handler, attr, provider, "node");
        }

        for (AttributeProvider<E> provider : edgeAttributeProviders) {
            addDataAttribute(handler, attr, provider, "edge");
        }

        // <graph>
        attr.clear();
        attr.addAttribute("", "", "edgedefault", CDATA, (g instanceof DirectedGraph<?, ?>) ? "directed" : "undirected");
        handler.startElement("", "", "graph", attr);

        // Add all the vertices as <node> elements...
        for (V v : g.vertexSet()) {
            // <node>
            attr.clear();
            attr.addAttribute("", "", "id", CDATA, vertexIDProvider.getVertexName(v));
            handler.startElement("", "", "node", attr);

            for (AttributeProvider<V> provider : vertexAttributeProviders) {
                // <data>
                attr.clear();
                attr.addAttribute("", "", "key", CDATA, provider.getAttributeId());
                handler.startElement("", "", "data", attr);

                // Content for <data>
                String vertexLabel = provider.getAttributeValue(v);
                handler.characters(vertexLabel.toCharArray(), 0, vertexLabel.length());

                handler.endElement("", "", "data");
            }

            handler.endElement("", "", "node");
        }

        // Add all the edges as <edge> elements...
        for (E e : g.edgeSet()) {
            // <edge>
            attr.clear();
            attr.addAttribute("", "", "id", CDATA, edgeIDProvider.getEdgeName(e));
            attr.addAttribute("", "", "source", CDATA, vertexIDProvider.getVertexName(g.getEdgeSource(e)));
            attr.addAttribute("", "", "target", CDATA, vertexIDProvider.getVertexName(g.getEdgeTarget(e)));
            handler.startElement("", "", "edge", attr);

            for (AttributeProvider<E> edgeAttributeProvider : edgeAttributeProviders) {
                // <data>
                attr.clear();
                attr.addAttribute("", "", "key", CDATA, edgeAttributeProvider.getAttributeId());
                handler.startElement("", "", "data", attr);

                // Content for <data>
                String edgeLabel = edgeAttributeProvider.getAttributeValue(e);
                handler.characters(edgeLabel.toCharArray(), 0, edgeLabel.length());
                handler.endElement("", "", "data");
            }

            handler.endElement("", "", "edge");
        }

        handler.endElement("", "", "graph");
        handler.endElement("", "", "graphml");
        handler.endDocument();

        out.flush();
    }

    private TransformerFactoryImpl hackInTransformerFactory() {
        return new TransformerFactoryImpl();
    }

    private void addDataAttribute(TransformerHandler handler, AttributesImpl attr, AttributeProvider provider, String attrFor) throws SAXException {
        attr.clear();
        String id = provider.getAttributeId();
        attr.addAttribute("", "", "id", CDATA, id);
        attr.addAttribute("", "", "for", CDATA, attrFor);
        attr.addAttribute("", "", "attr.name", CDATA, provider.getAttributeName());
        attr.addAttribute("", "", "attr.type", CDATA, "string");
        handler.startElement("", "", "key", attr);
        handler.endElement("", "", "key");
    }
}
