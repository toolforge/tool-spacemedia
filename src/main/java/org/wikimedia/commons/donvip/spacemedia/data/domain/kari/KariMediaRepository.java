package org.wikimedia.commons.donvip.spacemedia.data.domain.kari;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;

public interface KariMediaRepository extends MediaRepository<KariMedia, Integer> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "kariCount", "kariCountIgnored", "kariCountMissing", "kariCountUploaded"})
    @interface CacheEvictKariAll {

    }

    // COUNT

    @Override
    @Cacheable("kariCount")
    long count();

    @Override
    @Cacheable("kariCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("kariCountMissing")
    @Query("select count(*) from #{#entityName} m where not exists elements (m.commonsFileNames)")
    long countMissingInCommons();

    @Override
    @Cacheable("kariCountUploaded")
    @Query("select count(*) from #{#entityName} m where exists elements (m.commonsFileNames)")
    long countUploadedToCommons();

    // FIND

    @Override
    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames)")
    List<KariMedia> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames)")
    Page<KariMedia> findMissingInCommons(Pageable page);

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames)")
    List<KariMedia> findUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames)")
    Page<KariMedia> findUploadedToCommons(Pageable page);

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2")
    List<KariMedia> findDuplicateInCommons();

    // SAVE

    @Override
    @CacheEvictKariAll
    <S extends KariMedia> S save(S entity);

    @Override
    @CacheEvictKariAll
    <S extends KariMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictKariAll
    void deleteById(Integer id);

    @Override
    @CacheEvictKariAll
    void delete(KariMedia entity);

    @Override
    @CacheEvictKariAll
    void deleteAll(Iterable<? extends KariMedia> entities);

    @Override
    @CacheEvictKariAll
    void deleteAll();
}
