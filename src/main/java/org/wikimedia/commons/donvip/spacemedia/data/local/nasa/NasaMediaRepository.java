package org.wikimedia.commons.donvip.spacemedia.data.local.nasa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.local.MediaRepository;

public interface NasaMediaRepository extends MediaRepository<NasaMedia, String> {

    @Override
    @Query("select m from NasaMedia m where not exists elements (m.commonsFileNames)")
    List<NasaMedia> findMissingInCommons();

    @Override
    @Query("select m from NasaMedia m where size (m.commonsFileNames) >= 2")
    List<NasaMedia> findDuplicateInCommons();
}
