package org.wikimedia.commons.donvip.spacemedia.exception;

public class FileDecodingException extends Exception {

    private static final long serialVersionUID = 1L;

    private final long contentLength;

    public FileDecodingException(long contentLength, String message) {
        super(message);
        this.contentLength = contentLength;
    }

    public FileDecodingException(long contentLength, Throwable cause) {
        super(cause);
        this.contentLength = contentLength;
    }

    public long getContentLength() {
        return contentLength;
    }
}
