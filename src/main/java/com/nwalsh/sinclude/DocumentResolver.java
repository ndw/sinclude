package com.nwalsh.sinclude;

import net.sf.saxon.s9api.XdmNode;

import java.net.URI;

public interface DocumentResolver {
    public XdmNode resolveXml(XdmNode base, String uri, String accept, String acceptLanguage);
    public XdmNode resolveText(XdmNode base, String uri, String encoding, String accept, String acceptLanguage);
}
