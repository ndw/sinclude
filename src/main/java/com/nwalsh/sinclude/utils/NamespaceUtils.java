package com.nwalsh.sinclude.utils;

import com.nwalsh.sinclude.exceptions.XIncludeObjectModelException;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.NamespaceMap;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.value.QNameValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

// This class uses reflection to handle construction of QNames and FingerprintedQNames in a way
// that's compatible with Saxon 10, 11, or 12

public class NamespaceUtils {
    private static int version = 0;

    private static int getVersion() {
        if (version == 0) {
            try {
                Class<?> uriClass = Class.forName("net.sf.saxon.om.NamespaceUri");
                Method uriOf = uriClass.getMethod("of", String.class);
                uriOf.invoke(null, "http://example.com/");
                version = 12;
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                version = 11;
            }
        }

        return version;
    }

    public static FingerprintedQName fqName(QName qname) {
        Method getns;

        try {
            if (getVersion() == 12) {
                // Saxon 12
                getns = QName.class.getMethod("getNamespace");
                Object ns = getns.invoke(qname);
                return fqName(qname.getPrefix(), (String) ns, qname.getLocalName());
            } else {
                // Saxon 10 or 11
                getns = QName.class.getMethod("getNamespaceURI");
                Object ns = getns.invoke(qname);
                return fqName(qname.getPrefix(), (String) ns, qname.getLocalName());
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException err) {
            throw new XIncludeObjectModelException("Failed to instantiate QName", err);
        }
    }

    public static FingerprintedQName fqName(String prefix, String nsuri, String localName) {
        Constructor<?> fqcon;

        try {
            if (getVersion() == 12) {
                // Saxon 12
                Class<?> uriClass = Class.forName("net.sf.saxon.om.NamespaceUri");
                fqcon = FingerprintedQName.class.getConstructor(String.class, uriClass, String.class);
                Method uriOf = uriClass.getMethod("of", String.class);
                Object uri = uriOf.invoke(null, nsuri);
                return (FingerprintedQName) fqcon.newInstance(prefix, (Object) uri, localName);
            } else {
                // Saxon 10 or 11
                fqcon = FingerprintedQName.class.getConstructor(String.class, String.class, String.class);
                return (FingerprintedQName) fqcon.newInstance(prefix, nsuri, localName);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException err) {
            throw new XIncludeObjectModelException("Failed to instantiate FingerprintedQName", err);
        }
    }

    public static NamespaceMap addNamespace(NamespaceMap map, String nsprefix, String nsuri) {
        try {
            if (getVersion() == 12) {
                // Saxon 12
                Class<?> uriClass = Class.forName("net.sf.saxon.om.NamespaceUri");
                Method uriOf = uriClass.getMethod("of", String.class);
                Object uri = uriOf.invoke(null, nsuri);
                Method put = map.getClass().getMethod("put", String.class, uriClass);
                return (NamespaceMap) put.invoke(map, nsprefix, uri);
            } else {
                // Saxon 10 or 11
                Method put = map.getClass().getMethod("put", String.class, String.class);
                return (NamespaceMap) put.invoke(map, nsprefix, nsuri);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException err) {
            throw new XIncludeObjectModelException("Failed to remove namespace", err);
        }
    }

    public static NamespaceMap removeNamespace(NamespaceMap map, String ns) {
        try {
            if (getVersion() == 12) {
                // Saxon 12
                for (String prefix : map.getPrefixArray()) {
                    Class<?> uriClass = Class.forName("net.sf.saxon.om.NamespaceUri");
                    Method getUri = map.getClass().getMethod("getNamespaceUri", String.class);
                    Object uri = getUri.invoke(map, prefix);
                    if (ns.equals(uri.toString())) {
                        map = map.remove(prefix);
                    }
                }
            } else {
                // Saxon 10 or 11
                for (String prefix : map.getPrefixArray()) {
                    Method getUri = map.getClass().getMethod("getURI", String.class);
                    Object uri = getUri.invoke(map, prefix);
                    if (ns.equals(uri.toString())) {
                        map = map.remove(prefix);
                    }
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException err) {
            throw new XIncludeObjectModelException("Failed to remove namespace", err);
        }

        return map;
    }

    public static QName qName(QNameValue qname) {
        String nsString;

        // getNamespaceURI is deprecated in 12.x, but still exists; this may have
        // to be more complicated in future versions.
        try {
            Method getns = qname.getClass().getMethod("getNamespaceURI");
            Object ns = getns.invoke(qname);
            nsString = (ns instanceof String) ? (String) ns : ns.toString();
        } catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException err) {
            throw new XIncludeObjectModelException("Failed to getNamespaceURI on QNameValue", err);
        }

        return new QName(qname.getPrefix(), nsString, qname.getLocalName());
    }
}
