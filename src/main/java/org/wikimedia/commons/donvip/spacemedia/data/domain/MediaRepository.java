package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface MediaRepository<T extends Media, ID> extends CrudRepository<T, ID> {

    int countBySha1(String sha1);

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

    List<T> findDuplicateInCommons();

    List<T> findByAssetUrl(URL imageUrl);

    Optional<T> findBySha1(String sha1);

    List<T> findByIgnoredTrue();
}
