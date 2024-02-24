package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.lroc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaLrocMediaRepository extends MediaRepository<NasaLrocMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaLrocCount", "nasaLrocCountRepo", "nasaLrocCountIgnored", "nasaLrocCountIgnoredRepo",
            "nasaLrocCountMissing", "nasaLrocCountMissingRepo", "nasaLrocCountMissingImagesRepo",
            "nasaLrocCountMissingVideosRepo", "nasaLrocCountMissingDocumentsRepo", "nasaLrocCountUploaded",
            "nasaLrocCountUploadedRepo", "nasaLrocCountPhashNotNull", "nasaLrocCountPhashNotNullRepo" })
    @interface CacheEvictnasaLrocAll {

    }

    @Override
    @CacheEvictnasaLrocAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaLrocCount")
    long count();

    @Override
    @Cacheable("nasaLrocCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("nasaLrocCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("nasaLrocCountIgnoredRepo")
    long countByMetadata_IgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("nasaLrocCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaLrocCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaLrocCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaLrocCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaLrocCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaLrocCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("nasaLrocCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("nasaLrocCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("nasaLrocCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictnasaLrocAll
    <S extends NasaLrocMedia> S save(S entity);

    @Override
    @CacheEvictnasaLrocAll
    <S extends NasaLrocMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictnasaLrocAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictnasaLrocAll
    void delete(NasaLrocMedia entity);

    @Override
    @CacheEvictnasaLrocAll
    void deleteAll(Iterable<? extends NasaLrocMedia> entities);

    @Override
    @CacheEvictnasaLrocAll
    void deleteAll();
}
