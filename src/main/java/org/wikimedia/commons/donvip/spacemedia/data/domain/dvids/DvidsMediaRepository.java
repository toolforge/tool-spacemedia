package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;

public interface DvidsMediaRepository<T extends DvidsMedia> extends MediaRepository<T> {

    @Retention(RetentionPolicy.RUNTIME)
    @CacheEvict(allEntries = true, cacheNames = { "dvidsCount", "dvidsCountByUnit", "dvidsCountIgnored", "dvidsCountIgnoredByUnit",
            "dvidsCountMissing", "dvidsCountMissingImagesByUnit", "dvidsCountMissingVideosByUnit",
            "dvidsCountMissingDocumentsByUnit", "dvidsCountMissingByUnit", "dvidsCountUploaded",
            "dvidsCountUploadedByUnit", "dvidsCountPhashNotNull", "dvidsCountPhashNotNullByUnit", "dvidsCountByCountry",
            "dvidsCountIgnoredByCountry", "dvidsCountMissingByCountry", "dvidsCountMissingImagesByCountry",
            "dvidsCountMissingVideosByCountry", "dvidsCountMissingDocumentsByCountry", "dvidsCountUploadedByCountry",
            "dvidsCountPhashNotNullByCountry" })
    @interface CacheEvictDvidsAll {

    }

    @Override
    @CacheEvictDvidsAll
    default void evictCaches() {

    }

    // COUNT

    @Override
    @Cacheable("dvidsCount")
    long count();

    @Override
    @Cacheable("dvidsCountByUnit")
    long count(Set<String> units);

    @Query("select count(*) from #{#entityName} m where m.location.country in ?1")
    @Cacheable("dvidsCountByCountry")
    long countByCountry(Set<String> countries);

    @Override
    @Cacheable("dvidsCountIgnored")
    long countByMetadata_IgnoredTrue();

    @Override
    @Cacheable("dvidsCountIgnoredByUnit")
    long countByMetadata_IgnoredTrue(Set<String> units);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where md.ignored = true and m.location.country in ?1")
    @Cacheable("dvidsCountIgnoredByCountry")
    long countByMetadata_IgnoredTrueByCountry(Set<String> countries);

    @Override
    @Cacheable("dvidsCountMissing")
    long countMissingInCommons();

    @Override
    @Cacheable("dvidsCountMissingByUnit")
    long countMissingInCommons(Set<String> units);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.location.country in ?1")
    @Cacheable("dvidsCountMissingByCountry")
    long countMissingInCommonsByCountry(Set<String> countries);

    @Override
    @Cacheable("dvidsCountMissingImagesByUnit")
    long countMissingImagesInCommons(Set<String> units);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg','exr') and m.location.country in ?1")
    @Cacheable("dvidsCountMissingImagesByCountry")
    long countMissingImagesInCommonsByCountry(Set<String> countries);

    @Override
    @Cacheable("dvidsCountMissingVideosByUnit")
    long countMissingVideosInCommons(Set<String> units);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg','wmv','avi') and m.location.country in ?1")
    @Cacheable("dvidsCountMissingVideosByCountry")
    long countMissingVideosInCommonsByCountry(Set<String> countries);

    @Override
    @Cacheable("dvidsCountMissingDocumentsByUnit")
    long countMissingDocumentsInCommons(Set<String> units);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and md.extension in ('pdf','stl','epub','ppt','pptm','pptx') and m.location.country in ?1")
    @Cacheable("dvidsCountMissingDocumentsByCountry")
    long countMissingDocumentsInCommonsByCountry(Set<String> countries);

    @Override
    @Cacheable("dvidsCountUploaded")
    long countUploadedToCommons();

    @Override
    @Cacheable("dvidsCountUploadedByUnit")
    long countUploadedToCommons(Set<String> units);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.location.country in ?1")
    @Cacheable("dvidsCountUploadedByCountry")
    long countUploadedToCommonsByCountry(Set<String> countries);

    @Override
    @Cacheable("dvidsCountPhashNotNull")
    long countByMetadata_PhashNotNull();

    @Override
    @Cacheable("dvidsCountPhashNotNullByUnit")
    long countByMetadata_PhashNotNull(Set<String> units);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where md.phash is not null and m.location.country in ?1")
    @Cacheable("dvidsCountPhashNotNullByCountry")
    long countByMetadata_PhashNotNullByCountry(Set<String> countries);

    // FIND

    @Query("select m from #{#entityName} m where m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Set<T> findAllByCountry(Set<String> countries);

    @Query("select m from #{#entityName} m where m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findAllByCountry(Set<String> countries, Pageable page);

    @Query("select m from #{#entityName} m where m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc limit 1")
    Optional<T> findFirstByCountry(Set<String> countries);

    @Query("select m from #{#entityName} m where m.location.country in ?1 and m.id.mediaId not in ?2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Set<T> findNotInByCountry(Set<String> countries, Set<String> mediaIds);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.ignored = true and m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findByMetadata_IgnoredTrueByCountry(Set<String> countries);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.ignored = true and m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findByMetadata_IgnoredTrueByCountry(Set<String> countries, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where size (md.commonsFileNames) >= 2 and m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findDuplicateInCommonsByCountry(Set<String> countries);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg','exr') and m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findMissingImagesInCommonsByCountry(Set<String> countries, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg','wmv','avi') and m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findMissingVideosInCommonsByCountry(Set<String> countries, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and md.extension in ('pdf','stl','epub','ppt','pptm','pptx') and m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findMissingDocumentsInCommonsByCountry(Set<String> countries, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findMissingInCommonsByCountry(Set<String> countries);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.location.country in ?1 and m.id.mediaId not in ?2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findMissingInCommonsNotInByCountry(Set<String> countries, Set<String> mediaIds);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findMissingInCommonsByCountry(Set<String> countries, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.location.country in ?1 and m.publicationDate = ?2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findMissingInCommonsByPublicationDateByCountry(Set<String> countries, LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.location.country in ?1 and m.publicationMonth = ?2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findMissingInCommonsByPublicationMonthByCountry(Set<String> countries, YearMonth month);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.location.country in ?1 and m.publicationYear = ?2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findMissingInCommonsByPublicationYearByCountry(Set<String> countries, Year year);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.location.country in ?1 and m.title = ?2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findMissingInCommonsByTitleByCountry(Set<String> countries, String title);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findUploadedToCommonsByCountry(Set<String> countries);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findUploadedToCommonsByCountry(Set<String> countries, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.location.country in ?1 and m.publicationDate = ?2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findUploadedToCommonsByPublicationDateByCountry(Set<String> countries, LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.phash is not null and m.location.country in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findByMetadata_PhashNotNullByCountry(Set<String> countries, Pageable page);

    // SAVE

    @Override
    @CacheEvictDvidsAll
    <S extends T> S save(S entity);

    @Override
    @CacheEvictDvidsAll
    <S extends T> List<S> saveAll(Iterable<S> entities);

    // DELETE

    @Override
    @CacheEvictDvidsAll
    void deleteById(CompositeMediaId id);

    @Override
    @CacheEvictDvidsAll
    void delete(T entity);

    @Override
    @CacheEvictDvidsAll
    void deleteAll(Iterable<? extends T> entities);

    @Override
    @CacheEvictDvidsAll
    void deleteAll();
}
