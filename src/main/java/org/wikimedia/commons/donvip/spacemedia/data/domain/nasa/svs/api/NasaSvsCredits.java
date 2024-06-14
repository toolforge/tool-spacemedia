package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api;

import java.util.List;

public record NasaSvsCredits(
        /**
         * The role that this person is credited as. The pluralized version of the role
         * is indicated in square brackets.
         */
        String role,
        /** A list of people who are credited with this role. */
        List<NasaSvsPeople> people) {
}
