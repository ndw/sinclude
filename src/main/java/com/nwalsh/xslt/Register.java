package com.nwalsh.xslt;

import com.nwalsh.BuildConfig;
import com.nwalsh.DebuggingLogger;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.Initializer;

public class Register implements Initializer {
    @Override
    public void initialize(Configuration config) {
        // Unfortunately, Saxon doesn't have a .debug() method on its standard logger
        DebuggingLogger logger = new DebuggingLogger(config.getLogger());

        logger.debug(DebuggingLogger.REGISTRATION,
                "Registering " + BuildConfig.TITLE + " extension functions (version " + BuildConfig.VERSION + ")");

        config.registerExtensionFunction(new XIncludeFunction());
    }
}
