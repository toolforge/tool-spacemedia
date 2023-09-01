package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaPhotojournalMediaRepository extends MediaRepository<NasaPhotojournalMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "nasaPjCount", "nasaPjCountIgnored", "nasaPjCountMissing",
            "nasaPjCountMissingImages", "nasaPjCountMissingVideos", "nasaPjCountUploaded" })
    @interface CacheEvictNasaPhotojournalAll {

    }

    @Override
    @CacheEvictNasaPhotojournalAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaPjCount")
    long count();

    @Override
    @Cacheable("nasaPjCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("nasaPjCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaPjCountMissingImages")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("nasaPjCountMissingVideos")
    long countMissingVideosInCommons();

    @Override
    @Cacheable("nasaPjCountUploaded")
    long countUploadedToCommons();

    // SAVE

    @Override
    @CacheEvictNasaPhotojournalAll
    <S extends NasaPhotojournalMedia> S save(S entity);

    @Override
    @CacheEvictNasaPhotojournalAll
    <S extends NasaPhotojournalMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaPhotojournalAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictNasaPhotojournalAll
    void delete(NasaPhotojournalMedia entity);

    @Override
    @CacheEvictNasaPhotojournalAll
    void deleteAll(Iterable<? extends NasaPhotojournalMedia> entities);

    @Override
    @CacheEvictNasaPhotojournalAll
    void deleteAll();
}
