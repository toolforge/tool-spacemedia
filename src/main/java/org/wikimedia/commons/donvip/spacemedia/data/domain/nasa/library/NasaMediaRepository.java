package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaMediaRepository<T extends NasaMedia> extends MediaRepository<T, String> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaCount", "nasaCountByCenter", "nasaCountIgnored", "nasaCountIgnoredByCenter", "nasaCountMissing",
            "nasaCountMissingByCenter", "nasaCountMissingImages", "nasaCountMissingImagesByCenter",
            "nasaCountMissingVideos", "nasaCountMissingVideosByCenter",
            "nasaCountUploaded", "nasaCountUploadedByCenter", "nasaCountPhashNotNull",
            "nasaCountPhashNotNullByCenter", "nasaCenters", "nasaFindByPhashNotNull" })
    @interface CacheEvictNasaAll {

    }

    @Override
    @CacheEvictNasaAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaCount")
    long count();

    @Cacheable("nasaCountByCenter")
    long countByCenter(String center);

    @Override
    @Cacheable("nasaCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Cacheable("nasaCountPhashNotNullByCenter")
    long countByMetadata_PhashNotNullAndCenter(String center);

    @Override
    @Cacheable("nasaCountIgnored")
    long countByIgnoredTrue();

    @Cacheable("nasaCountIgnoredByCenter")
    @Query("select count(*) from #{#entityName} m where m.ignored is true and m.center = ?1")
    long countIgnoredByCenter(String center);

    @Override
    @Cacheable("nasaCountMissing")
    long countMissingInCommons();

    @Cacheable("nasaCountMissingByCenter")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.center = ?1")
    long countMissingInCommonsByCenter(String center);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.mediaType = ?1 and m.center = ?2")
    long countMissingInCommonsByTypeAndCenter(NasaMediaType type, String center);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and m.mediaType = ?1 and not exists elements (md.commonsFileNames)")
    long countMissingInCommons(NasaMediaType type);

    @Override
    @Cacheable("nasaCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons(NasaMediaType.image);
    }

    @Cacheable("nasaCountMissingImagesByCenter")
    default long countMissingImagesInCommons(String center) {
        return countMissingInCommonsByTypeAndCenter(NasaMediaType.image, center);
    }

    @Override
    @Cacheable("nasaCountMissingVideos")
    default long countMissingVideosInCommons() {
        return countMissingInCommons(NasaMediaType.video);
    }

    @Cacheable("nasaCountMissingVideosByCenter")
    default long countMissingVideosInCommons(String center) {
        return countMissingInCommonsByTypeAndCenter(NasaMediaType.video, center);
    }

    @Override
    @Cacheable("nasaCountUploaded")
    long countUploadedToCommons();

    @Cacheable("nasaCountUploadedByCenter")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.center = ?1")
    long countUploadedToCommonsByCenter(String center);

    // CUSTOM

    @Cacheable("nasaCenters")
    @Query("select distinct(center) from #{#entityName}")
    List<String> findCenters();

    // FIND

    List<T> findByCenter(String center);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where size (md.commonsFileNames) >= 2 and m.center = ?1")
    List<T> findDuplicateInCommonsByCenter(String center);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.center = ?1")
    List<T> findMissingInCommonsByCenter(String center);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and m.mediaType = ?1 and not exists elements (md.commonsFileNames)")
    Page<T> findMissingInCommonsByType(NasaMediaType type, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.center = ?1 and m.id not in ?2")
    List<T> findMissingInCommonsByCenterNotIn(String center, Set<String> ids);

    @Override
    default Page<T> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommonsByType(NasaMediaType.image, page);
    }

    @Override
    default Page<T> findMissingVideosInCommons(Pageable page) {
        return findMissingInCommonsByType(NasaMediaType.video, page);
    }

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.center = ?1")
    List<T> findUploadedToCommonsByCenter(String center);

    @Override
    @Cacheable("nasaFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    // SAVE

    @Override
    @CacheEvictNasaAll
    <S extends T> S save(S entity);

    @Override
    @CacheEvictNasaAll
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaAll
    void deleteById(String id);

    @Override
    @CacheEvictNasaAll
    void delete(T entity);

    @Override
    @CacheEvictNasaAll
    void deleteAll(Iterable<? extends T> entities);

    @Override
    @CacheEvictNasaAll
    void deleteAll();
}
