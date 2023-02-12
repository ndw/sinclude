package com.nwalsh.sinclude.xpointer;

import net.sf.saxon.s9api.QName;

public interface FragmentIdParser {
    public void setProperty(QName property, String value);
    public String getProperty(QName property);
    public Scheme[] parseFragmentIdentifier(ParseType parseType, String fragid);
}
