package com.nwalsh.sinclude;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class DefaultDocumentResolver implements DocumentResolver {
    @Override
    public XdmNode resolveXml(XdmNode base, String uri, String accept, String acceptLanguage) {
        Processor processor = base.getProcessor();
        URIResolver resolver = processor.getUnderlyingConfiguration().getURIResolver();
        DocumentBuilder builder = processor.newDocumentBuilder();
        builder.setDTDValidation(false);
        builder.setLineNumbering(true);
        try {
            Source source = resolver.resolve(uri, base.getBaseURI().toASCIIString());
            return builder.build(source);
        } catch (TransformerException | SaxonApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public XdmNode resolveText(XdmNode base, String uri, String accept, String acceptLanguage) {
        Processor processor = base.getProcessor();
        UnparsedTextURIResolver resolver = processor.getUnderlyingConfiguration().getUnparsedTextURIResolver();
        try {
            Reader reader = resolver.resolve(base.getBaseURI().resolve(uri), "utf-8", processor.getUnderlyingConfiguration());
            BufferedReader breader = new BufferedReader(reader);
            StringBuilder text = new StringBuilder();
            String line = breader.readLine();
            while (line != null) {
                text.append(line).append("\n");
                line = breader.readLine();
            }
            breader.close();
            reader.close();

            XdmDestination destination = new XdmDestination();
            PipelineConfiguration pipe = processor.getUnderlyingConfiguration().makePipelineConfiguration();
            Receiver receiver = destination.getReceiver(pipe,  new SerializationProperties());
            try {
                receiver.open();
                receiver.startDocument(0);
                receiver.characters(text.toString(), Loc.NONE, 0);
                receiver.endDocument();
                receiver.close();
                return destination.getXdmNode();
            } catch (XPathException e) {
                throw new RuntimeException(e);
            }
        } catch (TransformerException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
