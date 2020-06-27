package com.nwalsh.sinclude.data;

import com.nwalsh.sinclude.xpointer.SchemeData;

public class XmlnsData implements SchemeData {
    private String prefix = null;
    private String uri = null;

    public XmlnsData(String prefix, String uri) {
        this.prefix = prefix;
        this.uri = uri;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getUri() {
        return uri;
    }
}
