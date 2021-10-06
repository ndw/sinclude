package com.nwalsh.sinclude;

import com.nwalsh.sinclude.exceptions.TextContentException;
import com.nwalsh.sinclude.exceptions.XIncludeIOException;
import com.nwalsh.sinclude.utils.ReceiverUtils;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

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
            if (source == null) {
                String systemId = base.getBaseURI().resolve(uri).toASCIIString();
                return builder.build(new SAXSource(new InputSource(systemId)));
            } else {
                return builder.build(source);
            }
        } catch (TransformerException | SaxonApiException e) {
            throw new XIncludeIOException(uri, e);
        }
    }

    @Override
    public XdmNode resolveText(XdmNode base, String uri, String encoding, String accept, String acceptLanguage) {
        Processor processor = base.getProcessor();
        UnparsedTextURIResolver resolver = processor.getUnderlyingConfiguration().getUnparsedTextURIResolver();
        try {
            Reader reader = resolver.resolve(base.getBaseURI().resolve(uri), encoding, processor.getUnderlyingConfiguration());
            BufferedReader breader = new BufferedReader(reader);
            StringBuilder text = new StringBuilder();
            String line = breader.readLine();
            while (line != null) {
                text.append(line).append("\n");
                line = breader.readLine();
            }
            breader.close();
            reader.close();

            URI baseURI = ReceiverUtils.nodeBaseURI(base);
            if (baseURI != null) {
                baseURI = baseURI.resolve(uri);
            } else {
                try {
                    baseURI = new URI(uri);
                } catch (URISyntaxException e) {
                    throw new XIncludeIOException(e.getMessage(), e);
                }
            }

            XdmDestination destination = ReceiverUtils.makeDestination(baseURI);
            try {
                Receiver receiver = ReceiverUtils.makeReceiver(base, destination);
                receiver.startDocument(0);
                receiver.characters(text.toString(), Loc.NONE, 0);
                receiver.endDocument();
                receiver.close();
                return destination.getXdmNode();
            } catch (XPathException e) {
                throw new TextContentException(e);
            }
        } catch (TransformerException | IOException e) {
            throw new XIncludeIOException(uri, e);
        }
    }
}
