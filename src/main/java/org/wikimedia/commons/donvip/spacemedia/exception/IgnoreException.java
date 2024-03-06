package org.wikimedia.commons.donvip.spacemedia.exception;

public class IgnoreException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IgnoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public IgnoreException(String message) {
        super(message);
    }

    public IgnoreException(Throwable cause) {
        super(cause);
    }
}
