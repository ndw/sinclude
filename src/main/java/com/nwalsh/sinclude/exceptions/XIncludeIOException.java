package com.nwalsh.sinclude.exceptions;

public class XIncludeIOException extends XIncludeException {
    public XIncludeIOException(String uri, String message) {
        super(message);
    }
    public XIncludeIOException(String uri, Throwable cause) {
        super(cause);
    }
}
