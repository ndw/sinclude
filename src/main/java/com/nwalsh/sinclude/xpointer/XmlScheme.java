package com.nwalsh.sinclude.xpointer;

public interface XmlScheme extends Scheme {
    public Scheme newInstance(String fragid, boolean fixupBase, boolean fixupLang);
}
