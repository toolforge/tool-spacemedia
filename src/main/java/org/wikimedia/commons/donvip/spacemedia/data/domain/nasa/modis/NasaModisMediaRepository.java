package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.modis;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaModisMediaRepository extends MediaRepository<NasaModisMedia, String, LocalDate> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaModisCount", "nasaModisCountIgnored", "nasaModisCountMissing", "nasaModisCountMissingImages",
            "nasaModisCountUploaded", "nasaModisFindByPhashNotNull" })
    @interface CacheEvictNasaModisAll {

    }

    @Override
    @CacheEvictNasaModisAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaModisCount")
    long count();

    @Override
    @Cacheable("nasaModisCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("nasaModisCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaModisCountMissingImages")
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.metadata.commonsFileNames)")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("nasaModisCountMissingVideos")
    default long countMissingVideosInCommons() {
        return 0;
    }

    @Override
    @Cacheable("nasaModisCountUploaded")
    long countUploadedToCommons();

    // FIND

    Optional<NasaModisMedia> findByPublicationDate(LocalDate date);

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.metadata.commonsFileNames)")
    Page<NasaModisMedia> findMissingImagesInCommons(Pageable page);

    @Override
    default Page<NasaModisMedia> findMissingVideosInCommons(Pageable page) {
        return Page.empty();
    }

    @Override
    @Cacheable("nasaModisFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    // SAVE

    @Override
    @CacheEvictNasaModisAll
    <S extends NasaModisMedia> S save(S entity);

    @Override
    @CacheEvictNasaModisAll
    <S extends NasaModisMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaModisAll
    void deleteById(String id);

    @Override
    @CacheEvictNasaModisAll
    void delete(NasaModisMedia entity);

    @Override
    @CacheEvictNasaModisAll
    void deleteAll(Iterable<? extends NasaModisMedia> entities);

    @Override
    @CacheEvictNasaModisAll
    void deleteAll();
}
