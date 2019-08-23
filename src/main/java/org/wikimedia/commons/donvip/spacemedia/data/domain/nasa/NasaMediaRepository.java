package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;

public interface NasaMediaRepository<T extends NasaMedia> extends MediaRepository<T, String> {

    @CacheEvict(allEntries = true, cacheNames = {
            "nasaCount", "nasaCountByCenter", "nasaCountMissing", "nasaCountMissingByCenter", "nasaCenters"})
    @interface CacheEvictNasaAll {

    }

    // COUNT

    @Override
    @Cacheable("nasaCount")
    long count();

    @Cacheable("nasaCountByCenter")
    long countByCenter(String center);

    @Cacheable("nasaCountIgnoredByCenter")
    @Query("select count(*) from #{#entityName} m where m.ignored is true and m.center = ?1")
    long countIgnoredByCenter(String center);

    @Override
    @Cacheable("nasaCountMissing")
    @Query("select count(*) from #{#entityName} m where not exists elements (m.commonsFileNames)")
    long countMissingInCommons();

    @Cacheable("nasaCountMissingByCenter")
    @Query("select count(*) from #{#entityName} m where not exists elements (m.commonsFileNames) and m.center = ?1")
    long countMissingInCommonsByCenter(String center);

    // CUSTOM

    @Cacheable("nasaCenters")
    @Query("select distinct(center) from #{#entityName}")
    List<String> findCenters();

    // FIND

    List<T> findByCenter(String center);

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2")
    List<T> findDuplicateInCommons();

    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2 and m.center = ?1")
    List<T> findDuplicateInCommonsByCenter(String center);

    @Override
    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames)")
    List<T> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames)")
    Page<T> findMissingInCommons(Pageable page);

    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames) and m.center = ?1")
    List<T> findMissingInCommonsByCenter(String center);

    // SAVE

    @Override
    @CacheEvictNasaAll
    <S extends T> S save(S entity);

    @Override
    @CacheEvictNasaAll
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaAll
    void deleteById(String id);

    @Override
    @CacheEvictNasaAll
    void delete(T entity);

    @Override
    @CacheEvictNasaAll
    void deleteAll(Iterable<? extends T> entities);

    @Override
    @CacheEvictNasaAll
    void deleteAll();
}
