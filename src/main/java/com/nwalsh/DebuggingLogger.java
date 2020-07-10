package com.nwalsh;

import net.sf.saxon.lib.Logger;
import net.sf.saxon.lib.StandardLogger;

import java.util.HashSet;

public class DebuggingLogger extends StandardLogger {
    public static final String REGISTRATION = "registration";
    public static final String XINCLUDE = "xinclude";

    private static final String propertyName = "com.nwalsh.sinclude.verbose";
    private static final String docBookPropertyName = "org.docbook.extensions.xslt.verbose";
    private Logger logger = null;
    private boolean noisy = false;
    private HashSet<String> flags = new HashSet<> ();

    public DebuggingLogger(Logger logger) {
        this.logger = logger;
        setFlags(System.getProperty(propertyName));

        // Bit of a cheek, but one of the primary consumers of this API is the
        // DocBook xslTNG Stylesheets. Let's not make the users set two properties.
        if (System.getProperty(propertyName) == null
            && System.getProperty(docBookPropertyName) != null) {
            setFlags(System.getProperty(docBookPropertyName));
        }
    }

    private void setFlags(String val) {
        if (val == null) {
            return;
        }

        if ("1".equals(val) || "yes".equals(val) || "true".equals(val)) {
            noisy = true;
        } else if (!("0".equals(val) || "no".equals(val) || "false".equals(val))) {
            noisy = false;
        } else {
            String[] tokens = System.getProperty(propertyName).split("[,\\s]+");
            for (String token : tokens) {
                String flag = token.trim().toLowerCase();
                if (!"".equals(flag)) {
                    flags.add(flag);
                }
            }
        }
    }

    public boolean enabled(String flag) {
        return flags.contains(flag);
    }

    public void debug(String flag, String message) {
        if ((flags.contains(flag) || noisy) && logger != null) {
            logger.info(message);
        }
    }
}
