package com.nwalsh.sinclude;

import com.nwalsh.sinclude.xpointer.DefaultSelectionResult;
import com.nwalsh.sinclude.xpointer.FragmentIdParser;
import com.nwalsh.sinclude.xpointer.Scheme;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import junit.framework.TestCase;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
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
import java.nio.charset.StandardCharsets;

public class SchemeTextTest extends TestCase {
    private static final String NS_XML = "http://www.w3.org/XML/1998/namespace";
    private static final QName xml_id = new QName(NS_XML, "id");

    private Processor processor = null;
    private XInclude xinclude = null;
    private FragmentIdParser fragidParser = null;
    private XdmNode document = null;

    public void setUp() {
        processor = new Processor(false);
        xinclude = new XInclude();
        fragidParser = xinclude.getFragmentIdParser();

        String doc = "This is line one.\n"
                + "\n"
                + "\n"
                + "This is line four.\n"
                + "\n"
                + "This is line six.\n"
                + "\n"
                + "     This is line eight.\n"
                + "     This is line nine.\n"
                + "       This is line ten.\n"
                + "\n"
                + "This is line twelve.\n"
                + "\n"
                + "This is line fourteen.\n"
                + "This is line fifteen.\n"
                + "This is line sixteen.\n"
                + "\n"
                + "This is line eighteen.\n"
                + "\n"
                + "This is line twenty.\n";

        XdmDestination destination = new XdmDestination();
        PipelineConfiguration pipe = processor.getUnderlyingConfiguration().makePipelineConfiguration();
        Receiver receiver = destination.getReceiver(pipe, new SerializationProperties());

        try {
            receiver.open();
            receiver.startDocument(0);
            receiver.characters(doc, VoidLocation.instance(), 0);
            receiver.endDocument();
            receiver.close();
            document = destination.getXdmNode();
        } catch (XPathException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public void testLineRange0() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("text", "line=0,1");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This is line one.\n", fragment);
    }

    public void testLineRange3() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("text", "line=3,6");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This is line four.\n\nThis is line six.\n", fragment);
    }

    public void testLineRangeTo5() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("text", "line=,4");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This is line one.\n\n\nThis is line four.\n", fragment);
    }

    public void testLineRangeFrom18() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("text", "line=17,");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This is line eighteen.\n\nThis is line twenty.\n", fragment);
    }

    public void testLineRangePointless() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("text", "line=3");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("", fragment);
    }

    public void testCharRange0() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("text", "char=0,1");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("T", fragment);
    }

    public void testCharRange3() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("text", "char=3,6");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("s i", fragment);
    }

    public void testCharRangeTo5() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("text", "char=,4");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This", fragment);
    }

    public void testCharRangeFrom250() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("text", "char=248,");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This is line twenty.\n", fragment);
    }

    public void testCharRangePointless() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("text", "char=3");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("", fragment);
    }
}