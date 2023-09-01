package org.wikimedia.commons.donvip.spacemedia.data.domain.stsci;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface StsciMediaRepository extends MediaRepository<StsciMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "stsciCount", "stsciCountIgnored", "stsciCountMissing", "stsciCountMissingImages",
            "stsciCountMissingImagesByMission", "stsciCountMissingVideos", "stsciCountMissingVideosByMission",
            "stsciCountUploaded" })
    @interface CacheEvictStsciAll {

    }

    @Override
    @CacheEvictStsciAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("stsciCount")
    long count();

    @Override
    @Cacheable("stsciCountByMission")
    long count(Set<String> missions);

    @Override
    @Cacheable("stsciCountIgnored")
    long countByIgnoredTrue(Set<String> missions);

    @Override
    @Cacheable("stsciCountMissing")
    long countMissingInCommons(Set<String> missions);

    @Override
    @Cacheable("stsciCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons();
    }

    @Override
    @Cacheable("stsciCountMissingImagesByMission")
    default long countMissingImagesInCommons(Set<String> missions) {
        return countMissingInCommons(missions);
    }

    @Override
    @Cacheable("stsciCountMissingVideos")
    default long countMissingVideosInCommons() {
        return 0;
    }

    @Override
    @Cacheable("stsciCountMissingVideosByMission")
    default long countMissingVideosInCommons(Set<String> missions) {
        return 0;
    }

    @Override
    @Cacheable("stsciCountUploaded")
    long countUploadedToCommons(Set<String> missions);

    @Override
    @Cacheable("stsciCountPhashNotNullByMission")
    long countByMetadata_PhashNotNull(Set<String> missions);

    // SAVE

    @Override
    @CacheEvictStsciAll
    <S extends StsciMedia> S save(S entity);

    @Override
    @CacheEvictStsciAll
    <S extends StsciMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictStsciAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictStsciAll
    void delete(StsciMedia entity);

    @Override
    @CacheEvictStsciAll
    void deleteAll(Iterable<? extends StsciMedia> entities);

    @Override
    @CacheEvictStsciAll
    void deleteAll();
}
