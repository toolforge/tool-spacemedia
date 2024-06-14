package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.List;

public record NasaSvsVizualisation(
        /** The id for this visualization. */
        int id,
        /** The url that this visualization page can be found at. */
        URL url,
        /** What type of page this is. */
        NasaSvsPageType page_type,
        /** The title of the visualization. */
        String title,
        /** A brief description of the visualization. */
        String description,
        /**
         * Indicates which studio created the material, or what product the material was
         * created for.
         */
        NasaSvsStudio studio,
        /** A list of sources that funded the creation of this visualization. */
        List<String> funding_sources,
        /** The date and time (ET) the visualization was released. */
        ZonedDateTime release_date,
        /** The date and time (ET) the visualization was last updated. */
        ZonedDateTime update_date,
        /**
         * The media item that's been selected to be the "main" image for this
         * visualization. This is also used as the thumbnail for this item in a variety
         * of places across the site. For more information on media items, see the
         * section on media items.
         */
        NasaSvsMediaItem main_image,
        /**
         * The media item that's been selected to be the "main" video for this
         * visualization. This is mostly used on other sites, if they link to SVS
         * visualizations. For more information on media items, see the section on media
         * items.
         */
        NasaSvsMediaItem main_video,
        /**
         * What stage this visualization is at in the development process. This will
         * always be Complete for visualizations publicly released on this site.
         */
        NasaSvsProgress progress,
        /**
         * Media items on the SVS website are organized into groups for display
         * purposes. These are essentially the "blocks" that compose a visualization
         * page.
         */
        List<NasaSvsMediaGroup> media_groups,
        /** A list of credits for this visualization. */
        List<NasaSvsCredits> credits,
        /** A list of missions that this visualization is associated with. */
        List<String> missions,
        /**
         * A list of series (sets of visualizations) that this visualization is
         * associated with.
         */
        List<String> series,
        /** A list of tapes that this visualization appeared on. */
        List<String> tapes,
        /** A list of research papers associated with this visualization. */
        List<String> papers,
        /** A list of datasets used in the creation of this visualization. */
        List<NasaSvsDataset> datasets,
        /** The NASA Science Categories that this visualization belongs to. */
        List<String> nasa_science_categories,
        /** A list of keywords associated with this visualization. */
        List<String> keywords,
        /** A list of pages (on our site) recommended by this page's author */
        List<String> recommended_pages,
        /** A list of pages (on our site) considered to be "related" to this page. */
        List<NasaSvsPage> related,
        /** A list of pages (on our site) used as sources for this page. */
        List<NasaSvsPage> sources,
        /** A list of pages (on our site) that use this page as a source. */
        List<NasaSvsPage> products,
        /** A list of pages (on our site) that are newer versions of this page. */
        List<NasaSvsPage> newer_versions,
        /** A list of pages (on our site) that are older versions of this page. */
        List<NasaSvsPage> older_versions,
        /**
         * A list of pages (on our site) that are considered to be alternate versions of
         * this page. These are usually versions of the page in a different language, a
         * different part of the world, or a different viewpoint.
         */
        List<NasaSvsPage> alternate_versions) {
}