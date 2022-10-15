package org.wikimedia.commons.donvip.spacemedia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ImageNotFoundException extends ResponseStatusException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code ImageNotFoundException}.
     * @param message image identifier that can't be found
     */
    public ImageNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
