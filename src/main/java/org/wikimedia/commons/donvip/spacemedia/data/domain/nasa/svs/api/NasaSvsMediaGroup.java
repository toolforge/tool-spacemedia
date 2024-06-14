package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api;

import java.util.List;
import java.util.stream.Stream;

public record NasaSvsMediaGroup(
        /** The ID of this media group. */
        int id,
        /** A link to this particular media group. */
        String url,
        /** The widget used for this media item. */
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
        List<NasaSvsMediaGroupItem> items,
        /**
         * Additional data used the generate the media group. The most common piece of
         * information contained in this block is page_list, which is a full list of
         * pages linked to this media group.
         */
        NasaSvsExtraData extra_data) {

    public Stream<NasaSvsMediaItem> mediaItemsStream() {
        return items().stream().filter(x -> x.type() == NasaSvsMediaGroupItemType.media).map(x -> x.instance());
    }
}