package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaSirsMediaRepository extends MediaRepository<NasaSirsMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaSirsCount", "nasaSirsCountIgnored", "nasaSirsCountMissing", "nasaSirsCountMissingImages",
            "nasaSirsCountUploaded" })
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
    long countByIgnoredTrue();

    @Override
    @Cacheable("nasaSirsCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaSirsCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons();
    }

    @Override
    default long countMissingVideosInCommons() {
        return 0;
    }

    @Override
    @Cacheable("nasaSirsCountUploaded")
    long countUploadedToCommons();

    // FIND

    @Override
    default Page<NasaSirsMedia> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommons(page);
    }

    @Override
    default Page<NasaSirsMedia> findMissingVideosInCommons(Pageable page) {
        return Page.empty();
    }

    // SAVE

    @Override
    @CacheEvictNasaSirsAll
    <S extends NasaSirsMedia> S save(S entity);

    @Override
    @CacheEvictNasaSirsAll
    <S extends NasaSirsMedia> Iterable<S> saveAll(Iterable<S> entities);

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

    // UPDATE

    @Override
    @CacheEvictNasaSirsAll
    int resetIgnored();

    @Override
    @CacheEvictNasaSirsAll
    int resetIgnored(Set<String> repos);
}
