package org.wikimedia.commons.donvip.spacemedia.data.domain.esa.webb;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;

public interface WebbEsaMediaRepository extends DjangoplicityMediaRepository<WebbEsaMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "webbEsaCount", "webbEsaCountIgnored", "webbEsaCountMissing", "webbEsaCountMissingImages",
            "webbEsaCountMissingVideos", "webbEsaCountUploaded", "webbEsaFindByPhashNotNull" })
    @interface CacheEvictWebbEsaAll {

    }

    @Override
    @CacheEvictWebbEsaAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("webbEsaCount")
    long count();

    @Override
    @Cacheable("webbEsaCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("webbEsaCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("webbEsaCountMissingImages")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("webbEsaCountMissingVideos")
    long countMissingVideosInCommons();

    @Override
    @Cacheable("webbEsaCountUploaded")
    long countUploadedToCommons();

    // FIND

    @Override
    @Cacheable("webbEsaFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    // SAVE

    @Override
    @CacheEvictWebbEsaAll
    <S extends WebbEsaMedia> S save(S entity);

    @Override
    @CacheEvictWebbEsaAll
    <S extends WebbEsaMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictWebbEsaAll
    void deleteById(String id);

    @Override
    @CacheEvictWebbEsaAll
    void delete(WebbEsaMedia entity);

    @Override
    @CacheEvictWebbEsaAll
    void deleteAll(Iterable<? extends WebbEsaMedia> entities);

    @Override
    @CacheEvictWebbEsaAll
    void deleteAll();
}
