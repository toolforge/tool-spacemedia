package org.wikimedia.commons.donvip.spacemedia.data.domain.esa;

import java.net.URL;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMediaRepository;

public interface EsaMediaRepository extends FullResMediaRepository<EsaMedia, Integer> {

    @CacheEvict(allEntries = true, cacheNames = {"esaCount", "esaCountIgnored", "esaCountMissing", "esaCountUploaded"})
    @interface CacheEvictEsaAll {

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
    @Cacheable("esaCountUploaded")
    long countUploadedToCommons();

    // FIND

    Optional<EsaMedia> findByUrl(URL mediaUrl);

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
