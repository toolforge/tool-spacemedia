package org.wikimedia.commons.donvip.spacemedia.data.domain.noaa.nesdis;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NoaaNesdisMediaRepository extends MediaRepository<NoaaNesdisMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "noaaNesdisCount", "noaaNesdisCountRepo", "noaaNesdisCountIgnored", "noaaNesdisCountIgnoredRepo",
            "noaaNesdisCountMissing", "noaaNesdisCountMissingRepo", "noaaNesdisCountMissingImagesRepo",
            "noaaNesdisCountMissingVideosRepo", "noaaNesdisCountMissingDocumentsRepo", "noaaNesdisCountUploaded",
            "noaaNesdisCountUploadedRepo", "noaaNesdisCountPhashNotNull", "noaaNesdisCountPhashNotNullRepo" })
    @interface CacheEvictNoaaNesdisAll {

    }

    @Override
    @CacheEvictNoaaNesdisAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("noaaNesdisCount")
    long count();

    @Override
    @Cacheable("noaaNesdisCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("noaaNesdisCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("noaaNesdisCountIgnoredRepo")
    long countByMetadata_IgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("noaaNesdisCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("noaaNesdisCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("noaaNesdisCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("noaaNesdisCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("noaaNesdisCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("noaaNesdisCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("noaaNesdisCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("noaaNesdisCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("noaaNesdisCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictNoaaNesdisAll
    <S extends NoaaNesdisMedia> S save(S entity);

    @Override
    @CacheEvictNoaaNesdisAll
    <S extends NoaaNesdisMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNoaaNesdisAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictNoaaNesdisAll
    void delete(NoaaNesdisMedia entity);

    @Override
    @CacheEvictNoaaNesdisAll
    void deleteAll(Iterable<? extends NoaaNesdisMedia> entities);

    @Override
    @CacheEvictNoaaNesdisAll
    void deleteAll();
}
