package org.wikimedia.commons.donvip.spacemedia.data.local.nasa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.local.MediaRepository;

public interface NasaVideoRepository extends MediaRepository<NasaVideo, String> {

    @Override
    @Query("select m from NasaVideo m where not exists elements (m.commonsFileNames)")
    List<NasaVideo> findMissingInCommons();

    @Override
    @Query("select m from NasaVideo m where size (m.commonsFileNames) >= 2")
    List<NasaVideo> findDuplicateInCommons();
}
