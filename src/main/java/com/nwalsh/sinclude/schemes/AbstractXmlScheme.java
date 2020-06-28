package com.nwalsh.sinclude.schemes;

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
import java.util.HashSet;

public abstract class AbstractXmlScheme {
    public static final QName xml_lang = new QName("xml", XMLConstants.XML_NS_URI, "lang");
    public static final QName xml_base = new QName("xml", XMLConstants.XML_NS_URI, "base");

    protected boolean fixupLang = false;
    protected boolean fixupBase = false;

    private static final FingerprintedQName fq_xml_lang =
            new FingerprintedQName(xml_lang.getPrefix(), xml_lang.getNamespaceURI(), xml_lang.getLocalName());
    private static final FingerprintedQName fq_xml_base =
            new FingerprintedQName(xml_base.getPrefix(), xml_base.getNamespaceURI(), xml_base.getLocalName());

    public String getLang(XdmNode node) {
        String lang = null;
        while (lang == null && node.getNodeKind() == XdmNodeKind.ELEMENT) {
            lang = node.getAttributeValue(xml_lang);
            node = node.getParent();
        }
        return lang;
    }

    protected XdmNode fixup(XdmNode node, boolean fixupBase, String lang) {
        XdmDestination destination = new XdmDestination();
        PipelineConfiguration pipe = node.getUnderlyingNode().getConfiguration().makePipelineConfiguration();
        Receiver receiver = destination.getReceiver(pipe, new SerializationProperties());

        if (node.getNodeKind() != XdmNodeKind.ELEMENT) {
            throw new IllegalArgumentException("You can only fixup elements");
        }

        try {
            receiver.open();
            receiver.startDocument(0);

            AttributeMap attributes = node.getUnderlyingNode().attributes();

            if (fixupBase && node.getBaseURI() != null) {
                AttributeInfo base = new AttributeInfo(fq_xml_base,
                        BuiltInAtomicType.UNTYPED_ATOMIC,
                        node.getBaseURI().toASCIIString(),
                        Loc.NONE, ReceiverOption.NONE);
                attributes = attributes.put(base);
            }

            if (lang != null) {
                AttributeInfo base = new AttributeInfo(fq_xml_lang,
                        BuiltInAtomicType.UNTYPED_ATOMIC,
                        lang,
                        Loc.NONE, ReceiverOption.NONE);
                attributes = attributes.put(base);
            }

            NodeInfo ni = node.getUnderlyingNode();
            FingerprintedQName name = new FingerprintedQName(ni.getPrefix(), ni.getURI(), ni.getLocalPart());
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
            throw new RuntimeException(e);
        }
    }
}
