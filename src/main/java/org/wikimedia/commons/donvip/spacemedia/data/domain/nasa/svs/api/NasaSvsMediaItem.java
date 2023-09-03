package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api;

import java.net.URL;

public record NasaSvsMediaItem(
        /** The url that this media item can be found at. */
        URL url,
        /** The filename of this media item */
        String filename,
        /** A string indicating the type of this media item. */
        NasaSvsMediaType media_type,
        /** Alt text for this media item. */
        String alt_text,
        /**
         * The width of this item (in pixels). Note: this value is 0 for items that
         * don't contain resolution information (e.g., audio files or caption files).
         */
        int width,
        /**
         * The height of this item (in pixels). Note: this value is 0 for items that
         * don't contain resolution information (e.g., audio files or caption files).
         */
        int height,
        /**
         * The total number of pixels in this image (= width x height). This is used
         * internally and can probably be ignored.
         */
        long pixels) {
}