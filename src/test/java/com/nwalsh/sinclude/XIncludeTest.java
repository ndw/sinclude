package com.nwalsh.sinclude;

import com.nwalsh.sinclude.exceptions.XIncludeIntegrityCheckException;
import com.nwalsh.sinclude.exceptions.XIncludeLoopException;
import com.nwalsh.sinclude.exceptions.XIncludeNoFragmentException;
import junit.framework.TestCase;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class XIncludeTest extends TestCase {
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

    public void testCreate() {
        XInclude include = new XInclude();
        assertNotNull(include);
    }

    public void testNoIncludes() {
        XInclude include = new XInclude(resolver);
        XdmNode doc = resolver.resolveXml(emptyDoc, "one.xml", null, null);
        XdmNode resolved = null;
        try {
            resolved = include.expandXIncludes(doc);
        } catch (XPathException e) {
            throw new RuntimeException(e);
        }
        assertTrue(resolver.theSame(doc, resolved));
    }

    private void compareDocs(String key) {
        XInclude include = new XInclude(resolver);

        if ("trimtext.xml".equals(key)) {
            include.setTrimText(true);
        }

        XdmNode doc = resolver.resolveXml(emptyDoc, key, null, null);
        XdmNode resolved = null;
        try {
            resolved = include.expandXIncludes(doc);
        } catch (XPathException e) {
            throw new RuntimeException(e);
        }

        XdmNode expected = resolver.expected(emptyDoc, key);

        boolean pass = resolver.theSame(expected, resolved);
        if (!pass) {
            System.err.println("Expected:");
            System.err.println(expected);
            System.err.println("Actual:");
            System.err.println(resolved);
        }

        assertTrue(pass);
    }

    public void testXmlIncludeTwo() {
        compareDocs("two.xml");
    }

    public void testXmlIncludeFour() {
        compareDocs("four.xml");
    }

    public void testXmlIncludeFive() {
        compareDocs("five.xml");
    }

    public void testXmlIncludeSix() {
        compareDocs("six.xml");
    }

    public void testXmlIncludeSeven() {
        compareDocs("seven.xml");
    }

    public void testXmlIncludeEight() {
        compareDocs("eight.xml");
    }

    public void testXmlIncludeNine() {
        compareDocs("nine.xml");
    }

    public void testXmlIncludeTen() {
        compareDocs("ten.xml");
    }

    public void testXmlIncludeEleven() {
        compareDocs("eleven.xml");
    }

    public void testXmlIncludeTwelve() {
        String key = "twelve.xml";
        XInclude include = new XInclude(resolver);
        include.setFixupXmlBase(false);
        XdmNode doc = resolver.resolveXml(emptyDoc, key, null, null);
        XdmNode resolved = null;
        try {
            resolved = include.expandXIncludes(doc);
        } catch (XPathException e) {
            throw new RuntimeException(e);
        }

        XdmNode expected = resolver.expected(emptyDoc, key);

        boolean pass = resolver.theSame(expected, resolved);
        if (!pass) {
            System.err.println("Expected:");
            System.err.println(expected);
            System.err.println("Actual:");
            System.err.println(resolved);
        }

        assertTrue(pass);
    }

    public void testXmlIncludeThirteen() {
        compareDocs("thirteen.xml");
    }

    public void testXmlIncludeFourteen() {
        String key = "fourteen.xml";
        XInclude include = new XInclude(resolver);
        include.setFixupXmlLang(false);
        XdmNode doc = resolver.resolveXml(emptyDoc, key, null, null);
        XdmNode resolved = null;
        try {
            resolved = include.expandXIncludes(doc);
        } catch (XPathException e) {
            throw new RuntimeException(e);
        }

        XdmNode expected = resolver.expected(emptyDoc, key);

        boolean pass = resolver.theSame(expected, resolved);
        if (!pass) {
            System.err.println("Expected:");
            System.err.println(expected);
            System.err.println("Actual:");
            System.err.println(resolved);
        }

        assertTrue(pass);
    }

    public void testXmlIncludeFifteen() {
        String key = "fifteen.xml";
        XInclude include = new XInclude(resolver);
        include.setFixupXmlLang(true);
        XdmNode doc = resolver.resolveXml(emptyDoc, key, null, null);
        XdmNode resolved = null;
        try {
            resolved = include.expandXIncludes(doc);
        } catch (XPathException e) {
            throw new RuntimeException(e);
        }

        XdmNode expected = resolver.expected(emptyDoc, key);

        boolean pass = resolver.theSame(expected, resolved);
        if (!pass) {
            System.err.println("Expected:");
            System.err.println(expected);
            System.err.println("Actual:");
            System.err.println(resolved);
        }

        assertTrue(pass);
    }

    public void testXmlIncludeSixteen() {
        String key = "sixteen.xml";
        XInclude include = new XInclude(resolver);
        include.setFixupXmlLang(true);
        XdmNode doc = resolver.resolveXml(emptyDoc, key, null, null);
        XdmNode resolved = null;
        try {
            resolved = include.expandXIncludes(doc);
        } catch (XPathException e) {
            throw new RuntimeException(e);
        }

        XdmNode expected = resolver.expected(emptyDoc, key);

        boolean pass = resolver.theSame(expected, resolved);
        if (!pass) {
            System.err.println("Expected:");
            System.err.println(expected);
            System.err.println("Actual:");
            System.err.println(resolved);
        }

        assertTrue(pass);
    }

    public void testXmlIncludeSeventeen() {
        String key = "seventeen.xml";
        XInclude include = new XInclude(resolver);
        include.setFixupXmlLang(true);
        include.setCopyAttributes(false);
        XdmNode doc = resolver.resolveXml(emptyDoc, key, null, null);
        XdmNode resolved = null;
        try {
            resolved = include.expandXIncludes(doc);
        } catch (XPathException e) {
            throw new RuntimeException(e);
        }

        XdmNode expected = resolver.expected(emptyDoc, key);

        boolean pass = resolver.theSame(expected, resolved);
        if (!pass) {
            System.err.println("Expected:");
            System.err.println(expected);
            System.err.println("Actual:");
            System.err.println(resolved);
        }

        assertTrue(pass);
    }

    public void testXmlIncludeNest1() {
        compareDocs("nest1.xml");
    }

    public void testXmlIncludeLoop1() {
        try {
            compareDocs("loop1.xml");
            fail();
        } catch (XIncludeLoopException le) {
            // ok
        } catch (Throwable cause) {
            fail();
        }
    }

    public void testXmlIncludeLoop2() {
        compareDocs("loop3.xml");
    }

    public void testXmlIncludeICheck1() {
        compareDocs("icheck1.xml");
    }

    public void testXmlIncludeICheck2() {
        try {
            compareDocs("icheck2.xml");
            fail();
        } catch (XIncludeNoFragmentException nfe) {
            if (! (nfe.getCause() instanceof XIncludeIntegrityCheckException)) {
                fail();
            }
        } catch (Throwable cause) {
            fail();
        }
    }

    public void testXmlIncludeICheck3() {
        compareDocs("icheck3.xml");
    }

    public void testXmlIncludeICheck4() {
        compareDocs("icheck4.xml");
    }

    public void testXmlIncludeICheck5() {
        compareDocs("icheck5.xml");
    }

    public void testXmlIncludeICheck6() {
        compareDocs("icheck5.xml");
    }

    public void testXmlIncludeSelfRef() {
        compareDocs("selfref.xml");
    }

    public void testEscapeText() {
        compareDocs("escapetext.xml");
    }

    public void testTextIntegrity() {
        compareDocs("textintegrity.xml");
    }

    public void testTextIntegrityFail() {
        try {
            compareDocs("textintegrityfail.xml");
            fail();
        } catch (XIncludeNoFragmentException nfe) {
            if (! (nfe.getCause() instanceof XIncludeIntegrityCheckException)) {
                fail();
            }
        } catch (Throwable cause) {
            fail();
        }
    }

    public void testTextLeadingBlanks() {
        compareDocs("leadingblanks.xml");
    }

    public void testTextGhLine() {
        compareDocs("ghline.xml");
    }

    public void testTextGhLineRange() {
        compareDocs("ghlinerange.xml");
    }

    public void testEncoding() {
        XInclude include = new XInclude();

        try {
            DocumentBuilder builder = processor.newDocumentBuilder();
            XdmNode doc = builder.build(new File("src/test/resources/iso-8859-1.xml"));
            XdmNode resolved = include.expandXIncludes(doc);
            assertNotNull(resolved);
        } catch (Exception e) {
            fail();
        }
    }

    public void testNoLoopWithText() {
        XInclude include = new XInclude();

        try {
            DocumentBuilder builder = processor.newDocumentBuilder();
            XdmNode doc = builder.build(new File("src/test/resources/notaloop-doc.xml"));
            XdmNode resolved = include.expandXIncludes(doc);
            assertNotNull(resolved);
        } catch (Exception e) {
            fail();
        }
    }

    public void testNestedIncludeTest() {
        XInclude include = new XInclude();

        try {
            DocumentBuilder builder = processor.newDocumentBuilder();
            XdmNode doc = builder.build(new File("src/test/resources/xproc-root.xml"));
            XdmNode resolved = include.expandXIncludes(doc);
            assertNotNull(resolved);
        } catch (Exception e) {
            fail();
        }
    }

    public void testXmlTextSelfRef() {
        compareDocs("textselfref.xml");
    }

    public void testMixedXmlTextSelfRef() {
        compareDocs("mixedselfref.xml");
    }

    public void testXmlSelfRef() {
        compareDocs("xmlselfref.xml");
    }

    public void testTrimText() {
        compareDocs("trimtext.xml");
    }

    public void testXmlSelfRefLoop() {
        try {
            compareDocs("selfrefloop.xml");
            fail();
        } catch (Exception e) {
            if (!(e instanceof XIncludeNoFragmentException)) {
                fail();
            }
        }
    }
}
