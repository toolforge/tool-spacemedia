package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaSvsMediaRepository extends MediaRepository<NasaSvsMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "nasaSvsCount", "nasaSvsCountIgnored", "nasaSvsCountMissing",
            "nasaSvsCountMissingImages", "nasaSvsCountMissingVideos", "nasaSvsCountUploaded" })
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
    @Cacheable("nasaSvsCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("nasaSvsCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaSvsCountMissingImages")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("nasaSvsCountMissingVideos")
    long countMissingVideosInCommons();

    @Override
    @Cacheable("nasaSvsCountUploaded")
    long countUploadedToCommons();

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
}
