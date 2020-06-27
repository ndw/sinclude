package com.nwalsh.sinclude.xpointer;

import net.sf.saxon.s9api.XdmNode;

public interface TextScheme extends Scheme {
    public XdmNode select(SchemeData[] data, String text);
}
