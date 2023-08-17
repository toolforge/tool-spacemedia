package org.wikimedia.commons.donvip.spacemedia.data.domain.esa;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface EsaMediaRepository extends MediaRepository<EsaMedia, Integer> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "esaCount", "esaCountIgnored", "esaCountMissing", "esaCountMissingImages", "esaCountMissingVideos",
            "esaCountUploaded" })
    @interface CacheEvictEsaAll {

    }

    @Override
    @CacheEvictEsaAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("esaCount")
    long count();

    @Override
    @Cacheable("esaCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("esaCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("esaCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons();
    }

    @Override
    @Cacheable("esaCountMissingVideos")
    default long countMissingVideosInCommons() {
        return 0;
    }

    @Override
    @Cacheable("esaCountUploaded")
    long countUploadedToCommons();

    // FIND

    Optional<EsaMedia> findByUrl(URL mediaUrl);

    @Override
    default Page<EsaMedia> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommons(page);
    }

    @Override
    default Page<EsaMedia> findMissingVideosInCommons(Pageable page) {
        return Page.empty();
    }

    // SAVE

    @Override
    @CacheEvictEsaAll
    <S extends EsaMedia> S save(S entity);

    @Override
    @CacheEvictEsaAll
    <S extends EsaMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictEsaAll
    void deleteById(Integer id);

    @Override
    @CacheEvictEsaAll
    void delete(EsaMedia entity);

    @Override
    @CacheEvictEsaAll
    void deleteAll(Iterable<? extends EsaMedia> entities);

    @Override
    @CacheEvictEsaAll
    void deleteAll();
}
