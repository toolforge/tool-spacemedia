package org.wikimedia.commons.donvip.spacemedia.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "image not found")
public class ImageNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code ImageNotFoundException}.
     * @param message image identifier that can't be found
     */
    public ImageNotFoundException(String message) {
        super(message);
    }
}
