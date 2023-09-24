package org.wikimedia.commons.donvip.spacemedia.data.domain.youtube;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface YouTubeMediaRepository extends MediaRepository<YouTubeMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "youtubeCount", "youtubeCountByChannel",
            "youtubeCountIgnored", "youtubeCountIgnoredByChannel", "youtubeCountMissing",
            "youtubeCountMissingByChannel",
            "youtubeCountUploaded", "youtubeCountUploadedByChannel", "youtubeCountPhashNotNull",
            "youtubeCountPhashNotNullByChannel" })
    @interface CacheEvictYouTubeAll {

    }

    @Override
    @CacheEvictYouTubeAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("youtubeCount")
    long count();

    @Override
    @Cacheable("youtubeCountByChannel")
    long count(Set<String> youtubeChannels);

    @Override
    @Cacheable("youtubeCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("youtubeCountIgnoredByChannel")
    long countByIgnoredTrue(Set<String> youtubeChannels);

    @Override
    @Cacheable("youtubeCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("youtubeCountMissingByChannel")
    long countMissingInCommons(Set<String> youtubeChannels);

    @Override
    default long countMissingImagesInCommons() {
        return 0;
    }

    @Override
    @Cacheable("youtubeCountMissingVideos")
    default long countMissingVideosInCommons() {
        return countMissingInCommons();
    }

    @Override
    @Cacheable("youtubeCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("youtubeCountUploadedByChannel")
    long countUploadedToCommons(Set<String> youtubeChannels);

    @Override
    @Cacheable("youtubeCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("youtubeCountPhashNotNullByChannel")
    long countByMetadata_PhashNotNull(Set<String> youtubeChannels);

    // FIND

    @Override
    default Page<YouTubeMedia> findMissingImagesInCommons(Pageable page) {
        return Page.empty();
    }

    @Override
    default Page<YouTubeMedia> findMissingVideosInCommons(Pageable page) {
        return findMissingInCommons(page);
    }

    // SAVE

    @Override
    @CacheEvictYouTubeAll
    <S extends YouTubeMedia> S save(S entity);

    @Override
    @CacheEvictYouTubeAll
    <S extends YouTubeMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictYouTubeAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictYouTubeAll
    void delete(YouTubeMedia entity);

    @Override
    @CacheEvictYouTubeAll
    void deleteAll(Iterable<? extends YouTubeMedia> entities);

    @Override
    @CacheEvictYouTubeAll
    void deleteAll();

    // UPDATE

    @Override
    @CacheEvictYouTubeAll
    int resetIgnored();

    @Override
    @CacheEvictYouTubeAll
    int resetIgnored(Set<String> repos);
}
