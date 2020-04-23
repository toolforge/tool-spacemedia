package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;

public interface DvidsMediaRepository<T extends DvidsMedia>
        extends MediaRepository<T, DvidsMediaTypedId, ZonedDateTime> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "dvidsCount", "dvidsCountIgnored", "dvidsCountMissing", "dvidsCountUploaded" })
    @interface CacheEvictDvidsAll {

    }

    // COUNT

    @Override
    @Cacheable("dvidsCount")
    long count();

    @Override
    @Cacheable("dvidsCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("dvidsCountMissing")
    @Query("select count(*) from #{#entityName} m where not exists elements (m.commonsFileNames)")
    long countMissingInCommons();

    @Override
    @Cacheable("dvidsCountUploaded")
    @Query("select count(*) from #{#entityName} m where exists elements (m.commonsFileNames)")
    long countUploadedToCommons();

    // FIND

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2")
    List<T> findDuplicateInCommons();

    @Override
    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames)")
    List<T> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames)")
    Page<T> findMissingInCommons(Pageable page);

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames)")
    List<T> findUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames)")
    Page<T> findUploadedToCommons(Pageable page);

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
