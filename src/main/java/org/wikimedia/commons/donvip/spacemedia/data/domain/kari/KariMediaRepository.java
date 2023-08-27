package org.wikimedia.commons.donvip.spacemedia.data.domain.kari;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface KariMediaRepository extends MediaRepository<KariMedia, String> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "kariCount", "kariCountIgnored", "kariCountMissing", "kariCountMissingImages", "kariCountUploaded" })
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
    @Cacheable("kariCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("kariCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("kariCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons();
    }

    @Override
    default long countMissingVideosInCommons() {
        return 0;
    }

    @Override
    @Cacheable("kariCountUploaded")
    long countUploadedToCommons();

    // FIND

    @Query("select max(id) from #{#entityName}")
    Optional<String> findMaxId();

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
    void deleteById(String id);

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
