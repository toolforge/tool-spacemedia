package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface NasaPhotojournalMediaRepository extends MediaRepository<NasaPhotojournalMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "nasaPjCount", "nasaPjCountRepo", "nasaPjCountIgnored",
            "nasaPjCountIgnoredRepo", "nasaPjCountMissing", "nasaPjCountMissingRepo",
            "nasaPjCountMissingImagesRepo", "nasaPjCountMissingVideosRepo", "nasaPjCountMissingDocumentsRepo",
            "nasaPjCountUploaded", "nasaPjCountUploadedRepo", "nasaPjCountPhashNotNull",
            "nasaPjCountPhashNotNullRepo" })
    @interface CacheEvictNasaPhotojournalAll {

    }

    @Override
    @CacheEvictNasaPhotojournalAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaPjCount")
    long count();

    @Override
    @Cacheable("nasaPjCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("nasaPjCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("nasaPjCountIgnoredRepo")
    long countByMetadata_IgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("nasaPjCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaPjCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaPjCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaPjCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaPjCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaPjCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("nasaPjCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("nasaPjCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("nasaPjCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // SAVE

    @Override
    @CacheEvictNasaPhotojournalAll
    <S extends NasaPhotojournalMedia> S save(S entity);

    @Override
    @CacheEvictNasaPhotojournalAll
    <S extends NasaPhotojournalMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaPhotojournalAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictNasaPhotojournalAll
    void delete(NasaPhotojournalMedia entity);

    @Override
    @CacheEvictNasaPhotojournalAll
    void deleteAll(Iterable<? extends NasaPhotojournalMedia> entities);

    @Override
    @CacheEvictNasaPhotojournalAll
    void deleteAll();
}
