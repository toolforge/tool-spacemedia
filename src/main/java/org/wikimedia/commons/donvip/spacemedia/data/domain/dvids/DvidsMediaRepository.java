package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Modifying;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface DvidsMediaRepository<T extends DvidsMedia> extends MediaRepository<T> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "dvidsCount", "dvidsCountByUnit", "dvidsCountIgnored", "dvidsCountIgnoredByUnit",
            "dvidsCountMissing", "dvidsCountMissingImages", "dvidsCountMissingVideos", "dvidsCountMissingImagesByUnit",
            "dvidsCountMissingVideosByUnit", "dvidsCountMissingByUnit", "dvidsCountUploaded",
            "dvidsCountUploadedByUnit", "dvidsCountPhashNotNull", "dvidsCountPhashNotNullByAccount" })
    @interface CacheEvictDvidsAll {

    }

    @Override
    @CacheEvictDvidsAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("dvidsCount")
    long count();

    @Override
    @Cacheable("dvidsCountByUnit")
    long count(Set<String> units);

    @Override
    @Cacheable("dvidsCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("dvidsCountIgnoredByUnit")
    long countByIgnoredTrue(Set<String> units);

    @Override
    @Cacheable("dvidsCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("dvidsCountMissingByUnit")
    long countMissingInCommons(Set<String> units);

    @Override
    @Cacheable("dvidsCountMissingImages")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("dvidsCountMissingVideos")
    long countMissingVideosInCommons();

    @Override
    @Cacheable("dvidsCountMissingImagesByUnit")
    long countMissingImagesInCommons(Set<String> units);

    @Override
    @Cacheable("dvidsCountMissingVideosByUnit")
    long countMissingVideosInCommons(Set<String> units);

    @Override
    @Cacheable("dvidsCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("dvidsCountUploadedByUnit")
    long countUploadedToCommons(Set<String> units);

    @Override
    @Cacheable("dvidsCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("dvidsCountPhashNotNullByAccount")
    long countByMetadata_PhashNotNull(Set<String> units);

    // SAVE

    @Override
    @CacheEvictDvidsAll
    <S extends T> S save(S entity);

    @Override
    @CacheEvictDvidsAll
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictDvidsAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictDvidsAll
    void delete(T entity);

    @Override
    @CacheEvictDvidsAll
    void deleteAll(Iterable<? extends T> entities);

    @Override
    @CacheEvictDvidsAll
    void deleteAll();

    // UPDATE

    @Override
    @CacheEvictDvidsAll
    int resetIgnored();

    @Override
    @Modifying
    @CacheEvictDvidsAll
    int resetIgnored(Set<String> units);
}
