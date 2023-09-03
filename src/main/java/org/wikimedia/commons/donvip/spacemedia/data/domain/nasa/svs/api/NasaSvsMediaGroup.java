package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api;

import java.util.List;

public record NasaSvsMediaGroup(
        /**
         * The widget used for this media item. This is used for internal purposes, and
         * can probably be ignored.
         */
        String widget,
        /** The title of this media group. */
        String title,
        /**
         * The caption of this media group. In a standard media group, this is rendered
         * just below the media display, and to the right of the "downloads" button.
         */
        String caption,
        /**
         * The description of this media group. This is where the vast majority of the
         * text in a media group is stored.
         */
        String description,
        /**
         * A list of media items contained in this media group. For more information on
         * media items, see the section on media items.
         */
        List<NasaSvsMediaItem> media) {
}