package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.util.Set;

public interface MediaProjection<ID> {

    /**
     * Returns the media identifier.
     *
     * @return the media identifier
     */
    ID getId();

    /**
     * Returns the metadata.
     *
     * @return the metadata
     */
    MetadataProjection getMetadata();

    /**
     * Returns the duplicates. Duplicates are other media considered strictly or nearly identical, thus ignored and not to be uploaded.
     *
     * @return the duplicates
     */
    Set<Duplicate> getDuplicates();

    /**
     * Returns the variants.
     * Variants are other media considered similar but not identical, thus not ignored and to be uploaded and linked to this media.
     *
     * @return the variants
     */
    Set<Duplicate> getVariants();
}
