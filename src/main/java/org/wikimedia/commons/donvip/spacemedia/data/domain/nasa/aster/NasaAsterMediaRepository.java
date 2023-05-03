package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMediaType;

public interface NasaAsterMediaRepository extends MediaRepository<NasaAsterMedia, String, LocalDate> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaAsterCount", "nasaAsterCountIgnored", "nasaAsterCountMissing", "nasaAsterCountMissingImages",
            "nasaAsterCountUploaded", "nasaAsterFindByPhashNotNull" })
    @interface CacheEvictNasaAsterAll {

    }

    @Override
    @CacheEvictNasaAsterAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaAsterCount")
    long count();

    @Override
    @Cacheable("nasaAsterCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("nasaAsterCountMissing")
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.metadata.commonsFileNames)")
    long countMissingInCommons();

    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and m.mediaType = ?1 and not exists elements (m.metadata.commonsFileNames)")
    long countMissingInCommons(NasaMediaType type);

    @Override
    @Cacheable("nasaAsterCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons(NasaMediaType.image);
    }

    @Override
    @Cacheable("nasaAsterCountMissingVideos")
    default long countMissingVideosInCommons() {
        return countMissingInCommons(NasaMediaType.video);
    }

    @Override
    @Cacheable("nasaAsterCountUploaded")
    @Query("select count(*) from #{#entityName} m where exists elements (m.metadata.commonsFileNames)")
    long countUploadedToCommons();

    // FIND

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.metadata.commonsFileNames)")
    List<NasaAsterMedia> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.metadata.commonsFileNames)")
    Page<NasaAsterMedia> findMissingInCommons(Pageable page);

    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and m.mediaType = ?1 and not exists elements (m.metadata.commonsFileNames)")
    Page<NasaAsterMedia> findMissingInCommonsByType(NasaMediaType type, Pageable page);

    @Override
    default Page<NasaAsterMedia> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommonsByType(NasaMediaType.image, page);
    }

    @Override
    default Page<NasaAsterMedia> findMissingVideosInCommons(Pageable page) {
        return findMissingInCommonsByType(NasaMediaType.video, page);
    }

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.metadata.commonsFileNames)")
    List<NasaAsterMedia> findUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.metadata.commonsFileNames)")
    Page<NasaAsterMedia> findUploadedToCommons(Pageable page);

    @Override
    @Query("select m from #{#entityName} m where size (m.metadata.commonsFileNames) >= 2")
    List<NasaAsterMedia> findDuplicateInCommons();

    @Override
    @Cacheable("nasaAsterFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    // SAVE

    @Override
    @CacheEvictNasaAsterAll
    <S extends NasaAsterMedia> S save(S entity);

    @Override
    @CacheEvictNasaAsterAll
    <S extends NasaAsterMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaAsterAll
    void deleteById(String id);

    @Override
    @CacheEvictNasaAsterAll
    void delete(NasaAsterMedia entity);

    @Override
    @CacheEvictNasaAsterAll
    void deleteAll(Iterable<? extends NasaAsterMedia> entities);

    @Override
    @CacheEvictNasaAsterAll
    void deleteAll();
}