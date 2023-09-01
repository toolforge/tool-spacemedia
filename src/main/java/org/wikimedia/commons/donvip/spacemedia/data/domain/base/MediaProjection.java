package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.util.Set;

public interface MediaProjection {

    /**
     * Returns the media identifier.
     *
     * @return the media identifier
     */
    CompositeMediaId getId();

    /**
     * Returns the metadata.
     *
     * @return the metadata
     */
    Set<? extends FileMetadataProjection> getMetadata();
}
