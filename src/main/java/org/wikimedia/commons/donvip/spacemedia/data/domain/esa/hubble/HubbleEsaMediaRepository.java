package org.wikimedia.commons.donvip.spacemedia.data.domain.esa.hubble;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;

public interface HubbleEsaMediaRepository extends DjangoplicityMediaRepository<HubbleEsaMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "hubEsaCount", "hubEsaCountIgnored", "hubEsaCountMissing", "hubEsaCountMissingImages",
            "hubEsaCountMissingVideos", "hubEsaCountUploaded", "hubEsaFindByPhashNotNull" })
    @interface CacheEvictHubEsaAll {

    }

    @Override
    @CacheEvictHubEsaAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("hubEsaCount")
    long count();

    @Override
    @Cacheable("hubEsaCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("hubEsaCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("hubEsaCountMissingImages")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("hubEsaCountMissingVideos")
    long countMissingVideosInCommons();

    @Override
    @Cacheable("hubEsaCountUploaded")
    long countUploadedToCommons();

    // FIND

    @Override
    @Cacheable("hubEsaFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    // SAVE

    @Override
    @CacheEvictHubEsaAll
    <S extends HubbleEsaMedia> S save(S entity);

    @Override
    @CacheEvictHubEsaAll
    <S extends HubbleEsaMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictHubEsaAll
    void deleteById(String id);

    @Override
    @CacheEvictHubEsaAll
    void delete(HubbleEsaMedia entity);

    @Override
    @CacheEvictHubEsaAll
    void deleteAll(Iterable<? extends HubbleEsaMedia> entities);

    @Override
    @CacheEvictHubEsaAll
    void deleteAll();
}
