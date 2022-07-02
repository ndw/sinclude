package com.nwalsh.xslt;

import com.nwalsh.DebuggingLogger;
import com.nwalsh.sinclude.XInclude;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.QNameValue;
import net.sf.saxon.value.SequenceType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class XIncludeFunction extends ExtensionFunctionDefinition {
    private static final StructuredQName qName =
            new StructuredQName("", "http://nwalsh.com/xslt", "xinclude");

    private static final QName _fixup_xml_base = new QName("", "fixup-xml-base");
    private static final QName _fixup_xml_lang = new QName("", "fixup-xml-lang");
    private static final QName _trim_text = new QName("", "trim-text");

    HashMap<QName,String> options = new HashMap<>();

    @Override
    public StructuredQName getFunctionQName() {
        return qName;
    }

    @Override
    public int getMinimumNumberOfArguments() {
        return 1;
    }

    @Override
    public int getMaximumNumberOfArguments() {
        return 2;
    }

    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.SINGLE_NODE, SequenceType.OPTIONAL_ITEM};
    }

    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_ITEM;
    }

    public ExtensionFunctionCall makeCallExpression() {
        return new XIncludeFunction.PropertiesCall();
    }

    private class PropertiesCall extends ExtensionFunctionCall {
        DebuggingLogger logger = null;

        public Sequence call(XPathContext xpathContext, Sequence[] sequences) throws XPathException {
            logger = new DebuggingLogger(xpathContext.getConfiguration().getLogger());
            NodeInfo source = (NodeInfo) sequences[0].head();

            if (sequences.length > 1) {
                Item item = sequences[1].head();
                if (item != null) {
                    if (item instanceof MapItem) {
                        options = parseMap((MapItem) item);
                    } else {
                        throw new IllegalArgumentException("ext:xinclude options parameter must be a map");
                    }
                }
            }

            boolean fixupBase = getBooleanOption(_fixup_xml_base, true);
            boolean fixupLang = getBooleanOption(_fixup_xml_lang, true);

            boolean defaultTrimText = false;
            boolean trimText = defaultTrimText;
            if (getBooleanOption(_trim_text, false)) {
                trimText = true;
            }

            XdmNode doc = new XdmNode(source);
            XInclude xinclude = new XInclude();
            xinclude.setFixupXmlBase(fixupBase);
            xinclude.setFixupXmlLang(fixupLang);
            xinclude.setTrimText(trimText);
            return xinclude.expandXIncludes(doc).getUnderlyingNode();
        }
    }

    private boolean getBooleanOption(QName name, boolean defvalue) {
        if (options.containsKey(name)) {
            String value = options.get(name);
            if ("true".equals(value) || "false".equals(value)) {
                return "true".equals(value);
            }
            if ("1".equals(value) || "0".equals(value)) {
                return "1".equals(value);
            }
            if ("yes".equals(value) || "no".equals(value)) {
                return "yes".equals(value);
            }
            throw new IllegalArgumentException("Boolean option " + name + " cannot be " + value);
        }
        return defvalue;
    }

    private HashMap<QName,String> parseMap(MapItem item) throws XPathException {
        HashMap<QName,String> options = new HashMap<>();

        // The implementation of the keyValuePairs() method is incompatible between Saxon 10 and Saxon 11.
        // In order to avoid having to publish two versions of this class, we use reflection to
        // work it out at runtime. (Insert programmer barfing on his shoes emoji here.)
        try {
            Method keys = MapItem.class.getMethod("keys");
            Method get = MapItem.class.getMethod("get", AtomicValue.class);
            AtomicIterator aiter = (AtomicIterator) keys.invoke(item);
            AtomicValue next = aiter.next();
            while (next != null) {
                final QName key;
                if (next.getItemType() == BuiltInAtomicType.QNAME) {
                    QNameValue qkey = (QNameValue) next;
                    key = new QName(qkey.getPrefix(), qkey.getNamespaceURI(), qkey.getLocalName());
                } else {
                    throw new IllegalArgumentException("Option map keys must be QNames");
                }

                AtomicValue value = (AtomicValue) get.invoke(item, next);
                options.put(key, value.getStringValue());
                next = aiter.next();
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalArgumentException("Failed to resolve MapItem with reflection");
        }

        return options;
    }
}
