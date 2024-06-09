package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.net.URL;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Superclass of Media CRUD repositories, handling pagination and sorting.
 *
 * @param <T>  the media type the repository manages
 */
@NoRepositoryBean
public interface MediaRepository<T extends Media> extends JpaRepository<T, CompositeMediaId> {

    void evictCaches();

    // COUNT simple

    /**
     * Count files with a computed perceptual hash.
     *
     * @return number of files with a computed perceptual hash
     */
    long countByMetadata_PhashNotNull();

    /**
     * Count files matching the given SHA-1.
     *
     * @param sha1 SHA-1 hash
     *
     * @return number of files matching the given SHA-1
     */
    long countByMetadata_Sha1(String sha1);

    /**
     * Count files marked as ignored, that won't be automatically uploaded.
     *
     * @return number of ignored files
     */
    long countByMetadata_IgnoredTrue();

    /**
     * Count files not yet uploaded to Wikimedia Commons.
     *
     * @return number of files not yet uploaded to Wikimedia Commons
     */
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames)")
    long countMissingInCommons();

    /**
     * Count files already uploaded to Wikimedia Commons.
     *
     * @return number of files already uploaded to Wikimedia Commons
     */
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames)")
    long countUploadedToCommons();

    // COUNT composite

    @Query("select count(*) from #{#entityName} m where m.id.repoId in ?1")
    long count(Set<String> repos);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where md.ignored = true and m.id.repoId in ?1")
    long countByMetadata_IgnoredTrue(Set<String> repos);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    long countMissingInCommons(Set<String> repos);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg','exr') and m.id.repoId in ?1")
    long countMissingImagesInCommons(Set<String> repos);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg','wmv','avi') and m.id.repoId in ?1")
    long countMissingVideosInCommons(Set<String> repos);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and md.extension in ('pdf','stl','epub','ppt','pptm','pptx') and m.id.repoId in ?1")
    long countMissingDocumentsInCommons(Set<String> repos);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    long countUploadedToCommons(Set<String> repos);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where md.phash is not null and m.id.repoId in ?1")
    long countByMetadata_PhashNotNull(Set<String> repos);

    // FIND simple

    /**
     * Find files not yet uploaded to Wikimedia Commons.
     *
     * @return files not yet uploaded to Wikimedia Commons
     */
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findMissingInCommons();

    /**
     * Find files not yet uploaded to Wikimedia Commons.
     *
     * @param page pagination information
     *
     * @return files not yet uploaded to Wikimedia Commons
     */
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findMissingInCommons(Pageable page);

    /**
     * Find files already uploaded to Wikimedia Commons.
     *
     * @return files already uploaded to Wikimedia Commons
     */
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findUploadedToCommons();

    /**
     * Find files already uploaded to Wikimedia Commons.
     *
     * @param page pagination information
     *
     * @return files already uploaded to Wikimedia Commons
     */
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findUploadedToCommons(Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where size (md.commonsFileNames) >= 2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findDuplicateInCommons();

    List<T> findByMetadata_AssetUrl(URL imageUrl);

    List<T> findByMetadata_Phash(String phash);

    List<MediaProjection> findByMetadata_PhashNotNull();

    Page<T> findByMetadata_PhashNotNull(Pageable page);

    List<T> findByMetadata_Sha1(String sha1);

    Optional<T> findByPublicationDate(LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and (m.creationDate = ?1 or m.publicationDate = ?1) and not exists elements (md.commonsFileNames)")
    List<T> findMissingByDate(LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and m.title = ?1 and not exists elements (md.commonsFileNames)")
    List<T> findMissingByTitle(String title);

    List<T> findByMetadata_IgnoredTrue();

    Page<T> findByMetadata_IgnoredTrue(Pageable page);

    // FIND Composite

    @Query("select m from #{#entityName} m where m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Set<T> findAll(Set<String> repos);

    @Query("select m from #{#entityName} m where m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findAll(Set<String> repos, Pageable page);

    @Query("select m from #{#entityName} m where m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc limit 1")
    Optional<T> findFirst(Set<String> repos);

    @Query("select m from #{#entityName} m where m.id.repoId in ?1 and m.id.mediaId not in ?2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Set<T> findNotIn(Set<String> repos, Set<String> mediaIds);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.ignored = true and m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findByMetadata_IgnoredTrue(Set<String> repos);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.ignored = true and m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findByMetadata_IgnoredTrue(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where size (md.commonsFileNames) >= 2 and m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findDuplicateInCommons(Set<String> repos);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg','exr') and m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findMissingImagesInCommons(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg','wmv','avi') and m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findMissingVideosInCommons(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and md.extension in ('pdf','stl','epub','ppt','pptm','pptx') and m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findMissingDocumentsInCommons(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findMissingInCommons(Set<String> repos);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and m.id.mediaId not in ?2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findMissingInCommonsNotIn(Set<String> repos, Set<String> mediaIds);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findMissingInCommons(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and m.publicationDate = ?2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findMissingInCommonsByPublicationDate(Set<String> repos, LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and m.publicationMonth = ?2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findMissingInCommonsByPublicationMonth(Set<String> repos, YearMonth month);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and m.publicationYear = ?2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findMissingInCommonsByPublicationYear(Set<String> repos, Year year);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (md.ignored is null or md.ignored = false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and m.title = ?2 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findMissingInCommonsByTitle(Set<String> repos, String title);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    List<T> findUploadedToCommons(Set<String> repos);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findUploadedToCommons(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.phash is not null and m.id.repoId in ?1 order by m.publicationYear desc, m.publicationMonth desc, m.publicationDate desc")
    Page<T> findByMetadata_PhashNotNull(Set<String> repos, Pageable page);
}
