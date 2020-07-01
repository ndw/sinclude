package com.nwalsh.sinclude.exceptions;

public class XIncludeException extends UnsupportedOperationException {
    public XIncludeException(String message) {
        super(message);
    }
    public XIncludeException(Throwable cause) {
        super(cause);
    }
    public XIncludeException(String message, Throwable cause) {
        super(message, cause);
    }
}
