package org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.DefaultMediaRepository;

public interface DjangoplicityMediaRepository extends DefaultMediaRepository<DjangoplicityMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "djangoCount", "djangoCountByWebsite", "djangoCountIgnoredByWebsite",
            "djangoCountMissing", "djangoCountMissingByWebsite", "djangoCountUploaded", "djangoCountUploadedByWebsite",
            "djangoCountPhashNotNull", "djangoCountPhashNotNullByWebsite" })
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
    @Query("select count(*) from #{#entityName} m where m.id.repoId = ?1")
    long count(String website);

    @Cacheable("djangoCountIgnoredByWebsite")
    @Query("select count(*) from #{#entityName} m where m.ignored = true and m.id.repoId = ?1")
    long countByIgnoredTrue(String website);

    @Override
    @Cacheable("djangoCountMissing")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames)")
    long countMissingInCommons();

    @Cacheable("djangoCountMissingByWebsite")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId = ?1")
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
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.repoId = ?1")
    long countUploadedToCommons(String website);

    @Override
    @Cacheable("djangoCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Cacheable("djangoCountPhashNotNullByWebsite")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where md.phash is not null and m.id.repoId = ?1")
    long countByMetadata_PhashNotNull(String website);

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
    void deleteById(CompositeMediaId id);

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
    @CacheEvictDjangoAll
    @Query("update #{#entityName} m set m.ignored = null, m.ignoredReason = null where m.ignored = true and m.id.repoId = ?1")
    int resetIgnored(String website);
}
