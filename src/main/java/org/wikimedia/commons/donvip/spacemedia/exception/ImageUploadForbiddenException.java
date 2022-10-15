package org.wikimedia.commons.donvip.spacemedia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ImageUploadForbiddenException extends ResponseStatusException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code ImageForbiddenException}.
     * @param message image identifier that can't be found
     */
    public ImageUploadForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
