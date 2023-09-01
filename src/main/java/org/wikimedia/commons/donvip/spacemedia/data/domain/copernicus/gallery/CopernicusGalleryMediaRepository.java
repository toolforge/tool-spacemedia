package org.wikimedia.commons.donvip.spacemedia.data.domain.copernicus.gallery;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface CopernicusGalleryMediaRepository extends MediaRepository<CopernicusGalleryMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "copGalCount", "copGalCountIgnored", "copGalCountMissing",
            "copGalCountMissingImages", "copGalCountMissingVideos", "copGalCountUploaded" })
    @interface CacheEvictCopernicusGalleryAll {

    }

    @Override
    @CacheEvictCopernicusGalleryAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("copGalCount")
    long count();

    @Override
    @Cacheable("copGalCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("copGalCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("copGalCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons();
    }

    @Override
    @Cacheable("copGalCountMissingVideos")
    default long countMissingVideosInCommons() {
        return 0;
    }

    @Override
    @Cacheable("copGalCountUploaded")
    long countUploadedToCommons();

    // SAVE

    @Override
    @CacheEvictCopernicusGalleryAll
    <S extends CopernicusGalleryMedia> S save(S entity);

    @Override
    @CacheEvictCopernicusGalleryAll
    <S extends CopernicusGalleryMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictCopernicusGalleryAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictCopernicusGalleryAll
    void delete(CopernicusGalleryMedia entity);

    @Override
    @CacheEvictCopernicusGalleryAll
    void deleteAll(Iterable<? extends CopernicusGalleryMedia> entities);

    @Override
    @CacheEvictCopernicusGalleryAll
    void deleteAll();
}
