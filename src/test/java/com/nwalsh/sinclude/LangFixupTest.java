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
import org.junit.Assert;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class LangFixupTest extends TestCase {
    private Processor processor = new Processor(false);

    public void testLangFixup() {
        try {
            XInclude include = new XInclude();
            include.setFixupXmlLang(true);
            include.setFixupXmlBase(false);
            DocumentBuilder builder = processor.newDocumentBuilder();
            XdmNode doc = builder.build(new File("src/test/resources/langfixup.xml"));
            XdmNode resolved = include.expandXIncludes(doc);
            String aug = resolved.toString();

            Assert.assertTrue(aug.contains("<p xml:lang=\"en\">English")
                    || aug.contains("<p xml:lang='en'>English"));
            Assert.assertTrue(aug.contains("<p xml:lang=\"de\">Deutsch")
                    || aug.contains("<p xml:lang='de'>Deutsch"));
            Assert.assertTrue(aug.contains("<p xml:lang=\"\">Unspecified")
                    || aug.contains("<p xml:lang=''>Unspecified"));

            Assert.assertTrue(aug.contains("<chap xml:lang=\"en\">")
                    || aug.contains("<chap xml:lang='en'>"));
            Assert.assertTrue(aug.contains("<chap xml:lang=\"de\">")
                    || aug.contains("<chap xml:lang='de'>"));
            Assert.assertTrue(aug.contains("<chap xml:lang=\"\">")
                    || aug.contains("<chap xml:lang=''>"));

        } catch (SaxonApiException|XPathException ex) {
            fail();
        }
    }
}
