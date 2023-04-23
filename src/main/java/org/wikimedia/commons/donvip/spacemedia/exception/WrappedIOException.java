package org.wikimedia.commons.donvip.spacemedia.exception;

import java.io.IOException;
import java.util.Objects;

public class WrappedIOException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WrappedIOException(IOException e) {
        super(Objects.requireNonNull(e));
    }

    @Override
    public synchronized IOException getCause() {
        return (IOException) super.getCause();
    }
}