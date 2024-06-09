package org.wikimedia.commons.donvip.spacemedia.data.domain.noaa.library;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NoaaLibraryMediaRepository extends MediaRepository<NoaaLibraryMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "noaaLibraryCount", "noaaLibraryCountRepo", "noaaLibraryCountIgnored", "noaaLibraryCountIgnoredRepo",
            "noaaLibraryCountMissing", "noaaLibraryCountMissingRepo", "noaaLibraryCountMissingImagesRepo",
            "noaaLibraryCountMissingVideosRepo", "noaaLibraryCountMissingDocumentsRepo", "noaaLibraryCountUploaded",
            "noaaLibraryCountUploadedRepo", "noaaLibraryCountPhashNotNull", "noaaLibraryCountPhashNotNullRepo" })
    @interface CacheEvictNoaaLibraryAll {

    }

    @Override
    @CacheEvictNoaaLibraryAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("noaaLibraryCount")
    long count();

    @Override
    @Cacheable("noaaLibraryCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("noaaLibraryCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("noaaLibraryCountIgnoredRepo")
    long countByMetadata_IgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("noaaLibraryCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("noaaLibraryCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("noaaLibraryCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("noaaLibraryCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("noaaLibraryCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("noaaLibraryCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("noaaLibraryCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("noaaLibraryCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("noaaLibraryCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictNoaaLibraryAll
    <S extends NoaaLibraryMedia> S save(S entity);

    @Override
    @CacheEvictNoaaLibraryAll
    <S extends NoaaLibraryMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNoaaLibraryAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictNoaaLibraryAll
    void delete(NoaaLibraryMedia entity);

    @Override
    @CacheEvictNoaaLibraryAll
    void deleteAll(Iterable<? extends NoaaLibraryMedia> entities);

    @Override
    @CacheEvictNoaaLibraryAll
    void deleteAll();
}
