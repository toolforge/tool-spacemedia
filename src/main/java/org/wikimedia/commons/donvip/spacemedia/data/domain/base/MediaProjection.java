package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

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
    Set<? extends FileMetadataProjection> getMetadata();
}
