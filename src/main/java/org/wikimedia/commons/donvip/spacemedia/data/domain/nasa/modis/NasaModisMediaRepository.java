package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.modis;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaModisMediaRepository extends MediaRepository<NasaModisMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaModisCount", "nasaModisCountIgnored", "nasaModisCountMissing", "nasaModisCountMissingImages",
            "nasaModisCountUploaded" })
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
    void deleteById(CompositeMediaId id);

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
