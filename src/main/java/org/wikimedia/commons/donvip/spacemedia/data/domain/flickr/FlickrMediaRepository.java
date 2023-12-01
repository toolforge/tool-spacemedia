package org.wikimedia.commons.donvip.spacemedia.data.domain.flickr;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface FlickrMediaRepository extends MediaRepository<FlickrMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "flickrCount", "flickrCountByAccount", "flickrCountIgnored", "flickrCountIgnoredByAccount",
            "flickrCountMissing", "flickrCountMissingByAccount", "flickrCountMissingByType",
            "flickrCountMissingByTypeAndAccount", "flickrCountMissingImagesByAccount",
            "flickrCountMissingVideosByAccount", "flickrCountMissingDocumentsByAccount", "flickrCountUploaded",
            "flickrCountUploadedByAccount", "flickrCountPhashNotNull", "flickrCountPhashNotNullByAccount" })
    @interface CacheEvictFlickrAll {

    }

    @Override
    @CacheEvictFlickrAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("flickrCount")
    long count();

    @Override
    @Cacheable("flickrCountByAccount")
    long count(Set<String> flickrAccounts);

    @Override
    @Cacheable("flickrCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("flickrCountIgnoredByAccount")
    long countByIgnoredTrue(Set<String> flickrAccounts);

    @Override
    @Cacheable("flickrCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("flickrCountMissingByAccount")
    long countMissingInCommons(Set<String> flickrAccounts);

    @Cacheable("flickrCountMissingByType")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored = false) and not exists elements (md.commonsFileNames) and m.media = ?1")
    long countMissingInCommons(FlickrMediaType type);

    @Cacheable("flickrCountMissingByTypeAndAccount")
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored = false) and not exists elements (md.commonsFileNames) and m.media = ?1 and m.id.repoId in ?2")
    long countMissingInCommons(FlickrMediaType type, Set<String> flickrAccounts);

    @Override
    @Cacheable("flickrCountMissingImagesByAccount")
    default long countMissingImagesInCommons(Set<String> flickrAccounts) {
        return countMissingInCommons(FlickrMediaType.photo, flickrAccounts);
    }

    @Override
    @Cacheable("flickrCountMissingVideosByAccount")
    default long countMissingVideosInCommons(Set<String> flickrAccounts) {
        return countMissingInCommons(FlickrMediaType.video, flickrAccounts);
    }

    @Override
    @Cacheable("flickrCountMissingDocumentsByAccount")
    long countMissingDocumentsInCommons(Set<String> flickrAccounts);

    @Override
    @Cacheable("flickrCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("flickrCountUploadedByAccount")
    long countUploadedToCommons(Set<String> flickrAccounts);

    @Override
    @Cacheable("flickrCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("flickrCountPhashNotNullByAccount")
    long countByMetadata_PhashNotNull(Set<String> flickrAccounts);

    // SAVE

    @Override
    @CacheEvictFlickrAll
    <S extends FlickrMedia> S save(S entity);

    @Override
    @CacheEvictFlickrAll
    <S extends FlickrMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictFlickrAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictFlickrAll
    void delete(FlickrMedia entity);

    @Override
    @CacheEvictFlickrAll
    void deleteAll(Iterable<? extends FlickrMedia> entities);

    @Override
    @CacheEvictFlickrAll
    void deleteAll();

    // UPDATE

    @Override
    @CacheEvictFlickrAll
    int resetIgnored();

    @Override
    @CacheEvictFlickrAll
    int resetIgnored(Set<String> flickrAccounts);
}
