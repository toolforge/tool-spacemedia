package org.wikimedia.commons.donvip.spacemedia.data.domain.inpe.dpi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface InpeDpiMediaRepository extends MediaRepository<InpeDpiMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "inpeDpiCount", "inpeDpiCountRepo", "inpeDpiCountIgnored", "inpeDpiCountIgnoredRepo", "inpeDpiCountMissing",
            "inpeDpiCountMissingRepo", "inpeDpiCountMissingImagesRepo", "inpeDpiCountMissingVideosRepo",
            "inpeDpiCountMissingDocumentsRepo", "inpeDpiCountUploaded", "inpeDpiCountUploadedRepo",
            "inpeDpiCountPhashNotNull", "inpeDpiCountPhashNotNullRepo" })
    @interface CacheEvictInpeDpiAll {

    }

    @Override
    @CacheEvictInpeDpiAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("inpeDpiCount")
    long count();

    @Override
    @Cacheable("inpeDpiCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("inpeDpiCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("inpeDpiCountIgnoredRepo")
    long countByMetadata_IgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("inpeDpiCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("inpeDpiCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("inpeDpiCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("inpeDpiCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("inpeDpiCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("inpeDpiCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("inpeDpiCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("inpeDpiCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("inpeDpiCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictInpeDpiAll
    <S extends InpeDpiMedia> S save(S entity);

    @Override
    @CacheEvictInpeDpiAll
    <S extends InpeDpiMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictInpeDpiAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictInpeDpiAll
    void delete(InpeDpiMedia entity);

    @Override
    @CacheEvictInpeDpiAll
    void deleteAll(Iterable<? extends InpeDpiMedia> entities);

    @Override
    @CacheEvictInpeDpiAll
    void deleteAll();
}
