package org.wikimedia.commons.donvip.spacemedia.data.domain.iau;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;

public interface IauMediaRepository extends DjangoplicityMediaRepository<IauMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "iauCount", "iauCountIgnored", "iauCountMissing", "iauCountMissingImages", "iauCountMissingVideos",
            "iauCountUploaded", "iauFindByPhashNotNull" })
    @interface CacheEvictIauAll {

    }

    @Override
    @CacheEvictIauAll
    default void evictCaches() {

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
    @Cacheable("iauCountMissingImages")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("iauCountMissingVideos")
    long countMissingVideosInCommons();

    @Override
    @Cacheable("iauCountUploaded")
    long countUploadedToCommons();

    // FIND

    @Override
    @Cacheable("iauFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

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
