package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMediaType;

public interface NasaAsterMediaRepository extends MediaRepository<NasaAsterMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaAsterCount", "nasaAsterCountRepo", "nasaAsterCountIgnored", "nasaAsterCountIgnoredRepo",
            "nasaAsterCountMissing", "nasaAsterCountMissingRepo",
            "nasaAsterCountMissingImages",
            "nasaAsterCountUploaded", "nasaAsterCountUploadedRepo", "nasaAsterCountPhashNotNull",
            "nasaAsterCountPhashNotNullRepo" })
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
    @Cacheable("nasaAsterCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("nasaAsterCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("nasaAsterCountIgnoredRepo")
    long countByIgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("nasaAsterCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaAsterCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

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
    long countUploadedToCommons();

    @Override
    @Cacheable("nasaAsterCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("nasaAsterCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("nasaAsterCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // FIND

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
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictNasaAsterAll
    void delete(NasaAsterMedia entity);

    @Override
    @CacheEvictNasaAsterAll
    void deleteAll(Iterable<? extends NasaAsterMedia> entities);

    @Override
    @CacheEvictNasaAsterAll
    void deleteAll();

    // UPDATE

    @Override
    @CacheEvictNasaAsterAll
    int resetIgnored();

    @Override
    @CacheEvictNasaAsterAll
    int resetIgnored(Set<String> repos);
}
