package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Superclass of Media CRUD repositories, handling pagination and sorting.
 *
 * @param <T>  the media type the repository manages
 */
@NoRepositoryBean
public interface MediaRepository<T extends Media> extends PagingAndSortingRepository<T, CompositeMediaId> {

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
    long countByIgnoredTrue();

    /**
     * Count files not yet uploaded to Wikimedia Commons.
     *
     * @return number of files not yet uploaded to Wikimedia Commons
     */
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames)")
    long countMissingInCommons();

    /**
     * Count images not yet uploaded to Wikimedia Commons.
     *
     * @return number of images not yet uploaded to Wikimedia Commons
     */
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg')")
    long countMissingImagesInCommons();

    /**
     * Count videos not yet uploaded to Wikimedia Commons.
     *
     * @return number of videos not yet uploaded to Wikimedia Commons
     */
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg')")
    long countMissingVideosInCommons();

    // COUNT composite

    @Query("select count(*) from #{#entityName} m where m.id.repoId in ?1")
    long count(Set<String> repos);

    @Query("select count(*) from #{#entityName} m where m.ignored = true and m.id.repoId in ?1")
    long countByIgnoredTrue(Set<String> repos);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    long countMissingInCommons(Set<String> repos);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg') and m.id.repoId in ?1")
    long countMissingImagesInCommons(Set<String> repos);

    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg') and m.id.repoId in ?1")
    long countMissingVideosInCommons(Set<String> repos);

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
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames)")
    List<T> findMissingInCommons();

    /**
     * Find files not yet uploaded to Wikimedia Commons.
     *
     * @param page pagination information
     *
     * @return files not yet uploaded to Wikimedia Commons
     */
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames)")
    Page<T> findMissingInCommons(Pageable page);

    /**
     * Find images not yet uploaded to Wikimedia Commons.
     *
     * @param page pagination information
     *
     * @return images not yet uploaded to Wikimedia Commons
     */
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg')")
    Page<T> findMissingImagesInCommons(Pageable page);

    /**
     * Find videos not yet uploaded to Wikimedia Commons.
     *
     * @param page pagination information
     *
     * @return videos not yet uploaded to Wikimedia Commons
     */
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg')")
    Page<T> findMissingVideosInCommons(Pageable page);

    /**
     * Count files already uploaded to Wikimedia Commons.
     *
     * @return number of files already uploaded to Wikimedia Commons
     */
    @Query("select count(distinct (m.id)) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames)")
    long countUploadedToCommons();

    /**
     * Find files already uploaded to Wikimedia Commons.
     *
     * @return files already uploaded to Wikimedia Commons
     */
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames)")
    List<T> findUploadedToCommons();

    /**
     * Find files already uploaded to Wikimedia Commons.
     *
     * @param page pagination information
     *
     * @return files already uploaded to Wikimedia Commons
     */
    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames)")
    Page<T> findUploadedToCommons(Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where size (md.commonsFileNames) >= 2")
    List<T> findDuplicateInCommons();

    List<T> findByMetadata_AssetUrl(URL imageUrl);

    List<T> findByMetadata_Phash(String phash);

    List<MediaProjection> findByMetadata_PhashNotNull();

    Page<T> findByMetadata_PhashNotNull(Pageable page);

    List<T> findByMetadata_Sha1(String sha1);

    Optional<T> findByPublicationDate(LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and (m.creationDate = ?1 or m.publicationDate = ?1) and not exists elements (md.commonsFileNames)")
    List<T> findMissingByDate(LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and m.title = ?1 and not exists elements (md.commonsFileNames)")
    List<T> findMissingByTitle(String title);

    List<T> findByIgnoredTrue();

    Page<T> findByIgnoredTrue(Pageable page);

    @Query("select max(id.mediaId) from #{#entityName}")
    Optional<String> findMaxId();

    // FIND Composite

    @Query("select m from #{#entityName} m where m.id.repoId in ?1")
    Set<T> findAll(Set<String> repos);

    @Query("select m from #{#entityName} m where m.id.repoId in ?1")
    Page<T> findAll(Set<String> repos, Pageable page);

    @Query("select m from #{#entityName} m where m.id.repoId in ?1 and m.id.mediaId not in ?2")
    Set<T> findNotIn(Set<String> repos, Set<String> mediaIds);

    @Query("select m from #{#entityName} m where m.ignored = true and m.id.repoId in ?1")
    List<T> findByIgnoredTrue(Set<String> repos);

    @Query("select m from #{#entityName} m where m.ignored = true and m.id.repoId in ?1")
    Page<T> findByIgnoredTrue(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where size (md.commonsFileNames) >= 2 and m.id.repoId in ?1")
    List<T> findDuplicateInCommons(Set<String> repos);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('bmp','jpg','jpeg','tif','tiff','png','webp','xcf','gif','svg') and m.id.repoId in ?1")
    Page<T> findMissingImagesInCommons(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and md.extension in ('mp4','webm','ogv','mpeg') and m.id.repoId in ?1")
    Page<T> findMissingVideosInCommons(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    List<T> findMissingInCommons(Set<String> repos);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and m.id.mediaId not in ?2")
    List<T> findMissingInCommonsNotIn(Set<String> repos, Set<String> mediaIds);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    Page<T> findMissingInCommons(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and (m.creationDate = ?2 or m.publicationDate = ?2)")
    List<T> findMissingInCommonsByDate(Set<String> repos, LocalDate date);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where (m.ignored is null or m.ignored is false) and not exists elements (md.commonsFileNames) and m.id.repoId in ?1 and m.title = ?2")
    List<T> findMissingInCommonsByTitle(Set<String> repos, String title);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    List<T> findUploadedToCommons(Set<String> repos);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where exists elements (md.commonsFileNames) and m.id.repoId in ?1")
    Page<T> findUploadedToCommons(Set<String> repos, Pageable page);

    @Query("select distinct(m) from #{#entityName} m join m.metadata md where md.phash is not null and m.id.repoId in ?1")
    Page<T> findByMetadata_PhashNotNull(Set<String> repos, Pageable page);

    // UPDATE

    @Modifying
    @Query("update #{#entityName} m set m.ignored = null, m.ignoredReason = null where m.ignored = true")
    int resetIgnored();

    @Modifying
    @Query("update #{#entityName} m set m.ignored = null, m.ignoredReason = null where m.ignored = true and m.id.repoId in ?1")
    int resetIgnored(Set<String> repos);

}
