package com.nwalsh.sinclude.xpointer;

import com.nwalsh.sinclude.XInclude;

public interface XmlScheme extends Scheme {
    public Scheme newInstance(String fragid, XInclude xinclude, String contextLanguage, String contextBaseURI);
}

