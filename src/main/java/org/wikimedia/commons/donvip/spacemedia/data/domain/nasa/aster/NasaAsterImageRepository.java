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

public interface NasaAsterImageRepository extends MediaRepository<NasaAsterImage, String, LocalDate> {

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

    @Override
    @Cacheable("nasaAsterCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons();
    }

    @Override
    default long countMissingVideosInCommons() {
        return 0;
    }

    @Override
    @Cacheable("nasaAsterCountUploaded")
    @Query("select count(*) from #{#entityName} m where exists elements (m.metadata.commonsFileNames)")
    long countUploadedToCommons();

    // FIND

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.metadata.commonsFileNames)")
    List<NasaAsterImage> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.metadata.commonsFileNames)")
    Page<NasaAsterImage> findMissingInCommons(Pageable page);

    @Override
    default Page<NasaAsterImage> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommons(page);
    }

    @Override
    default Page<NasaAsterImage> findMissingVideosInCommons(Pageable page) {
        return Page.empty();
    }

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.metadata.commonsFileNames)")
    List<NasaAsterImage> findUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.metadata.commonsFileNames)")
    Page<NasaAsterImage> findUploadedToCommons(Pageable page);

    @Override
    @Query("select m from #{#entityName} m where size (m.metadata.commonsFileNames) >= 2")
    List<NasaAsterImage> findDuplicateInCommons();

    @Override
    @Cacheable("nasaAsterFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    // SAVE

    @Override
    @CacheEvictNasaAsterAll
    <S extends NasaAsterImage> S save(S entity);

    @Override
    @CacheEvictNasaAsterAll
    <S extends NasaAsterImage> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaAsterAll
    void deleteById(String id);

    @Override
    @CacheEvictNasaAsterAll
    void delete(NasaAsterImage entity);

    @Override
    @CacheEvictNasaAsterAll
    void deleteAll(Iterable<? extends NasaAsterImage> entities);

    @Override
    @CacheEvictNasaAsterAll
    void deleteAll();
}
