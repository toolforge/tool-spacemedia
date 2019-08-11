package org.wikimedia.commons.donvip.spacemedia.data.local.flickr;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.local.MediaRepository;

public interface FlickrMediaRepository extends MediaRepository<FlickrMedia, Long> {

    @Override
    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames)")
    List<FlickrMedia> findMissingInCommons();

    @Override
    @Query("select count(*) from #{#entityName} m where not exists elements (m.commonsFileNames)")
    long countMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2")
    List<FlickrMedia> findDuplicateInCommons();

    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames) and m.pathAlias in ?1")
    List<FlickrMedia> findMissingInCommons(Set<String> flickrAccounts);

    @Query("select count(*) from #{#entityName} m where not exists elements (m.commonsFileNames) and m.pathAlias in ?1")
    long countMissingInCommons(Set<String> flickrAccounts);

    @Query("select count(*) from #{#entityName} m where m.pathAlias in ?1")
    long count(Set<String> flickrAccounts);

    @Query("select m from #{#entityName} m where m.pathAlias in ?1")
    Iterable<FlickrMedia> findAll(Set<String> flickrAccounts);

    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2 and m.pathAlias in ?1")
    List<FlickrMedia> findDuplicateInCommons(Set<String> flickrAccounts);
}
