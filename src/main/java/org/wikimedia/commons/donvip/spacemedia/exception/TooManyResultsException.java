package org.wikimedia.commons.donvip.spacemedia.exception;

public class TooManyResultsException extends Exception {

    private static final long serialVersionUID = 1L;

    public TooManyResultsException(String message) {
        super(message);
    }
}
