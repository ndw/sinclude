package com.nwalsh.sinclude.utils;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;

import javax.xml.XMLConstants;

public class NodeUtils {
    public static final QName xml_id = new QName("xml", XMLConstants.XML_NS_URI, "id");
    public static final QName xml_lang = new QName("xml", XMLConstants.XML_NS_URI, "lang");
    public static final QName xml_base = new QName("xml", XMLConstants.XML_NS_URI, "base");

    public static String getLang(XdmNode node) {
        String lang = null;
        while (lang == null && node.getNodeKind() == XdmNodeKind.ELEMENT) {
            lang = node.getAttributeValue(xml_lang);
            node = node.getParent();
        }
        return lang;
    }
}

