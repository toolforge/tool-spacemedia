package org.wikimedia.commons.donvip.spacemedia.data.domain.s3;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface S3MediaRepository extends MediaRepository<S3Media> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "s3Count", "s3CountByBucket", "s3CountIgnored",
            "s3CountIgnoredByBucket", "s3CountMissing", "s3CountMissingImagesByBucket", "s3CountMissingVideosByBucket",
            "s3CountMissingDocumentsByBucket", "s3CountMissingByBucket", "s3CountUploaded", "s3CountUploadedByBucket",
            "s3CountPhashNotNull", "s3CountPhashNotNullByBucket" })
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
    @Cacheable("s3CountByBucket")
    long count(Set<String> buckets);

    @Override
    @Cacheable("s3CountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("s3CountIgnoredByBucket")
    long countByMetadata_IgnoredTrue(Set<String> buckets);

    @Override
    @Cacheable("s3CountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("s3CountMissingByBucket")
    long countMissingInCommons(Set<String> buckets);

    @Override
    @Cacheable("s3CountMissingImagesByBucket")
    long countMissingImagesInCommons(Set<String> buckets);

    @Override
    @Cacheable("s3CountMissingVideosByBucket")
    long countMissingVideosInCommons(Set<String> buckets);

    @Override
    @Cacheable("s3CountMissingDocumentsByBucket")
    long countMissingDocumentsInCommons(Set<String> buckets);

    @Override
    @Cacheable("s3CountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("s3CountUploadedByBucket")
    long countUploadedToCommons(Set<String> buckets);

    @Override
    @Cacheable("s3CountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("s3CountPhashNotNullByBucket")
    long countByMetadata_PhashNotNull(Set<String> buckets);

    // SAVE

    @Override
    @CacheEvictS3All
    <S extends S3Media> S save(S entity);

    @Override
    @CacheEvictS3All
    <S extends S3Media> List<S> saveAll(Iterable<S> entities);

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
}
