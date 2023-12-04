package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.website;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaWebsiteMediaRepository extends MediaRepository<NasaWebsiteMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaWebsiteCount", "nasaWebsiteCountRepo", "nasaWebsiteCountIgnored", "nasaWebsiteCountIgnoredRepo",
            "nasaWebsiteCountMissing", "nasaWebsiteCountMissingRepo", "nasaWebsiteCountMissingImagesRepo",
            "nasaWebsiteCountMissingVideosRepo", "nasaWebsiteCountMissingDocumentsRepo", "nasaWebsiteCountUploaded",
            "nasaWebsiteCountUploadedRepo", "nasaWebsiteCountPhashNotNull", "nasaWebsiteCountPhashNotNullRepo" })
    @interface CacheEvictNasaWebsiteAll {

    }

    @Override
    @CacheEvictNasaWebsiteAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaWebsiteCount")
    long count();

    @Override
    @Cacheable("nasaWebsiteCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("nasaWebsiteCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("nasaWebsiteCountIgnoredRepo")
    long countByMetadata_IgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("nasaWebsiteCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaWebsiteCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaWebsiteCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaWebsiteCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaWebsiteCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaWebsiteCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("nasaWebsiteCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("nasaWebsiteCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("nasaWebsiteCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictNasaWebsiteAll
    <S extends NasaWebsiteMedia> S save(S entity);

    @Override
    @CacheEvictNasaWebsiteAll
    <S extends NasaWebsiteMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaWebsiteAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictNasaWebsiteAll
    void delete(NasaWebsiteMedia entity);

    @Override
    @CacheEvictNasaWebsiteAll
    void deleteAll(Iterable<? extends NasaWebsiteMedia> entities);

    @Override
    @CacheEvictNasaWebsiteAll
    void deleteAll();
}
