package org.wikimedia.commons.donvip.spacemedia.data.domain.box;

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

public interface BoxMediaRepository extends MediaRepository<BoxMedia, BoxMediaId> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "boxCount", "boxCountByShare", "boxCountIgnored",
            "boxCountIgnoredByShare", "boxCountMissing", "boxCountMissingImages", "boxCountMissingVideos",
            "boxCountMissingImagesByShare", "boxCountMissingVideosByShare", "boxCountMissingByShare",
            "boxCountUploaded", "boxCountUploadedByShare", "boxCountPhashNotNull", "boxCountPhashNotNullByShare" })
    @interface CacheEvictBoxAll {

    }

    @Override
    @CacheEvictBoxAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("boxCount")
    long count();

    @Cacheable("boxCountByShare")
    @Query("select count(*) from #{#entityName} m where m.id.share in ?1")
    long count(Set<String> shares);

    @Override
    @Cacheable("boxCountIgnored")
    long countByIgnoredTrue();

    @Cacheable("boxCountIgnoredByShare")
    @Query("select count(*) from #{#entityName} m where m.ignored = true and m.id.share in ?1")
    long countByIgnoredTrue(Set<String> shares);

    @Override
    @Cacheable("boxCountMissing")
    long countMissingInCommons();

    @Cacheable("boxCountMissingByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.share in ?1")
    long countMissingInCommonsByShare(Set<String> shares);

    @Override
    @Cacheable("boxCountMissingImages")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg')")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("boxCountMissingVideos")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg')")
    long countMissingVideosInCommons();

    @Cacheable("boxCountMissingImagesByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg') and m.id.share in ?1")
    long countMissingImagesInCommons(Set<String> shares);

    @Cacheable("boxCountMissingVideosByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg') and m.id.share in ?1")
    long countMissingVideosInCommons(Set<String> shares);

    @Override
    @Cacheable("boxCountUploaded")
    long countUploadedToCommons();

    @Cacheable("boxCountUploadedByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.share in ?1")
    long countUploadedToCommons(Set<String> shares);

    @Override
    @Cacheable("boxCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Cacheable("boxCountPhashNotNullByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where md.phash is not null and m.id.share in ?1")
    long countByMetadata_PhashNotNull(Set<String> shares);

    // FIND

    @Query("select m from #{#entityName} m where m.id.share in ?1")
    Set<BoxMedia> findAll(Set<String> shares);

    @Query("select m from #{#entityName} m where m.id.share in ?1")
    Page<BoxMedia> findAll(Set<String> shares, Pageable page);

    @Query("select m from #{#entityName} m where m.ignored = true and m.id.share in ?1")
    List<BoxMedia> findByIgnoredTrue(Set<String> shares);

    @Query("select m from #{#entityName} m where m.ignored = true and m.id.share in ?1")
    Page<BoxMedia> findByIgnoredTrue(Set<String> shares, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where size (md.commonsFileNames) >= 2 and m.id.share in ?1")
    List<BoxMedia> findDuplicateInCommons(Set<String> shares);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg')")
    Page<BoxMedia> findMissingImagesInCommons(Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg') and m.id.share in ?1")
    Page<BoxMedia> findMissingImagesInCommons(Set<String> shares, Pageable page);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg')")
    Page<BoxMedia> findMissingVideosInCommons(Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg') and m.id.share in ?1")
    Page<BoxMedia> findMissingVideosInCommons(Set<String> shares, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.share in ?1")
    List<BoxMedia> findMissingInCommonsByShare(Set<String> shares);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.share in ?1")
    Page<BoxMedia> findMissingInCommonsByShare(Set<String> shares, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.share in ?1 and (m.creationDate = ?2 or m.publicationDate = ?2)")
    List<BoxMedia> findMissingInCommonsByShareAndDate(Set<String> shares, LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.share in ?1 and m.title = ?2")
    List<BoxMedia> findMissingInCommonsByShareAndTitle(Set<String> shares, String title);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.share in ?1")
    List<BoxMedia> findUploadedToCommons(Set<String> shares);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.share in ?1")
    Page<BoxMedia> findUploadedToCommons(Set<String> shares, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.phash is not null and m.id.share in ?1")
    Page<BoxMedia> findByMetadata_PhashNotNull(Set<String> shares, Pageable page);

    // SAVE

    @Override
    @CacheEvictBoxAll
    <S extends BoxMedia> S save(S entity);

    @Override
    @CacheEvictBoxAll
    <S extends BoxMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictBoxAll
    void deleteById(BoxMediaId id);

    @Override
    @CacheEvictBoxAll
    void delete(BoxMedia entity);

    @Override
    @CacheEvictBoxAll
    void deleteAll(Iterable<? extends BoxMedia> entities);

    @Override
    @CacheEvictBoxAll
    void deleteAll();

    // UPDATE

    @Modifying
    @CacheEvictBoxAll
    @Query("update #{#entityName} m set m.ignored = null, m.ignoredReason = null where m.ignored = true and m.id.share in ?1")
    int resetIgnored(Set<String> shares);
}
