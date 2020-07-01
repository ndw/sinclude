package com.nwalsh.sinclude;

import com.nwalsh.sinclude.xpointer.FragmentIdParser;
import com.nwalsh.sinclude.xpointer.ParseType;
import com.nwalsh.sinclude.xpointer.Scheme;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import junit.framework.TestCase;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class SchemeXPathTest extends TestCase {
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

        String doc = "<doc>"
                + "  <p xml:id='one'/>"
                + "  <p xml:id='two' xmlns='http://example.com/p'>"
                + "    <i xml:id='i1'>"
                + "      italic"
                + "      <b xml:id='b1'>italic bold</b>"
                + "      <b xml:id='b2'>"
                + "        italic bold"
                + "        <u xml:id='u1'>italic bold underline</u>"
                + "      </b>"
                + "    </i>"
                + "    <b xml:id='b3'>bold</b>"
                + "  </p>"
                + "</doc>";

        ByteArrayInputStream bais = new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8));
        DocumentBuilder builder = processor.newDocumentBuilder();
        builder.setLineNumbering(false);
        builder.setDTDValidation(false);
        try {
            document = builder.build(new SAXSource(new InputSource(bais)));
        } catch (SaxonApiException e) {
            // this can't happen
            throw new RuntimeException(e);
        }
    }

    public void testId() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.XMLPARSE, "xpath(id^('one'^))");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        int count = 0;
        XdmSequenceIterator<XdmNode> iter = result.getResult().axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode node = iter.next();
            count++;
            assertEquals(XdmNodeKind.ELEMENT, node.getNodeKind());
            assertEquals("one", node.getAttributeValue(xml_id));
        }
        assertEquals(1, count);
    }

    public void testIdFail() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.XMLPARSE, "xpath(id^('no-such-id'^))");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertFalse(result.finished());
        assertNull(result.getResult());
    }

    public void testElement() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.XMLPARSE, "xpath(/*/p)");
        SelectionResult result = schemes[0].select(new SchemeData[]{}, document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        int count = 0;
        XdmSequenceIterator<XdmNode> iter = result.getResult().axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode node = iter.next();
            count++;
            assertEquals(XdmNodeKind.ELEMENT, node.getNodeKind());
            assertEquals("one", node.getAttributeValue(xml_id));
        }
        assertEquals(1, count);
    }

    public void testNsElement() {
        String xmlns = "xmlns(p=http://example.com/p)";
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.XMLPARSE, xmlns);
        SelectionResult result = schemes[0].select(new SchemeData[]{}, null);

        String xpath = "xpath(/*/p:p)";
        schemes = fragidParser.parseFragmentIdentifier(ParseType.XMLPARSE, xpath);
        result = schemes[0].select(result.getSchemeData(), document);
        assertTrue(result.finished());
        assertNotNull(result.getResult());

        int count = 0;
        XdmSequenceIterator<XdmNode> iter = result.getResult().axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode node = iter.next();
            count++;
            assertEquals(XdmNodeKind.ELEMENT, node.getNodeKind());
            assertEquals("two", node.getAttributeValue(xml_id));
        }
        assertEquals(1, count);
    }
}
