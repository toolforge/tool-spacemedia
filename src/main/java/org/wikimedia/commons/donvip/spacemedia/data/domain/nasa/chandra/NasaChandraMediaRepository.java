package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.chandra;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaChandraMediaRepository extends MediaRepository<NasaChandraMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaChandraCount", "nasaChandraCountRepo", "nasaChandraCountIgnored", "nasaChandraCountIgnoredRepo",
            "nasaChandraCountMissing", "nasaChandraCountMissingRepo", "nasaChandraCountMissingImagesRepo",
            "nasaChandraCountMissingVideosRepo", "nasaChandraCountMissingDocumentsRepo", "nasaChandraCountUploaded",
            "nasaChandraCountUploadedRepo", "nasaChandraCountPhashNotNull", "nasaChandraCountPhashNotNullRepo" })
    @interface CacheEvictNasaChandraAll {

    }

    @Override
    @CacheEvictNasaChandraAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaChandraCount")
    long count();

    @Override
    @Cacheable("nasaChandraCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("nasaChandraCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("nasaChandraCountIgnoredRepo")
    long countByMetadata_IgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("nasaChandraCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaChandraCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaChandraCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaChandraCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaChandraCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaChandraCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("nasaChandraCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("nasaChandraCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("nasaChandraCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictNasaChandraAll
    <S extends NasaChandraMedia> S save(S entity);

    @Override
    @CacheEvictNasaChandraAll
    <S extends NasaChandraMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaChandraAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictNasaChandraAll
    void delete(NasaChandraMedia entity);

    @Override
    @CacheEvictNasaChandraAll
    void deleteAll(Iterable<? extends NasaChandraMedia> entities);

    @Override
    @CacheEvictNasaChandraAll
    void deleteAll();
}
