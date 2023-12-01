package org.wikimedia.commons.donvip.spacemedia.data.domain.copernicus.gallery;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface CopernicusGalleryMediaRepository extends MediaRepository<CopernicusGalleryMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "copGalCount", "copGalCountRepo", "copGalCountIgnored",
            "copGalCountIgnoredRepo", "copGalCountMissing", "copGalCountMissingRepo",
            "copGalCountMissingImagesRepo", "copGalCountMissingVideosRepo", "copGalCountMissingDocumentsRepo",
            "copGalCountUploaded", "copGalCountUploadedRepo", "copGalCountPhashNotNull",
            "copGalCountPhashNotNullRepo" })
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
    @Cacheable("copGalCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("copGalCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("copGalCountIgnoredRepo")
    long countByIgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("copGalCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("copGalCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("copGalCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("copGalCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("copGalCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("copGalCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("copGalCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("copGalCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("copGalCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictCopernicusGalleryAll
    <S extends CopernicusGalleryMedia> S save(S entity);

    @Override
    @CacheEvictCopernicusGalleryAll
    <S extends CopernicusGalleryMedia> List<S> saveAll(Iterable<S> entities);

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

    // UPDATE

    @Override
    @CacheEvictCopernicusGalleryAll
    int resetIgnored();

    @Override
    @CacheEvictCopernicusGalleryAll
    int resetIgnored(Set<String> repos);
}
