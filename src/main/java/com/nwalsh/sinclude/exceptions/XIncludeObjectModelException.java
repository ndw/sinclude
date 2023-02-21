package com.nwalsh.sinclude.exceptions;

public class XIncludeObjectModelException extends UnsupportedOperationException {
    public XIncludeObjectModelException(String message) {
        super(message);
    }
    public XIncludeObjectModelException(Throwable cause) {
        super(cause);
    }
    public XIncludeObjectModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
