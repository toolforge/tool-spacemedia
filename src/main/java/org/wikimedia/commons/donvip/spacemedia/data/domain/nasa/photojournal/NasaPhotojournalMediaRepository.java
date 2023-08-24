package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaPhotojournalMediaRepository extends MediaRepository<NasaPhotojournalMedia, String> {

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
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg')")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("nasaPjCountMissingVideos")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg')")
    long countMissingVideosInCommons();

    @Override
    @Cacheable("nasaPjCountUploaded")
    long countUploadedToCommons();

    // FIND

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg')")
    Page<NasaPhotojournalMedia> findMissingImagesInCommons(Pageable page);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg')")
    Page<NasaPhotojournalMedia> findMissingVideosInCommons(Pageable page);

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
