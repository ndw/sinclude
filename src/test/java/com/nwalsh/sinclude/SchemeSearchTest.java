package com.nwalsh.sinclude;

import com.nwalsh.sinclude.xpointer.FragmentIdParser;
import com.nwalsh.sinclude.xpointer.ParseType;
import com.nwalsh.sinclude.xpointer.Scheme;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import junit.framework.TestCase;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.trans.XPathException;

public class SchemeSearchTest extends TestCase {
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
                + "This is line four four four.\n"
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
            receiver.characters(doc, Loc.NONE, 0);
            receiver.endDocument();
            receiver.close();
            document = destination.getXdmNode();
        } catch (XPathException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public void testSearchFoundStart() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.TEXTPARSE, "search=/twenty/");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This is line twenty.\n", fragment);
    }

    public void testSearchFoundEnd() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.TEXTPARSE, "search=,/one/");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This is line one.\n", fragment);
    }

    public void testSearchFoundStartEnd() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.TEXTPARSE, "search=/one/,/four/");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This is line one.\n\n\nThis is line four four four.\n", fragment);
    }

    public void testSearchFoundStartEnd2() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.TEXTPARSE, "search=2/four/,/six/");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This is line fourteen.\nThis is line fifteen.\nThis is line sixteen.\n", fragment);
    }

    public void testSearchFoundAfter() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.TEXTPARSE, "search=/one/;after,/six/");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("\n\nThis is line four four four.\n\nThis is line six.\n", fragment);
    }

    public void testSearchFoundAfterTrim() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.TEXTPARSE, "search=/one/;trim,/six/");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This is line four four four.\n\nThis is line six.\n", fragment);
    }

    public void testSearchFoundBefore() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.TEXTPARSE, "search=/one/,/six/;before");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This is line one.\n\n\nThis is line four four four.\n\n", fragment);
    }

    public void testSearchFoundBeforeTrim() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.TEXTPARSE, "search=/one/,/six/;trim");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This is line one.\n\n\nThis is line four four four.\n", fragment);
    }

    public void testSearchFoundBeforeAfterTrim() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.TEXTPARSE, "search=/one/;trim,/six/;trim");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String fragment = result.getResult().getStringValue();
        assertEquals("This is line four four four.\n", fragment);
    }

    public void testSearchFoundStrip() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.TEXTPARSE, "search=/six/,/twelve/;strip");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String answer = "This is line six.\n"
                + "\n"
                + "     This is line eight.\n"
                + "     This is line nine.\n"
                + "       This is line ten.\n"
                + "\n"
                + "This is line twelve.\n";

        String fragment = result.getResult().getStringValue();
        assertEquals(answer, fragment);
    }

    public void testSearchFoundAfterStrip() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.TEXTPARSE, "search=/six/;trim,/twelve/;strip");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String answer = "     This is line eight.\n"
                + "     This is line nine.\n"
                + "       This is line ten.\n"
                + "\n"
                + "This is line twelve.\n";

        String fragment = result.getResult().getStringValue();
        assertEquals(answer, fragment);
    }

    public void testSearchFoundBeforeAfterStrip() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.TEXTPARSE, "search=/six/;trim,/twelve/;trim;strip");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String answer = "This is line eight.\n"
                + "This is line nine.\n"
                + "  This is line ten.\n";

        String fragment = result.getResult().getStringValue();
        assertEquals(answer, fragment);
    }

    public void testSearchFoundAfterNoStrip() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.TEXTPARSE, "search=/six/;trim,/twelve/;trim");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        String answer = "     This is line eight.\n"
                + "     This is line nine.\n"
                + "       This is line ten.\n";

        String fragment = result.getResult().getStringValue();
        assertEquals(answer, fragment);
    }

}
