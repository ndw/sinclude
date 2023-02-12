package com.nwalsh.sinclude.schemes;

import com.nwalsh.sinclude.XInclude;
import com.nwalsh.sinclude.data.XmlnsData;
import com.nwalsh.sinclude.exceptions.MalformedXPointerSchemeException;
import com.nwalsh.sinclude.exceptions.XPointerSchemeMatchException;
import com.nwalsh.sinclude.utils.ReceiverUtils;
import com.nwalsh.sinclude.xpointer.DefaultSelectionResult;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import com.nwalsh.sinclude.xpointer.XmlScheme;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.XPathException;

import java.net.URI;
import java.util.Vector;

public class XPathScheme extends AbstractXmlScheme implements XmlScheme {
    protected String xpath = null;

    @Override
    public XPathScheme newInstance(String fdata, XInclude xinclude, String contextLanguage, String contextBaseURI) {
        XPathScheme scheme = new XPathScheme();
        scheme.xpath = fdata;
        scheme.xinclude = xinclude;
        scheme.contextLanguage = contextLanguage;
        scheme.contextBaseURI = contextBaseURI;
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

        // There's an (apparent) bug in the Saxon API where setting the location when appending to the
        // receiver has no effect if you haven't also set the system ID on the receiver.
        // See https://saxonica.plan.io/issues/4618
        //
        // I evaluate the expression and store there results so that I can work out what base URI
        // to use on the receiver. I have to do that before I open the receiver...

        URI baseURI = null;
        try {
            Vector<XdmNode> results = new Vector<>();
            Vector<XdmNode> nodes = new Vector<>();

            selector.setContextItem(document);
            for (XdmItem item : selector.evaluate()) {
                if (item.isNode()) {
                    XdmNode node = (XdmNode) item;
                    URI nodeBaseURI = ReceiverUtils.nodeBaseURI(node);
                    if (baseURI == null && nodeBaseURI != null) {
                        baseURI = nodeBaseURI;
                    }

                    nodes.add(node);
                    if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
                        results.add(fixup(node));
                    } else {
                        results.add(node);
                    }
                } else {
                    throw new XPointerSchemeMatchException("XPath matched non-node item?: " + schemeName() + "(" + xpath + ")");
                }
            }

            if (baseURI == null) {
                baseURI = ReceiverUtils.nodeBaseURI(document);
            }

            XdmDestination destination = ReceiverUtils.makeDestination(baseURI);
            Receiver receiver = ReceiverUtils.makeReceiver(document, destination);
            receiver.startDocument(0);
            for (XdmNode node : results) {
                Location loc = new Loc(node.getBaseURI().toASCIIString(), -1, -1);
                if (node.getNodeKind() == XdmNodeKind.ATTRIBUTE) {
                    ReceiverUtils.handleCharacters(receiver, node.getStringValue());
                } else {
                    receiver.append(node.getUnderlyingValue(), loc, 0);
                }
            }

            receiver.endDocument();
            receiver.close();

            if (results.isEmpty()) {
                return new DefaultSelectionResult(false, null, null);
            } else {
                return new DefaultSelectionResult(true, destination.getXdmNode(), nodes.toArray(new XdmNode[0]));
            }
        } catch (SaxonApiException | XPathException e) {
            throw new XPointerSchemeMatchException(e);
        }
    }
}
