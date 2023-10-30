package org.wikimedia.commons.donvip.spacemedia.data.domain.box;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface BoxMediaRepository extends MediaRepository<BoxMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "boxCount", "boxCountByShare", "boxCountIgnored",
            "boxCountIgnoredByShare", "boxCountMissing", "boxCountMissingByShare",
            "boxCountMissingImagesByShare", "boxCountMissingVideosByShare", "boxCountMissingDocumentsByShare",
            "boxCountUploaded", "boxCountUploadedByShare", "boxCountPhashNotNull", "boxCountPhashNotNullByShare" })
    @interface CacheEvictBoxAll {

    }

    @Override
    @CacheEvictBoxAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("boxCount")
    long count();

    @Override
    @Cacheable("boxCountByShare")
    long count(Set<String> appShares);

    @Override
    @Cacheable("boxCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("boxCountIgnoredByShare")
    long countByIgnoredTrue(Set<String> appShares);

    @Override
    @Cacheable("boxCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("boxCountMissingByShare")
    long countMissingInCommons(Set<String> appShares);

    @Override
    @Cacheable("boxCountMissingImagesByShare")
    long countMissingImagesInCommons(Set<String> appShares);

    @Override
    @Cacheable("boxCountMissingVideosByShare")
    long countMissingVideosInCommons(Set<String> appShares);

    @Override
    @Cacheable("boxCountMissingDocumentsByShare")
    long countMissingDocumentsInCommons(Set<String> appShares);

    @Override
    @Cacheable("boxCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("boxCountUploadedByShare")
    long countUploadedToCommons(Set<String> appShares);

    @Override
    @Cacheable("boxCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("boxCountPhashNotNullByShare")
    long countByMetadata_PhashNotNull(Set<String> appShares);

    // SAVE

    @Override
    @CacheEvictBoxAll
    <S extends BoxMedia> S save(S entity);

    @Override
    @CacheEvictBoxAll
    <S extends BoxMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictBoxAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictBoxAll
    void delete(BoxMedia entity);

    @Override
    @CacheEvictBoxAll
    void deleteAll(Iterable<? extends BoxMedia> entities);

    @Override
    @CacheEvictBoxAll
    void deleteAll();

    // UPDATE

    @Override
    @CacheEvictBoxAll
    int resetIgnored();

    @Override
    @CacheEvictBoxAll
    int resetIgnored(Set<String> appShares);
}
