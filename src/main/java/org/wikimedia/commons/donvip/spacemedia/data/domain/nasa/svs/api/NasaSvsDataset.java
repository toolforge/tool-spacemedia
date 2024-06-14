package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api;

import java.util.List;

public record NasaSvsDataset(
        /** The full name of this dataset. */
        String name,
        /** A more common name for this dataset (if applicable). */
        String common_name,
        /**
         * The location where this dataset was collected from. It's easiest to think of
         * "where this dataset was collected from physically". Often, this is the name
         * of a particular mission (e.g. TERRA, SDO, OSIRIS-REx, etc.)
         */
        String platform,
        /** The sensor this dataset was collected with (if applicable). */
        String sensor,
        /** What type of dataset this is. */
        String type,
        /**
         * The organizations responsible for maintaining this dataset (if applicable).
         */
        List<String> organizations,
        /** A description of this dataset. */
        String description,
        /** Who should be credited for this dataset. */
        String credit,
        /** A URL that this dataset can be found at (if applicable). */
        String url,
        /** The range of dates used from this dataset. */
        Object date_range) {
}