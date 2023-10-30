package org.wikimedia.commons.donvip.spacemedia.data.domain.s3;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface S3MediaRepository extends MediaRepository<S3Media> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "s3Count", "s3CountByShare", "s3CountIgnored",
            "s3CountIgnoredByShare", "s3CountMissing", "s3CountMissingImagesByShare", "s3CountMissingVideosByShare",
            "s3CountMissingDocumentsByShare", "s3CountMissingByShare", "s3CountUploaded",
            "s3CountUploadedByShare", "s3CountPhashNotNull", "s3CountPhashNotNullByShare" })
    @interface CacheEvictS3All {

    }

    @Override
    @CacheEvictS3All
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("s3Count")
    long count();

    @Override
    @Cacheable("s3CountByShare")
    long count(Set<String> buckets);

    @Override
    @Cacheable("s3CountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("s3CountIgnoredByShare")
    long countByIgnoredTrue(Set<String> buckets);

    @Override
    @Cacheable("s3CountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("s3CountMissingByShare")
    long countMissingInCommons(Set<String> buckets);

    @Override
    @Cacheable("s3CountMissingImagesByShare")
    long countMissingImagesInCommons(Set<String> buckets);

    @Override
    @Cacheable("s3CountMissingVideosByShare")
    long countMissingVideosInCommons(Set<String> buckets);

    @Override
    @Cacheable("s3CountMissingDocumentsByShare")
    long countMissingDocumentsInCommons(Set<String> buckets);

    @Override
    @Cacheable("s3CountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("s3CountUploadedByShare")
    long countUploadedToCommons(Set<String> buckets);

    @Override
    @Cacheable("s3CountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("s3CountPhashNotNullByShare")
    long countByMetadata_PhashNotNull(Set<String> buckets);

    // SAVE

    @Override
    @CacheEvictS3All
    <S extends S3Media> S save(S entity);

    @Override
    @CacheEvictS3All
    <S extends S3Media> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictS3All
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictS3All
    void delete(S3Media entity);

    @Override
    @CacheEvictS3All
    void deleteAll(Iterable<? extends S3Media> entities);

    @Override
    @CacheEvictS3All
    void deleteAll();

    // UPDATE

    @Override
    @CacheEvictS3All
    int resetIgnored();

    @Override
    @CacheEvictS3All
    int resetIgnored(Set<String> buckets);
}
