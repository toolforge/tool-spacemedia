package org.wikimedia.commons.donvip.spacemedia.data.local.flickr;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface FlickrMediaRepository extends CrudRepository<FlickrMedia, Long> {

    /**
     * Find Flickr files not yet uploaded to Wikimedia Commons.
     * 
     * @return Flickr files not yet uploaded to Wikimedia Commons
     */
    @Query("select m from FlickrMedia m where not exists elements (m.commonsFileNames)")
    List<FlickrMedia> findMissingInCommons();

    @Query("select m from FlickrMedia m where size (m.commonsFileNames) >= 2")
    List<FlickrMedia> findDuplicateInCommons();
}
