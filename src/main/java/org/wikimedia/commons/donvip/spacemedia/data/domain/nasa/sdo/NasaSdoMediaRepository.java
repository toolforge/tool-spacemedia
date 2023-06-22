package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMediaType;

public interface NasaSdoMediaRepository extends MediaRepository<NasaSdoMedia, String, LocalDateTime> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = {
            "nasaSdoCount", "nasaSdoCountIgnored", "nasaSdoCountMissing", "nasaSdoCountMissingImages",
            "nasaSdoCountMissingVideos", "nasaSdoCountUploaded", "nasaSdoFindByPhashNotNull" })
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
    @Cacheable("nasaSdoCountIgnored")
    long countByIgnoredTrue();

    @Override
    @Cacheable("nasaSdoCountMissing")
    long countMissingInCommons();

    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and m.mediaType = ?1 and not exists elements (m.metadata.commonsFileNames)")
    long countMissingInCommons(NasaMediaType type);

    @Override
    @Cacheable("nasaSdoCountMissingImages")
    default long countMissingImagesInCommons() {
        return countMissingInCommons(NasaMediaType.image);
    }

    @Override
    @Cacheable("nasaSdoCountMissingVideos")
    default long countMissingVideosInCommons() {
        return countMissingInCommons(NasaMediaType.video);
    }

    @Override
    @Cacheable("nasaSdoCountUploaded")
    long countUploadedToCommons();

    @Query(value = """
            select count(*)
            from nasa_sdo_media left join (nasa_sdo_media_metadata, file_metadata)
            on (nasa_sdo_media.id = nasa_sdo_media_metadata.nasa_sdo_media_id and nasa_sdo_media_metadata.metadata_id = file_metadata.id)
            where media_type = ?1 and width = ?2 and height = ?3 and DATE(date) = ?4
            and exists (select * from file_metadata_commons_file_names where file_metadata_commons_file_names.file_metadata_id = file_metadata.id)
            """, nativeQuery = true)
    long countUploadedByMediaTypeAndDimensionsAndDate(int mediaType, int width, int height, LocalDate date);

    default long countUploadedByMediaTypeAndDimensionsAndDate(NasaMediaType mediaType, ImageDimensions dim, LocalDate date) {
        return countUploadedByMediaTypeAndDimensionsAndDate(mediaType.ordinal(), dim.getWidth(), dim.getHeight(), date);
    }

    @Query(value = """
            select count(*)
            from nasa_sdo_media
            where media_type = ?1 and data_type in ?2 and DATE(date) = ?3 and fsn is null
            """, nativeQuery = true)
    long countByMediaTypeAndDataTypeInAndDateAndKeywords_FsnIsNull(int mediaType, Collection<String> dataTypes,
            LocalDate date);

    default boolean existsByMediaTypeAndDataTypeInAndDateAndKeywords_FsnIsNull(NasaMediaType mediaType,
            Collection<NasaSdoDataType> dataTypes, LocalDate date) {
        return countByMediaTypeAndDataTypeInAndDateAndKeywords_FsnIsNull(mediaType.ordinal(),
                dataTypes.stream().map(NasaSdoDataType::name).toList(), date) > 0;
    }

    // FIND

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

    @Override
    @Cacheable("nasaSdoFindByPhashNotNull")
    List<MediaProjection<String>> findByMetadata_PhashNotNull();

    @Query(value = """
            select *
            from nasa_sdo_media left join (nasa_sdo_media_metadata, file_metadata)
            on (nasa_sdo_media.id = nasa_sdo_media_metadata.nasa_sdo_media_id and nasa_sdo_media_metadata.metadata_id = file_metadata.id)
            where media_type = ?1 and width = ?2 and height = ?3 and DATE(date) = ?4 and fsn is null
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
    void deleteById(String id);

    @Override
    @CacheEvictNasaSdoAll
    void delete(NasaSdoMedia entity);

    @Override
    @CacheEvictNasaSdoAll
    void deleteAll(Iterable<? extends NasaSdoMedia> entities);

    @Override
    @CacheEvictNasaSdoAll
    void deleteAll();
}
