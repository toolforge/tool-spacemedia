package org.wikimedia.commons.donvip.spacemedia.data.domain.kari;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface KariMediaRepository extends MediaRepository<KariMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "kariCount", "kariCountRepo", "kariCountIgnored", "kariCountIgnoredRepo", "kariCountMissing",
            "kariCountMissingRepo", "kariCountMissingImagesRepo", "kariCountUploaded",
            "kariCountUploadedRepo", "kariCountPhashNotNull", "kariCountPhashNotNullRepo" })
    @interface CacheEvictKariAll {

    }

    @Override
    @CacheEvictKariAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("kariCount")
    long count();

    @Override
    @Cacheable("kariCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("kariCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("kariCountIgnoredRepo")
    long countByMetadata_IgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("kariCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("kariCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("kariCountMissingImagesRepo")
    default long countMissingImagesInCommons(Set<String> repos) {
        return countMissingInCommons(repos);
    }

    @Override
    default long countMissingVideosInCommons(Set<String> repos) {
        return 0;
    }

    @Override
    default long countMissingDocumentsInCommons(Set<String> repos) {
        return 0;
    }

    @Override
    @Cacheable("kariCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("kariCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("kariCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("kariCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictKariAll
    <S extends KariMedia> S save(S entity);

    @Override
    @CacheEvictKariAll
    <S extends KariMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictKariAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictKariAll
    void delete(KariMedia entity);

    @Override
    @CacheEvictKariAll
    void deleteAll(Iterable<? extends KariMedia> entities);

    @Override
    @CacheEvictKariAll
    void deleteAll();
}
