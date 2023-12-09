package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaAsterMediaRepository extends MediaRepository<NasaAsterMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaAsterCount", "nasaAsterCountRepo", "nasaAsterCountIgnored", "nasaAsterCountIgnoredRepo",
            "nasaAsterCountMissing", "nasaAsterCountMissingRepo",
            "nasaAsterCountMissingImagesRepo", "nasaAsterCountMissingVideosRepo", "nasaAsterCountMissingDocumentsRepo",
            "nasaAsterCountUploaded", "nasaAsterCountUploadedRepo", "nasaAsterCountPhashNotNull",
            "nasaAsterCountPhashNotNullRepo" })
    @interface CacheEvictNasaAsterAll {

    }

    @Override
    @CacheEvictNasaAsterAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaAsterCount")
    long count();

    @Override
    @Cacheable("nasaAsterCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("nasaAsterCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("nasaAsterCountIgnoredRepo")
    long countByMetadata_IgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("nasaAsterCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaAsterCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaAsterCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaAsterCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaAsterCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaAsterCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("nasaAsterCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("nasaAsterCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("nasaAsterCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictNasaAsterAll
    <S extends NasaAsterMedia> S save(S entity);

    @Override
    @CacheEvictNasaAsterAll
    <S extends NasaAsterMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaAsterAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictNasaAsterAll
    void delete(NasaAsterMedia entity);

    @Override
    @CacheEvictNasaAsterAll
    void deleteAll(Iterable<? extends NasaAsterMedia> entities);

    @Override
    @CacheEvictNasaAsterAll
    void deleteAll();
}
