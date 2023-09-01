package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.DefaultMediaRepository;

public interface DvidsMediaRepository<T extends DvidsMedia> extends DefaultMediaRepository<T> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "dvidsCount", "dvidsCountByUnit", "dvidsCountIgnored", "dvidsCountIgnoredByUnit",
            "dvidsCountMissing", "dvidsCountMissingImages", "dvidsCountMissingVideos", "dvidsCountMissingImagesByUnit",
            "dvidsCountMissingVideosByUnit", "dvidsCountMissingByUnit", "dvidsCountMissingByType",
            "dvidsCountUploaded", "dvidsCountUploadedByUnit", "dvidsCountPhashNotNull",
            "dvidsCountPhashNotNullByAccount" })
    @interface CacheEvictDvidsAll {

    }

    @Override
    @CacheEvictDvidsAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("dvidsCount")
    long count();

    @Cacheable("dvidsCountByUnit")
    @Query("select count(*) from #{#entityName} m where m.unit in ?1")
    long count(Set<String> units);

    @Override
    @Cacheable("dvidsCountIgnored")
    long countByIgnoredTrue();

    @Cacheable("dvidsCountIgnoredByUnit")
    @Query("select count(*) from #{#entityName} m where m.ignored = true and m.unit in ?1")
    long countByIgnoredTrue(Set<String> units);

    @Override
    @Cacheable("dvidsCountMissing")
    long countMissingInCommons();

    @Cacheable("dvidsCountMissingByUnit")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.unit in ?1")
    long countMissingInCommonsByUnit(Set<String> units);

    @Cacheable("dvidsCountMissingByType")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    long countMissingInCommonsByType(Set<String> types);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and m.unit in ?2")
    long countMissingInCommonsByTypeAndUnit(Set<String> types, Set<String> units);

    @Override
    @Cacheable("dvidsCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommonsByType(DvidsMediaType.images());
    }

    @Override
    @Cacheable("dvidsCountMissingVideos")
    default long countMissingVideosInCommons() {
        return countMissingInCommonsByType(DvidsMediaType.videos());
    }

    @Cacheable("dvidsCountMissingImagesByUnit")
    default long countMissingImagesInCommons(Set<String> units) {
        return countMissingInCommonsByTypeAndUnit(DvidsMediaType.images(), units);
    }

    @Cacheable("dvidsCountMissingVideosByUnit")
    default long countMissingVideosInCommons(Set<String> units) {
        return countMissingInCommonsByTypeAndUnit(DvidsMediaType.videos(), units);
    }

    @Override
    @Cacheable("dvidsCountUploaded")
    long countUploadedToCommons();

    @Cacheable("dvidsCountUploadedByUnit")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.unit in ?1")
    long countUploadedToCommons(Set<String> units);

    @Override
    @Cacheable("dvidsCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Cacheable("dvidsCountPhashNotNullByAccount")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where md.phash is not null and m.unit in ?1")
    long countByMetadata_PhashNotNull(Set<String> units);

    // FIND

    @Override
    @Query("select m from #{#entityName} m where m.unit in ?1")
    Set<T> findAll(Set<String> units);

    @Override
    @Query("select m from #{#entityName} m where m.unit in ?1")
    Page<T> findAll(Set<String> units, Pageable page);

    @Override
    @Query("select m from #{#entityName} m where m.ignored = true and m.unit in ?1")
    List<T> findByIgnoredTrue(Set<String> units);

    @Override
    @Query("select m from #{#entityName} m where m.ignored = true and m.unit in ?1")
    Page<T> findByIgnoredTrue(Set<String> units, Pageable page);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where size (md.commonsFileNames) >= 2 and m.unit in ?1")
    List<T> findDuplicateInCommons(Set<String> units);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    Page<T> findMissingInCommonsByType(Set<String> types, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and m.unit in ?2")
    Page<T> findMissingInCommonsByTypeAndUnit(Set<String> types, Set<String> units, Pageable page);

    @Override
    default Page<T> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommonsByType(DvidsMediaType.images(), page);
    }

    @Override
    default Page<T> findMissingImagesInCommons(Set<String> units, Pageable page) {
        return findMissingInCommonsByTypeAndUnit(DvidsMediaType.images(), units, page);
    }

    @Override
    default Page<T> findMissingVideosInCommons(Pageable page) {
        return findMissingInCommonsByType(DvidsMediaType.videos(), page);
    }

    @Override
    default Page<T> findMissingVideosInCommons(Set<String> units, Pageable page) {
        return findMissingInCommonsByTypeAndUnit(DvidsMediaType.videos(), units, page);
    }

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.unit in ?1")
    List<T> findMissingInCommonsByUnit(Set<String> units);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.unit in ?1")
    Page<T> findMissingInCommonsByUnit(Set<String> units, Pageable page);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.unit in ?1 and (m.creationDate = ?2 or m.publicationDate = ?2)")
    List<T> findMissingInCommonsByDate(Set<String> units, LocalDate date);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.unit in ?1 and m.title = ?2")
    List<T> findMissingInCommonsByTitle(Set<String> units, String title);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.unit in ?1")
    List<T> findUploadedToCommons(Set<String> units);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.unit in ?1")
    Page<T> findUploadedToCommons(Set<String> units, Pageable page);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.phash is not null and m.unit in ?1")
    Page<T> findByMetadata_PhashNotNull(Set<String> units, Pageable page);

    // SAVE

    @Override
    @CacheEvictDvidsAll
    <S extends T> S save(S entity);

    @Override
    @CacheEvictDvidsAll
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictDvidsAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictDvidsAll
    void delete(T entity);

    @Override
    @CacheEvictDvidsAll
    void deleteAll(Iterable<? extends T> entities);

    @Override
    @CacheEvictDvidsAll
    void deleteAll();

    // UPDATE

    @Modifying
    @CacheEvictDvidsAll
    @Query("update #{#entityName} m set m.ignored = null, m.ignoredReason = null where m.ignored = true and m.unit in ?1")
    int resetIgnored(Set<String> units);
}
