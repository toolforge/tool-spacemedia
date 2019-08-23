package org.wikimedia.commons.donvip.spacemedia.data.domain.flickr;

import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;

public interface FlickrMediaRepository extends MediaRepository<FlickrMedia, Long> {

    @CacheEvict(allEntries = true, cacheNames = {
            "flickrCount", "flickrCountByAccount", "flickrCountMissing", "flickrCountMissingByAccount"})
    @interface CacheEvictFlickrAll {

    }

    // COUNT

    @Override
    @Cacheable("flickrCount")
    long count();

    @Cacheable("flickrCountByAccount")
    @Query("select count(*) from #{#entityName} m where m.pathAlias in ?1")
    long count(Set<String> flickrAccounts);

    @Cacheable("flickrCountMissing")
    @Query("select count(*) from #{#entityName} m where m.ignored = true and m.pathAlias in ?1")
    long countIgnored(Set<String> flickrAccounts);

    @Override
    @Cacheable("flickrCountMissing")
    @Query("select count(*) from #{#entityName} m where not exists elements (m.commonsFileNames)")
    long countMissingInCommons();

    @Cacheable("flickrCountMissingByAccount")
    @Query("select count(*) from #{#entityName} m where not exists elements (m.commonsFileNames) and m.pathAlias in ?1")
    long countMissingInCommons(Set<String> flickrAccounts);

    // FIND

    @Query("select m from #{#entityName} m where m.pathAlias in ?1")
    Iterable<FlickrMedia> findAll(Set<String> flickrAccounts);

    @Override
    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames)")
    List<FlickrMedia> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames)")
    Page<FlickrMedia> findMissingInCommons(Pageable page);

    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames) and m.pathAlias in ?1")
    List<FlickrMedia> findMissingInCommons(Set<String> flickrAccounts);

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2")
    List<FlickrMedia> findDuplicateInCommons();

    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2 and m.pathAlias in ?1")
    List<FlickrMedia> findDuplicateInCommons(Set<String> flickrAccounts);

    // SAVE

    @Override
    @CacheEvictFlickrAll
    <S extends FlickrMedia> S save(S entity);

    @Override
    @CacheEvictFlickrAll
    <S extends FlickrMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictFlickrAll
    void deleteById(Long id);

    @Override
    @CacheEvictFlickrAll
    void delete(FlickrMedia entity);

    @Override
    @CacheEvictFlickrAll
    void deleteAll(Iterable<? extends FlickrMedia> entities);

    @Override
    @CacheEvictFlickrAll
    void deleteAll();
}
