package org.wikimedia.commons.donvip.spacemedia.data.domain.eso;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaProjection;

public interface EsoMediaRepository extends CommonEsoMediaRepository<EsoMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "esoCount", "esoCountIgnored", "esoCountMissing", "esoCountMissingImages", "esoCountMissingVideos",
            "esoCountUploaded", "esoFindByPhashNotNull" })
    @interface CacheEvictEsoAll {

    }

    // COUNT

    @Override
    @Cacheable("esoCount")
    long count();

    @Override
    @Cacheable("esoCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("esoCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("esoCountMissingImages")
    long countMissingImagesInCommons();

    @Override
    @Cacheable("esoCountMissingVideos")
    long countMissingVideosInCommons();

    @Override
    @Cacheable("esoCountUploaded")
    long countUploadedToCommons();

    // FIND

    @Override
    @Cacheable("esoFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    // SAVE

    @Override
    @CacheEvictEsoAll
    <S extends EsoMedia> S save(S entity);

    @Override
    @CacheEvictEsoAll
    <S extends EsoMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictEsoAll
    void deleteById(String id);

    @Override
    @CacheEvictEsoAll
    void delete(EsoMedia entity);

    @Override
    @CacheEvictEsoAll
    void deleteAll(Iterable<? extends EsoMedia> entities);

    @Override
    @CacheEvictEsoAll
    void deleteAll();
}
