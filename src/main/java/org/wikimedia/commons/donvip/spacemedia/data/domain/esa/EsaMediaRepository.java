package org.wikimedia.commons.donvip.spacemedia.data.domain.esa;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface EsaMediaRepository extends MediaRepository<EsaMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "esaCount", "esaCountRepo", "esaCountIgnored", "esaCountIgnoredRepo", "esaCountMissing",
            "esaCountMissingRepo", "esaCountMissingImagesRepo", "esaCountMissingVideosRepo",
            "esaCountMissingDocumentsRepo", "esaCountUploaded", "esaCountUploadedRepo",
            "esaCountPhashNotNull", "esaCountPhashNotNullRepo" })
    @interface CacheEvictEsaAll {

    }

    @Override
    @CacheEvictEsaAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("esaCount")
    long count();

    @Override
    @Cacheable("esaCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("esaCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("esaCountIgnoredRepo")
    long countByIgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("esaCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("esaCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("esaCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("esaCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("esaCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String>repos);

    @Override
    @Cacheable("esaCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("esaCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("esaCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("esaCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // FIND

    Optional<EsaMedia> findByUrl(URL mediaUrl);

    // SAVE

    @Override
    @CacheEvictEsaAll
    <S extends EsaMedia> S save(S entity);

    @Override
    @CacheEvictEsaAll
    <S extends EsaMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictEsaAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictEsaAll
    void delete(EsaMedia entity);

    @Override
    @CacheEvictEsaAll
    void deleteAll(Iterable<? extends EsaMedia> entities);

    @Override
    @CacheEvictEsaAll
    void deleteAll();

    // UPDATE

    @Override
    @CacheEvictEsaAll
    int resetIgnored();

    @Override
    @CacheEvictEsaAll
    int resetIgnored(Set<String> repos);
}
