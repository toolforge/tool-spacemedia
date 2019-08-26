package org.wikimedia.commons.donvip.spacemedia.data.domain.iau;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.CommonEsoMediaRepository;

public interface IauMediaRepository extends CommonEsoMediaRepository<IauMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {"iauCount", "iauCountIgnored", "iauCountMissing", "iauCountUploaded"})
    @interface CacheEvictIauAll {

    }

    // COUNT

    @Override
    @Cacheable("iauCount")
    long count();

    @Override
    @Cacheable("iauCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("iauCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("iauCountUploaded")
    long countUploadedToCommons();

    // SAVE

    @Override
    @CacheEvictIauAll
    <S extends IauMedia> S save(S entity);

    @Override
    @CacheEvictIauAll
    <S extends IauMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictIauAll
    void deleteById(String id);

    @Override
    @CacheEvictIauAll
    void delete(IauMedia entity);

    @Override
    @CacheEvictIauAll
    void deleteAll(Iterable<? extends IauMedia> entities);

    @Override
    @CacheEvictIauAll
    void deleteAll();
}
