package com.nwalsh.sinclude;

import com.nwalsh.sinclude.exceptions.UnknownXPointerSchemeException;
import com.nwalsh.sinclude.xpointer.DefaultFragmentIdParser;
import com.nwalsh.sinclude.xpointer.FragmentIdParser;
import com.nwalsh.sinclude.xpointer.Scheme;
import junit.framework.TestCase;

public class FragIdParseTest extends TestCase {
    private XInclude xinclude = null;
    private FragmentIdParser fragidParser = null;

    public void setUp() {
        xinclude = new XInclude();
        fragidParser = xinclude.getFragmentIdParser();
    }

    public void testInvalidParseType() {
        try {
            fragidParser.parseFragmentIdentifier("fred", "id");
            fail();
        } catch (IllegalArgumentException iae) {
            // ok
        }
    }

    public void testUnknownScheme() {
        xinclude.clearSchemes();
        try {
            fragidParser.parseFragmentIdentifier("xml", "id");
            fail();
        } catch (UnknownXPointerSchemeException e) {
            // ok;
        }

        xinclude = new XInclude();
        fragidParser = xinclude.getFragmentIdParser();
    }

    public void testId() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("xml", "id");
        assertEquals(1, schemes.length);
        assertEquals("element", schemes[0].schemeName());
        assertEquals("xml", schemes[0].parseType());
    }

    public void testTumbler() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("xml", "/1/2");
        assertEquals(1, schemes.length);
        assertEquals("element", schemes[0].schemeName());
        assertEquals("xml", schemes[0].parseType());
    }

    public void testElementScheme() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier("xml", "element(id)");
        assertEquals(1, schemes.length);
        assertEquals("element", schemes[0].schemeName());
        assertEquals("xml", schemes[0].parseType());
    }

}
