package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.modis;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaModisMediaRepository extends MediaRepository<NasaModisMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaModisCount", "nasaModisCountRepo", "nasaModisCountIgnored", "nasaModisCountIgnoredRepo",
            "nasaModisCountMissing", "nasaModisCountMissingRepo", "nasaModisCountMissingImagesRepo",
            "nasaModisCountMissingVideosRepo", "nasaModisCountMissingDocumentsRepo", "nasaModisCountUploaded",
            "nasaModisCountUploadedRepo", "nasaModisCountPhashNotNull", "nasaModisCountPhashNotNullRepo" })
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
    @Cacheable("nasaModisCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("nasaModisCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("nasaModisCountIgnoredRepo")
    long countByIgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("nasaModisCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaModisCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaModisCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaModisCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaModisCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaModisCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("nasaModisCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("nasaModisCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("nasaModisCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictNasaModisAll
    <S extends NasaModisMedia> S save(S entity);

    @Override
    @CacheEvictNasaModisAll
    <S extends NasaModisMedia> List<S> saveAll(Iterable<S> entities);

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

    // UPDATE

    @Override
    @CacheEvictNasaModisAll
    int resetIgnored();

    @Override
    @CacheEvictNasaModisAll
    int resetIgnored(Set<String> repos);
}
