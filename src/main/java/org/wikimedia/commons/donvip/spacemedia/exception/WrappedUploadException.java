package org.wikimedia.commons.donvip.spacemedia.exception;

import java.util.Objects;

public class WrappedUploadException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WrappedUploadException(UploadException e) {
        super(Objects.requireNonNull(e));
    }

    @Override
    public synchronized UploadException getCause() {
        return (UploadException) super.getCause();
    }
}