package com.nwalsh.sinclude.utils;

import com.nwalsh.sinclude.exceptions.TextContentException;
import com.nwalsh.sinclude.exceptions.XIncludeException;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.s9api.Location;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.trans.XPathException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;

public class ReceiverUtils {
    private static Boolean saxon10 = null;
    private static Method characters;
    private static Method of;

    public static Receiver makeReceiver(XdmNode node, XdmDestination destination) throws XPathException {
        return makeReceiver(node, destination, node.getBaseURI());
    }

    public static Receiver makeReceiver(XdmNode node, XdmDestination destination, URI baseURI) throws XPathException {
        PipelineConfiguration pipe = node.getUnderlyingNode().getConfiguration().makePipelineConfiguration();
        return makeReceiver(pipe, destination, baseURI);
    }

    public static Receiver makeReceiver(PipelineConfiguration pipe, XdmDestination destination, URI baseURI) throws XPathException {
        Receiver receiver = destination.getReceiver(pipe, new SerializationProperties());
        if (baseURI != null) {
            receiver.setSystemId(baseURI.toString());
        }
        receiver.open();
        return receiver;
    }

    public static XdmNode makeTextDocument(XdmNode node, String text) {
        XdmDestination destination = new XdmDestination();
        try {
            Receiver receiver = ReceiverUtils.makeReceiver(node, destination);
            receiver.startDocument(0);
            ReceiverUtils.handleCharacters(receiver, text);
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

    public static void handleCharacters(Receiver receiver, String text) {
        if (saxon10 == null) {
            try {
                characters = receiver.getClass().getMethod("characters", CharSequence.class, Location.class, int.class);
                saxon10 = true;
            } catch (NoSuchMethodException ex) {
                saxon10 = false;
                try {
                    Class<?> clazzUnicodeString = Class.forName("net.sf.saxon.str.UnicodeString");
                    Class<?> clazzStringView= Class.forName("net.sf.saxon.str.StringView");
                    of = clazzStringView.getMethod("of", String.class);
                    characters = receiver.getClass().getMethod("characters", clazzUnicodeString, Location.class, int.class);
                } catch (ClassNotFoundException | NoSuchMethodException ex11) {
                    throw new XIncludeException("Failed to resolve Saxon 11 methods with reflection");
                }
            }
        }

        try {
            if (saxon10) {
                characters.invoke(receiver, text, Loc.NONE, 0);
            } else {
                characters.invoke(receiver, of.invoke(null, text), Loc.NONE, 0);
            }
        } catch (InvocationTargetException | IllegalAccessException ex) {
            throw new XIncludeException("Failed to handle characters with reflection");
        }
    }
}