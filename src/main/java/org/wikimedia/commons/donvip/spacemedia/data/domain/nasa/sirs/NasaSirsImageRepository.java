package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs;

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

public interface NasaSirsImageRepository extends MediaRepository<NasaSirsImage, String, LocalDate> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaSirsCount", "nasaSirsCountIgnored", "nasaSirsCountMissing", "nasaSirsCountMissingImages",
            "nasaSirsCountUploaded", "nasaSirsFindByPhashNotNull" })
    @interface CacheEvictNasaSirsAll {

    }

    // COUNT

    @Override
    @Cacheable("nasaSirsCount")
    long count();

    @Override
    @Cacheable("nasaSirsCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("nasaSirsCountMissing")
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames)")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaSirsCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons();
    }

    @Override
    default long countMissingVideosInCommons() {
        return 0;
    }

    @Override
    @Cacheable("nasaSirsCountUploaded")
    @Query("select count(*) from #{#entityName} m where exists elements (m.commonsFileNames)")
    long countUploadedToCommons();

    // FIND

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames)")
    List<NasaSirsImage> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames)")
    Page<NasaSirsImage> findMissingInCommons(Pageable page);

    @Override
    default Page<NasaSirsImage> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommons(page);
    }

    @Override
    default Page<NasaSirsImage> findMissingVideosInCommons(Pageable page) {
        return Page.empty();
    }

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames)")
    List<NasaSirsImage> findUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames)")
    Page<NasaSirsImage> findUploadedToCommons(Pageable page);

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2")
    List<NasaSirsImage> findDuplicateInCommons();

    @Override
    @Cacheable("nasaSirsFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    // SAVE

    @Override
    @CacheEvictNasaSirsAll
    <S extends NasaSirsImage> S save(S entity);

    @Override
    @CacheEvictNasaSirsAll
    <S extends NasaSirsImage> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaSirsAll
    void deleteById(String id);

    @Override
    @CacheEvictNasaSirsAll
    void delete(NasaSirsImage entity);

    @Override
    @CacheEvictNasaSirsAll
    void deleteAll(Iterable<? extends NasaSirsImage> entities);

    @Override
    @CacheEvictNasaSirsAll
    void deleteAll();
}
