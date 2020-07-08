package com.nwalsh.xslt;

import com.nwalsh.sinclude.DocumentResolver;
import com.nwalsh.sinclude.FakeDocumentResolver;
import com.nwalsh.sinclude.xpointer.SchemeData;
import junit.framework.TestCase;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.RawDestination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Vector;

public class ExtFunctionTest extends TestCase {
    private Processor processor = null;
    private XsltCompiler xsltCompiler = null;
    private XPathCompiler xpathCompiler = null;
    private DocumentBuilder builder = null;

    @Override
    public void setUp() {
        processor = new Processor(false);
        Configuration config = processor.getUnderlyingConfiguration();
        config.registerExtensionFunction(new XIncludeFunction());
        xsltCompiler = processor.newXsltCompiler();
        xpathCompiler = processor.newXPathCompiler();
        builder = processor.newDocumentBuilder();
    }

    public void testXmlDocument() {
        try {
            XdmNode node = applyStylesheet("src/test/resources/xinclude.xsl", "src/test/resources/includedoc.xml");

            assertEquals(1, count(node, "/*/*"));
            assertEquals(new QName("", "doc"), nodeName(node, "/*/*"));
            String base = stringValue(node, "/*/*/@xml:base");
            assertTrue(base.endsWith("test/resources/document.xml"));

            assertEquals(1, count(node, "/*/*/*"));
            assertEquals(new QName("", "p"), nodeName(node, "/*/*/*"));
            assertEquals("This is a document.", stringValue(node, "/*/*/*"));
        } catch (SaxonApiException | FileNotFoundException sae) {
            sae.printStackTrace();
            TestCase.fail();
        }
    }

    public void testXmlFragment() {
        try {
            XdmNode node = applyStylesheet("src/test/resources/xinclude.xsl",
                    "src/test/resources/includexmlfrag.xml");

            assertEquals(1, count(node, "/*/*"));
            assertEquals(new QName("", "p"), nodeName(node, "/*/*"));
            String base = stringValue(node, "/*/*/@xml:base");
            assertTrue(base.endsWith("test/resources/long.xml"));

            assertEquals("This is a document.", stringValue(node, "/*/*"));
        } catch (SaxonApiException | FileNotFoundException sae) {
            sae.printStackTrace();
            TestCase.fail();
        }
    }

    public void testTextFragment() {
        try {
            XdmNode node = applyStylesheet("src/test/resources/xinclude.xsl",
                    "src/test/resources/includetextfrag.xml");
            XdmNode[] nodes = xpathSelection(node, "/*/node()");
            assertEquals(1, nodes.length);
            String text = nodes[0].getStringValue();
            assertTrue(text.startsWith("This is line two"));
            assertTrue(text.endsWith("line four.\n"));
        } catch (SaxonApiException | FileNotFoundException sae) {
            sae.printStackTrace();
            TestCase.fail();
        }
    }

    private int count(XdmNode context, String expression) {
        XdmNode[] nodes = xpathSelection(context, expression);
        return nodes.length;
    }

    private QName nodeName(XdmNode context, String expression) {
        XdmNode[] nodes = xpathSelection(context, expression);
        assertEquals(nodes.length, 1);
        return nodes[0].getNodeName();
    }

    private String stringValue(XdmNode context, String expression) {
        XdmNode[] nodes = xpathSelection(context, expression);
        assertEquals(nodes.length, 1);
        return nodes[0].getStringValue();
    }

    private XdmNode[] xpathSelection(XdmNode context, String expression) {
        Vector<XdmNode> results = new Vector<> ();
        try {
            XPathExecutable exec = xpathCompiler.compile(expression);
            XPathSelector selector = exec.load();
            selector.setContextItem(context);
            for (XdmItem xdmItem : selector) {
                XdmNode next = (XdmNode) xdmItem;
                results.add(next);
            }
            XdmNode[] array = new XdmNode[results.size()];
            results.toArray(array);
            return array;
        } catch (SaxonApiException sae) {
            sae.printStackTrace();
            throw new RuntimeException("Unexpected error evaluating XPath");
        }
    }

    private XdmNode applyStylesheet(String stylesheet, String document) throws SaxonApiException, FileNotFoundException {
        File ssfile = new File(stylesheet);
        File inputfile = new File(document);
        XdmNode context = builder.build(inputfile);

        RawDestination result = new RawDestination();
        XsltExecutable xsltExec = xsltCompiler.compile(new SAXSource(new InputSource(new FileInputStream(ssfile))));
        XsltTransformer transformer = xsltExec.load();
        transformer.setDestination(result);
        transformer.setInitialContextNode(context);
        transformer.transform();
        XdmValue value = result.getXdmValue();

        if (value instanceof XdmNode) {
            return (XdmNode) value;
        } else {
            throw new RuntimeException("Value returned where node was expected");
        }
    }

    public void foo() {
        String stylesheet = "";
        stylesheet += "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'\n";
        stylesheet += "                xmlns:ext='http://nwalsh.com/xslt'\n";
        stylesheet += "                version='3.0'>\n\n";
        stylesheet += "<xsl:output method='text' encoding='utf-8'/>\n\n";
        stylesheet += "<xsl:template match='/' name='main'>\n";
        stylesheet += "  <xsl:value-of select=\"'hello world'\"/>\n";
        stylesheet += "</xsl:template>\n\n";
        stylesheet += "</xsl:stylesheet>\n";

        ByteArrayInputStream bais = new ByteArrayInputStream(
                stylesheet.getBytes(StandardCharsets.UTF_8));

        try {
            RawDestination result = new RawDestination();
            XsltCompiler compiler = processor.newXsltCompiler();
            XsltExecutable exec = compiler.compile(new SAXSource(new InputSource(bais)));
            XsltTransformer transformer = exec.load();
            transformer.setDestination(result);
            transformer.setInitialTemplate(new QName("", "main"));
            transformer.transform();
            XdmValue value = result.getXdmValue();
            System.err.println(value);
        } catch (SaxonApiException sae) {
            sae.printStackTrace();
            TestCase.fail();
        }
    }

}
