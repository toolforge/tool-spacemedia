package org.wikimedia.commons.donvip.spacemedia.data.local.esa;

import java.net.URL;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface EsaImageRepository extends CrudRepository<EsaImage, Integer> {

    List<EsaImage> findByUrl(URL imageUrl);

    @Query("select distinct i from EsaImage i, EsaFile f where f member of i.files and f.ignored is true")
    List<EsaImage> findByIgnoredTrue();

    /**
     * Find ESA images not yet uploaded to Wikimedia Commons.
     * @return ESA images not yet uploaded to Wikimedia Commons
     */
    @Query("select distinct i from EsaImage i, EsaFile f where f member of i.files and (f.ignored is null or f.ignored is false) and not exists elements (f.commonsFileNames)")
    List<EsaImage> findMissingInCommons();

    @Query("select distinct i from EsaImage i, EsaFile f where f member of i.files and size (f.commonsFileNames) >= 2")
    List<EsaImage> findDuplicateInCommons();

    @Query("select i from EsaImage i where ?1 member of i.files")
    List<EsaImage> findByFile(EsaFile file);
}
