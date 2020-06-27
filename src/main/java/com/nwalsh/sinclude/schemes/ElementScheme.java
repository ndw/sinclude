package com.nwalsh.sinclude.schemes;

import com.nwalsh.sinclude.data.XmlnsData;
import com.nwalsh.sinclude.exceptions.MalformedXPointerSchemeException;
import com.nwalsh.sinclude.xpointer.DefaultSelectionResult;
import com.nwalsh.sinclude.xpointer.Scheme;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.trans.XPathException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElementScheme extends XPathScheme {
    private String fragid = null;

    @Override
    public ElementScheme newInstance(String fdata) {
        ElementScheme scheme = new ElementScheme();
        scheme.fragid = fdata;
        scheme.xpath = toXPath(fdata);
        return scheme;
    }

    @Override
    public String schemeName() {
        return "element";
    }

    @Override
    public SelectionResult select(SchemeData[] schemeData, XdmNode document) {
        try {
            return super.select(schemeData, document);
        } catch (RuntimeException e) {
            throw new RuntimeException("Element scheme matched non-node item?: " + schemeName() + "(" + fragid + ")");
        }
    }


    private String toXPath(String schemeData) {
        String xpath = "";
        String data = schemeData;
        int pos = data.indexOf("/");
        if (pos < 0) {
            return "id('" + data + "')";
        }

        if (pos > 0) {
            xpath = "id('" + data.substring(0,pos) + "')";
            data = data.substring(pos);
        }

        Pattern dscheme = Pattern.compile("^/(\\d+)(.*)$");
        Matcher dmatcher = dscheme.matcher(data);
        StringBuilder builder = new StringBuilder();
        builder.append(xpath);
        while (dmatcher.matches()) {
            builder.append("/*[").append(dmatcher.group(1)).append("]");
            data = dmatcher.group(2);
            dmatcher = dscheme.matcher(data);
        }
        xpath = builder.toString();

        if (!"".equals(data)) {
            throw new MalformedXPointerSchemeException("Invalid element scheme data: " + schemeData);
        }

        return xpath;
    }
}
