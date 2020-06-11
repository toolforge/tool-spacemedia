package org.wikimedia.commons.donvip.spacemedia.exception;

/**
 * Exception thrown when an upload error occurs.
 */
public class UploadException extends Exception {

    private static final long serialVersionUID = 1L;

    public UploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public UploadException(String message) {
        super(message);
    }

    public UploadException(Throwable cause) {
        super(cause);
    }
}
