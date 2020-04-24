package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;

public interface DvidsMediaRepository<T extends DvidsMedia>
        extends MediaRepository<T, DvidsMediaTypedId, ZonedDateTime> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "dvidsCount", "dvidsCountByUnit", "dvidsCountIgnored", "dvidsCountIgnoredByUnit",
            "dvidsCountMissing", "dvidsCountMissingByUnit", "dvidsCountUploaded", "dvidsCountUploadedByUnit" })
    @interface CacheEvictDvidsAll {

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
    @Query("select count(*) from #{#entityName} m where not exists elements (m.commonsFileNames)")
    long countMissingInCommons();

    @Cacheable("dvidsCountMissingByUnit")
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames) and m.unit in ?1")
    long countMissingInCommons(Set<String> units);

    @Override
    @Cacheable("dvidsCountUploaded")
    @Query("select count(*) from #{#entityName} m where exists elements (m.commonsFileNames)")
    long countUploadedToCommons();

    @Cacheable("dvidsCountUploadedByUnit")
    @Query("select count(*) from #{#entityName} m where exists elements (m.commonsFileNames) and m.unit in ?1")
    long countUploadedToCommons(Set<String> units);

    // FIND

    @Query("select m from #{#entityName} m where m.unit in ?1")
    Set<T> findAll(Set<String> units);

    @Query("select m from #{#entityName} m where m.unit in ?1")
    Page<T> findAll(Set<String> units, Pageable page);

    @Query("select m from #{#entityName} m where m.ignored = true and m.unit in ?1")
    List<T> findByIgnoredTrue(Set<String> units);

    @Query("select m from #{#entityName} m where m.ignored = true and m.unit in ?1")
    Page<T> findByIgnoredTrue(Set<String> units, Pageable page);

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2")
    List<T> findDuplicateInCommons();

    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2 and m.unit in ?1")
    List<T> findDuplicateInCommons(Set<String> units);

    @Override
    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames)")
    List<T> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames)")
    Page<T> findMissingInCommons(Pageable page);

    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames) and m.unit in ?1")
    List<T> findMissingInCommons(Set<String> units);

    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames) and m.unit in ?1")
    Page<T> findMissingInCommons(Set<String> units, Pageable page);

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames)")
    List<T> findUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames)")
    Page<T> findUploadedToCommons(Pageable page);

    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames) and m.unit in ?1")
    List<T> findUploadedToCommons(Set<String> units);

    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames) and m.unit in ?1")
    Page<T> findUploadedToCommons(Set<String> units, Pageable page);

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
    void deleteById(DvidsMediaTypedId id);

    @Override
    @CacheEvictDvidsAll
    void delete(T entity);

    @Override
    @CacheEvictDvidsAll
    void deleteAll(Iterable<? extends T> entities);

    @Override
    @CacheEvictDvidsAll
    void deleteAll();
}
