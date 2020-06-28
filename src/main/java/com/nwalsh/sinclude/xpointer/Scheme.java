package com.nwalsh.sinclude.xpointer;

import net.sf.saxon.s9api.XdmNode;

public interface Scheme {
    public String schemeName();
    public String parseType();
    public SelectionResult select(SchemeData[] schemeData, XdmNode document);
}
