package org.wikimedia.commons.donvip.spacemedia.data.domain.stsci;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface StsciMediaRepository extends MediaRepository<StsciMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "stsciCount", "stsciCountIgnored", "stsciCountMissing",
            "stsciCountMissingImagesByMission", "stsciCountMissingVideosByMission",
            "stsciCountMissingDocumentsByMission", "stsciCountUploaded" })
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
    @Cacheable("stsciCountMissingImagesByMission")
    long countMissingImagesInCommons(Set<String> missions);

    @Override
    @Cacheable("stsciCountMissingVideosByMission")
    long countMissingVideosInCommons(Set<String> missions);

    @Override
    @Cacheable("stsciCountMissingDocumentsByMission")
    long countMissingDocumentsInCommons(Set<String> missions);

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
    <S extends StsciMedia> List<S> saveAll(Iterable<S> entities);

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

    // UPDATE

    @Override
    @CacheEvictStsciAll
    int resetIgnored();

    @Override
    @CacheEvictStsciAll
    int resetIgnored(Set<String> repos);
}
