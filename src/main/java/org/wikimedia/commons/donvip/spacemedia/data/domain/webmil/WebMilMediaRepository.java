package org.wikimedia.commons.donvip.spacemedia.data.domain.webmil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface WebMilMediaRepository extends MediaRepository<WebMilMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "webmilCount", "webmilCountByWebsite", "webmilCountIgnored",
            "webmilCountIgnoredByWebsite", "webmilCountMissing",
            "webmilCountMissingImagesByWebsite", "webmilCountMissingVideosByWebsite",
            "webmilCountMissingDocumentsByWebsite", "webmilCountMissingByWebsite",
            "webmilCountUploaded", "webmilCountUploadedByWebsite", "webmilCountPhashNotNull",
            "webmilCountPhashNotNullByWebsite" })
    @interface CacheEvictWebmilAll {

    }

    @Override
    @CacheEvictWebmilAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("webmilCount")
    long count();

    @Override
    @Cacheable("webmilCountByWebsite")
    long count(Set<String> websites);

    @Override
    @Cacheable("webmilCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("webmilCountIgnoredByWebsite")
    long countByMetadata_IgnoredTrue(Set<String> websites);

    @Override
    @Cacheable("webmilCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("webmilCountMissingByWebsite")
    long countMissingInCommons(Set<String> websites);

    @Override
    @Cacheable("webmilCountMissingImagesByWebsite")
    long countMissingImagesInCommons(Set<String> websites);

    @Override
    @Cacheable("webmilCountMissingVideosByWebsite")
    long countMissingVideosInCommons(Set<String> websites);

    @Override
    @Cacheable("webmilCountMissingDocumentsByWebsite")
    long countMissingDocumentsInCommons(Set<String> websites);

    @Override
    @Cacheable("webmilCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("webmilCountUploadedByWebsite")
    long countUploadedToCommons(Set<String> websites);

    @Override
    @Cacheable("webmilCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("webmilCountPhashNotNullByWebsite")
    long countByMetadata_PhashNotNull(Set<String> websites);

    // SAVE

    @Override
    @CacheEvictWebmilAll
    <S extends WebMilMedia> S save(S entity);

    @Override
    @CacheEvictWebmilAll
    <S extends WebMilMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictWebmilAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictWebmilAll
    void delete(WebMilMedia entity);

    @Override
    @CacheEvictWebmilAll
    void deleteAll(Iterable<? extends WebMilMedia> entities);

    @Override
    @CacheEvictWebmilAll
    void deleteAll();
}
