package org.wikimedia.commons.donvip.spacemedia.data.domain.flickr;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository.CacheEvictDvidsAll;

public interface FlickrMediaRepository extends MediaRepository<FlickrMedia, Long, LocalDateTime> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "flickrCount", "flickrCountByAccount", "flickrCountIgnoredByAccount", "flickrCountMissing",
            "flickrCountMissingByAccount", "flickrCountUploaded", "flickrCountUploadedByAccount",
            "flickrCountPhashNotNull", "flickrCountPhashNotNullByAccount", "flickrFindByPhashNotNull" })
    @interface CacheEvictFlickrAll {

    }

    // COUNT

    @Override
    @Cacheable("flickrCount")
    long count();

    @Cacheable("flickrCountByAccount")
    @Query("select count(*) from #{#entityName} m where m.pathAlias in ?1")
    long count(Set<String> flickrAccounts);

    @Cacheable("flickrCountIgnoredByAccount")
    @Query("select count(*) from #{#entityName} m where m.ignored = true and m.pathAlias in ?1")
    long countByIgnoredTrue(Set<String> flickrAccounts);

    @Override
    @Cacheable("flickrCountMissing")
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames)")
    long countMissingInCommons();

    @Cacheable("flickrCountMissingByAccount")
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames) and m.pathAlias in ?1")
    long countMissingInCommons(Set<String> flickrAccounts);

    @Override
    @Cacheable("flickrCountUploaded")
    @Query("select count(*) from #{#entityName} m where exists elements (m.commonsFileNames)")
    long countUploadedToCommons();

    @Cacheable("flickrCountUploadedByAccount")
    @Query("select count(*) from #{#entityName} m where exists elements (m.commonsFileNames) and m.pathAlias in ?1")
    long countUploadedToCommons(Set<String> flickrAccounts);

    @Override
    @Cacheable("flickrCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Cacheable("flickrCountPhashNotNullByAccount")
    @Query("select count(*) from #{#entityName} m where m.metadata.phash is not null and m.pathAlias in ?1")
    long countByMetadata_PhashNotNull(Set<String> flickrAccounts);

    // FIND

    @Query("select m from #{#entityName} m where m.pathAlias in ?1")
    Set<FlickrMedia> findAll(Set<String> flickrAccounts);

    @Query("select m from #{#entityName} m where m.pathAlias in ?1")
    Page<FlickrMedia> findAll(Set<String> flickrAccounts, Pageable page);

    @Query("select m from #{#entityName} m where m.ignored = true and m.pathAlias in ?1")
    List<FlickrMedia> findByIgnoredTrue(Set<String> flickrAccounts);

    @Query("select m from #{#entityName} m where m.ignored = true and m.pathAlias in ?1")
    Page<FlickrMedia> findByIgnoredTrue(Set<String> flickrAccounts, Pageable page);

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames)")
    List<FlickrMedia> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames)")
    Page<FlickrMedia> findMissingInCommons(Pageable page);

    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames) and m.pathAlias in ?1")
    List<FlickrMedia> findMissingInCommons(Set<String> flickrAccounts);

    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames) and m.pathAlias in ?1")
    Page<FlickrMedia> findMissingInCommons(Set<String> flickrAccounts, Pageable page);

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames)")
    List<FlickrMedia> findUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames)")
    Page<FlickrMedia> findUploadedToCommons(Pageable page);

    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames) and m.pathAlias in ?1")
    List<FlickrMedia> findUploadedToCommons(Set<String> flickrAccounts);

    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames) and m.pathAlias in ?1")
    Page<FlickrMedia> findUploadedToCommons(Set<String> flickrAccounts, Pageable page);

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2")
    List<FlickrMedia> findDuplicateInCommons();

    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2 and m.pathAlias in ?1")
    List<FlickrMedia> findDuplicateInCommons(Set<String> flickrAccounts);

    @Override
    @Cacheable("flickrFindByPhashNotNull")
    List<MediaProjection<Long>> findByMetadata_PhashNotNull();

    @Query("select m from #{#entityName} m where m.metadata.phash is not null and m.pathAlias in ?1")
    Page<FlickrMedia> findByMetadata_PhashNotNull(Set<String> flickrAccounts, Pageable page);

    @Query("select m from #{#entityName} m where m.duplicates is not empty and m.pathAlias in ?1")
    List<FlickrMedia> findByDuplicatesIsNotEmpty(Set<String> flickrAccounts);

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

    // UPDATE

    @Modifying
    @CacheEvictDvidsAll
    @Query("update #{#entityName} m set m.ignored = null, m.ignoredReason = null where m.ignored = true and m.pathAlias in ?1")
    int resetIgnored(Set<String> flickrAccounts);

    @Modifying
    @CacheEvictFlickrAll
    @Query("update #{#entityName} m set m.metadata.phash = null where m.metadata.phash is not null and m.pathAlias in ?1")
    int resetPerceptualHashes(Set<String> flickrAccounts);
}
