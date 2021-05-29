package org.wikimedia.commons.donvip.spacemedia.exception;

public class ApiException extends Exception {

    private static final long serialVersionUID = 1L;

    public ApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiException(String message) {
        super(message);
    }

    public ApiException(Throwable cause) {
        super(cause);
    }

}
