package org.wikimedia.commons.donvip.spacemedia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "category page not found")
public class CategoryPageNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code CategoryPageNotFoundException}.
     * 
     * @param message category page title that can't be found
     */
    public CategoryPageNotFoundException(String message) {
        super(message);
    }
}
