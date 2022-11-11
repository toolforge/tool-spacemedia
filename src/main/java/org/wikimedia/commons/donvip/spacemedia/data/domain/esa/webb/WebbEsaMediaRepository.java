package org.wikimedia.commons.donvip.spacemedia.data.domain.esa.webb;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.CommonEsoMediaRepository;

public interface WebbEsaMediaRepository extends CommonEsoMediaRepository<WebbEsaMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "webbEsaCount", "webbEsaCountIgnored", "webbEsaCountMissing", "webbEsaCountMissingImages",
            "webbEsaCountMissingVideos", "webbEsaCountUploaded", "webbEsaFindByPhashNotNull" })
    @interface CacheEvictHubEsaAll {

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
    @CacheEvictHubEsaAll
    <S extends WebbEsaMedia> S save(S entity);

    @Override
    @CacheEvictHubEsaAll
    <S extends WebbEsaMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictHubEsaAll
    void deleteById(String id);

    @Override
    @CacheEvictHubEsaAll
    void delete(WebbEsaMedia entity);

    @Override
    @CacheEvictHubEsaAll
    void deleteAll(Iterable<? extends WebbEsaMedia> entities);

    @Override
    @CacheEvictHubEsaAll
    void deleteAll();
}
