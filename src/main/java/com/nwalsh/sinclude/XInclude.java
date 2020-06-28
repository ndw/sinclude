package com.nwalsh.sinclude;

import com.nwalsh.sinclude.schemes.ElementScheme;
import com.nwalsh.sinclude.schemes.SearchScheme;
import com.nwalsh.sinclude.schemes.RFC5147;
import com.nwalsh.sinclude.schemes.XPathScheme;
import com.nwalsh.sinclude.schemes.XmlnsScheme;
import com.nwalsh.sinclude.xpointer.DefaultFragmentIdParser;
import com.nwalsh.sinclude.xpointer.FragmentIdParser;
import com.nwalsh.sinclude.xpointer.Scheme;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceiverOption;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.om.AttributeInfo;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.EmptyAttributeMap;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;

import javax.xml.XMLConstants;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    private static final QName _set_xml_id = new QName("", "set-xml-id");
    private static final QName _accept = new QName("", "accept");
    private static final QName _accept_language = new QName("", "accept-language");
    private static final QName _encoding = new QName("", "encoding");
    private static final QName _href = new QName("", "href");
    private static final QName _parse = new QName("", "parse");
    private static final QName _fragid = new QName("", "fragid");
    private static final QName _xpointer = new QName("", "xpointer");

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
    private boolean fixupXmlBase = true;
    private boolean fixupXmlLang = true;
    private boolean copyAttributes = true; // XInclude 1.1
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
        registerScheme(new RFC5147());
        registerScheme(new SearchScheme());
    }

    public XInclude newInstance() {
        XInclude include = new XInclude();
        include.resolver = resolver;
        include.fragmentIdParser = fragmentIdParser;
        // not the data
        include.schemes.addAll(schemes);
        include.fixupXmlBase = fixupXmlBase;
        include.fixupXmlLang = fixupXmlLang;
        include.copyAttributes = copyAttributes;
        include.trimText = trimText;
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

    public boolean getFixupXmlBase() {
        return fixupXmlBase;
    }

    public void setFixupXmlBase(boolean fixup) {
        fixupXmlBase = fixup;
    }

    public boolean getFixupXmlLang() {
        return fixupXmlLang;
    }

    public void setFixupXmlLang(boolean fixup) {
        fixupXmlLang = fixup;
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

            if (forceFallback) {
                if (fallback != null) {
                    return processFallback(fallback);
                } else {
                    throw new RuntimeException("Fallback forced but no xi:fallback provided");
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

            /*
            System.err.println("ORIGINAL DOC:");
            TreeDumper.dump(doc);
             */

            if (xptr == null) {
                return fixup(doc, setId, fixupXmlBase);
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
                            /*
                            System.err.println("XINCLUDED DOC:");
                            TreeDumper.dump(xidoc);
                             */
                            XdmNode fixed = fixup(xidoc, setId, false);
                            /*
                            System.err.println("FIXED DOC:");
                            TreeDumper.dump(fixed);
                             */
                            return fixed;
                        }
                    } catch (Exception e) {
                        // nop
                    }
                }
                throw new RuntimeException("Failed to find");
            }
        }

        private XdmNode fixup(XdmNode document, String setId, boolean fixupBase) {
            // Fixing up xml:base is usually handled by the fragid processor.
            // It's only ever true here if we're XIncluding a whole document.
            // Consequently, fixupLang never applies here.

            if (document == null) {
                return document;
            }

            if (document.getNodeKind() != XdmNodeKind.DOCUMENT) {
                throw new IllegalArgumentException("Fixup can only be called on a document");
            }

            XdmDestination destination = new XdmDestination();
            PipelineConfiguration pipe = document.getUnderlyingNode().getConfiguration().makePipelineConfiguration();
            Receiver receiver = destination.getReceiver(pipe, new SerializationProperties());

            try {
                receiver.open();
                receiver.startDocument(0);

                // Note: node is a document.
                XdmSequenceIterator<XdmNode> iter = document.axisIterator(Axis.CHILD);
                while (iter.hasNext()) {
                    XdmNode node = iter.next();

                    if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
                        HashSet<NodeName> copied = new HashSet<>();
                        AttributeMap amap = EmptyAttributeMap.getInstance();
                        AttributeMap attributes = node.getUnderlyingNode().attributes();

                        if (copyAttributes) {
                            // Handle set-xml-id; it suppresses copying the xml:id attribute and optionally
                            // provides a value for it. (The value "" removes the xml:id.)
                            if (setId != null) {
                                copied.add(fq_xml_id);
                                if (!"".equals(setId)) {
                                    // If we have an EE processor, this should probably be of type ID.
                                    amap = amap.put(new AttributeInfo(fq_xml_id, BuiltInAtomicType.UNTYPED_ATOMIC, setId, null, ReceiverOption.NONE));
                                }
                            }

                            for (AttributeInfo ainfo : node.getUnderlyingNode().attributes()) {
                                // Attribute must be in a namespace
                                String nsuri = ainfo.getNodeName().getURI();
                                boolean copy = (nsuri != null && !"".equals(nsuri));

                                // But not in the XML namespace
                                copy = copy && !NS_XML.equals(nsuri);

                                if (copy) {
                                    NodeName aname = ainfo.getNodeName();
                                    if (localAttrNS.equals(aname.getURI())) {
                                        aname = new FingerprintedQName("", "", aname.getLocalPart());
                                    }

                                    copied.add(aname);
                                    amap = amap.put(new AttributeInfo(aname, ainfo.getType(), ainfo.getValue(), ainfo.getLocation(), ReceiverOption.NONE));
                                }
                            }
                        }

                        if (fixupBase) {
                            AttributeInfo base = new AttributeInfo(fq_xml_base,
                                    BuiltInAtomicType.UNTYPED_ATOMIC,
                                    node.getBaseURI().toASCIIString(),
                                    Loc.NONE, ReceiverOption.NONE);
                            amap = amap.put(base);
                        }

                        for (AttributeInfo ainfo : attributes) {
                            if (!copied.contains(ainfo.getNodeName())) {
                                copied.add(ainfo.getNodeName());
                                amap = amap.put(ainfo);
                            }
                        }

                        NodeInfo ni = node.getUnderlyingNode();
                        FingerprintedQName name = new FingerprintedQName(ni.getPrefix(), ni.getURI(), ni.getLocalPart());
                        receiver.startElement(name, ni.getSchemaType(), amap, ni.getAllNamespaces(), ni.saveLocation(), 0);
                        XdmSequenceIterator<XdmNode> citer = node.axisIterator(Axis.CHILD);
                        while (citer.hasNext()) {
                            receiver.append(citer.next().getUnderlyingNode());
                        }
                        receiver.endElement();
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
