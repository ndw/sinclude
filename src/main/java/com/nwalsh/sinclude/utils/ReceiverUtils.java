package com.nwalsh.sinclude.utils;

import com.nwalsh.sinclude.exceptions.TextContentException;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.str.StringView;
import net.sf.saxon.trans.XPathException;

import java.net.URI;

public class ReceiverUtils {
    public static XdmDestination makeDestination(XdmNode node) {
        return makeDestination(nodeBaseURI(node));
    }

    public static XdmDestination makeDestination(URI baseURI) {
        XdmDestination destination = new XdmDestination();
        if (baseURI != null) {
            destination.setBaseURI(baseURI);
        }
        return destination;
    }

    public static Receiver makeReceiver(XdmNode node, XdmDestination destination) throws XPathException {
        PipelineConfiguration pipe = node.getUnderlyingNode().getConfiguration().makePipelineConfiguration();
        return makeReceiver(pipe, destination);
    }

    public static Receiver makeReceiver(PipelineConfiguration pipe, XdmDestination destination) throws XPathException {
        Receiver receiver = destination.getReceiver(pipe, new SerializationProperties());
        receiver.open();
        if (destination.getBaseURI() != null) {
            receiver.setSystemId(destination.getBaseURI().toASCIIString());
        }
        return receiver;
    }

    public static XdmNode makeTextDocument(XdmNode node, String text) {
        XdmDestination destination = ReceiverUtils.makeDestination(node);
        try {
            Receiver receiver = ReceiverUtils.makeReceiver(node, destination);
            receiver.startDocument(0);
            receiver.characters(StringView.of(text), Loc.NONE, 0);
            receiver.endDocument();
            receiver.close();
            return destination.getXdmNode();
        } catch (XPathException e) {
            throw new TextContentException(e);
        }
    }

    public static URI nodeBaseURI(XdmNode node) {
        if (node.getBaseURI() == null || "".equals(node.getBaseURI().toString())) {
            return null;
        }
        return node.getBaseURI();
    }
}