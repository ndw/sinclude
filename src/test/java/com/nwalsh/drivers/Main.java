package com.nwalsh.drivers;

import com.nwalsh.sinclude.utils.ReceiverUtils;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.om.EmptyAttributeMap;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Location;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] argv) throws SaxonApiException {
        Processor processor = new Processor(false);
        String docString = "<doc><p1/>text<p2 xml:base='http://base.com/'><p4/></p2><p3><p4/></p3></doc>";
        DocumentBuilder builder = processor.newDocumentBuilder();
        builder.setBaseURI(URI.create("http://foo.com/"));
        XdmNode document = builder.build(new SAXSource(new InputSource(new ByteArrayInputStream(docString.getBytes(StandardCharsets.UTF_8)))));

        System.err.println("==============");
        System.err.println("document:");
        dump(document);

        XdmSequenceIterator<XdmNode> iter = document.axisIterator(Axis.CHILD);
        XdmNode doc = iter.next();
        iter = doc.axisIterator(Axis.CHILD);


        XdmNode newdoc = null;
        try {
            XdmDestination destination = ReceiverUtils.makeDestination(URI.create("http://example.com/receiver"));
            PipelineConfiguration pipe = processor.getUnderlyingConfiguration().makePipelineConfiguration();
            Receiver receiver = ReceiverUtils.makeReceiver(pipe, destination);

            receiver.startDocument(0);

            Location loc = new Loc("http://example.com/", -1, -1);

            /*
            FingerprintedQName wrapper = new FingerprintedQName("", "", "wrapper2");
            receiver.startElement(wrapper, doc.getUnderlyingNode().getSchemaType(), EmptyAttributeMap.getInstance(),
                    doc.getUnderlyingNode().getAllNamespaces(), loc, 0);
            */

            loc = new Loc("http://bar.com/", -1, -1);
            while (iter.hasNext()) {
                XdmNode node = iter.next();
                if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
                    receiver.append(node.getUnderlyingNode(), loc, 0);
                } else {
                    receiver.append(node.getUnderlyingNode(), loc, 0);
                }
            }

            /*
            receiver.endElement();
             */
            receiver.endDocument();
            receiver.close();
            newdoc = destination.getXdmNode();
        } catch (XPathException e) {
            throw new RuntimeException(e);
        }

        System.err.println("==============");
        System.err.println("newdoc:");
        dump(newdoc);
    }

    private static void dump(XdmNode node) {
        switch (node.getNodeKind()) {
            case ELEMENT:
                System.err.println("E: " + node.getNodeName() + ": " + node.getBaseURI());
                break;
            case TEXT:
                System.err.println("T: " + node.getStringValue() + ": " + node.getBaseURI());
                break;
            case COMMENT:
                System.err.println("C: " + node.getStringValue() + ": " + node.getBaseURI());
                break;
            case PROCESSING_INSTRUCTION:
                System.err.println("P: " + node.getStringValue() + ": " + node.getBaseURI());
                break;
            case DOCUMENT:
                System.err.println("D: " + node.getBaseURI());
                break;
            default:
                throw new RuntimeException("Unexpected node type: " + node);
        }

        XdmSequenceIterator<XdmNode> iter = node.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmNode child = iter.next();
            dump(child);
        }
    }
}
