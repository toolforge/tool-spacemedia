package org.wikimedia.commons.donvip.spacemedia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "category not found")
public class CategoryNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code CategoryNotFoundException}.
     * 
     * @param message category title that can't be found
     */
    public CategoryNotFoundException(String message) {
        super(message);
    }
}
