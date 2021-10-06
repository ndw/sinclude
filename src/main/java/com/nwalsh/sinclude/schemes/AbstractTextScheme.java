package com.nwalsh.sinclude.schemes;

import com.nwalsh.sinclude.exceptions.TextContentException;
import com.nwalsh.sinclude.utils.ReceiverUtils;
import com.nwalsh.sinclude.xpointer.DefaultSelectionResult;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;

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
                throw new TextContentException("Document must contain a text node");
            }
            count++;
            if (count > 1) {
                throw new TextContentException("Document must contain a single text node");
            }
        }
        return text;
    }

    protected SelectionResult textResults(XdmNode document, String text) {
        return new DefaultSelectionResult(true, ReceiverUtils.makeTextDocument(document, text));
    }
}
