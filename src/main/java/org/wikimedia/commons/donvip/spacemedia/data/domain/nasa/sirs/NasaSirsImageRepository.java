package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaSirsImageRepository extends MediaRepository<NasaSirsImage, String> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaSirsCount", "nasaSirsCountIgnored", "nasaSirsCountMissing", "nasaSirsCountMissingImages",
            "nasaSirsCountUploaded", "nasaSirsFindByPhashNotNull" })
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
    default Page<NasaSirsImage> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommons(page);
    }

    @Override
    default Page<NasaSirsImage> findMissingVideosInCommons(Pageable page) {
        return Page.empty();
    }

    @Override
    @Cacheable("nasaSirsFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    // SAVE

    @Override
    @CacheEvictNasaSirsAll
    <S extends NasaSirsImage> S save(S entity);

    @Override
    @CacheEvictNasaSirsAll
    <S extends NasaSirsImage> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaSirsAll
    void deleteById(String id);

    @Override
    @CacheEvictNasaSirsAll
    void delete(NasaSirsImage entity);

    @Override
    @CacheEvictNasaSirsAll
    void deleteAll(Iterable<? extends NasaSirsImage> entities);

    @Override
    @CacheEvictNasaSirsAll
    void deleteAll();
}
