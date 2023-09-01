package org.wikimedia.commons.donvip.spacemedia.data.domain.box;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.DefaultMediaRepository;

public interface BoxMediaRepository extends DefaultMediaRepository<BoxMedia> {

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
    @Query("select count(*) from #{#entityName} m where m.id.repoId in ?1")
    long count(Set<String> appShares);

    @Override
    @Cacheable("boxCountIgnored")
    long countByIgnoredTrue();

    @Cacheable("boxCountIgnoredByShare")
    @Query("select count(*) from #{#entityName} m where m.ignored = true and m.id.repoId in ?1")
    long countByIgnoredTrue(Set<String> appShares);

    @Override
    @Cacheable("boxCountMissing")
    long countMissingInCommons();

    @Cacheable("boxCountMissingByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    long countMissingInCommonsByShare(Set<String> appShares);

    @Override
    @Cacheable("boxCountMissingImages")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg')")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("boxCountMissingVideos")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg')")
    long countMissingVideosInCommons();

    @Cacheable("boxCountMissingImagesByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg') and m.id.repoId in ?1")
    long countMissingImagesInCommons(Set<String> appShares);

    @Cacheable("boxCountMissingVideosByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg') and m.id.repoId in ?1")
    long countMissingVideosInCommons(Set<String> appShares);

    @Override
    @Cacheable("boxCountUploaded")
    long countUploadedToCommons();

    @Cacheable("boxCountUploadedByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    long countUploadedToCommons(Set<String> appShares);

    @Override
    @Cacheable("boxCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Cacheable("boxCountPhashNotNullByShare")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where md.phash is not null and m.id.repoId in ?1")
    long countByMetadata_PhashNotNull(Set<String> appShares);

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
    void deleteById(CompositeMediaId id);

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
    @Query("update #{#entityName} m set m.ignored = null, m.ignoredReason = null where m.ignored = true and m.id.repoId in ?1")
    int resetIgnored(Set<String> appShares);
}
