package org.wikimedia.commons.donvip.spacemedia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;;

public class RevisionNotFoundException extends ResponseStatusException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code RevisionNotFoundException}.
     * 
     * @param revisionId revision id that can't be found
     */
    public RevisionNotFoundException(int revisionId) {
        super(HttpStatus.NOT_FOUND, Integer.toString(revisionId));
    }
}
