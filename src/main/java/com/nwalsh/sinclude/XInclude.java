package com.nwalsh.sinclude;

import com.nwalsh.DebuggingLogger;
import com.nwalsh.sinclude.exceptions.XIncludeException;
import com.nwalsh.sinclude.exceptions.XIncludeFallbackException;
import com.nwalsh.sinclude.exceptions.XIncludeLoopException;
import com.nwalsh.sinclude.exceptions.XIncludeNoFragmentException;
import com.nwalsh.sinclude.exceptions.XIncludeSyntaxException;
import com.nwalsh.sinclude.schemes.ElementScheme;
import com.nwalsh.sinclude.schemes.RFC5147Scheme;
import com.nwalsh.sinclude.schemes.SearchScheme;
import com.nwalsh.sinclude.schemes.XPathScheme;
import com.nwalsh.sinclude.schemes.XmlnsScheme;
import com.nwalsh.sinclude.utils.NamespaceUtils;
import com.nwalsh.sinclude.utils.NodeUtils;
import com.nwalsh.sinclude.utils.ReceiverUtils;
import com.nwalsh.sinclude.xpointer.DefaultFragmentIdParser;
import com.nwalsh.sinclude.xpointer.FragmentIdParser;
import com.nwalsh.sinclude.xpointer.ParseType;
import com.nwalsh.sinclude.xpointer.Scheme;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceiverOption;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.om.*;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;

import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nwalsh.sinclude.utils.NodeUtils.xml_base;
import static com.nwalsh.sinclude.utils.NodeUtils.xml_id;
import static com.nwalsh.sinclude.utils.NodeUtils.xml_lang;

public class XInclude {
    private static final URI MAGIC_IMPOSSIBLE_URI = URI.create("https://nwalsh.com/fake/uri/for/text/include.txt");
    private static final String MAGIC_ID_URI = "https://nwalsh.com/ns/xinclude/id";
    private static final String NS_XML = "http://www.w3.org/XML/1998/namespace";
    private static final String NS_XINCLUDE = "http://www.w3.org/2001/XInclude";
    private static final QName xi_include = new QName(NS_XINCLUDE, "include");
    private static final QName xi_fallback = new QName(NS_XINCLUDE, "fallback");
    private static final QName magic_id = new QName(MAGIC_ID_URI, "magic:id");

    private static final String localAttrNS = "http://www.w3.org/2001/XInclude/local-attributes";

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

    private static final FingerprintedQName fq_xml_id = NamespaceUtils.fqName(xml_id);
    private static final FingerprintedQName fq_xml_lang = NamespaceUtils.fqName(xml_lang);
    private static final FingerprintedQName fq_xml_base = NamespaceUtils.fqName(xml_base);
    private static final FingerprintedQName fq_magic_id = NamespaceUtils.fqName(magic_id);

    private DebuggingLogger logger = null;
    private boolean trimText = false;
    private boolean fixupXmlBase = true;
    private boolean fixupXmlLang = true;
    private boolean copyAttributes = true; // XInclude 1.1
    private final Vector<SchemeData> data = new Vector<>();
    private final Vector<Scheme> schemes = new Vector<>();
    private DocumentResolver resolver = null;
    private FragmentIdParser fragmentIdParser = null;
    private final Stack<URI> uriStack = new Stack<>();
    private MagicId magicId = new MagicId();
    private Map<String, Location> magicBaseUriMap = new HashMap<>();

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

    public XInclude(DocumentResolver resolver, FragmentIdParser fragidParser) {
        this.resolver = resolver;
        fragmentIdParser = fragidParser;
        init();
    }

    private void init() {
        registerScheme(new XmlnsScheme());
        registerScheme(new ElementScheme());
        registerScheme(new XPathScheme());
        registerScheme(new RFC5147Scheme());
        registerScheme(new SearchScheme());
    }

    private XInclude newInstance() {
        XInclude include = new XInclude();
        include.logger = logger;
        include.resolver = resolver;
        include.fragmentIdParser = fragmentIdParser;
        // not the data
        include.schemes.addAll(schemes);
        include.fixupXmlBase = fixupXmlBase;
        include.fixupXmlLang = fixupXmlLang;
        include.copyAttributes = copyAttributes;
        include.trimText = trimText;
        include.uriStack.addAll(uriStack);
        include.magicId = magicId;
        include.magicBaseUriMap = magicBaseUriMap;
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

    public boolean getCopyAttributes() {
        return copyAttributes;
    }

    public void setCopyAttributes(boolean copy) {
        copyAttributes = copy;
    }

    public XdmNode expandXIncludes(XdmNode node) throws XPathException {
        logger = new DebuggingLogger(node.getUnderlyingNode().getConfiguration().getLogger());
        XdmNode result = internalExpandXIncludes(node);
        result = remapBaseUris(result);
        return result;
    }

    public void expandXIncludes(File input, File output) throws SaxonApiException, XPathException {
        Processor processor = new Processor(false);
        DocumentBuilder builder = processor.newDocumentBuilder();
        XdmNode node = builder.build(input);

        XdmNode result = internalExpandXIncludes(node);
        result = remapBaseUris(result);

        Serializer serializer = processor.newSerializer(output);
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        serializer.setOutputProperty(Serializer.Property.INDENT, "no");
        serializer.serializeNode(result);
        serializer.close();
    }

    public XdmNode internalExpandXIncludes(XdmNode node) throws XPathException {
        logger = new DebuggingLogger(node.getUnderlyingNode().getConfiguration().getLogger());
        TreeWalker walker = new TreeWalker();
        walker.register(xi_include, new XiIncludeHandler(this));
        walker.register(xi_fallback, new XiFallbackHandler());
        XdmNode result = walker.walk(node);
        return result;
    }

    private XdmNode remapBaseUris(XdmNode node) throws XPathException {
        if (fixupXmlBase) {
            return node;
        }

        XdmDestination destination = new XdmDestination();
        Receiver receiver = ReceiverUtils.makeReceiver(node, destination);
        receiver.startDocument(0);
        remapTraversal(receiver, node, new XInclude.IncludeLocation(node));
        receiver.endDocument();
        receiver.close();
        return destination.getXdmNode();
    }

    private void remapTraversal(Receiver receiver, XdmNode node, Location location) throws XPathException {
        XdmSequenceIterator<XdmNode> iter = null;

        if (node.getNodeKind() == XdmNodeKind.DOCUMENT) {
            iter = node.axisIterator(Axis.CHILD);
            while (iter.hasNext()) {
                XdmNode item = iter.next();
                if (item.getNodeKind() == XdmNodeKind.ELEMENT) {
                    remapTraversal(receiver, item, location);
                } else {
                    receiver.append(item.getUnderlyingNode());
                }
            }
        } else if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
            String id = node.getAttributeValue(magic_id);
            Location loc;

            if (node.getAttributeValue(xml_base) != null) {
                loc = new XInclude.IncludeLocation(node, node.getBaseURI().toString());
            } else if (magicBaseUriMap.containsKey(id)) {
                loc = magicBaseUriMap.get(id);
            } else {
                loc = new XInclude.IncludeLocation(node, location.getSystemId());
            }

            NodeInfo inode = node.getUnderlyingNode();
            FingerprintedQName name = NamespaceUtils.fqName(inode.getPrefix(), inode.getURI(), inode.getLocalPart());

            AttributeMap amap = inode.attributes();
            amap = amap.remove(fq_magic_id);

            NamespaceMap nmap =  NamespaceUtils.removeNamespace(inode.getAllNamespaces(), MAGIC_ID_URI);

            receiver.startElement(name, inode.getSchemaType(), amap, nmap, loc, 0);
            iter = node.axisIterator(Axis.CHILD);
            while (iter.hasNext()) {
                XdmNode item = iter.next();
                if (item.getNodeKind() == XdmNodeKind.ELEMENT) {
                    remapTraversal(receiver, item, loc);
                } else {
                    receiver.append(item.getUnderlyingNode());
                }
            }
            receiver.endElement();
        } else {
            receiver.append(node.getUnderlyingNode());
        }
    }

    private interface ElementHandler {
        XdmNode process(XdmNode node) throws XPathException;
    }

    private class XiIncludeHandler implements ElementHandler {
        private XInclude xinclude = null;

        private XiIncludeHandler() {
            // no.
        }

        public XiIncludeHandler(XInclude include) {
            xinclude = include;
        }

        public XdmNode process(XdmNode node) throws XPathException {
            String href = node.getAttributeValue(_href);
            String xptr = node.getAttributeValue(_xpointer);
            String fragid = node.getAttributeValue(_fragid);
            String setId = node.getAttributeValue(_set_xml_id);
            String encoding = node.getAttributeValue(_encoding);
            String accept = node.getAttributeValue(_accept);
            String accept_lang = node.getAttributeValue(_accept_language);
            String parseAttr = node.getAttributeValue(_parse);

            if (href == null) {
                href = "";
            } else {
                href = href.trim();
            }

            if (encoding == null) {
                encoding = "UTF-8";
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

            if (parseAttr == null) {
                parseAttr = "xml";
            }

            if (parseAttr.contains(";")) {
                parseAttr = parseAttr.substring(0, parseAttr.indexOf(";")).trim();
            }

            final ParseType parse;
            if ("xml".equals(parseAttr) || "application/xml".equals(parseAttr) || ("text/xml".equals(parseAttr) || parseAttr.endsWith("+xml"))) {
                parse = ParseType.XMLPARSE;
            } else if ("text".equals(parseAttr) || parseAttr.startsWith("text/")) {
                parse = ParseType.TEXTPARSE;
            } else {
                // Unrecognized parse type; fallback will be forced
                parse = ParseType.NOPARSE;
                xptr = null;
                fragid = null;
            }

            if (xptr != null && fragid != null && !xptr.equals(fragid)) {
                if (parse == ParseType.XMLPARSE) {
                    if (logger != null) {
                        logger.debug(DebuggingLogger.XINCLUDE, "XInclude specifies different xpointer/fragid, using xpointer for xml: " + xptr);
                    }
                } else {
                    xptr = fragid;
                    if (logger != null) {
                        logger.debug(DebuggingLogger.XINCLUDE, "XInclude specifies different xpointer/fragid, using fragid for text: " + xptr);
                    }
                }
            }

            if (xptr == null && fragid != null) {
                xptr = fragid;
            }

            if (xptr != null && parse == ParseType.TEXTPARSE) {
                /* HACK */
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

            if (parse == ParseType.NOPARSE) {
                if (fallback != null) {
                    return processFallback(fallback);
                } else {
                    throw new XIncludeFallbackException("Fallback forced (invalid parse attribute) but no xi:fallback provided");
                }
            }

            HashSet<XdmNode> ancestors = null;
            XdmNode doc = null;
            try {
                if (href.isEmpty()) {
                    if (logger != null) {
                        logger.debug(DebuggingLogger.XINCLUDE, "XInclude same document");
                    }

                    ancestors = new HashSet<>();
                    XdmNode parent = node;
                    while (parent.getParent() != null) {
                        ancestors.add(parent);
                        parent = parent.getParent();
                    }
                    ancestors.add(parent);
                    doc = parent;
                    if (parse == ParseType.TEXTPARSE) {
                        doc = resolver.resolveText(doc, "", encoding, accept, accept_lang);
                    }
                } else {
                    if (logger != null) {
                        logger.debug(DebuggingLogger.XINCLUDE, "XInclude parse: " + href);
                    }
                    URI next = node.getBaseURI().resolve(href);
                    if (parse == ParseType.TEXTPARSE) {
                        uriStack.push(MAGIC_IMPOSSIBLE_URI);
                        doc = resolver.resolveText(node, href, encoding, accept, accept_lang);
                    } else {
                        if (uriStack.contains(next)) {
                            throw new XIncludeLoopException("XInclude loops: " + next.toASCIIString());
                        }
                        uriStack.push(next);
                        doc = resolver.resolveXml(node, href, accept, accept_lang);
                    }
                }

                XInclude nested = xinclude.newInstance();
                if (href.isEmpty()) {
                    if (xptr == null && parse == ParseType.XMLPARSE) {
                        throw new XIncludeLoopException("Recursive same document reference");
                    }
                } else {
                    doc = fixup(node, doc, setId);
                    doc = nested.internalExpandXIncludes(doc);
                    uriStack.pop();
                }

                if (xptr != null) {
                    Exception lastException = null;
                    boolean success = false;

                    fragmentIdParser.setProperty(xml_base, node.getParent().getBaseURI().toString());
                    fragmentIdParser.setProperty(xml_lang, NodeUtils.getLang(node.getParent()));

                    Scheme[] pointers = fragmentIdParser.parseFragmentIdentifier(parse, xptr);
                    for (Scheme pointer : pointers) {
                        if (!success) {
                            try {
                                SchemeData[] array = new SchemeData[data.size()];
                                data.toArray(array);
                                SelectionResult result = pointer.select(array, doc);

                                if (href.isEmpty()) {
                                    for (XdmNode selected : result.getSelectedNodes()) {
                                        if (ancestors.contains(selected)) {
                                            throw new XIncludeLoopException("XInclude same-document reference to ancestor forms a loop");
                                        }
                                    }
                                }

                                Collections.addAll(data, result.getSchemeData());
                                if (result.finished()) {
                                    XdmNode xidoc = result.getResult();
                                    if (xidoc != null) {
                                        doc = xidoc;
                                        success = true;
                                    }
                                }
                            } catch (Exception e) {
                                lastException = e;
                            }
                        }
                    }

                    fragmentIdParser.setProperty(xml_base, null);
                    fragmentIdParser.setProperty(xml_lang, null);

                    if (!success) {
                        if (lastException != null) {
                            throw new XIncludeNoFragmentException("Failed to locate fragment: " + xptr + " (" + lastException.getMessage() + ")", lastException);
                        }
                        throw new XIncludeNoFragmentException("Failed to locate fragment: " + xptr);
                    }
                    doc = fixup(node, doc, setId);
                    doc = nested.internalExpandXIncludes(doc);
                }
            } catch (Exception e) {
                if (fallback != null) {
                    doc = processFallback(fallback);

                    XInclude nested = xinclude.newInstance();
                    doc = fixup(node, doc, setId);
                    doc = nested.internalExpandXIncludes(doc);

                } else {
                    throw e;
                }
            }

            // If we did a text parse and trim text is true, strip leading and trailing
            // spaces off each line. All trailing spaces are stripped, the number of leading
            // spaces is determined by the number of spaces on the first line.
            if (getTrimText() && parse == ParseType.TEXTPARSE) {
                String[] lines = doc.getStringValue().split("\n", -1); // -1 == include empty trailing strings

                if (lines.length > 0) {
                    int trimleading = -1;
                    for (String line : lines) {
                        // (Effectively) blank lines don't count
                        if (!line.trim().isEmpty()) {
                            int leading = 0;
                            while (leading < line.length() && line.charAt(leading) == ' ') {
                                leading++;
                            }
                            if (trimleading < 0 || leading < trimleading) {
                                trimleading = leading;
                            }
                            if (trimleading == 0) {
                                break;
                            }
                        }
                    }

                    XdmDestination destination = new XdmDestination();
                    Receiver receiver = ReceiverUtils.makeReceiver(doc, destination);
                    for (int pos = 0; pos < lines.length; pos++) {
                        if (!lines[pos].isEmpty()) {
                            int first = 0;
                            while (first < trimleading && first < lines[pos].length() && lines[pos].charAt(first) == ' ') {
                                first++;
                            }
                            if (first < lines[pos].length()) {
                                int last = lines[pos].length()-1;
                                while (last > first && lines[pos].charAt(last) == ' ') {
                                    last--;
                                }
                                // There must be at least one non-space character on the line, so
                                // this is always going to be a legal substring
                                ReceiverUtils.handleCharacters(receiver, lines[pos].substring(first, last+1));
                            }
                        }
                        // Only output newlines between lines, not after the last line
                        if (pos+1 < lines.length) {
                            ReceiverUtils.handleCharacters(receiver, "\n");
                        }
                    }
                    receiver.close();
                    doc = destination.getXdmNode();
                }
            }

            return doc;
        }

        private XdmNode fixup(XdmNode xinclude, XdmNode document, String setId) {
            // Fixup is usually handled by the fragid processor.

            // It's only ever true here if we're XIncluding a whole document.
            if (document.getNodeKind() != XdmNodeKind.DOCUMENT) {
                // This is an internal error and should never happen
                throw new IllegalArgumentException("XInclude fixup can only be called on a document");
            }

            String contextLanguage = NodeUtils.getLang(xinclude.getParent());
            String contextBaseURI = NodeUtils.getLang(xinclude.getParent());

            try {
                XdmDestination destination = new XdmDestination();
                Receiver receiver = ReceiverUtils.makeReceiver(document, destination);
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
                                if (!setId.isEmpty()) {
                                    // If we have an EE processor, this should probably be of type ID.
                                    amap = amap.put(new AttributeInfo(fq_xml_id, BuiltInAtomicType.UNTYPED_ATOMIC, setId, null, ReceiverOption.NONE));
                                }
                            }

                            for (AttributeInfo ainfo : xinclude.getUnderlyingNode().attributes()) {
                                // Attribute must be in a namespace
                                String nsuri = ainfo.getNodeName().getURI();
                                boolean copy = (nsuri != null && !nsuri.isEmpty());

                                // But not in the XML namespace
                                copy = copy && !NS_XML.equals(nsuri);

                                if (copy) {
                                    NodeName aname = ainfo.getNodeName();
                                    if (localAttrNS.equals(aname.getURI())) {
                                        aname = NamespaceUtils.fqName("", "", aname.getLocalPart());
                                    }

                                    copied.add(aname);
                                    amap = amap.put(new AttributeInfo(aname, ainfo.getType(), ainfo.getValue(), ainfo.getLocation(), ReceiverOption.NONE));
                                }
                            }
                        }

                        if (getFixupXmlBase() && node.getBaseURI() != null) {
                            if (contextBaseURI == null || !contextBaseURI.equals(node.getBaseURI().toString())) {
                                AttributeInfo base = new AttributeInfo(fq_xml_base,
                                        BuiltInAtomicType.UNTYPED_ATOMIC,
                                        node.getBaseURI().toString(),
                                        Loc.NONE, ReceiverOption.NONE);
                                amap = amap.put(base);
                            }
                        }

                        if (getFixupXmlLang()) {
                            String lang = NodeUtils.getLang(node);
                            if (lang == null && contextLanguage != null) {
                                lang = "";
                            }
                            if (lang != null) {
                                AttributeInfo xml_lang = new AttributeInfo(fq_xml_lang,
                                        BuiltInAtomicType.UNTYPED_ATOMIC,
                                        lang,
                                        Loc.NONE, ReceiverOption.NONE);
                                amap = amap.put(xml_lang);
                            }
                        }

                        for (AttributeInfo ainfo : attributes) {
                            if (!copied.contains(ainfo.getNodeName())) {
                                copied.add(ainfo.getNodeName());
                                amap = amap.put(ainfo);
                            }
                        }

                        NodeInfo ni = node.getUnderlyingNode();
                        FingerprintedQName name = NamespaceUtils.fqName(ni.getPrefix(), ni.getURI(), ni.getLocalPart());
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
                // I don't actually think this can happen...
                throw new XIncludeException(e);
            }
        }

        private XdmNode processFallback(XdmNode fallback) {
            XdmSequenceIterator<XdmNode> iter = fallback.axisIterator(Axis.CHILD);

            try {
                XdmDestination destination = new XdmDestination();
                Receiver receiver = ReceiverUtils.makeReceiver(fallback, destination);
                receiver.startDocument(0);
                while (iter.hasNext()) {
                    XdmNode node = iter.next();
                    if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
                        data.clear();
                        TreeWalker walker = new TreeWalker();
                        walker.traverse(receiver, node, true);
                    } else {
                        receiver.append(node.getUnderlyingNode());
                    }
                }
                receiver.endDocument();
                receiver.close();
                return destination.getXdmNode();
            } catch (XPathException e) {
                // I don't think this can actually happen...
                throw new XIncludeException(e);
            }
        }
    }

    private static class XiFallbackHandler implements ElementHandler {
        public XdmNode process(XdmNode node) {
            throw new XIncludeSyntaxException("An xi:fallback element isn't allowed here");
        }
    }

    private class TreeWalker {
        private HashMap<QName,ElementHandler> handlers = new HashMap<>();
        private URI overrideBaseURI = null;
        private boolean root = true;

        public void register(QName name, ElementHandler handler) {
            if (handlers.containsKey(name)) {
                throw new UnsupportedOperationException("Cannot have multiple handlers for the same element type");
            }
            handlers.put(name, handler);
        }

        public void setXmlBase(URI base) {
            overrideBaseURI = base;
        }

        public XdmNode walk(XdmNode node) throws XPathException {
            XdmDestination destination = new XdmDestination();
            Receiver receiver = ReceiverUtils.makeReceiver(node, destination);
            receiver.startDocument(0);
            traverse(receiver, node, true);
            receiver.endDocument();
            receiver.close();
            XdmNode result = destination.getXdmNode();
            return result;
        }

        private void traverse(Receiver receiver, XdmNode node, boolean addMagicId) throws XPathException {
            XdmSequenceIterator<XdmNode> iter = null;

            if (node.getNodeKind() == XdmNodeKind.DOCUMENT) {
                iter = node.axisIterator(Axis.CHILD);
                while (iter.hasNext()) {
                    XdmNode item = iter.next();
                    if (item.getNodeKind() == XdmNodeKind.ELEMENT) {
                        traverse(receiver, item, addMagicId);
                    } else {
                        receiver.append(item.getUnderlyingNode());
                    }
                }
            } else if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
                if (handlers.containsKey(node.getNodeName())) {
                    XdmNode handled = handlers.get(node.getNodeName()).process(node);
                    Location loc = new IncludeLocation(handled.getBaseURI().toString(), handled.getLineNumber(), handled.getColumnNumber());
                    if (!fixupXmlBase) {
                        for (XdmNode child : handled.children()) {
                            if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
                                String id = child.getAttributeValue(magic_id);
                                magicBaseUriMap.put(id, loc);
                            }
                        }
                    }
                    receiver.append(handled.getUnderlyingNode(), loc, ReceiverOption.ALL_NAMESPACES);
                } else {
                    root = false;
                    NodeInfo inode = node.getUnderlyingNode();
                    FingerprintedQName name = NamespaceUtils.fqName(inode.getPrefix(), inode.getURI(), inode.getLocalPart());

                    AttributeMap amap;
                    if (root && fixupXmlBase && overrideBaseURI != null && inode.getAttributeValue(NS_XML, "base") == null) {
                        FingerprintedQName xml_base = NamespaceUtils.fqName("xml", NS_XML, "base");
                        AttributeInfo base = new AttributeInfo(xml_base, BuiltInAtomicType.ANY_URI, overrideBaseURI.toString(), inode.saveLocation(), 0);
                        amap = inode.attributes().put(base);
                    } else {
                        amap = inode.attributes();
                    }

                    NamespaceMap nmap = inode.getAllNamespaces();

                    if (!fixupXmlBase) {
                        String mprefix = "xi_magic";
                        int mcount = 1;
                        boolean found = true;
                        while (found) {
                            found = false;
                            for (String prefix : nmap.getPrefixArray()) {
                                if (mprefix.equals(prefix)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (found) {
                                mprefix = "xi_magic_" + mcount++;
                            }
                        }

                        if (addMagicId) {
                            String nextId = String.valueOf(magicId.nextId());
                            FingerprintedQName element_id = NamespaceUtils.fqName(mprefix, MAGIC_ID_URI, "id");
                            amap = amap.put(new AttributeInfo(element_id, BuiltInAtomicType.UNTYPED_ATOMIC, nextId, inode.saveLocation(), 0));
                            nmap = NamespaceUtils.addNamespace(nmap, element_id.getPrefix(), MAGIC_ID_URI);
                        }
                    }

                    receiver.startElement(name, inode.getSchemaType(), amap, nmap, inode.saveLocation(), 0);
                    iter = node.axisIterator(Axis.CHILD);
                    while (iter.hasNext()) {
                        XdmNode item = iter.next();
                        if (item.getNodeKind() == XdmNodeKind.ELEMENT) {
                            traverse(receiver, item, false);
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

    private static class IncludeLocation implements Location {
        private final String systemId;
        private final int lineNumber;
        private final int columnNumber;

        IncludeLocation(String systemId, int lineNumber, int columnNumber) {
            this.systemId = systemId;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
        }

        IncludeLocation(XdmNode node) {
            systemId = node.getUnderlyingNode().getSystemId();
            lineNumber = node.getUnderlyingNode().getLineNumber();
            columnNumber = node.getUnderlyingNode().getColumnNumber();
        }

        IncludeLocation(XdmNode node, String overrideSystemId) {
            systemId = overrideSystemId;
            lineNumber = node.getUnderlyingNode().getLineNumber();
            columnNumber = node.getUnderlyingNode().getColumnNumber();
        }

        IncludeLocation(Location location) {
            systemId = location.getSystemId();
            lineNumber = location.getLineNumber();
            columnNumber = location.getColumnNumber();
        }

        @Override
        public String getSystemId() {
            return systemId;
        }

        @Override
        public String getPublicId() {
            return "";
        }

        @Override
        public int getLineNumber() {
            return lineNumber;
        }

        @Override
        public int getColumnNumber() {
            return columnNumber;
        }

        @Override
        public Location saveLocation() {
            return new IncludeLocation(systemId, lineNumber, columnNumber);
        }
    }

    private static class MagicId {
        private int id = 0;
        public int nextId() {
            return id++;
        }
    }
}
