package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaPhotojournalMediaRepository extends MediaRepository<NasaPhotojournalMedia, String, ZonedDateTime> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "nasaPjCount", "nasaPjCountIgnored", "nasaPjCountMissing",
            "nasaPjCountMissingImages", "nasaPjCountMissingVideos", "nasaPjCountUploaded", "nasaPjFindByPhashNotNull" })
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
    default long countMissingImagesInCommons() {
        return countMissingInCommons();
    }

    @Override
    @Cacheable("nasaPjCountMissingVideos")
    default long countMissingVideosInCommons() {
        return 0;
    }

    @Override
    @Cacheable("nasaPjCountUploaded")
    long countUploadedToCommons();

    // FIND

    @Override
    @Cacheable("nasaPjFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    @Override
    default Page<NasaPhotojournalMedia> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommons(page);
    }

    @Override
    default Page<NasaPhotojournalMedia> findMissingVideosInCommons(Pageable page) {
        return Page.empty();
    }

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
    void deleteById(String id);

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
