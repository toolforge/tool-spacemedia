package org.wikimedia.commons.donvip.spacemedia.data.domain.kari;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface KariMediaRepository extends MediaRepository<KariMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "kariCount", "kariCountRepo", "kariCountIgnored", "kariCountIgnoredRepo", "kariCountMissing",
            "kariCountMissingRepo", "kariCountMissingImages", "kariCountMissingImagesRepo", "kariCountUploaded",
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
    long countByIgnoredTrue();

    @Override
    @Cacheable("kariCountIgnoredRepo")
    long countByIgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("kariCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("kariCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("kariCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons();
    }

    @Override
    @Cacheable("kariCountMissingImagesRepo")
    default long countMissingImagesInCommons(Set<String> repos) {
        return countMissingInCommons(repos);
    }

    @Override
    default long countMissingVideosInCommons() {
        return 0;
    }

    @Override
    default long countMissingVideosInCommons(Set<String> repos) {
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

    // FIND

    @Override
    default Page<KariMedia> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommons(page);
    }

    @Override
    default Page<KariMedia> findMissingVideosInCommons(Pageable page) {
        return Page.empty();
    }

    // SAVE

    @Override
    @CacheEvictKariAll
    <S extends KariMedia> S save(S entity);

    @Override
    @CacheEvictKariAll
    <S extends KariMedia> Iterable<S> saveAll(Iterable<S> entities);

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

    // UPDATE

    @Override
    @CacheEvictKariAll
    int resetIgnored();

    @Override
    @CacheEvictKariAll
    int resetIgnored(Set<String> repos);
}
