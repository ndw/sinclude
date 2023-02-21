package com.nwalsh.sinclude;

import com.nwalsh.sinclude.exceptions.UnknownXPointerSchemeException;
import com.nwalsh.sinclude.exceptions.XIncludeSyntaxException;
import com.nwalsh.sinclude.xpointer.FragmentIdParser;
import com.nwalsh.sinclude.xpointer.ParseType;
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
            fragidParser.parseFragmentIdentifier(ParseType.NOPARSE, "id");
            fail();
        } catch (XIncludeSyntaxException iae) {
            // ok
        }
    }

    public void testUnknownScheme() {
        xinclude.clearSchemes();
        try {
            fragidParser.parseFragmentIdentifier(ParseType.XMLPARSE, "id");
            fail();
        } catch (UnknownXPointerSchemeException e) {
            // ok;
        }

        xinclude = new XInclude();
        fragidParser = xinclude.getFragmentIdParser();
    }

    public void testId() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.XMLPARSE, "id");
        assertEquals(1, schemes.length);
        assertEquals("element", schemes[0].schemeName());
        assertEquals("xml", schemes[0].parseType());
    }

    public void testTumbler() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.XMLPARSE, "/1/2");
        assertEquals(1, schemes.length);
        assertEquals("element", schemes[0].schemeName());
        assertEquals("xml", schemes[0].parseType());
    }

    public void testElementScheme() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.XMLPARSE, "element(id)");
        assertEquals(1, schemes.length);
        assertEquals("element", schemes[0].schemeName());
        assertEquals("xml", schemes[0].parseType());
    }

    public void testBalancedParens() {
        Scheme[] schemes = fragidParser.parseFragmentIdentifier(ParseType.XMLPARSE, "xpath(//*[contains(., 'test')])");
        assertEquals(1, schemes.length);
        assertEquals("xpath", schemes[0].schemeName());
        assertEquals("xml", schemes[0].parseType());
    }

}
