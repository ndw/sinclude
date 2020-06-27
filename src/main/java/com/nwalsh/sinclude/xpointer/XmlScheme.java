package com.nwalsh.sinclude.xpointer;

import net.sf.saxon.s9api.XdmNode;

public interface XmlScheme {
    public SelectionResult select(SchemeData[] data, XdmNode document, String fragid);
}
