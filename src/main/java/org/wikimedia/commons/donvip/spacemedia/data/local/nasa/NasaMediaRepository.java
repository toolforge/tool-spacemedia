package org.wikimedia.commons.donvip.spacemedia.data.local.nasa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface NasaMediaRepository<T extends NasaMedia> extends CrudRepository<T, String> {

    /**
     * Find NASA files not yet uploaded to Wikimedia Commons.
     * 
     * @return NASA files not yet uploaded to Wikimedia Commons
     */
    @Query("select m from NasaMedia m where not exists elements (m.commonsFileNames)")
    List<NasaMedia> findMissingInCommons();

    @Query("select m from NasaMedia m where size (m.commonsFileNames) >= 2")
    List<NasaMedia> findDuplicateInCommons();
}
