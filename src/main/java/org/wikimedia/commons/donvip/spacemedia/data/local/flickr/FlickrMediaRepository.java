package org.wikimedia.commons.donvip.spacemedia.data.local.flickr;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.local.MediaRepository;

public interface FlickrMediaRepository extends MediaRepository<FlickrMedia, Long> {

    @Override
    @Query("select m from FlickrMedia m where not exists elements (m.commonsFileNames)")
    List<FlickrMedia> findMissingInCommons();

    @Override
    @Query("select count(*) from FlickrMedia m where not exists elements (m.commonsFileNames)")
    long countMissingInCommons();

    @Override
    @Query("select m from FlickrMedia m where size (m.commonsFileNames) >= 2")
    List<FlickrMedia> findDuplicateInCommons();
}
