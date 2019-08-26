package org.wikimedia.commons.donvip.spacemedia.data.domain.hubble;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.CommonEsoMediaRepository;

public interface HubbleMediaRepository extends CommonEsoMediaRepository<HubbleMedia> {

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
    <S extends HubbleMedia> S save(S entity);

    @Override
    @CacheEvictHubAll
    <S extends HubbleMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictHubAll
    void deleteById(String id);

    @Override
    @CacheEvictHubAll
    void delete(HubbleMedia entity);

    @Override
    @CacheEvictHubAll
    void deleteAll(Iterable<? extends HubbleMedia> entities);

    @Override
    @CacheEvictHubAll
    void deleteAll();
}
