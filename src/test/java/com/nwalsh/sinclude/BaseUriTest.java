package com.nwalsh.sinclude;

import junit.framework.TestCase;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
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

}
