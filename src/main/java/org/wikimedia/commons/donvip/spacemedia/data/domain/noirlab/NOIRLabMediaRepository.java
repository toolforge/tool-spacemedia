package org.wikimedia.commons.donvip.spacemedia.data.domain.noirlab;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.CommonEsoMediaRepository;

public interface NOIRLabMediaRepository extends CommonEsoMediaRepository<NOIRLabMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "noirLabCount", "noirLabCountIgnored", "noirLabCountMissing", "noirLabCountMissingImages",
            "noirLabCountMissingVideos", "noirLabCountUploaded", "noirLabFindByPhashNotNull" })
    @interface CacheEvictNOIRLabAll {

    }

    @Override
    @CacheEvictNOIRLabAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("noirLabCount")
    long count();

    @Override
    @Cacheable("noirLabCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("noirLabCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("noirLabCountMissingImages")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("noirLabCountMissingVideos")
    long countMissingVideosInCommons();

    @Override
    @Cacheable("noirLabCountUploaded")
    long countUploadedToCommons();

    // FIND

    @Override
    @Cacheable("noirLabFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    // SAVE

    @Override
    @CacheEvictNOIRLabAll
    <S extends NOIRLabMedia> S save(S entity);

    @Override
    @CacheEvictNOIRLabAll
    <S extends NOIRLabMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNOIRLabAll
    void deleteById(String id);

    @Override
    @CacheEvictNOIRLabAll
    void delete(NOIRLabMedia entity);

    @Override
    @CacheEvictNOIRLabAll
    void deleteAll(Iterable<? extends NOIRLabMedia> entities);

    @Override
    @CacheEvictNOIRLabAll
    void deleteAll();
}
