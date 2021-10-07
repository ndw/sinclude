package com.nwalsh.sinclude.xpointer;

import net.sf.saxon.s9api.XdmNode;

public interface SelectionResult {
    SchemeData[] getSchemeData();
    boolean finished();
    XdmNode getResult();
    XdmNode[] getSelectedNodes();
}
