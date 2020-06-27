package com.nwalsh.sinclude;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.AttributeInfo;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FakeDocumentResolver implements DocumentResolver {
    private static Map<String, String> xmlMap = null;
    static {
        xmlMap = new HashMap<>();
        xmlMap.put("one.xml", "<doc>"
                + "  <p xml:id='one'>Paragraph one.</p>"
                + "  <p xmlns='http://example.com/'>Paragraph two.</p>"
                + "</doc>");
        xmlMap.put("two.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='three.xml'/>"
                + "</doc>");
        xmlMap.put("three.xml", "<doc>Document three.</doc>");
        xmlMap.put("four.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.xml' fragid='one'/>"
                + "</doc>");
        xmlMap.put("five.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.xml' fragid='xmlns(e=http://example.com/) xpath(/doc/e:p)'/>"
                + "</doc>");
        xmlMap.put("six.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.xml' fragid='xpath(/doc/*[1]/@xml:id)'/>"
                + "</doc>");
        xmlMap.put("seven.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.txt' fragid='char=8,12' parse='text'/>"
                + "</doc>");
        xmlMap.put("eight.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='nosuchfile.txt' fragid='char=8,12' parse='text'>"
                + "<xi:fallback>fallback</xi:fallback></xi:include>"
                + "</doc>");
        xmlMap.put("nine.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.xml' fragid='element(nosuchid) element(one)'/>"
                + "</doc>");
        xmlMap.put("ten.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.xml' fragid='xpath(/doc/e:q) xmlns(e=http://example.com/) xpath(/doc/e:p)'/>"
                + "</doc>");
        xmlMap.put("eleven.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='three.xml' set-xml-id='foo' xml:base='http://example.com/'/>"
                + "</doc>");
        xmlMap.put("twelve.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='three.xml' set-xml-id='foo'/>"
                + "</doc>");
    }

    private static Map<String, String> textMap = null;
    static {
        textMap = new HashMap<>();
        textMap.put("one.txt", "This is line one.\n");
    }

    private static Map<String, String> expandedMap = null;
    static {
        expandedMap = new HashMap<>();
        expandedMap.put("two.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                    + "  <doc>Document three.</doc>"
                    + "</doc>");
        expandedMap.put("four.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                    + "  <p xml:id='one'>Paragraph one.</p>"
                    + "</doc>");
        expandedMap.put("five.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                    + "  <p xmlns='http://example.com/'>Paragraph two.</p>"
                    + "</doc>");
        expandedMap.put("six.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                    + "  one"
                    + "</doc>");
        expandedMap.put("seven.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                    + "  line"
                    + "</doc>");
        expandedMap.put("eight.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  fallback"
                + "</doc>");
        expandedMap.put("nine.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <p xml:id='one'>Paragraph one.</p>"
                + "</doc>");
        expandedMap.put("ten.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <p xmlns='http://example.com/'>Paragraph two.</p>"
                + "</doc>");
        expandedMap.put("eleven.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <doc xml:id='foo'>Document three.</doc>"
                + "</doc>");
        expandedMap.put("twelve.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <doc xml:id='foo'>Document three.</doc>"
                + "</doc>");
    }

    @Override
    public XdmNode resolveXml(XdmNode base, String uri, String accept, String acceptLanguage) {
        if (xmlMap.containsKey(uri)) {
            try {
                Processor processor = base.getProcessor();
                DocumentBuilder builder = processor.newDocumentBuilder();
                builder.setBaseURI(URI.create("http://example.com/"));
                String text = xmlMap.get(uri);
                return builder.build(new SAXSource(new InputSource(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)))));
            } catch (SaxonApiException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("File not found: " + uri);
        }
    }

    @Override
    public XdmNode resolveText(XdmNode base, String uri, String accept, String acceptLanguage) {
        if (textMap.containsKey(uri)) {
            String text = textMap.get(uri);
            XdmDestination destination = new XdmDestination();
            PipelineConfiguration pipe = base.getUnderlyingNode().getConfiguration().makePipelineConfiguration();
            Receiver receiver = destination.getReceiver(pipe, new SerializationProperties());
            try {
                receiver.open();
                receiver.startDocument(0);
                receiver.characters(text, VoidLocation.instance(), 0);
                receiver.endDocument();
                receiver.close();
                return destination.getXdmNode();
            } catch (XPathException e) {
                throw new UnsupportedOperationException(e);
            }
        } else {
            throw new RuntimeException("File not found: " + uri);
        }
    }

    public XdmNode expected(XdmNode base, String key) {
        return xmlTree(base, expandedMap.get(key));
    }

    // Hack; this has nothing to do with resolving documents, it's just a convenient place to put it
    public boolean theSame(XdmNode doc1, XdmNode doc2) {
        if (doc1 == null) {
            return doc2 == null;
        }

        if (doc2 == null) {
            return false;
        }

        if (doc1.getNodeKind() != doc2.getNodeKind()) {
            return false;
        }

        switch (doc1.getNodeKind()) {
            case TEXT:
            case COMMENT:
            case PROCESSING_INSTRUCTION:
                return doc1.getStringValue().equals(doc2.getStringValue());
            default:
                if (doc1.getNodeKind() == XdmNodeKind.ELEMENT) {
                    if (!doc1.getNodeName().equals(doc2.getNodeName())) {
                        return false;
                    }
                    if (!theSameAttributes(doc1, doc2)) {
                        return false;
                    }
                }
                XdmSequenceIterator<XdmNode> iter1 = doc1.axisIterator(Axis.CHILD);
                XdmSequenceIterator<XdmNode> iter2 = doc2.axisIterator(Axis.CHILD);
                while (iter1.hasNext()) {
                    XdmNode child1 = iter1.next();
                    if (!iter2.hasNext()) {
                        return false;
                    }
                    XdmNode child2 = iter2.next();
                    if (!theSame(child1,child2)) {
                        return false;
                    }
                }
                if (iter2.hasNext()) {
                    return false;
                }
        }
        return true;
    }

    private boolean theSameAttributes(XdmNode elem1, XdmNode elem2) {
        AttributeMap amap1 = elem1.getUnderlyingNode().attributes();
        AttributeMap amap2 = elem2.getUnderlyingNode().attributes();
        if (amap1.size() != amap2.size()) {
            return false;
        }
        for (int pos = 0; pos < amap1.size(); pos++) {
            AttributeInfo a1 = amap1.itemAt(pos);
            AttributeInfo a2 = amap2.get(a1.getNodeName());
            if (a2 == null) {
                return false;
            }
            if (!a1.getValue().equals(a2.getValue())) {
                return false;
            }
        }
        return true;
    }

    private XdmNode xmlTree(XdmNode base, String result) {
        try {
            Processor processor = base.getProcessor();
            DocumentBuilder builder = processor.newDocumentBuilder();
            return builder.build(new SAXSource(new InputSource(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)))));
        } catch (SaxonApiException e) {
            throw new RuntimeException(e);
        }
    }
}
