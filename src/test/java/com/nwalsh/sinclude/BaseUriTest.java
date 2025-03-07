package com.nwalsh.sinclude;

import junit.framework.TestCase;
import net.sf.saxon.s9api.*;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class BaseUriTest extends TestCase {
    private FakeDocumentResolver resolver = new FakeDocumentResolver();
    private Processor processor = new Processor(false);
    private XdmNode emptyDoc = null;

    public void setUp() {
        try {
            DocumentBuilder builder = processor.newDocumentBuilder();
            ByteArrayInputStream bais = new ByteArrayInputStream("<doc/>".getBytes(StandardCharsets.UTF_8));
            emptyDoc = builder.build(new SAXSource(new InputSource(bais)));
        } catch (SaxonApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void testBaseURI() {
        XdmNode doc = resolver.resolveXml(emptyDoc, "three.xml", null, null);
        XdmSequenceIterator<XdmNode> iter = doc.axisIterator(Axis.CHILD);
        XdmNode root = iter.next();
        assertEquals(URI.create("http://example.com/docs/three.xml"), root.getBaseURI());
    }

    public void testNestedBaseURI() {
        try {
            DocumentBuilder documentBuilder = processor.newDocumentBuilder();
            XdmNode doc = documentBuilder.build(new File("src/test/resources/xmlcalabash-iss-321/in-1.xml"));

            XInclude xincluder = new XInclude();
            xincluder.setTrimText(false);
            xincluder.setFixupXmlBase(false);
            xincluder.setFixupXmlLang(false);
            xincluder.setCopyAttributes(true);

            XdmNode included = xincluder.expandXIncludes(doc);

            for (XdmNode child : included.children()) {
                assertTrue(child.getBaseURI().toString().endsWith("321/in-1.xml"));
                for (XdmNode gchild : child.children()) {
                    assertTrue(gchild.getBaseURI().toString().endsWith("321/include/in-2.xml"));
                }
            }
        } catch (Exception ex) {
            fail();
        }
    }

}
