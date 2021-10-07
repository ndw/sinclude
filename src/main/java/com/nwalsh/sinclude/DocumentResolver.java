package com.nwalsh.sinclude;

import net.sf.saxon.s9api.XdmNode;

public interface DocumentResolver {
    XdmNode resolveXml(XdmNode base, String uri, String accept, String acceptLanguage);
    XdmNode resolveText(XdmNode base, String uri, String encoding, String accept, String acceptLanguage);
}
