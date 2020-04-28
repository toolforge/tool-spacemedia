package org.wikimedia.commons.donvip.spacemedia.data.domain.esa.hubble;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.CommonEsoMediaRepository;

public interface HubbleEsaMediaRepository extends CommonEsoMediaRepository<HubbleEsaMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "hubEsaCount", "hubEsaCountIgnored", "hubEsaCountMissing", "hubEsaCountUploaded", "hubEsaFindByPhashNotNull" })
    @interface CacheEvictHubEsaAll {

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
