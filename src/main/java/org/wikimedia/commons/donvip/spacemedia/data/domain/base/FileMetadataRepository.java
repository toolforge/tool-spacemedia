package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.net.URL;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface FileMetadataRepository extends CrudRepository<FileMetadata, Long> {

    FileMetadata findBySha1(String sha1);

    Optional<FileMetadata> findByAssetUrl(URL assetUrl);

    boolean existsByAssetUrl(URL assetUrl);

    /**
     * Count files matching the given perceptual hash.
     *
     * @param phash perceptual hash
     *
     * @return number of files matching the given perceptual hash
     */
    long countByPhash(String phash);

    @Modifying
    @Query("update #{#entityName} m set phash = null where phash is not null")
    int resetPerceptualHashes();

    @Modifying
    @Query("update #{#entityName} m set sha1 = null where sha1 is not null")
    int resetSha1Hashes();
}
