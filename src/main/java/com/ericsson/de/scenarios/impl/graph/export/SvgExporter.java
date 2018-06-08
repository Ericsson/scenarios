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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.ericsson.de.scenarios.impl.graph.ScenarioExecutionGraph;
import com.google.common.io.CharStreams;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl;

@SuppressWarnings("Duplicates")
public class SvgExporter extends AbstractGraphExporter {

    private static final Logger logger = LoggerFactory.getLogger(SvgExporter.class);

    private static final String EXTENSION = ".svg";

    private static final int TOOLTIP_WIDTH = 320;
    private static final int TOOLTIP_HEIGHT = 520;

    private Transformer transformer;
    private DocumentBuilder documentBuilder;

    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";

    public SvgExporter() {
        super(EXTENSION);
    }

    @Override
    protected void init() {
        try {
            documentBuilder = hackInDocumentBuilderFactory().newDocumentBuilder();
            transformer = hackInTransformerFactory().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //ASCII
        } catch (TransformerConfigurationException | ParserConfigurationException e) {
            logger.error("Exception: ", e);
        }
    }

    /**
     * correct way to to this would be
     * DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
     * see reason in {link: com.ericsson.cifwk.taf.scenario.ext.exporter.SvgExporter.hackInTransformerFactory}
     */
    private DocumentBuilderFactoryImpl hackInDocumentBuilderFactory() {
        return new DocumentBuilderFactoryImpl();
    }

    /**
     * correct way to to this would be <pre>SAXTransformerFactory factory =
     * (SAXTransformerFactory) SAXTransformerFactory.newInstance();</pre>
     * but because class path contains {@code net.sf.saxon.saxon} it is loaded via SPI
     * Because saxon generates random text instead of xml, we instantiate xalan manually
     */
    private TransformerFactoryImpl hackInTransformerFactory() {
        return new TransformerFactoryImpl();
    }

    @Override
    public void export(ScenarioExecutionGraph scenarioExecutionGraph, Writer writer) throws Exception {
        mxGraph graph = new SvgGraphAdapter(scenarioExecutionGraph);
        mxGraphLayout layout = new mxHierarchicalLayout(graph);
        layout.execute(graph.getDefaultParent());

        Document svgDocument = mxCellRenderer.createSvgDocument(graph, null, 1d, null, null);
        updateDimensions(svgDocument);

        addElementFromFile(svgDocument, "svg/style.css", "style", "text/css");
        addElementFromFile(svgDocument, "svg/script.js", "script", "text/javascript");

        addNotSupportedWarning(svgDocument);
        addToolTips(graph, svgDocument);

        transformer.transform(new DOMSource(svgDocument), new StreamResult(writer));
    }

    private void updateDimensions(Document document) {
        NamedNodeMap rootAttributes = document.getDocumentElement().getAttributes();
        assureMinValue(rootAttributes, WIDTH, TOOLTIP_WIDTH);
        assureMinValue(rootAttributes, HEIGHT, TOOLTIP_HEIGHT);

        String width = rootAttributes.getNamedItem(WIDTH).getNodeValue();
        String height = rootAttributes.getNamedItem(HEIGHT).getNodeValue();
        rootAttributes.getNamedItem("viewBox").setNodeValue("0 0 " + width + " " + height);
    }

    private void assureMinValue(NamedNodeMap rootAttributes, String dimension, int minValue) {
        Node attribute = rootAttributes.getNamedItem(dimension);
        String actualSize = attribute.getNodeValue();
        if (Integer.valueOf(actualSize) < minValue) {
            attribute.setNodeValue(Integer.toString(minValue));
        }
    }

    private void addElementFromFile(Document document, String filename, String tag, String type) throws IOException {
        InputStream stream = getClass().getResourceAsStream(filename);
        String text = CharStreams.toString(new InputStreamReader(stream));
        CDATASection cdata = document.createCDATASection(text);

        Element element = document.createElement(tag);
        element.setAttribute("type", type);
        element.appendChild(cdata);

        Element root = document.getDocumentElement();
        root.insertBefore(element, root.getFirstChild());
    }

    private void addNotSupportedWarning(Document document) {
        Element body = document.createElement("body");
        body.setAttribute("xmlns", "http://www.w3.org/1999/xhtml");
        body.setAttribute("onhashchange", "showTooltip();");
        body.setAttribute("onload", "showTooltip(); scrollToTooltip();");

        Element foreign = document.createElement("foreignObject");
        foreign.appendChild(body);

        Element text = document.createElement("text");
        text.setAttribute("x", "10");
        text.setAttribute("y", "30");
        text.setAttribute("fill", "red");
        text.setTextContent("Your browser does not support HTML as foreignObject. " + "To view scenario details open this file in Firefox or Chrome");

        Element aSwitch = document.createElement("switch");
        aSwitch.appendChild(foreign);
        aSwitch.appendChild(text);

        Element root = document.getDocumentElement();
        root.appendChild(aSwitch);
    }

    private void addToolTips(mxGraph graph, Document document) throws ParserConfigurationException, SAXException, IOException {
        Element root = document.getDocumentElement();

        Object[] cells = graph.getChildCells(graph.getDefaultParent(), true, true);
        for (Object cell : cells) {
            mxCell mxCell = (mxCell) cell;
            Object value = mxCell.getValue();
            if (value instanceof ScenarioExecutionGraph.GraphNode) {
                ScenarioExecutionGraph.GraphNode node = (ScenarioExecutionGraph.GraphNode) value;
                String toolTipId = "tooltip" + node.getVertexId();

                Node toolTip = createToolTipDOM(document, node);
                Element foreign = createForeignObject(document, mxCell, toolTipId);
                foreign.appendChild(toolTip);

                root.appendChild(foreign);
            }
        }
    }

    private Node createToolTipDOM(Document document, ScenarioExecutionGraph.GraphNode node) throws SAXException, IOException {
        String tooltipText = tooltip(node);
        Document parsedToolTip = documentBuilder.parse(new ByteArrayInputStream(tooltipText.getBytes()));
        Node importedToolTip = document.importNode(parsedToolTip.getDocumentElement(), true);

        Element closeLink = document.createElement("a");
        closeLink.setAttribute("href", "");
        closeLink.setAttribute("class", "close");
        closeLink.setTextContent("x"); // \u2716

        importedToolTip.insertBefore(closeLink, importedToolTip.getFirstChild());
        return importedToolTip;
    }

    private Element createForeignObject(Document document, mxCell mxCell, String toolTipId) {
        Element root = document.getDocumentElement();
        Integer maxWidth = Integer.valueOf(root.getAttributes().getNamedItem(WIDTH).getNodeValue());
        Integer maxHeight = Integer.valueOf(root.getAttributes().getNamedItem(HEIGHT).getNodeValue());

        Element foreign = document.createElement("foreignObject");
        foreign.setAttribute("id", toolTipId);
        foreign.setAttribute("class", "tooltip");
        foreign.setAttribute("x", applyBoundary(mxCell.getGeometry().getX(), maxWidth, TOOLTIP_WIDTH));
        foreign.setAttribute("y", applyBoundary(mxCell.getGeometry().getY(), maxHeight, TOOLTIP_HEIGHT));
        foreign.setAttribute(WIDTH, Integer.toString(TOOLTIP_WIDTH));
        foreign.setAttribute(HEIGHT, Integer.toString(TOOLTIP_HEIGHT));

        return foreign;
    }

    private String applyBoundary(double desired, int boundary, int size) {
        if (desired + size > boundary) {
            return Integer.toString(boundary - size);
        }
        return Double.toString(desired);
    }
}
