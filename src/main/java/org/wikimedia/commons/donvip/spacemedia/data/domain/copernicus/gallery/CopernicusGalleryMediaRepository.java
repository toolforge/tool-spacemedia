package org.wikimedia.commons.donvip.spacemedia.data.domain.copernicus.gallery;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface CopernicusGalleryMediaRepository extends MediaRepository<CopernicusGalleryMedia, String> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "copGalCount", "copGalCountIgnored", "copGalCountMissing",
            "copGalCountMissingImages", "copGalCountMissingVideos", "copGalCountUploaded", "copGalFindByPhashNotNull" })
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

    // FIND

    @Override
    @Cacheable("copGalFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    @Override
    default Page<CopernicusGalleryMedia> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommons(page);
    }

    @Override
    default Page<CopernicusGalleryMedia> findMissingVideosInCommons(Pageable page) {
        return Page.empty();
    }

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
    void deleteById(String id);

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
