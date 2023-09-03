package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api;

public record NasaSvsCredits(
        /** The name of the person that is credited on this visualization. */
        String person,
        /**
         * The role that this person is credited as. The pluralized version of the role
         * is indicated in square brackets.
         */
        String role,
        /**
         * Whether or not this person is considered to have taken a lead role on this
         * animation.
         */
        boolean lead) {
}