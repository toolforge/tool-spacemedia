package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaSirsMediaRepository extends MediaRepository<NasaSirsMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaSirsCount", "nasaSirsCountIgnored", "nasaSirsCountMissing", "nasaSirsCountMissingImagesRepo",
            "nasaSirsCountMissingVideosRepo", "nasaSirsCountMissingDocumentsRepo", "nasaSirsCountUploaded" })
    @interface CacheEvictNasaSirsAll {

    }

    @Override
    @CacheEvictNasaSirsAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaSirsCount")
    long count();

    @Override
    @Cacheable("nasaSirsCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("nasaSirsCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaSirsCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaSirsCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaSirsCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaSirsCountUploaded")
    long countUploadedToCommons();

    // SAVE

    @Override
    @CacheEvictNasaSirsAll
    <S extends NasaSirsMedia> S save(S entity);

    @Override
    @CacheEvictNasaSirsAll
    <S extends NasaSirsMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaSirsAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictNasaSirsAll
    void delete(NasaSirsMedia entity);

    @Override
    @CacheEvictNasaSirsAll
    void deleteAll(Iterable<? extends NasaSirsMedia> entities);

    @Override
    @CacheEvictNasaSirsAll
    void deleteAll();
}
