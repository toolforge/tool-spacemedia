package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaSvsMediaRepository extends MediaRepository<NasaSvsMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "nasaSvsCount", "nasaSvsCountRepo", "nasaSvsCountIgnored",
            "nasaSvsCountIgnoredRepo", "nasaSvsCountMissing", "nasaSvsCountMissingRepo",
            "nasaSvsCountMissingImages", "nasaSvsCountMissingImagesRepo", "nasaSvsCountMissingVideos",
            "nasaSvsCountMissingVideosRepo", "nasaSvsCountUploaded", "nasaSvsCountUploadedRepo",
            "nasaSvsCountPhashNotNull", "nasaSvsCountPhashNotNullRepo" })
    @interface CacheEvictNasaSvsAll {

    }

    @Override
    @CacheEvictNasaSvsAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaSvsCount")
    long count();

    @Override
    @Cacheable("nasaSvsCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("nasaSvsCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("nasaSvsCountIgnoredRepo")
    long countByIgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("nasaSvsCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaSvsCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaSvsCountMissingImages")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("nasaSvsCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaSvsCountMissingVideos")
    long countMissingVideosInCommons();

    @Override
    @Cacheable("nasaSvsCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaSvsCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("nasaSvsCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("nasaSvsCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("nasaSvsCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictNasaSvsAll
    <S extends NasaSvsMedia> S save(S entity);

    @Override
    @CacheEvictNasaSvsAll
    <S extends NasaSvsMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaSvsAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictNasaSvsAll
    void delete(NasaSvsMedia entity);

    @Override
    @CacheEvictNasaSvsAll
    void deleteAll(Iterable<? extends NasaSvsMedia> entities);

    @Override
    @CacheEvictNasaSvsAll
    void deleteAll();

    // UPDATE

    @Override
    @CacheEvictNasaSvsAll
    int resetIgnored();

    @Override
    @CacheEvictNasaSvsAll
    int resetIgnored(Set<String> repos);
}
