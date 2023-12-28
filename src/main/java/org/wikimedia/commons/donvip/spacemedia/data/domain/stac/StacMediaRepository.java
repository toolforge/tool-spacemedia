package org.wikimedia.commons.donvip.spacemedia.data.domain.stac;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface StacMediaRepository extends MediaRepository<StacMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "stacCount", "stacCountByCatalog", "stacCountIgnored",
            "stacCountIgnoredByCatalog", "stacCountMissing", "stacCountMissingImagesByCatalog",
            "stacCountMissingVideosByCatalog", "stacCountMissingDocumentsByCatalog", "stacCountMissingByCatalog",
            "stacCountUploaded", "stacCountUploadedByCatalog", "stacCountPhashNotNull",
            "stacCountPhashNotNullByCatalog" })
    @interface CacheEvictStacAll {

    }

    @Override
    @CacheEvictStacAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("stacCount")
    long count();

    @Override
    @Cacheable("stacCountByCatalog")
    long count(Set<String> catalogs);

    @Override
    @Cacheable("stacCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("stacCountIgnoredByCatalog")
    long countByMetadata_IgnoredTrue(Set<String> catalogs);

    @Override
    @Cacheable("stacCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("stacCountMissingByCatalog")
    long countMissingInCommons(Set<String> catalogs);

    @Override
    @Cacheable("stacCountMissingImagesByCatalog")
    long countMissingImagesInCommons(Set<String> catalogs);

    @Override
    @Cacheable("stacCountMissingVideosByCatalog")
    long countMissingVideosInCommons(Set<String> catalogs);

    @Override
    @Cacheable("stacCountMissingDocumentsByCatalog")
    long countMissingDocumentsInCommons(Set<String> catalogs);

    @Override
    @Cacheable("stacCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("stacCountUploadedByCatalog")
    long countUploadedToCommons(Set<String> catalogs);

    @Override
    @Cacheable("stacCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("stacCountPhashNotNullByCatalog")
    long countByMetadata_PhashNotNull(Set<String> catalogs);

    // FIND

    Optional<StacMedia> findByUrl(URL mediaUrl);

    // SAVE

    @Override
    @CacheEvictStacAll
    <S extends StacMedia> S save(S entity);

    @Override
    @CacheEvictStacAll
    <S extends StacMedia> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictStacAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictStacAll
    void delete(StacMedia entity);

    @Override
    @CacheEvictStacAll
    void deleteAll(Iterable<? extends StacMedia> entities);

    @Override
    @CacheEvictStacAll
    void deleteAll();
}
