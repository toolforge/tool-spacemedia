package org.wikimedia.commons.donvip.spacemedia.data.domain.flickr;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface FlickrMediaRepository extends MediaRepository<FlickrMedia, String> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "flickrCount", "flickrCountByAccount", "flickrCountIgnoredByAccount", "flickrCountMissing",
            "flickrCountMissingByAccount", "flickrCountMissingByType", "flickrCountMissingByTypeAndAccount",
            "flickrCountMissingImages", "flickrCountMissingVideos", "flickrCountMissingImagesByAccount",
            "flickrCountMissingVideosByAccount", "flickrCountUploaded", "flickrCountUploadedByAccount",
            "flickrCountPhashNotNull", "flickrCountPhashNotNullByAccount" })
    @interface CacheEvictFlickrAll {

    }

    @Override
    @CacheEvictFlickrAll
    default void evictCaches() {

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
    long countMissingInCommons();

    @Cacheable("flickrCountMissingByAccount")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.pathAlias in ?1")
    long countMissingInCommons(Set<String> flickrAccounts);

    @Cacheable("flickrCountMissingByType")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.media = ?1")
    long countMissingInCommons(FlickrMediaType type);

    @Cacheable("flickrCountMissingByTypeAndAccount")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.media = ?1 and m.pathAlias in ?2")
    long countMissingInCommons(FlickrMediaType type, Set<String> flickrAccounts);

    @Override
    @Cacheable("flickrCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons(FlickrMediaType.photo);
    }

    @Cacheable("flickrCountMissingImagesByAccount")
    default long countMissingImagesInCommons(Set<String> flickrAccounts) {
        return countMissingInCommons(FlickrMediaType.photo, flickrAccounts);
    }

    @Override
    @Cacheable("flickrCountMissingVideos")
    default long countMissingVideosInCommons() {
        return countMissingInCommons(FlickrMediaType.video);
    }

    @Cacheable("flickrCountMissingVideosByAccount")
    default long countMissingVideosInCommons(Set<String> flickrAccounts) {
        return countMissingInCommons(FlickrMediaType.video, flickrAccounts);
    }

    @Override
    @Cacheable("flickrCountUploaded")
    long countUploadedToCommons();

    @Cacheable("flickrCountUploadedByAccount")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.pathAlias in ?1")
    long countUploadedToCommons(Set<String> flickrAccounts);

    @Override
    @Cacheable("flickrCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Cacheable("flickrCountPhashNotNullByAccount")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where md.phash is not null and m.pathAlias in ?1")
    long countByMetadata_PhashNotNull(Set<String> flickrAccounts);

    // FIND

    @Query("select m from #{#entityName} m where m.pathAlias in ?1")
    Set<FlickrMedia> findAll(Set<String> flickrAccounts);

    @Query("select m from #{#entityName} m where m.pathAlias in ?1")
    Page<FlickrMedia> findAll(Set<String> flickrAccounts, Pageable page);

    @Query("select m from #{#entityName} m where m.pathAlias in ?1 and m.id not in ?2")
    Set<FlickrMedia> findNotIn(Set<String> flickrAccounts, Set<String> ids);

    @Query("select m from #{#entityName} m where m.ignored = true and m.pathAlias in ?1")
    List<FlickrMedia> findByIgnoredTrue(Set<String> flickrAccounts);

    @Query("select m from #{#entityName} m where m.ignored = true and m.pathAlias in ?1")
    Page<FlickrMedia> findByIgnoredTrue(Set<String> flickrAccounts, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.pathAlias in ?1")
    List<FlickrMedia> findMissingInCommons(Set<String> flickrAccounts);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.pathAlias in ?1")
    Page<FlickrMedia> findMissingInCommons(Set<String> flickrAccounts, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.media = ?1")
    Page<FlickrMedia> findMissingInCommons(FlickrMediaType type, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.media = ?1 and m.pathAlias in ?2")
    Page<FlickrMedia> findMissingInCommons(FlickrMediaType type, Set<String> flickrAccounts, Pageable page);

    @Override
    default Page<FlickrMedia> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommons(FlickrMediaType.photo, page);
    }

    default Page<FlickrMedia> findMissingImagesInCommons(Set<String> flickrAccounts, Pageable page) {
        return findMissingInCommons(FlickrMediaType.photo, flickrAccounts, page);
    }

    @Override
    default Page<FlickrMedia> findMissingVideosInCommons(Pageable page) {
        return findMissingInCommons(FlickrMediaType.video, page);
    }

    default Page<FlickrMedia> findMissingVideosInCommons(Set<String> flickrAccounts, Pageable page) {
        return findMissingInCommons(FlickrMediaType.video, flickrAccounts, page);
    }

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.pathAlias in ?1 and (m.creationDate = ?2 or m.publicationDate = ?2)")
    List<FlickrMedia> findMissingInCommonsByDate(Set<String> flickrAccounts, LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.pathAlias in ?1 and m.title = ?2")
    List<FlickrMedia> findMissingInCommonsByTitle(Set<String> flickrAccounts, String title);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.pathAlias in ?1")
    List<FlickrMedia> findUploadedToCommons(Set<String> flickrAccounts);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.pathAlias in ?1")
    Page<FlickrMedia> findUploadedToCommons(Set<String> flickrAccounts, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where size (md.commonsFileNames) >= 2 and m.pathAlias in ?1")
    List<FlickrMedia> findDuplicateInCommons(Set<String> flickrAccounts);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.phash is not null and m.pathAlias in ?1")
    Page<FlickrMedia> findByMetadata_PhashNotNull(Set<String> flickrAccounts, Pageable page);

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
    void deleteById(String id);

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
    @CacheEvictFlickrAll
    @Query("update #{#entityName} m set m.ignored = null, m.ignoredReason = null where m.ignored = true and m.pathAlias in ?1")
    int resetIgnored(Set<String> flickrAccounts);
}
