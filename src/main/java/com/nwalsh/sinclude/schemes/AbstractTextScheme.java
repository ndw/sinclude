package com.nwalsh.sinclude.schemes;

import com.nwalsh.sinclude.exceptions.XIncludeIOException;
import com.nwalsh.sinclude.xpointer.DefaultSelectionResult;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.trans.XPathException;

public abstract class AbstractTextScheme {
    protected String getText(XdmNode document) {
        String text = "";
        int count = 0;
        XdmSequenceIterator<XdmNode> iter = document.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode node = iter.next();
            if (node.getNodeKind() == XdmNodeKind.TEXT) {
                text = node.getStringValue();
            } else {
                throw new XIncludeIOException("Document must contain a text node");
            }
            count++;
            if (count > 1) {
                throw new XIncludeIOException("Document must contain a single text node");
            }
        }
        return text;
    }

    protected SelectionResult textResults(XdmNode document, String text) {
        XdmDestination destination = new XdmDestination();
        PipelineConfiguration pipe = document.getUnderlyingNode().getConfiguration().makePipelineConfiguration();
        Receiver receiver = destination.getReceiver(pipe, new SerializationProperties());
        try {
            receiver.open();
            receiver.startDocument(0);
            receiver.characters(text, Loc.NONE, 0);
            receiver.endDocument();
            receiver.close();
            return new DefaultSelectionResult(true, destination.getXdmNode());
        } catch (XPathException e) {
            throw new XIncludeIOException(e);
        }
    }
}
