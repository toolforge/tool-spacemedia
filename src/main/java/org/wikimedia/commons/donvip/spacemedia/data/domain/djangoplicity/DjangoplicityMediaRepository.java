package org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface DjangoplicityMediaRepository extends MediaRepository<DjangoplicityMedia> {

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

    @Override
    @Cacheable("djangoCountByWebsite")
    long count(Set<String> websites);

    @Override
    @Cacheable("djangoCountIgnoredByWebsite")
    long countByIgnoredTrue(Set<String> websites);

    @Override
    @Cacheable("djangoCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("djangoCountMissingByWebsite")
    long countMissingInCommons(Set<String> websites);

    @Override
    default long countMissingImagesInCommons() {
        return countMissingInCommons();
    }

    @Override
    default long countMissingImagesInCommons(Set<String> websites) {
        return countMissingInCommons(websites);
    }

    @Override
    @Query(value = "select 0", nativeQuery = true)
    long countMissingVideosInCommons();

    @Override
    @Cacheable("djangoCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("djangoCountUploadedByWebsite")
    long countUploadedToCommons(Set<String> websites);

    @Override
    @Cacheable("djangoCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("djangoCountPhashNotNullByWebsite")
    long countByMetadata_PhashNotNull(Set<String> websites);

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

    @Override
    @CacheEvictDjangoAll
    int resetIgnored();

    @Override
    @CacheEvictDjangoAll
    int resetIgnored(Set<String> websites);
}
