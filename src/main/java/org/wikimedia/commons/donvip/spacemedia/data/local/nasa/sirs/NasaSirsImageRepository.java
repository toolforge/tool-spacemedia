package org.wikimedia.commons.donvip.spacemedia.data.local.nasa.sirs;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.local.MediaRepository;

public interface NasaSirsImageRepository extends MediaRepository<NasaSirsImage, String> {

    @Override
    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames)")
    List<NasaSirsImage> findMissingInCommons();

    @Override
    @Query("select count(*) from #{#entityName} m where not exists elements (m.commonsFileNames)")
    long countMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2")
    List<NasaSirsImage> findDuplicateInCommons();
}
