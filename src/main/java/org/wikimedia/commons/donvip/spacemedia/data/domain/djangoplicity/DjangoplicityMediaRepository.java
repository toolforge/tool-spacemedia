package org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository.CacheEvictDvidsAll;

public interface DjangoplicityMediaRepository
        extends MediaRepository<DjangoplicityMedia, DjangoplicityMediaId, LocalDateTime> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "djangoCount", "djangoCountByWebsite", "djangoCountIgnoredByWebsite",
            "djangoCountMissing", "djangoCountMissingByWebsite", "djangoCountUploaded", "djangoCountUploadedByWebsite",
            "djangoCountPhashNotNull", "djangoCountPhashNotNullByWebsite", "djangoFindByPhashNotNull" })
    @interface CacheEvictDjangoAll {

    }

    @Override
    @CacheEvictDjangoAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("djangoCount")
    long count();

    @Cacheable("djangoCountByWebsite")
    @Query("select count(*) from #{#entityName} m where m.id.website = ?1")
    long count(String website);

    @Cacheable("djangoCountIgnoredByWebsite")
    @Query("select count(*) from #{#entityName} m where m.ignored = true and m.id.website = ?1")
    long countByIgnoredTrue(String website);

    @Override
    @Cacheable("djangoCountMissing")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames)")
    long countMissingInCommons();

    @Cacheable("djangoCountMissingByWebsite")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.website = ?1")
    long countMissingInCommons(String website);

    @Override
    default long countMissingImagesInCommons() {
        return countMissingInCommons();
    }

    default long countMissingImagesInCommons(String website) {
        return countMissingInCommons(website);
    }

    @Override
    @Query(value = "select 0", nativeQuery = true)
    long countMissingVideosInCommons();

    @Override
    @Cacheable("djangoCountUploaded")
    long countUploadedToCommons();

    @Cacheable("djangoCountUploadedByWebsite")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.website = ?1")
    long countUploadedToCommons(String website);

    @Override
    @Cacheable("djangoCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Cacheable("djangoCountPhashNotNullByWebsite")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where md.phash is not null and m.id.website = ?1")
    long countByMetadata_PhashNotNull(String website);

    // FIND

    @Query("select m from #{#entityName} m where m.id.website = ?1")
    Set<DjangoplicityMedia> findAll(String website);

    @Query("select m from #{#entityName} m where m.id.website = ?1")
    Page<DjangoplicityMedia> findAll(String website, Pageable page);

    @Query("select m from #{#entityName} m where m.id.website = ?1 and m.id not in ?2")
    Set<DjangoplicityMedia> findNotIn(String website, Set<DjangoplicityMediaId> ids);

    @Query("select m from #{#entityName} m where m.ignored = true and m.id.website = ?1")
    List<DjangoplicityMedia> findByIgnoredTrue(String website);

    @Query("select m from #{#entityName} m where m.ignored = true and m.id.website = ?1")
    Page<DjangoplicityMedia> findByIgnoredTrue(String website, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.website = ?1")
    List<DjangoplicityMedia> findMissingInCommons(String website);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.website = ?1")
    Page<DjangoplicityMedia> findMissingInCommons(String website, Pageable page);

    @Override
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames)")
    Page<DjangoplicityMedia> findMissingInCommons(Pageable page);

    @Override
    default Page<DjangoplicityMedia> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommons(page);
    }

    default Page<DjangoplicityMedia> findMissingImagesInCommons(String website, Pageable page) {
        return findMissingInCommons(website, page);
    }

    @Override
    default Page<DjangoplicityMedia> findMissingVideosInCommons(Pageable page) {
        return Page.empty();
    }

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.website = ?1")
    List<DjangoplicityMedia> findUploadedToCommons(String website);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.website = ?1")
    Page<DjangoplicityMedia> findUploadedToCommons(String website, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where size (md.commonsFileNames) >= 2 and m.id.website = ?1")
    List<DjangoplicityMedia> findDuplicateInCommons(String website);

    @Override
    @Cacheable("flickrFindByPhashNotNull")
    List<MediaProjection<DjangoplicityMediaId>> findByMetadata_PhashNotNull();

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.phash is not null and m.id.website = ?1")
    Page<DjangoplicityMedia> findByMetadata_PhashNotNull(String website, Pageable page);

    // SAVE

    @Override
    @CacheEvictDjangoAll
    <S extends DjangoplicityMedia> S save(S entity);

    @Override
    @CacheEvictDjangoAll
    <S extends DjangoplicityMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictDjangoAll
    void deleteById(DjangoplicityMediaId id);

    @Override
    @CacheEvictDjangoAll
    void delete(DjangoplicityMedia entity);

    @Override
    @CacheEvictDjangoAll
    void deleteAll(Iterable<? extends DjangoplicityMedia> entities);

    @Override
    @CacheEvictDjangoAll
    void deleteAll();

    // UPDATE

    @Modifying
    @CacheEvictDvidsAll
    @Query("update #{#entityName} m set m.ignored = null, m.ignoredReason = null where m.ignored = true and m.id.website = ?1")
    int resetIgnored(String website);
}
