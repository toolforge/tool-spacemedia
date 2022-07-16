package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaProjection;

public interface HubbleNasaMediaRepository extends FullResMediaRepository<HubbleNasaMedia, String, ZonedDateTime> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "hubNasaCount", "hubNasaCountIgnored", "hubNasaCountMissing", "hubNasaCountUploaded", "hubNasaFindByPhashNotNull" })
    @interface CacheEvictHubNasaAll {

    }

    // COUNT

    @Override
    @Cacheable("hubNasaCount")
    long count();

    @Override
    @Cacheable("hubNasaCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("hubNasaCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("hubNasaCountUploaded")
    long countUploadedToCommons();

    // FIND

    @Override
    @Cacheable("hubNasaFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    // SAVE

    @Override
    @CacheEvictHubNasaAll
    <S extends HubbleNasaMedia> S save(S entity);

    @Override
    @CacheEvictHubNasaAll
    <S extends HubbleNasaMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictHubNasaAll
    void deleteById(String id);

    @Override
    @CacheEvictHubNasaAll
    void delete(HubbleNasaMedia entity);

    @Override
    @CacheEvictHubNasaAll
    void deleteAll(Iterable<? extends HubbleNasaMedia> entities);

    @Override
    @CacheEvictHubNasaAll
    void deleteAll();
}
