package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaMediaRepository<T extends NasaMedia> extends MediaRepository<T> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaCount", "nasaCountByCenter", "nasaCountIgnored", "nasaCountIgnoredByCenter", "nasaCountMissing",
            "nasaCountMissingByCenter", "nasaCountMissingImagesByCenter", "nasaCountMissingVideosByCenter",
            "nasaCountMissingDocumentsByCenter", "nasaCountUploaded", "nasaCountUploadedByCenter",
            "nasaCountPhashNotNull", "nasaCountPhashNotNullByCenter" })
    @interface CacheEvictNasaAll {

    }

    @Override
    @CacheEvictNasaAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaCount")
    long count();

    @Override
    @Cacheable("nasaCountByCenter")
    long count(Set<String> centers);

    @Override
    @Cacheable("nasaCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("nasaCountPhashNotNullByCenter")
    long countByMetadata_PhashNotNull(Set<String> centers);

    @Override
    @Cacheable("nasaCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("nasaCountIgnoredByCenter")
    long countByMetadata_IgnoredTrue(Set<String> centers);

    @Override
    @Cacheable("nasaCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaCountMissingByCenter")
    long countMissingInCommons(Set<String> centers);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.mediaType = ?1 and m.id.repoId in ?2")
    long countMissingInCommonsByTypeAndCenters(NasaMediaType type, Set<String> centers);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and m.mediaType = ?1 and not exists elements (md.commonsFileNames)")
    long countMissingInCommons(NasaMediaType type);

    @Override
    @Cacheable("nasaCountMissingImagesByCenter")
    default long countMissingImagesInCommons(Set<String> centers) {
        return countMissingInCommonsByTypeAndCenters(NasaMediaType.image, centers);
    }

    @Override
    @Cacheable("nasaCountMissingVideosByCenter")
    default long countMissingVideosInCommons(Set<String> centers) {
        return countMissingInCommonsByTypeAndCenters(NasaMediaType.video, centers);
    }

    @Override
    @Cacheable("nasaCountMissingDocumentsByCenter")
    long countMissingDocumentsInCommons(Set<String> centers);

    @Override
    @Cacheable("nasaCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("nasaCountUploadedByCenter")
    long countUploadedToCommons(Set<String> centers);

    // SAVE

    @Override
    @CacheEvictNasaAll
    <S extends T> S save(S entity);

    @Override
    @CacheEvictNasaAll
    <S extends T> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaAll
    void deleteById(CompositeMediaId id);

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
