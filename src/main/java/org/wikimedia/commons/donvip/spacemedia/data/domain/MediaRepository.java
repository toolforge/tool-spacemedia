package org.wikimedia.commons.donvip.spacemedia.data.domain;

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

    /**
     * Count files matching the given perceptual hash.
     * 
     * @param phash perceptual hash
     * 
     * @return number of files matching the given perceptual hash
     */
    long countByMetadata_Phash(String phash);

    /**
     * Count files matching the given SHA-1.
     * 
     * @param sha1 SHA-1 hash
     * 
     * @return number of files matching the given SHA-1
     */
    long countByMetadata_Sha1(String sha1);

    /**
     * Count files marked as ignored, that won't be uploaded.
     * 
     * @return number of ignored files
     */
    long countByIgnoredTrue();

    /**
     * Count files not yet uploaded to Wikimedia Commons.
     * 
     * @return number of files not yet uploaded to Wikimedia Commons
     */
    long countMissingInCommons();

    /**
     * Find files not yet uploaded to Wikimedia Commons.
     * 
     * @return files not yet uploaded to Wikimedia Commons
     */
    List<T> findMissingInCommons();

    /**
     * Find files not yet uploaded to Wikimedia Commons.
     * 
     * @param page pagination information
     * 
     * @return files not yet uploaded to Wikimedia Commons
     */
    Page<T> findMissingInCommons(Pageable page);

    /**
     * Count files already uploaded to Wikimedia Commons.
     * 
     * @return number of files already uploaded to Wikimedia Commons
     */
    long countUploadedToCommons();

    /**
     * Find files already uploaded to Wikimedia Commons.
     * 
     * @return files already uploaded to Wikimedia Commons
     */
    List<T> findUploadedToCommons();

    /**
     * Find files already uploaded to Wikimedia Commons.
     * 
     * @param page pagination information
     * 
     * @return files already uploaded to Wikimedia Commons
     */
    Page<T> findUploadedToCommons(Pageable page);

    List<T> findDuplicateInCommons();

    List<T> findByMetadata_AssetUrl(URL imageUrl);

    List<T> findByMetadata_Phash(String phash);

    List<MediaProjection<ID>> findByMetadata_PhashNotNull();

    List<T> findByMetadata_Sha1(String sha1);

    List<T> findByIgnoredTrue();

    Page<T> findByIgnoredTrue(Pageable page);

    @Modifying
    @Query("update #{#entityName} m set m.metadata.phash = null where m.metadata.phash is not null")
    int resetPerceptualHashes();
}
