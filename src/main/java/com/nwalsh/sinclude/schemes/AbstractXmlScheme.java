package com.nwalsh.sinclude.schemes;

import com.nwalsh.sinclude.XInclude;
import com.nwalsh.sinclude.exceptions.FixupException;
import com.nwalsh.sinclude.utils.NamespaceUtils;
import com.nwalsh.sinclude.utils.NodeUtils;
import com.nwalsh.sinclude.utils.ReceiverUtils;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceiverOption;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.om.AttributeInfo;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;

import static com.nwalsh.sinclude.utils.NodeUtils.xml_base;
import static com.nwalsh.sinclude.utils.NodeUtils.xml_lang;

public abstract class AbstractXmlScheme {
    private static final FingerprintedQName fq_xml_lang =
            NamespaceUtils.fqName(xml_lang.getPrefix(), xml_lang.getNamespaceURI(), xml_lang.getLocalName());
    private static final FingerprintedQName fq_xml_base =
            NamespaceUtils.fqName(xml_base.getPrefix(), xml_base.getNamespaceURI(), xml_base.getLocalName());

    protected XInclude xinclude = null;
    protected String contextLanguage = null;
    protected String contextBaseURI = null;

    protected XdmNode fixup(XdmNode node) {
        if (!xinclude.getFixupXmlBase() && !xinclude.getFixupXmlLang()) {
            return node;
        }

        String lang = null;
        if (xinclude.getFixupXmlLang() && node.getAttributeValue(xml_lang) == null) {
            lang = NodeUtils.getLang(node);
            if (lang == null && contextLanguage != null) {
                lang = ""; // Issue #8
            }
        }

        if (node.getNodeKind() != XdmNodeKind.ELEMENT) {
            // This is an internal error and should never happen
            throw new IllegalArgumentException("XInclude scheme fixup can only be applied to elements");
        }

        try {
            XdmDestination destination = ReceiverUtils.makeDestination(node);
            Receiver receiver = ReceiverUtils.makeReceiver(node, destination);
            receiver.startDocument(0);

            AttributeMap attributes = node.getUnderlyingNode().attributes();

            if (xinclude.getFixupXmlBase() && node.getBaseURI() != null) {
                if (contextBaseURI == null || !contextBaseURI.equals(node.getBaseURI().toString())) {
                    AttributeInfo base = new AttributeInfo(fq_xml_base,
                            BuiltInAtomicType.UNTYPED_ATOMIC,
                            node.getBaseURI().toString(),
                            Loc.NONE, ReceiverOption.NONE);
                    attributes = attributes.put(base);
                }
            }

            if (lang != null) {
                AttributeInfo base = new AttributeInfo(fq_xml_lang,
                        BuiltInAtomicType.UNTYPED_ATOMIC,
                        lang,
                        Loc.NONE, ReceiverOption.NONE);
                attributes = attributes.put(base);
            }

            NodeInfo ni = node.getUnderlyingNode();
            FingerprintedQName name = NamespaceUtils.fqName(ni.getPrefix(), ni.getURI(), ni.getLocalPart());
            receiver.startElement(name, ni.getSchemaType(), attributes, ni.getAllNamespaces(), ni.saveLocation(), 0);
            XdmSequenceIterator<XdmNode> citer = node.axisIterator(Axis.CHILD);
            while (citer.hasNext()) {
                receiver.append(citer.next().getUnderlyingNode());
            }
            receiver.endElement();

            receiver.endDocument();
            receiver.close();

            XdmNode document = destination.getXdmNode();
            XdmSequenceIterator<XdmNode> iter = document.axisIterator(Axis.CHILD);
            return iter.next();
        } catch (XPathException e) {
            throw new FixupException(e);
        }
    }
}
