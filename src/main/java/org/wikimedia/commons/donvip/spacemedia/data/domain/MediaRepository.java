package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

@NoRepositoryBean
public interface MediaRepository<T extends Media, ID> extends PagingAndSortingRepository<T, ID> {

    /**
     * Count files matching the given SHA-1.
     * 
     * @param sha1 SHA-1 hash
     * 
     * @return number of files matching the given SHA-1
     */
    long countBySha1(String sha1);

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

    List<T> findDuplicateInCommons();

    List<T> findByAssetUrl(URL imageUrl);

    Optional<T> findBySha1(String sha1);

    List<T> findByIgnoredTrue();
}
