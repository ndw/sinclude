package com.nwalsh.sinclude.utils;

import com.nwalsh.sinclude.exceptions.XIncludeObjectModelException;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.value.QNameValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// This class uses reflection to handle construction of QNames and FingerprintedQNames in a way
// that's compatible with Saxon 10, 11, or 12

public class NamespaceUtils {
    public static FingerprintedQName fqName(QName qname) {
        Method getns;
        try {
            // Saxon 12
            getns = QName.class.getMethod("getNamespace");
            Object ns = getns.invoke(qname);
            return fqName(qname.getPrefix(), (String) ns, qname.getLocalName());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException err1) {
            try {
                // Saxon 10 or 11
                getns = QName.class.getMethod("getNamespaceURI");
                Object ns = getns.invoke(qname);
                return fqName(qname.getPrefix(), (String) ns, qname.getLocalName());
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException err2) {
                throw new XIncludeObjectModelException("Failed to instantiate QName", err2);
            }
        }
    }

    public static FingerprintedQName fqName(String prefix, String nsuri, String localName) {
        Constructor<?> fqcon;
        try {
            // Saxon 12
            Class<?> uriClass = Class.forName("net.sf.saxon.om.NamespaceUri");
            fqcon = FingerprintedQName.class.getConstructor(String.class, uriClass, String.class);
            Method uriOf = uriClass.getMethod("of", String.class);
            Object uri = uriOf.invoke(null, nsuri);
            return (FingerprintedQName) fqcon.newInstance(prefix, (Object) uri, localName);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException err1) {
            try {
                // Saxon 10 or 11
                fqcon = FingerprintedQName.class.getConstructor(String.class, String.class, String.class);
                return (FingerprintedQName) fqcon.newInstance(prefix, nsuri, localName);
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException err2) {
                throw new XIncludeObjectModelException("Failed to instantiate FingerprintedQName", err2);
            }
        }
    }

    public static QName qName(QNameValue qname) {
        String nsString;
        Object ns = qname.getNamespaceURI();
        nsString = (ns instanceof String) ? (String) ns : ns.toString();
        return new QName(qname.getPrefix(), nsString, qname.getLocalName());
    }
}
