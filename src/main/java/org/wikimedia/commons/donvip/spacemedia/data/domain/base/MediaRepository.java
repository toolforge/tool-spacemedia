package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.net.URL;
import java.time.temporal.Temporal;
import java.util.List;

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
 * @param <ID> the identifier type of the entity the repository manages
 * @param <D>  the media date type
 */
@NoRepositoryBean
public interface MediaRepository<T extends Media<ID, D>, ID, D extends Temporal>
        extends PagingAndSortingRepository<T, ID> {

    void evictCaches();

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
    long countMissingImagesInCommons();

    /**
     * Count videos not yet uploaded to Wikimedia Commons.
     *
     * @return number of videos not yet uploaded to Wikimedia Commons
     */
    long countMissingVideosInCommons();

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
    Page<T> findMissingImagesInCommons(Pageable page);

    /**
     * Find videos not yet uploaded to Wikimedia Commons.
     *
     * @param page pagination information
     *
     * @return videos not yet uploaded to Wikimedia Commons
     */
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

    List<MediaProjection<ID>> findByMetadata_PhashNotNull();

    Page<T> findByMetadata_PhashNotNull(Pageable page);

    List<T> findByMetadata_Sha1(String sha1);

    List<T> findByIgnoredTrue();

    Page<T> findByIgnoredTrue(Pageable page);

    @Modifying
    @Query("update #{#entityName} m set m.ignored = null, m.ignoredReason = null where m.ignored = true")
    int resetIgnored();
}