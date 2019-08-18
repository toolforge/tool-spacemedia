package org.wikimedia.commons.donvip.spacemedia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;;

public class CategoryPageNotFoundException extends ResponseStatusException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code CategoryPageNotFoundException}.
     * 
     * @param message category page title that can't be found
     */
    public CategoryPageNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
