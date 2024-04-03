package org.wikimedia.commons.donvip.spacemedia.exception;

public class FileDecodingException extends Exception {

    private static final long serialVersionUID = 1L;

    public FileDecodingException(String message) {
        super(message);
    }

    public FileDecodingException(Throwable cause) {
        super(cause);
    }
}
