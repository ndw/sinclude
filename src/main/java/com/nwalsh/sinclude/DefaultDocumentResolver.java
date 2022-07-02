package com.nwalsh.sinclude;

import com.nwalsh.sinclude.exceptions.TextContentException;
import com.nwalsh.sinclude.exceptions.XIncludeException;
import com.nwalsh.sinclude.exceptions.XIncludeIOException;
import com.nwalsh.sinclude.utils.ReceiverUtils;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

public class DefaultDocumentResolver implements DocumentResolver {
    @Override
    public XdmNode resolveXml(XdmNode base, String uri, String accept, String acceptLanguage) {
        Processor processor = base.getProcessor();

        DocumentBuilder builder = processor.newDocumentBuilder();
        builder.setDTDValidation(false);
        builder.setLineNumbering(true);

        try {
            Configuration underlyingConfig = processor.getUnderlyingConfiguration();
            Source source;
            try {
                Method getResourceResolver = Configuration.class.getMethod("getResourceResolver");
                source = resolveSaxon11(base, uri, underlyingConfig, getResourceResolver);
            } catch (NoSuchMethodException ex) {
                source = resolveSaxon10(base, uri, underlyingConfig);
            }

            if (source == null) {
                String systemId = base.getBaseURI().resolve(uri).toASCIIString();
                return builder.build(new SAXSource(new InputSource(systemId)));
            } else {
                return builder.build(source);
            }
        } catch (SaxonApiException e) {
            throw new XIncludeIOException(uri, e);
        }
    }

    private Source resolveSaxon11(XdmNode base, String uri, Configuration underlyingConfig, Method getResourceResolver) {
        try {
            Object resolver = getResourceResolver.invoke(underlyingConfig);
            Class<?> clazz = Class.forName("net.sf.saxon.lib.ResourceRequest");
            Constructor<?> constructor = clazz.getConstructor();
            Object request = constructor.newInstance();

            Field field = clazz.getDeclaredField("uri");
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            field.set(request, uri);
            field.setAccessible(accessible);


            field = clazz.getDeclaredField("baseUri");
            accessible = field.isAccessible();
            field.setAccessible(true);
            field.set(request, base.getBaseURI().toString());
            field.setAccessible(accessible);

            Method resolve = resolver.getClass().getMethod("resolve", request.getClass());
            return (Source) resolve.invoke(resolver, request);
        } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException
                 | NoSuchFieldException | InstantiationException | NoSuchMethodException ex) {
            throw new XIncludeException("Failed to resolve Saxon 11 resource with reflection");
        }
    }

    private Source resolveSaxon10(XdmNode base, String uri, Configuration underlyingConfig) {
        try {
            Method getURIResolver = Configuration.class.getMethod("getURIResolver");
            Object resolver = getURIResolver.invoke(underlyingConfig);
            Method resolve = resolver.getClass().getMethod("resolve", String.class, String.class);
            return (Source) resolve.invoke(resolver, uri, base.getBaseURI().toASCIIString());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new XIncludeException("Failed to resolve Saxon 10 resource with reflection");
        }
    }

    @Override
    public XdmNode resolveText(XdmNode base, String uri, String encoding, String accept, String acceptLanguage) {
        if (uri == null || "".equals(uri)) {
            return resolveSameDocumentText(base, encoding, accept, acceptLanguage);
        }
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
                ReceiverUtils.handleCharacters(receiver, text.toString());
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

    private XdmNode resolveSameDocumentText(XdmNode base, String encoding, String accept, String acceptLanguage) {
        URI baseURI = base.getBaseURI();

        if (baseURI != null && !"".equals(baseURI.toString())) {
            return resolveText(base, baseURI.toASCIIString(), encoding, accept, acceptLanguage);
        }

        XdmDestination destination = ReceiverUtils.makeDestination((URI) null);
        try {
            Receiver receiver = ReceiverUtils.makeReceiver(base, destination);
            receiver.startDocument(0);
            ReceiverUtils.handleCharacters(receiver, base.toString());
            receiver.endDocument();
            receiver.close();
            return destination.getXdmNode();
        } catch (XPathException e) {
            throw new TextContentException(e);
        }
    }
}
