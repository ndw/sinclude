package com.nwalsh.sinclude;

import com.nwalsh.sinclude.schemes.ElementScheme;
import com.nwalsh.sinclude.schemes.SearchScheme;
import com.nwalsh.sinclude.schemes.TextScheme;
import com.nwalsh.sinclude.schemes.XPathScheme;
import com.nwalsh.sinclude.schemes.XmlnsScheme;
import com.nwalsh.sinclude.xpointer.DefaultFragmentIdParser;
import com.nwalsh.sinclude.xpointer.FragmentIdParser;
import com.nwalsh.sinclude.xpointer.Scheme;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.trans.XPathException;

import javax.xml.XMLConstants;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XInclude {
    private static final String NS_XML = "http://www.w3.org/XML/1998/namespace";
    private static final String NS_XINCLUDE = "http://www.w3.org/2001/XInclude";
    private static final QName xi_include = new QName(NS_XINCLUDE, "include");
    private static final QName xi_fallback = new QName(NS_XINCLUDE, "fallback");

    private static final String localAttrNS = "http://www.w3.org/2001/XInclude/local-attributes";

    private static final QName xml_base = new QName("xml", XMLConstants.XML_NS_URI, "base");
    private static final QName xml_lang = new QName("xml", XMLConstants.XML_NS_URI, "lang");
    private static final QName xml_id = new QName("xml", XMLConstants.XML_NS_URI, "id");

    private static final QName _fixup_xml_base = new QName("", "fixup-xml-base");
    private static final QName _fixup_xml_lang = new QName("", "fixup-xml-lang");
    private static final QName _set_xml_id = new QName("", "set-xml-id");
    private static final QName _accept = new QName("", "accept");
    private static final QName _accept_language = new QName("", "accept-language");
    private static final QName _encoding = new QName("", "encoding");
    private static final QName _href = new QName("", "href");
    private static final QName _parse = new QName("", "parse");
    private static final QName _fragid = new QName("", "fragid");
    private static final QName _xpointer = new QName("", "xpointer");

    private static final Pattern linesXptrRE = Pattern.compile("\\s*lines\\s*\\(\\s*(\\d+)\\s*-\\s*(\\d+)\\s*\\)\\s*");
    private static final Pattern lineEqual = Pattern.compile("line\\s*=\\s*\\(.*\\)\\s*");
    private static final Pattern charEqual = Pattern.compile("char\\s*=\\s*\\(.*\\)\\s*");
    private static final Pattern searchEqual = Pattern.compile("search\\s*=\\s*\\(.*\\)\\s*");

    private static final FingerprintedQName fq_xml_id =
            new FingerprintedQName(xml_id.getPrefix(), xml_id.getNamespaceURI(), xml_id.getLocalName());
    private static final FingerprintedQName fq_xml_lang =
            new FingerprintedQName(xml_lang.getPrefix(), xml_lang.getNamespaceURI(), xml_lang.getLocalName());
    private static final FingerprintedQName fq_xml_base =
            new FingerprintedQName(xml_base.getPrefix(), xml_base.getNamespaceURI(), xml_base.getLocalName());

    private boolean trimText = false;
    private Vector<SchemeData> data = new Vector<>();
    private Vector<Scheme> schemes = new Vector<>();
    private DocumentResolver resolver = null;
    private FragmentIdParser fragmentIdParser = null;

    public XInclude() {
        resolver = new DefaultDocumentResolver();
        fragmentIdParser = new DefaultFragmentIdParser(this);
        init();
    }

    public XInclude(DocumentResolver resolver) {
        this.resolver = resolver;
        fragmentIdParser = new DefaultFragmentIdParser(this);
        init();
    }

    private void init() {
        registerScheme(new XmlnsScheme());
        registerScheme(new ElementScheme());
        registerScheme(new XPathScheme());
        registerScheme(new TextScheme());
        registerScheme(new SearchScheme());
    }

    public XInclude newInstance() {
        XInclude include = new XInclude();
        include.resolver = resolver;
        include.fragmentIdParser = fragmentIdParser;
        // not the data
        include.schemes.addAll(schemes);
        return include;
    }

    public void clearSchemes() {
        schemes.clear();
    }

    public void registerScheme(Scheme xpointerScheme) {
        schemes.add(xpointerScheme);
    }

    public Scheme getScheme(String name) {
        for (Scheme scheme : schemes) {
            if (name.equals(scheme.schemeName())) {
                return scheme;
            }
        }
        return null;
    }

    public FragmentIdParser getFragmentIdParser() {
        return fragmentIdParser;
    }

    public boolean getTrimText() {
        return trimText;
    }

    public void setTrimText(boolean trim) {
        trimText = trim;
    }

    public XdmNode expandXIncludes(XdmNode node) throws XPathException {
        TreeWalker walker = new TreeWalker();
        walker.register(xi_include, new XiIncludeHandler());
        walker.register(xi_fallback, new XiFallbackHandler());
        return walker.walk(node);
    }

    private interface ElementHandler {
        public XdmNode process(XdmNode node);
    }

    private class XiIncludeHandler implements ElementHandler {
        public XdmNode process(XdmNode node) {
            String href = node.getAttributeValue(_href);
            String parse = node.getAttributeValue(_parse);
            String xptr = node.getAttributeValue(_xpointer);
            String fragid = node.getAttributeValue(_fragid);
            String setId = node.getAttributeValue(_set_xml_id);
            String accept = node.getAttributeValue(_accept);
            String accept_lang = node.getAttributeValue(_accept_language);

            if (href == null) {
                href = "";
            }

            if (accept != null && accept.matches(".*[^\u0020-\u007e].*")) {
                throw new IllegalArgumentException("Invalid characters in accept value");
            }

            if (accept_lang != null && accept_lang.matches(".*[^\u0020-\u007e].*")) {
                throw new IllegalArgumentException("Invalid characters in accept-language value");
            }

            XdmNode fallback = null;
            XdmSequenceIterator<XdmNode> iter = node.axisIterator(Axis.CHILD);
            while (iter.hasNext()) {
                XdmNode child = iter.next();
                if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
                    if (xi_fallback.equals(child.getNodeName())) {
                        if (fallback != null) {
                            throw new IllegalArgumentException("XInclude element must contain at most one xi:fallback element.");
                        }
                        fallback = child;
                    } else if (NS_XINCLUDE.equals(child.getNodeName().getNamespaceURI())) {
                        throw new IllegalArgumentException("Element not allowed as child of XInclude: " + child.getNodeName());
                    }
                }
            }

            boolean forceFallback = false;

            if (parse == null) {
                parse = "xml";
            }

            if (parse.contains(";")) {
                parse = parse.substring(0, parse.indexOf(";")).trim();
            }

            if ("xml".equals(parse) || "application/xml".equals(parse) || ("text/xml".equals(parse) || parse.endsWith("+xml"))) {
                parse = "xml";
            } else if ("text".equals(parse) || parse.startsWith("text/")) {
                parse = "text";
            } else {
                xptr = null;
                fragid = null;
                forceFallback = true;
            }

            if (xptr != null && fragid != null) {
                if (!xptr.equals(fragid)) {
                    if ("xml".equals(parse)) {
                        // log something
                    } else {
                        // log something
                        xptr = fragid;
                    }
                }
            }

            if (xptr == null && fragid != null) {
                xptr = fragid;
                fragid = null;
            }

            if (xptr != null && fragid != null) {
                if (!xptr.equals(fragid)) {
                    if ("xml".equals(parse)) {
                        System.out.println("XInclude specifies different xpointer/fragid, using xpointer for xml: " + xptr);
                    } else {
                        xptr = fragid;
                        System.out.println("XInclude specifies different xpointer/fragid, using fragid for " + parse + ": " + xptr);
                    }
                }
            }

            if (xptr != null) {
                /* HACK */
                if ("text".equals(parse)) {
                    String xtrim = xptr.trim();
                    Matcher lmatcher = lineEqual.matcher(xtrim);
                    Matcher cmatcher = charEqual.matcher(xtrim);
                    Matcher smatcher = searchEqual.matcher(xtrim);
                    if (lmatcher.find() || cmatcher.find()) {
                        if (lmatcher.find()) {
                            xptr = "text(" + lmatcher.group(1) + ")";
                        } else {
                            xptr = "text(" + cmatcher.group(1) + ")";
                        }
                    } else if (smatcher.find()) {
                        xptr = "search(" + smatcher.group(1) + ")";
                    }
                }
            }

            XdmNode doc = null;
            try {
                if ("text".equals(parse)) {
                    doc = resolver.resolveText(node, href, accept, accept_lang);
                } else {
                    doc = resolver.resolveXml(node, href, accept, accept_lang);
                }
            } catch (Exception e) {
                if (fallback != null) {
                    return processFallback(fallback);
                } else {
                    throw e;
                }
            }

            if (xptr == null) {
                return doc;
            } else {
                Scheme[] pointers = fragmentIdParser.parseFragmentIdentifier(parse, xptr);
                for (Scheme pointer : pointers) {
                    SchemeData[] array = new SchemeData[data.size()];
                    data.toArray(array);
                    try {
                        SelectionResult result = pointer.select(array, doc);
                        Collections.addAll(data, result.getSchemeData());
                        if (result.finished()) {
                            XdmNode xidoc = result.getResult();
                            return xidoc;
                        }
                    } catch (Exception e) {
                        // nop
                    }
                }
                throw new RuntimeException("Failed to find");
            }
        }

        private XdmNode processFallback(XdmNode fallback) {
            XdmSequenceIterator<XdmNode> iter = fallback.axisIterator(Axis.CHILD);

            XdmDestination destination = new XdmDestination();
            PipelineConfiguration pipe = fallback.getUnderlyingNode().getConfiguration().makePipelineConfiguration();
            Receiver receiver = destination.getReceiver(pipe,  new SerializationProperties());
            try {
                receiver.open();
                receiver.startDocument(0);
                while (iter.hasNext()) {
                    XdmNode node = iter.next();
                    if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
                        data.clear();
                        TreeWalker walker = new TreeWalker();
                        walker.traverse(receiver, node);
                    } else {
                        receiver.append(node.getUnderlyingNode());
                    }
                }
                receiver.endDocument();
                receiver.close();
                return destination.getXdmNode();
            } catch (XPathException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class XiFallbackHandler implements ElementHandler {
        public XdmNode process(XdmNode node) {
            throw new UnsupportedOperationException("An xi:fallback element isn't allowed here");
        }
    }

    private class TreeWalker {
        private HashMap<QName,ElementHandler> handlers = new HashMap<>();

        public void register(QName name, ElementHandler handler) {
            if (handlers.containsKey(name)) {
                throw new UnsupportedOperationException("Cannot have multiple handlers for the same element type");
            }
            handlers.put(name, handler);
        }

        public XdmNode walk(XdmNode node) throws XPathException {
            XdmDestination destination = new XdmDestination();
            PipelineConfiguration pipe = node.getUnderlyingNode().getConfiguration().makePipelineConfiguration();
            Receiver receiver = destination.getReceiver(pipe,  new SerializationProperties());

            receiver.open();
            receiver.startDocument(0);
            traverse(receiver, node);
            receiver.endDocument();
            receiver.close();

            return destination.getXdmNode();
        }

        public void traverse(Receiver receiver, XdmNode node) throws XPathException {
            XdmSequenceIterator<XdmNode> iter = null;

            if (node.getNodeKind() == XdmNodeKind.DOCUMENT) {
                iter = node.axisIterator(Axis.CHILD);
                while (iter.hasNext()) {
                    XdmNode item = iter.next();
                    if (item.getNodeKind() == XdmNodeKind.ELEMENT) {
                        traverse(receiver, item);
                    } else {
                        receiver.append(item.getUnderlyingNode());
                    }
                }
            } else if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
                if (handlers.containsKey(node.getNodeName())) {
                    receiver.append(handlers.get(node.getNodeName()).process(node).getUnderlyingNode());
                } else {
                    NodeInfo inode = node.getUnderlyingNode();
                    FingerprintedQName name = new FingerprintedQName(inode.getPrefix(), inode.getURI(), inode.getLocalPart());
                    receiver.startElement(name, inode.getSchemaType(), inode.attributes(), inode.getAllNamespaces(), inode.saveLocation(), 0);
                    iter = node.axisIterator(Axis.CHILD);
                    while (iter.hasNext()) {
                        XdmNode item = iter.next();
                        if (item.getNodeKind() == XdmNodeKind.ELEMENT) {
                            traverse(receiver, item);
                        } else {
                            receiver.append(item.getUnderlyingNode());
                        }
                    }
                    receiver.endElement();
                }
            } else {
                receiver.append(node.getUnderlyingNode());
            }
        }
    }
}
