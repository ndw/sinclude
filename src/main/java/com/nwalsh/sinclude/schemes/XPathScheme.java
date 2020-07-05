package com.nwalsh.sinclude.schemes;

import com.nwalsh.sinclude.XInclude;
import com.nwalsh.sinclude.data.XmlnsData;
import com.nwalsh.sinclude.exceptions.MalformedXPointerSchemeException;
import com.nwalsh.sinclude.exceptions.XIncludeIOException;
import com.nwalsh.sinclude.xpointer.DefaultSelectionResult;
import com.nwalsh.sinclude.xpointer.Scheme;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import com.nwalsh.sinclude.xpointer.XmlScheme;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.s9api.Location;
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

import java.net.URI;
import java.util.Vector;

public class XPathScheme extends AbstractXmlScheme implements XmlScheme {
    protected String xpath = null;

    @Override
    public XPathScheme newInstance(String fdata, XInclude xinclude) {
        XPathScheme scheme = new XPathScheme();
        scheme.xpath = fdata;
        scheme.xinclude = xinclude;
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

        // There's an (apparent) bug in the Saxon API where setting the location when appending to the
        // receiver has no effect if you haven't also set the system ID on the receiver.
        // See https://saxonica.plan.io/issues/4618
        //
        // I evaluate the expression and store there results so that I can work out what base URI
        // to use on the receiver. I have to do that before I open the receiver...

        try {
            URI baseURI = null;
            Vector<XdmNode> results = new Vector<XdmNode>();

            selector.setContextItem(document);
            for (XdmItem item : selector.evaluate()) {
                if (item.isNode()) {
                    XdmNode node = (XdmNode) item;
                    URI nodeBaseURI = node.getBaseURI();
                    if (baseURI == null && nodeBaseURI != null && !"".equals(nodeBaseURI.toASCIIString())) {
                        baseURI = nodeBaseURI;
                    }

                    if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
                        results.add(fixup(node));
                    } else {
                        results.add(node);
                    }
                } else {
                    throw new XIncludeIOException("XPath matched non-node item?: " + schemeName() + "(" + xpath + ")");
                }
            }

            if (baseURI != null) {
                receiver.setSystemId(baseURI.toASCIIString());
            }

            receiver.open();
            receiver.startDocument(0);
            for (XdmNode node : results) {
                Location loc = new Loc(node.getBaseURI().toASCIIString(), -1, -1);
                if (node.getNodeKind() == XdmNodeKind.ATTRIBUTE) {
                    receiver.characters(node.getStringValue(), loc, 0);
                } else {
                    receiver.append(node.getUnderlyingValue(), loc, 0);
                }
            }

            receiver.endDocument();
            receiver.close();

            if (results.isEmpty()) {
                return new DefaultSelectionResult(false, null);
            } else {
                return new DefaultSelectionResult(true, destination.getXdmNode());
            }
        } catch (SaxonApiException | XPathException e) {
            throw new XIncludeIOException(e);
        }
    }
}
