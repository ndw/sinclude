package com.nwalsh.sinclude.schemes;

import com.nwalsh.sinclude.VoidLocation;
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
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.trans.XPathException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XPathScheme implements Scheme {
    protected String xpath = null;

    @Override
    public XPathScheme newInstance(String fdata) {
        XPathScheme scheme = new XPathScheme();
        scheme.xpath = fdata;
        return scheme;
    }

    @Override
    public String schemeName() {
        return "xpath";
    }

    @Override
    public String parseType() {
        return "xml";
    }

    @Override
    public SelectionResult select(SchemeData[] schemeData, XdmNode document) {
        XPathCompiler xcomp = document.getProcessor().newXPathCompiler();
        for (SchemeData data : schemeData) {
            if (data instanceof XmlnsData) {
                XmlnsData xdata = (XmlnsData) data;
                xcomp.declareNamespace(xdata.getPrefix(), xdata.getUri());
            }
        }

        XPathSelector selector = null;
        try {
            XPathExecutable xexec = xcomp.compile(xpath);
            selector = xexec.load();
        } catch (SaxonApiException sae) {
            throw new MalformedXPointerSchemeException(sae.getMessage());
        }

        XdmDestination destination = new XdmDestination();
        PipelineConfiguration pipe = document.getUnderlyingNode().getConfiguration().makePipelineConfiguration();
        Receiver receiver = destination.getReceiver(pipe,  new SerializationProperties());

        try {
            boolean found = false;
            receiver.open();
            receiver.startDocument(0);

            selector.setContextItem(document);
            for (XdmItem item : selector.evaluate()) {
                if (item.isNode()) {
                    found = true;
                    XdmNode node = (XdmNode) item;
                    if (node.getNodeKind() == XdmNodeKind.ATTRIBUTE) {
                        receiver.characters(node.getStringValue(), VoidLocation.instance(), 0);
                    } else {
                        receiver.append(item.getUnderlyingValue());
                    }
                } else {
                    throw new RuntimeException("XPath matched non-node item?: " + schemeName() + "(" + xpath + ")");
                }
            }

            receiver.endDocument();
            receiver.close();

            if (found) {
                return new DefaultSelectionResult(true, destination.getXdmNode());
            } else {
                return new DefaultSelectionResult(false, null);
            }
        } catch (SaxonApiException | XPathException e) {
            throw new UnsupportedOperationException(e);
        }
    }
}
