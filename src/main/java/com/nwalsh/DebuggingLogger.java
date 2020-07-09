package com.nwalsh;

import net.sf.saxon.lib.Logger;
import net.sf.saxon.lib.StandardLogger;

public class DebuggingLogger extends StandardLogger {
    private static final String propertyName = "com.nwalsh.sinclude.verbose";
    private static final String docBookPropertyName = "org.docbook.extensions.xslt.verbose";
    private Logger logger = null;
    private boolean noisy = false;

    public DebuggingLogger(Logger logger) {
        this.logger = logger;
        if (System.getProperty(propertyName) != null) {
            String val = System.getProperty(propertyName);
            if ("1".equals(val) || "yes".equals(val) || "true".equals(val)) {
                noisy = true;
            } else {
                if (!("0".equals(val) || "no".equals(val) || "false".equals(val))) {
                    if (logger != null) {
                        logger.info("Ignoring " + propertyName + " invalid value: " + val);
                    }
                }
            }
            return;
        }

        // Bit of a cheek, but one of the primary consumers of this API is the
        // DocBook xslTNG Stylesheets. Let's not make the users set two properties.
        if (System.getProperty(docBookPropertyName) != null) {
            String val = System.getProperty(docBookPropertyName);
            if ("1".equals(val) || "yes".equals(val) || "true".equals(val)) {
                noisy = true;
            } else {
                if (!("0".equals(val) || "no".equals(val) || "false".equals(val))) {
                    if (logger != null) {
                        logger.info("Ignoring " + docBookPropertyName + " invalid value: " + val);
                    }
                }
            }
        }
    }

    public void debug(String message) {
        if (noisy && logger != null) {
            logger.info(message);
        }
    }
}
