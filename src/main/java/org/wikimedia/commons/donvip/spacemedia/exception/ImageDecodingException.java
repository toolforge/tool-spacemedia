package org.wikimedia.commons.donvip.spacemedia.exception;

public class ImageDecodingException extends Exception {

    private static final long serialVersionUID = 1L;

    public ImageDecodingException(String message) {
        super(message);
    }

    public ImageDecodingException(Throwable cause) {
        super(cause);
    }
}
