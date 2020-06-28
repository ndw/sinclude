package com.nwalsh.sinclude;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;

/* This is just a debugging class. */

public class TreeDumper {
    public static void dump(XdmNode node) {
        switch (node.getNodeKind()) {
            case ELEMENT:
                System.err.println("E: " + node.getNodeName() + ": " + node.getBaseURI());
                break;
            case TEXT:
                System.err.println("T: " + node.getStringValue() + ": " + node.getBaseURI());
                break;
            case COMMENT:
                System.err.println("C: " + node.getStringValue() + ": " + node.getBaseURI());
                break;
            case PROCESSING_INSTRUCTION:
                System.err.println("P: " + node.getStringValue() + ": " + node.getBaseURI());
                break;
            case DOCUMENT:
                System.err.println("D: " + node.getBaseURI());
                break;
            default:
                throw new RuntimeException("Unexpected node type: " + node);
        }

        XdmSequenceIterator<XdmNode> iter = node.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode child = iter.next();
            dump(child);
        }
    }
}
