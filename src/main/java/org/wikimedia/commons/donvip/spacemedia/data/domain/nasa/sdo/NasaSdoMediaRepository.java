package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMediaType;

public interface NasaSdoMediaRepository extends MediaRepository<NasaSdoMedia> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaSdoCount", "nasaSdoCountRepo", "nasaSdoCountIgnored", "nasaSdoCountIgnoredRepo", "nasaSdoCountMissing",
            "nasaSdoCountMissingRepo", "nasaSdoCountMissingImagesRepo", "nasaSdoCountMissingVideosRepo",
            "nasaSdoCountMissingDocumentsRepo", "nasaSdoCountUploaded", "nasaSdoCountUploadedRepo",
            "nasaSdoCountPhashNotNullRepo" })
    @interface CacheEvictNasaSdoAll {

    }

    @Override
    @CacheEvictNasaSdoAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("nasaSdoCount")
    long count();

    @Override
    @Cacheable("nasaSdoCountRepo")
    long count(Set<String> repos);

    @Override
    @Cacheable("nasaSdoCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("nasaSdoCountIgnoredRepo")
    long countByIgnoredTrue(Set<String> repos);

    @Override
    @Cacheable("nasaSdoCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("nasaSdoCountMissingRepo")
    long countMissingInCommons(Set<String> repos);

    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and m.mediaType = ?1 and not exists elements (m.metadata.commonsFileNames)")
    long countMissingInCommons(NasaMediaType type);

    @Override
    @Cacheable("nasaSdoCountMissingImagesRepo")
    long countMissingImagesInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaSdoCountMissingVideosRepo")
    long countMissingVideosInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaSdoCountMissingDocumentsRepo")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Override
    @Cacheable("nasaSdoCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("nasaSdoCountUploadedRepo")
    long countUploadedToCommons(Set<String> repos);

    @Override
    @Cacheable("nasaSdoCountPhashNotNullRepo")
    long countByMetadata_PhashNotNull(Set<String> repos);

    @Query(value = """
            select count(*)
            from nasa_sdo_media left join (nasa_sdo_media_metadata, file_metadata)
            on (nasa_sdo_media.media_id = nasa_sdo_media_metadata.nasa_sdo_media_media_id and nasa_sdo_media_metadata.metadata_id = file_metadata.id)
            where media_type = ?1 and width = ?2 and height = ?3 and creation_date = ?4
            and exists (select * from file_metadata_commons_file_names where file_metadata_commons_file_names.file_metadata_id = file_metadata.id)
            """, nativeQuery = true)
    long countUploadedByMediaTypeAndDimensionsAndDate(int mediaType, int width, int height, LocalDate date);

    default long countUploadedByMediaTypeAndDimensionsAndDate(NasaMediaType mediaType, ImageDimensions dim, LocalDate date) {
        return countUploadedByMediaTypeAndDimensionsAndDate(mediaType.ordinal(), dim.getWidth(), dim.getHeight(), date);
    }

    @Query(value = """
            select count(*)
            from nasa_sdo_media
            where media_type = ?1 and data_type in ?2 and creation_date = ?3 and fsn is null
            """, nativeQuery = true)
    long countByMediaTypeAndDataTypeInAndDateAndKeywords_FsnIsNull(int mediaType, Collection<String> dataTypes,
            LocalDate date);

    default boolean existsByMediaTypeAndDataTypeInAndDateAndKeywords_FsnIsNull(NasaMediaType mediaType,
            Collection<NasaSdoDataType> dataTypes, LocalDate date) {
        return countByMediaTypeAndDataTypeInAndDateAndKeywords_FsnIsNull(mediaType.ordinal(),
                dataTypes.stream().map(NasaSdoDataType::name).toList(), date) > 0;
    }

    // FIND

    @Query(value = """
            select nasa_sdo_media.*
            from nasa_sdo_media left join (nasa_sdo_media_metadata, file_metadata)
            on (nasa_sdo_media.media_id = nasa_sdo_media_metadata.nasa_sdo_media_media_id and nasa_sdo_media_metadata.metadata_id = file_metadata.id)
            where media_type = ?1 and width = ?2 and height = ?3 and creation_date = ?4
            and not exists (select * from file_metadata_commons_file_names where file_metadata_commons_file_names.file_metadata_id = file_metadata.id)
            """, nativeQuery = true)
    List<NasaSdoMedia> findMissingByMediaTypeAndDimensionsAndDate(int mediaType, int width, int height, LocalDate date);

    default List<NasaSdoMedia> findMissingByMediaTypeAndDimensionsAndDate(NasaMediaType mediaType, ImageDimensions dim,
            LocalDate date) {
        return findMissingByMediaTypeAndDimensionsAndDate(mediaType.ordinal(), dim.getWidth(), dim.getHeight(), date);
    }

    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and m.mediaType = ?1 and not exists elements (m.metadata.commonsFileNames)")
    Page<NasaSdoMedia> findMissingInCommonsByType(NasaMediaType type, Pageable page);

    @Override
    default Page<NasaSdoMedia> findMissingImagesInCommons(Pageable page) {
        return findMissingInCommonsByType(NasaMediaType.image, page);
    }

    @Override
    default Page<NasaSdoMedia> findMissingVideosInCommons(Pageable page) {
        return findMissingInCommonsByType(NasaMediaType.video, page);
    }

    @Query(value = """
            select *
            from nasa_sdo_media left join (nasa_sdo_media_metadata, file_metadata)
            on (nasa_sdo_media.media_id = nasa_sdo_media_metadata.nasa_sdo_media_media_id and nasa_sdo_media_metadata.metadata_id = file_metadata.id)
            where media_type = ?1 and width = ?2 and height = ?3 and creation_date = ?4 and fsn is null
            """, nativeQuery = true)
    List<NasaSdoMedia> findByMediaTypeAndDimensionsAndDateAndFsnIsNull(int mediaType, int width,
            int height, LocalDate date);

    default List<NasaSdoMedia> findByMediaTypeAndDimensionsAndDateAndFsnIsNull(NasaMediaType mediaType,
            ImageDimensions dim, LocalDate date) {
        return findByMediaTypeAndDimensionsAndDateAndFsnIsNull(mediaType.ordinal(), dim.getWidth(), dim.getHeight(),
                date);
    }

    // SAVE

    @Override
    @CacheEvictNasaSdoAll
    <S extends NasaSdoMedia> S save(S entity);

    @Override
    @CacheEvictNasaSdoAll
    <S extends NasaSdoMedia> Iterable<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictNasaSdoAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictNasaSdoAll
    void delete(NasaSdoMedia entity);

    @Override
    @CacheEvictNasaSdoAll
    void deleteAll(Iterable<? extends NasaSdoMedia> entities);

    @Override
    @CacheEvictNasaSdoAll
    void deleteAll();

    // UPDATE

    @Override
    @CacheEvictNasaSdoAll
    int resetIgnored();

    @Override
    @CacheEvictNasaSdoAll
    int resetIgnored(Set<String> repos);
}
