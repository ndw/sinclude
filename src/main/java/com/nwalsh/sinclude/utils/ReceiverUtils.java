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