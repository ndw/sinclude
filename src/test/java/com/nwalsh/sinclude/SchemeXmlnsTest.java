package com.nwalsh.sinclude;

import com.nwalsh.sinclude.data.XmlnsData;
import com.nwalsh.sinclude.exceptions.MalformedXPointerSchemeException;
import com.nwalsh.sinclude.schemes.XmlnsScheme;
import com.nwalsh.sinclude.xpointer.FragmentIdParser;
import com.nwalsh.sinclude.xpointer.Scheme;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import junit.framework.TestCase;

public class SchemeXmlnsTest extends TestCase {
    private static XInclude include = null;
    private static FragmentIdParser fragidParser = null;

    public void setUp() {
        include = new XInclude();
        fragidParser = include.getFragmentIdParser();
    }

    public void testXmlnsScheme() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("xml", "xmlns(db=http://docbook.org/ns/docbook)");
        XmlnsScheme scheme = (XmlnsScheme) schemes[0];

        SelectionResult result = scheme.select(new SchemeData[]{}, null);
        assertFalse(result.finished());
        assertNull(result.getResult());
        assertEquals(1, result.getSchemeData().length);
        assertTrue(result.getSchemeData()[0] instanceof XmlnsData);
        XmlnsData data = (XmlnsData) result.getSchemeData()[0];
        assertEquals("db", data.getPrefix());
        assertEquals("http://docbook.org/ns/docbook", data.getUri());
    }

    public void testXmlnsSchemeError() {
        try {
            fragidParser.parseFragmentIdentifier("xml", "xmlns(db http://docbook.org/ns/docbook)");
            fail();
        } catch (MalformedXPointerSchemeException iae) {
            assertTrue(true);
        }
    }
}
