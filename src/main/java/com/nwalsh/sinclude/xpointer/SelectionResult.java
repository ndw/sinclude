package com.nwalsh.sinclude.xpointer;

import net.sf.saxon.s9api.XdmNode;

public interface SelectionResult {
    public SchemeData[] getSchemeData();
    public boolean finished();
    public XdmNode getResult();
}
