package org.wikimedia.commons.donvip.spacemedia.data.domain.dinamis;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface DinamisMediaRepository extends MediaRepository<DinamisMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "dinamisCount", "dinamisCountRepo", "dinamisCountIgnored",
            "dinamisCountIgnoredRepo", "dinamisCountMissing", "dinamisCountMissingRepo",
            "dinamisCountMissingImagesRepo", "dinamisCountMissingVideosRepo", "dinamisCountMissingDocumentsRepo",
            "dinamisCountUploaded", "dinamisCountUploadedRepo", "dinamisCountPhashNotNull",
            "dinamisCountPhashNotNullRepo" })
    @interface CacheEvictDinamisAll {

    }

    @Override
    @CacheEvictDinamisAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("dinamisCount")
    long count();

    @Override
    @Cacheable("dinamisCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("dinamisCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("dinamisCountIgnoredRepo")
    long countByMetadata_IgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("dinamisCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("dinamisCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("dinamisCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("dinamisCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("dinamisCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("dinamisCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("dinamisCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("dinamisCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("dinamisCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictDinamisAll
    <S extends DinamisMedia> S save(S entity);

    @Override
    @CacheEvictDinamisAll
    <S extends DinamisMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictDinamisAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictDinamisAll
    void delete(DinamisMedia entity);

    @Override
    @CacheEvictDinamisAll
    void deleteAll(Iterable<? extends DinamisMedia> entities);

    @Override
    @CacheEvictDinamisAll
    void deleteAll();
}
