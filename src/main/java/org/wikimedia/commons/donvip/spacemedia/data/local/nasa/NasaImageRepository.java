package org.wikimedia.commons.donvip.spacemedia.data.local.nasa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.local.MediaRepository;

public interface NasaImageRepository extends MediaRepository<NasaImage, String> {

    @Override
    @Query("select m from NasaImage m where not exists elements (m.commonsFileNames)")
    List<NasaImage> findMissingInCommons();

    @Override
    @Query("select m from NasaImage m where size (m.commonsFileNames) >= 2")
    List<NasaImage> findDuplicateInCommons();
}
