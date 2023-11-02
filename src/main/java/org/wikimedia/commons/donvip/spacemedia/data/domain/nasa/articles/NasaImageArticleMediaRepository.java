package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.articles;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaImageArticleMediaRepository extends MediaRepository<NasaImageArticleMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaImageArticleCount", "nasaImageArticleCountRepo", "nasaImageArticleCountIgnored",
            "nasaImageArticleCountIgnoredRepo", "nasaImageArticleCountMissing", "nasaImageArticleCountMissingRepo",
            "nasaImageArticleCountMissingImagesRepo", "nasaImageArticleCountMissingVideosRepo",
            "nasaImageArticleCountMissingDocumentsRepo", "nasaImageArticleCountUploaded",
            "nasaImageArticleCountUploadedRepo", "nasaImageArticleCountPhashNotNull",
            "nasaImageArticleCountPhashNotNullRepo" })
    @interface CacheEvictNasaImageArticleAll {

    }

    @Override
    @CacheEvictNasaImageArticleAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaImageArticleCount")
    long count();

    @Override
    @Cacheable("nasaImageArticleCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("nasaImageArticleCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("nasaImageArticleCountIgnoredRepo")
    long countByIgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("nasaImageArticleCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaImageArticleCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaImageArticleCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaImageArticleCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaImageArticleCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaImageArticleCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("nasaImageArticleCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("nasaImageArticleCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("nasaImageArticleCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictNasaImageArticleAll
    <S extends NasaImageArticleMedia> S save(S entity);

    @Override
    @CacheEvictNasaImageArticleAll
    <S extends NasaImageArticleMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaImageArticleAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictNasaImageArticleAll
    void delete(NasaImageArticleMedia entity);

    @Override
    @CacheEvictNasaImageArticleAll
    void deleteAll(Iterable<? extends NasaImageArticleMedia> entities);

    @Override
    @CacheEvictNasaImageArticleAll
    void deleteAll();

    // UPDATE

    @Override
    @CacheEvictNasaImageArticleAll
    int resetIgnored();

    @Override
    @CacheEvictNasaImageArticleAll
    int resetIgnored(Set<String> repos);
}
