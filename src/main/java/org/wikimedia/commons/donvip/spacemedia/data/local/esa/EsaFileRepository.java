package org.wikimedia.commons.donvip.spacemedia.data.local.esa;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface EsaFileRepository extends CrudRepository<EsaFile, String> {

    List<EsaFile> findByUrl(URL imageUrl);

    Optional<EsaFile> findBySha1(String sha1);

    List<EsaFile> findByIgnoredTrue();

    /**
     * Find ESA files not yet uploaded to Wikimedia Commons.
     * @return ESA files not yet uploaded to Wikimedia Commons
     */
    @Query("select f from EsaFile f where (f.ignored is null or f.ignored is false) and not exists elements (f.commonsFileNames)")
    List<EsaFile> findMissingInCommons();

    @Query("select f from EsaFile f where size (f.commonsFileNames) >= 2")
    List<EsaFile> findDuplicateInCommons();
}
