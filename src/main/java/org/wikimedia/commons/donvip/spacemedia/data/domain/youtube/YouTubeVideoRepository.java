package org.wikimedia.commons.donvip.spacemedia.data.domain.youtube;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface YouTubeVideoRepository extends MediaRepository<YouTubeVideo, String> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "youtubeCount", "youtubeCountByChannel",
            "youtubeCountIgnoredByChannel", "youtubeCountMissing", "youtubeCountMissingByChannel",
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

    @Cacheable("youtubeCountByChannel")
    @Query("select count(*) from #{#entityName} m where m.channelId in ?1")
    long count(Set<String> youtubeChannels);

    @Cacheable("youtubeCountIgnoredByChannel")
    @Query("select count(*) from #{#entityName} m where m.ignored = true and m.channelId in ?1")
    long countByIgnoredTrue(Set<String> youtubeChannels);

    @Override
    @Cacheable("youtubeCountMissing")
    long countMissingInCommons();

    @Cacheable("youtubeCountMissingByChannel")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.channelId in ?1")
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

    @Cacheable("youtubeCountUploadedByChannel")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.channelId in ?1")
    long countUploadedToCommons(Set<String> youtubeChannels);

    @Override
    @Cacheable("youtubeCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Cacheable("youtubeCountPhashNotNullByChannel")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where md.phash is not null and m.channelId in ?1")
    long countByMetadata_PhashNotNull(Set<String> youtubeChannels);

    // FIND

    Set<YouTubeVideo> findByChannelIdIn(Set<String> youtubeChannels);

    Page<YouTubeVideo> findByChannelIdIn(Set<String> youtubeChannels, Pageable page);

    Set<YouTubeVideo> findByChannelIdAndIdNotIn(String youtubeChannel, Collection<String> ids);

    @Query("select m from #{#entityName} m where m.ignored = true and m.channelId in ?1")
    List<YouTubeVideo> findByIgnoredTrue(Set<String> youtubeChannels);

    @Query("select m from #{#entityName} m where m.ignored = true and m.channelId in ?1")
    Page<YouTubeVideo> findByIgnoredTrue(Set<String> youtubeChannels, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.channelId in ?1")
    List<YouTubeVideo> findMissingInCommons(Set<String> youtubeChannels);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.channelId in ?1")
    Page<YouTubeVideo> findMissingInCommons(Set<String> youtubeChannels, Pageable page);

    @Override
    default Page<YouTubeVideo> findMissingImagesInCommons(Pageable page) {
        return Page.empty();
    }

    @Override
    default Page<YouTubeVideo> findMissingVideosInCommons(Pageable page) {
        return findMissingInCommons(page);
    }

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.channelId in ?1 and (m.creationDate = ?2 or m.publicationDate = ?2)")
    List<YouTubeVideo> findMissingInCommonsByDate(Set<String> youtubeChannels, LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.channelId in ?1 and m.title = ?2")
    List<YouTubeVideo> findMissingInCommonsByTitle(Set<String> youtubeChannels, String title);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.channelId in ?1")
    List<YouTubeVideo> findUploadedToCommons(Set<String> youtubeChannels);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.channelId in ?1")
    Page<YouTubeVideo> findUploadedToCommons(Set<String> youtubeChannels, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where size (md.commonsFileNames) >= 2 and m.channelId in ?1")
    List<YouTubeVideo> findDuplicateInCommons(Set<String> youtubeChannels);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.phash is not null and m.channelId in ?1")
    Page<YouTubeVideo> findByMetadata_PhashNotNull(Set<String> youtubeChannels, Pageable page);

    // SAVE

    @Override
    @CacheEvictYouTubeAll
    <S extends YouTubeVideo> S save(S entity);

    @Override
    @CacheEvictYouTubeAll
    <S extends YouTubeVideo> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictYouTubeAll
    void deleteById(String id);

    @Override
    @CacheEvictYouTubeAll
    void delete(YouTubeVideo entity);

    @Override
    @CacheEvictYouTubeAll
    void deleteAll(Iterable<? extends YouTubeVideo> entities);

    @Override
    @CacheEvictYouTubeAll
    void deleteAll();
}
