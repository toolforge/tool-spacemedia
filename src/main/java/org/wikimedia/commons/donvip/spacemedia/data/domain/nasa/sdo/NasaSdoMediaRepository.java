package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMediaType;

public interface NasaSdoMediaRepository extends MediaRepository<NasaSdoMedia, String, LocalDateTime> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaSdoCount", "nasaSdoCountIgnored", "nasaSdoCountMissing", "nasaSdoCountMissingImages",
            "nasaSdoCountMissingVideos", "nasaSdoCountUploaded", "nasaSdoFindByPhashNotNull" })
    @interface CacheEvictNasaAsterAll {

    }

    @Override
    @CacheEvictNasaAsterAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaSdoCount")
    long count();

    @Override
    @Cacheable("nasaSdoCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("nasaSdoCountMissing")
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.metadata.commonsFileNames)")
    long countMissingInCommons();

    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and m.mediaType = ?1 and not exists elements (m.metadata.commonsFileNames)")
    long countMissingInCommons(NasaMediaType type);

    @Override
    @Cacheable("nasaSdoCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons(NasaMediaType.image);
    }

    @Override
    @Cacheable("nasaSdoCountMissingVideos")
    default long countMissingVideosInCommons() {
        return countMissingInCommons(NasaMediaType.video);
    }

    @Override
    @Cacheable("nasaSdoCountUploaded")
    @Query("select count(*) from #{#entityName} m where exists elements (m.metadata.commonsFileNames)")
    long countUploadedToCommons();

    @Query(value = "select count(*) from nasa_sdo_media where media_type = ?1 and width = ?2 and height = ?3 and DATE(date) = ?3", nativeQuery = true)
    long countByMediaTypeAndDimensionsAndDate(NasaMediaType mediaType, int width, int height, LocalDate date);

    default long countByMediaTypeAndDimensionsAndDate(NasaMediaType mediaType, ImageDimensions dim, LocalDate date) {
        return countByMediaTypeAndDimensionsAndDate(mediaType, dim.getWidth(), dim.getHeight(), date);
    }

    // FIND

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.metadata.commonsFileNames)")
    List<NasaSdoMedia> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.metadata.commonsFileNames)")
    Page<NasaSdoMedia> findMissingInCommons(Pageable page);

    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and m.mediaType = ?1 and not exists elements (m.metadata.commonsFileNames)")
    Page<NasaSdoMedia> findMissingInCommonsByType(NasaMediaType type, Pageable page);

    @Override
    default Page<NasaSdoMedia> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommonsByType(NasaMediaType.image, page);
    }

    @Override
    default Page<NasaSdoMedia> findMissingVideosInCommons(Pageable page) {
        return findMissingInCommonsByType(NasaMediaType.video, page);
    }

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.metadata.commonsFileNames)")
    List<NasaSdoMedia> findUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.metadata.commonsFileNames)")
    Page<NasaSdoMedia> findUploadedToCommons(Pageable page);

    @Override
    @Query("select m from #{#entityName} m where size (m.metadata.commonsFileNames) >= 2")
    List<NasaSdoMedia> findDuplicateInCommons();

    @Override
    @Cacheable("nasaSdoFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    // SAVE

    @Override
    @CacheEvictNasaAsterAll
    <S extends NasaSdoMedia> S save(S entity);

    @Override
    @CacheEvictNasaAsterAll
    <S extends NasaSdoMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaAsterAll
    void deleteById(String id);

    @Override
    @CacheEvictNasaAsterAll
    void delete(NasaSdoMedia entity);

    @Override
    @CacheEvictNasaAsterAll
    void deleteAll(Iterable<? extends NasaSdoMedia> entities);

    @Override
    @CacheEvictNasaAsterAll
    void deleteAll();
}
