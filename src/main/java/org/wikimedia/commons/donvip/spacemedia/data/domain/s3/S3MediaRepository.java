package org.wikimedia.commons.donvip.spacemedia.data.domain.s3;

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
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface S3MediaRepository extends MediaRepository<S3Media, CompositeMediaId> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "s3Count", "s3CountByShare", "s3CountIgnored",
            "s3CountIgnoredByShare", "s3CountMissing", "s3CountMissingImages", "s3CountMissingVideos",
            "s3CountMissingImagesByShare", "s3CountMissingVideosByShare", "s3CountMissingByShare", "s3CountUploaded",
            "s3CountUploadedByShare", "s3CountPhashNotNull", "s3CountPhashNotNullByShare" })
    @interface CacheEvictS3All {

    }

    @Override
    @CacheEvictS3All
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("s3Count")
    long count();

    @Cacheable("s3CountByShare")
    @Query("select count(*) from #{#entityName} m where m.id.repoId in ?1")
    long count(Set<String> buckets);

    @Override
    @Cacheable("s3CountIgnored")
    long countByIgnoredTrue();

    @Cacheable("s3CountIgnoredByShare")
    @Query("select count(*) from #{#entityName} m where m.ignored = true and m.id.repoId in ?1")
    long countByIgnoredTrue(Set<String> buckets);

    @Override
    @Cacheable("s3CountMissing")
    long countMissingInCommons();

    @Cacheable("s3CountMissingByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    long countMissingInCommonsByShare(Set<String> buckets);

    @Override
    @Cacheable("s3CountMissingImages")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg')")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("s3CountMissingVideos")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg')")
    long countMissingVideosInCommons();

    @Cacheable("s3CountMissingImagesByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg') and m.id.repoId in ?1")
    long countMissingImagesInCommons(Set<String> buckets);

    @Cacheable("s3CountMissingVideosByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg') and m.id.repoId in ?1")
    long countMissingVideosInCommons(Set<String> buckets);

    @Override
    @Cacheable("s3CountUploaded")
    long countUploadedToCommons();

    @Cacheable("s3CountUploadedByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    long countUploadedToCommons(Set<String> buckets);

    @Override
    @Cacheable("s3CountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Cacheable("s3CountPhashNotNullByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where md.phash is not null and m.id.repoId in ?1")
    long countByMetadata_PhashNotNull(Set<String> buckets);

    // FIND

    @Query("select m from #{#entityName} m where m.id.repoId in ?1")
    Set<S3Media> findAll(Set<String> buckets);

    @Query("select m from #{#entityName} m where m.id.repoId in ?1")
    Page<S3Media> findAll(Set<String> buckets, Pageable page);

    @Query("select m from #{#entityName} m where m.ignored = true and m.id.repoId in ?1")
    List<S3Media> findByIgnoredTrue(Set<String> buckets);

    @Query("select m from #{#entityName} m where m.ignored = true and m.id.repoId in ?1")
    Page<S3Media> findByIgnoredTrue(Set<String> buckets, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where size (md.commonsFileNames) >= 2 and m.id.repoId in ?1")
    List<S3Media> findDuplicateInCommons(Set<String> buckets);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg')")
    Page<S3Media> findMissingImagesInCommons(Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg') and m.id.repoId in ?1")
    Page<S3Media> findMissingImagesInCommons(Set<String> buckets, Pageable page);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg')")
    Page<S3Media> findMissingVideosInCommons(Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg') and m.id.repoId in ?1")
    Page<S3Media> findMissingVideosInCommons(Set<String> buckets, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    List<S3Media> findMissingInCommonsByShare(Set<String> buckets);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    Page<S3Media> findMissingInCommonsByShare(Set<String> buckets, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and (m.creationDate = ?2 or m.publicationDate = ?2)")
    List<S3Media> findMissingInCommonsByShareAndDate(Set<String> buckets, LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and m.title = ?2")
    List<S3Media> findMissingInCommonsByShareAndTitle(Set<String> buckets, String title);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    List<S3Media> findUploadedToCommons(Set<String> buckets);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    Page<S3Media> findUploadedToCommons(Set<String> buckets, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.phash is not null and m.id.repoId in ?1")
    Page<S3Media> findByMetadata_PhashNotNull(Set<String> buckets, Pageable page);

    // SAVE

    @Override
    @CacheEvictS3All
    <S extends S3Media> S save(S entity);

    @Override
    @CacheEvictS3All
    <S extends S3Media> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictS3All
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictS3All
    void delete(S3Media entity);

    @Override
    @CacheEvictS3All
    void deleteAll(Iterable<? extends S3Media> entities);

    @Override
    @CacheEvictS3All
    void deleteAll();

    // UPDATE

    @Modifying
    @CacheEvictS3All
    @Query("update #{#entityName} m set m.ignored = null, m.ignoredReason = null where m.ignored = true and m.id.repoId in ?1")
    int resetIgnored(Set<String> buckets);
}
