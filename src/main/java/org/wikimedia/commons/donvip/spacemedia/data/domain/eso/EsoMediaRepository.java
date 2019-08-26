package org.wikimedia.commons.donvip.spacemedia.data.domain.eso;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

public interface EsoMediaRepository extends CommonEsoMediaRepository<EsoMedia> {

    @CacheEvict(allEntries = true, cacheNames = {"esoCount", "esoCountIgnored", "esoCountMissing", "esoCountUploaded"})
    @interface CacheEvictEsoAll {

    }

    // COUNT

    @Override
    @Cacheable("esoCount")
    long count();

    @Override
    @Cacheable("esoCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("esoCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("esoCountUploaded")
    long countUploadedToCommons();

    // SAVE

    @Override
    @CacheEvictEsoAll
    <S extends EsoMedia> S save(S entity);

    @Override
    @CacheEvictEsoAll
    <S extends EsoMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictEsoAll
    void deleteById(String id);

    @Override
    @CacheEvictEsoAll
    void delete(EsoMedia entity);

    @Override
    @CacheEvictEsoAll
    void deleteAll(Iterable<? extends EsoMedia> entities);

    @Override
    @CacheEvictEsoAll
    void deleteAll();
}
