package com.nwalsh.sinclude;

import com.nwalsh.sinclude.utils.NamespaceUtils;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.value.QNameValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NamespaceUtilsTest {
    static String version = null;

    @Before
    public void setUp() {
        if (version == null) {
            version = new Processor(false).getSaxonProductVersion();
            System.out.println("Running with Saxon version " + version);
        }
    }

    @Test
    public void fqNameFromParts() {
        FingerprintedQName fqname = NamespaceUtils.fqName("ex", "http://example.com/", "local");
        Assert.assertNotNull(fqname);
    }

    @Test
    public void fqNameFromQName() {
        QName qname = new QName("ex", "http://example.com/", "local");
        FingerprintedQName fqname = NamespaceUtils.fqName(qname);
        Assert.assertNotNull(fqname);
        Assert.assertEquals("ex", fqname.getPrefix());
        Assert.assertEquals("local", fqname.getLocalPart());
    }

    @Test
    public void qnameFromQNameValue() {
        // Sigh, we need reflection to test reflection...
        Constructor<?> qvcon;
        QNameValue value = null;
        try {
            // Saxon 12
            Class<?> uriClass = Class.forName("net.sf.saxon.om.NamespaceUri");
            qvcon = QNameValue.class.getConstructor(String.class, uriClass, String.class);
            Method uriOf = uriClass.getMethod("of", String.class);
            Object uri = uriOf.invoke(null, "http://example.com/");
            value = (QNameValue) qvcon.newInstance("ex", (Object) uri, "local");
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException |
                 InvocationTargetException err1) {
            try {
                // Saxon 10 or 11
                qvcon = QNameValue.class.getConstructor(String.class, String.class, String.class);
                value = (QNameValue) qvcon.newInstance("ex", "http://example.com/", "local");
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException err2) {
                Assert.fail();
            }
        }

        QName qname = NamespaceUtils.qName(value);
        Assert.assertNotNull(qname);
        Assert.assertEquals("ex", qname.getPrefix());
        Assert.assertEquals("local", qname.getLocalName());
    }

}
