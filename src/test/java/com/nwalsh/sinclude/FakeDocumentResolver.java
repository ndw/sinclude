package com.nwalsh.sinclude;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.lib.Logger;
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
                + "  <p xml:lang='fr'>Paragraphe trois.</p>"
                + "</doc>");
        xmlMap.put("onefr.xml", "<doc xml:lang='fr'>"
                + "  <p xml:lang='en'>Paragraph one.</p>"
                + "  <p xml:lang='en'>Paragraph two.</p>"
                + "  <p>Paragraphe trois.</p>"
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
                + "  <xi:include href='three.xml' set-xml-id='foo'/>"
                + "</doc>");
        // N.B. This is exactly the same as test four, but the test harness runs it with
        // fixup-xml-base disabled.
        xmlMap.put("twelve.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.xml' fragid='one'/>"
                + "</doc>");
        xmlMap.put("thirteen.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='onefr.xml' fragid='/1/3'/>"
                + "</doc>");
        xmlMap.put("fourteen.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='onefr.xml' fragid='/1/3'/>"
                + "</doc>");
        xmlMap.put("fifteen.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'"
                + "  xmlns:ex='http://example.com/'"
                + "  xmlns:local='http://www.w3.org/2001/XInclude/local-attributes'>"
                + "  <xi:include href='one.xml' ex:foo='ex:foo' local:bar='bar'/>"
                + "</doc>");
        xmlMap.put("sixteen.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'"
                + "  xmlns:ex='http://example.com/'"
                + "  xmlns:local='http://www.w3.org/2001/XInclude/local-attributes'>"
                + "  <xi:include href='one.xml' fragid='/1/1' ex:foo='ex:foo' local:bar='bar'/>"
                + "</doc>");
        // Seventeen is exactly the same as sixteen, but the harness runs it with
        // copyAttributes turned off
        xmlMap.put("seventeen.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'"
                + "  xmlns:ex='http://example.com/'"
                + "  xmlns:local='http://www.w3.org/2001/XInclude/local-attributes'>"
                + "  <xi:include href='one.xml' fragid='/1/1' ex:foo='ex:foo' local:bar='bar'/>"
                + "</doc>");
        xmlMap.put("nest1.xml", "<nest1 xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='nest2.xml'/>"
                + "</nest1>");
        xmlMap.put("nest2.xml", "<nest2 xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='nest3.xml'/>"
                + "</nest2>");
        xmlMap.put("nest3.xml", "<nest3 xmlns:xi='http://www.w3.org/2001/XInclude'/>");
        xmlMap.put("loop1.xml", "<nest1 xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='loop2.xml'/>"
                + "</nest1>");
        xmlMap.put("loop2.xml", "<nest2 xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='loop2.xml'/>"
                + "</nest2>");
        xmlMap.put("loop3.xml", "<not-a-loop xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='three.xml'/>"
                + "  <xi:include href='three.xml'/>"
                + "</not-a-loop>");
        xmlMap.put("icheck1.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.txt' xpointer='text(char=0,4;length=18)' parse='text'/>"
                + "</doc>");
        xmlMap.put("icheck2.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.txt' xpointer='text(char=0,4;length=1000000)' parse='text'/>"
                + "</doc>");
        xmlMap.put("icheck3.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.txt' xpointer='text(char=0,4;length=1000000)' parse='text'>"
                + "<xi:fallback>"
                + "<xi:include href='one.txt' xpointer='text(char=0,4;length=18)' parse='text'/>"
                + "</xi:fallback>"
                + "</xi:include>"
                + "</doc>");
        xmlMap.put("icheck4.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.txt' xpointer='text(char=0,4;unknown-integrity-check=12; length=18,  utf-8)' parse='text'/>"
                + "</doc>");
        xmlMap.put("icheck5.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.txt' xpointer='text(char=0,4;md5=fc8850705bc913a80edcc0dea2c300f1)' parse='text'/>"
                + "</doc>");
        xmlMap.put("icheck6.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.txt' xpointer='text(char=0,4;length=10000,ISO-8859-1)' parse='text'/>"
                + "</doc>");
        xmlMap.put("selfref.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='' xpointer='two' set-xml-id='one'/>"
                + "  <p xml:id='two'>This is a paragraph.</p>"
                + "</doc>");
        xmlMap.put("escapetext.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='three.xml' parse='text'/>"
                + "</doc>");
        xmlMap.put("textintegrity.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.txt' parse='text' xpointer='line=0,;length=18'/>"
                + "</doc>");
        xmlMap.put("textintegrityfail.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='one.txt' parse='text' xpointer='line=0,;length=10000'/>"
                + "</doc>");
        xmlMap.put("leadingblanks.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='two.txt' parse='text'/>"
                + "</doc>");
        xmlMap.put("ghline.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='two.txt' parse='text' xpointer='L10'/>"
                + "</doc>");
        xmlMap.put("ghlinerange.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <xi:include href='two.txt' parse='text' xpointer='L10-L15'/>"
                + "</doc>");
    }

    private static Map<String, String> textMap = null;
    static {
        textMap = new HashMap<>();
        textMap.put("one.txt", "This is line one.\n");
        textMap.put("two.txt", "\n\n\n\n\n\n\n\n\nThis is line 10.\n\n\n\n\nThis is line 15.");
        textMap.put("three.xml", "<doc>Document three.</doc>");
    }

    private static Map<String, String> expandedMap = null;
    static {
        expandedMap = new HashMap<>();
        expandedMap.put("two.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <doc xml:base='http://example.com/docs/three.xml'>Document three.</doc>"
                + "</doc>");
        expandedMap.put("four.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <p xml:id='one' xml:base='http://example.com/docs/one.xml'>Paragraph one.</p>"
                + "</doc>");
        expandedMap.put("five.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <p xmlns='http://example.com/' xml:base='http://example.com/docs/one.xml'>Paragraph two.</p>"
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
                + "  <p xml:id='one' xml:base='http://example.com/docs/one.xml'>Paragraph one.</p>"
                + "</doc>");
        expandedMap.put("ten.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <p xmlns='http://example.com/' xml:base='http://example.com/docs/one.xml'>Paragraph two.</p>"
                + "</doc>");
        expandedMap.put("eleven.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <doc xml:id='foo' xml:base='http://example.com/docs/three.xml'>Document three.</doc>"
                + "</doc>");
        // N.B. This is exactly the same as test four, but the test harness runs it with
        // fixup-xml-base disabled.
        expandedMap.put("twelve.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <p xml:id='one'>Paragraph one.</p>"
                + "</doc>");
        expandedMap.put("thirteen.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <p xml:lang='fr' xml:base='http://example.com/docs/onefr.xml'>Paragraphe trois.</p>"
                + "</doc>");
        expandedMap.put("fourteen.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <p xml:base='http://example.com/docs/onefr.xml'>Paragraphe trois.</p>"
                + "</doc>");
        expandedMap.put("fifteen.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'"
                + "  xmlns:ex='http://example.com/'"
                + "  xmlns:local='http://www.w3.org/2001/XInclude/local-attributes'>"
                + "  <doc ex:foo='ex:foo' bar='bar' xml:base='http://example.com/docs/one.xml'>"
                + "  <p xml:id='one'>Paragraph one.</p>"
                + "  <p xmlns='http://example.com/'>Paragraph two.</p>"
                + "  <p xml:lang='fr'>Paragraphe trois.</p>"
                + "</doc>"
                + "</doc>");
        expandedMap.put("sixteen.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'"
                + "  xmlns:ex='http://example.com/'"
                + "  xmlns:local='http://www.w3.org/2001/XInclude/local-attributes'>"
                + "  <p xml:id='one' ex:foo='ex:foo' bar='bar' xml:base='http://example.com/docs/one.xml'>Paragraph one.</p>"
                + "</doc>");
        // Seventeen is exactly the same as sixteen, but the harness runs it with
        // copyAttributes turned off
        expandedMap.put("seventeen.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'"
                + "  xmlns:ex='http://example.com/'"
                + "  xmlns:local='http://www.w3.org/2001/XInclude/local-attributes'>"
                + "  <p xml:id='one' xml:base='http://example.com/docs/one.xml'>Paragraph one.</p>"
                + "</doc>");
        expandedMap.put("nest1.xml", "<nest1 xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <nest2 xml:base='http://example.com/docs/nest2.xml'>"
                + "  <nest3 xml:base='http://example.com/docs/nest3.xml'/>"
                + "</nest2>"
                + "</nest1>");
        expandedMap.put("loop3.xml", "<not-a-loop xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <doc xml:base='http://example.com/docs/three.xml'>Document three.</doc>"
                + "  <doc xml:base='http://example.com/docs/three.xml'>Document three.</doc>"
                + "</not-a-loop>");
        expandedMap.put("icheck1.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  This"
                + "</doc>");
        expandedMap.put("icheck3.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  This"
                + "</doc>");
        expandedMap.put("icheck4.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  This"
                + "</doc>");
        expandedMap.put("icheck5.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  This"
                + "</doc>");
        expandedMap.put("icheck6.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  This"
                + "</doc>");
        // FIXME: technically the xml:base isn't needed here; is it worth trying to avoid generating it?
        expandedMap.put("selfref.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  <p xml:id='one' xml:base='http://example.com/docs/selfref.xml'>This is a paragraph.</p>"
                + "  <p xml:id='two'>This is a paragraph.</p>"
                + "</doc>");
        expandedMap.put("escapetext.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  &lt;doc&gt;Document three.&lt;/doc&gt;"
                + "</doc>");
        expandedMap.put("textintegrity.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  This is line one.\n"
                + "</doc>");
        expandedMap.put("leadingblanks.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  \n\n\n\n\n\n\n\n\nThis is line 10.\n\n\n\n\nThis is line 15."
                + "</doc>");
        expandedMap.put("ghline.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  This is line 10.\n"
                + "</doc>");
        expandedMap.put("ghlinerange.xml", "<doc xmlns:xi='http://www.w3.org/2001/XInclude'>"
                + "  This is line 10.\n\n\n\n\nThis is line 15.\n"
                + "</doc>");
    }

        @Override
    public XdmNode resolveXml(XdmNode base, String uri, String accept, String acceptLanguage) {
        Logger logger = base.getProcessor().getUnderlyingConfiguration().getLogger();
        logger.info("Resolving XML XInclude: " + uri + " (" + base.getBaseURI().resolve(uri).toASCIIString() + ")");
        if (xmlMap.containsKey(uri)) {
            try {
                String text = xmlMap.get(uri);
                String baseURI = "http://example.com/docs/" + uri;

                Processor processor = base.getProcessor();
                DocumentBuilder builder = processor.newDocumentBuilder();
                builder.setBaseURI(URI.create(baseURI));
                InputSource source = new InputSource(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
                source.setSystemId(baseURI);
                return builder.build(new SAXSource(source));
            } catch (SaxonApiException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("File not found: " + uri);
        }
    }

    @Override
    public XdmNode resolveText(XdmNode base, String uri, String encoding, String accept, String acceptLanguage) {
        Logger logger = base.getProcessor().getUnderlyingConfiguration().getLogger();
        logger.info("Resolving text XInclude: " + uri + " (" + base.getBaseURI().resolve(uri).toASCIIString() + ")");
        if (textMap.containsKey(uri)) {
            String text = textMap.get(uri);
            XdmDestination destination = new XdmDestination();
            PipelineConfiguration pipe = base.getUnderlyingNode().getConfiguration().makePipelineConfiguration();
            Receiver receiver = destination.getReceiver(pipe, new SerializationProperties());
            try {
                receiver.open();
                receiver.startDocument(0);
                receiver.characters(text, Loc.NONE, 0);
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
