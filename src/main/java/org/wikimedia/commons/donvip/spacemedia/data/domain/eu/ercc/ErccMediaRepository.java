package org.wikimedia.commons.donvip.spacemedia.data.domain.eu.ercc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface ErccMediaRepository extends MediaRepository<ErccMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "erccCount", "erccCountRepo", "erccCountIgnored", "erccCountIgnoredRepo", "erccCountMissing",
            "erccCountMissingRepo", "erccCountMissingImagesRepo", "erccCountMissingVideosRepo",
            "erccCountMissingDocumentsRepo", "erccCountUploaded", "erccCountUploadedRepo", "erccCountPhashNotNull",
            "erccCountPhashNotNullRepo" })
    @interface CacheEvictErccAll {

    }

    @Override
    @CacheEvictErccAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("erccCount")
    long count();

    @Override
    @Cacheable("erccCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("erccCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("erccCountIgnoredRepo")
    long countByMetadata_IgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("erccCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("erccCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("erccCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("erccCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("erccCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("erccCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("erccCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("erccCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("erccCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictErccAll
    <S extends ErccMedia> S save(S entity);

    @Override
    @CacheEvictErccAll
    <S extends ErccMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictErccAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictErccAll
    void delete(ErccMedia entity);

    @Override
    @CacheEvictErccAll
    void deleteAll(Iterable<? extends ErccMedia> entities);

    @Override
    @CacheEvictErccAll
    void deleteAll();
}
