package com.nwalsh.sinclude.schemes;

import com.nwalsh.sinclude.XInclude;
import com.nwalsh.sinclude.data.XmlnsData;
import com.nwalsh.sinclude.exceptions.MalformedXPointerSchemeException;
import com.nwalsh.sinclude.xpointer.DefaultSelectionResult;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import com.nwalsh.sinclude.xpointer.XmlScheme;
import net.sf.saxon.s9api.XdmNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlnsScheme implements XmlScheme {
    private static final Pattern nsRE = Pattern.compile("^\\s*([^\\s=]+)=\\s*(.*)$");
    private XmlnsData data = null;

    @Override
    public XmlnsScheme newInstance(String fdata, XInclude xinclude, String contextLanguage, String contexBaseURI) {
        XmlnsData data = null;
        Matcher matcher = nsRE.matcher(fdata);
        if (matcher.find()) {
            data = new XmlnsData(matcher.group(1), matcher.group(2));
        } else {
            throw new MalformedXPointerSchemeException("Invalid xmlns() data");
        }
        XmlnsScheme scheme = new XmlnsScheme();
        scheme.data = data;
        return scheme;
    }

    @Override
    public String schemeName() {
        return "xmlns";
    }

    @Override
    public String parseType() {
        return "xml";
    }

    @Override
    public SelectionResult select(SchemeData[] schemeData, XdmNode document) {
        return new DefaultSelectionResult(new SchemeData[]{data}, false);
    }
}
