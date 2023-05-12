package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.util.List;

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
    List<? extends FileMetadataProjection> getMetadata();
}
