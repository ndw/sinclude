package com.nwalsh.xslt;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.Initializer;

import javax.xml.transform.TransformerException;

public class Register implements Initializer {
    @Override
    public void initialize(Configuration config) throws TransformerException {
        config.registerExtensionFunction(new XIncludeFunction());
    }
}
