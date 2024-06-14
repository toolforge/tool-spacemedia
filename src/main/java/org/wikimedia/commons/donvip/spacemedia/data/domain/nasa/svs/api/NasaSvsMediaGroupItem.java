package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api;

import java.net.URL;

public record NasaSvsMediaGroupItem(

        /** The id number of this media group item. */
        String id,
        /** A string indicating the type of this media group item. */
        NasaSvsMediaGroupItemType type,
        /** A summary of the actual object this media group item points to. */
        NasaSvsMediaItem instance, // FIXME only true for media items
        /** media/link: An optional title used when displaying this item. */
        String title,
        /** media/link: An optional caption used when displaying this item. */
        String caption,
        /** link: The URL that this item points to. */
        URL target, NasaSvsExtraData extra_data) {
}
