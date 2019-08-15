package org.wikimedia.commons.donvip.spacemedia.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;;

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "image forbidden to upload")
public class ImageUploadForbiddenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code ImageForbiddenException}.
     * @param message image identifier that can't be found
     */
    public ImageUploadForbiddenException(String message) {
        super(message);
    }
}
