package org.wikimedia.commons.donvip.spacemedia.data.domain.esa.hubble;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.CommonEsoMediaRepository;

public interface HubbleEsaMediaRepository extends CommonEsoMediaRepository<HubbleEsaMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {"hubCount", "hubCountIgnored", "hubCountMissing", "hubCountUploaded"})
    @interface CacheEvictHubAll {

    }

    // COUNT

    @Override
    @Cacheable("hubCount")
    long count();

    @Override
    @Cacheable("hubCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("hubCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("hubCountUploaded")
    long countUploadedToCommons();

    // SAVE

    @Override
    @CacheEvictHubAll
    <S extends HubbleEsaMedia> S save(S entity);

    @Override
    @CacheEvictHubAll
    <S extends HubbleEsaMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictHubAll
    void deleteById(String id);

    @Override
    @CacheEvictHubAll
    void delete(HubbleEsaMedia entity);

    @Override
    @CacheEvictHubAll
    void deleteAll(Iterable<? extends HubbleEsaMedia> entities);

    @Override
    @CacheEvictHubAll
    void deleteAll();
}
