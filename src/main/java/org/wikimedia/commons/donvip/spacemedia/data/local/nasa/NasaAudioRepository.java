package org.wikimedia.commons.donvip.spacemedia.data.local.nasa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.local.MediaRepository;

public interface NasaAudioRepository extends MediaRepository<NasaAudio, String> {

    @Override
    @Query("select m from NasaAudio m where not exists elements (m.commonsFileNames)")
    List<NasaAudio> findMissingInCommons();

    @Override
    @Query("select count(*) from NasaAudio m where not exists elements (m.commonsFileNames)")
    long countMissingInCommons();

    @Override
    @Query("select m from NasaAudio m where size (m.commonsFileNames) >= 2")
    List<NasaAudio> findDuplicateInCommons();
}
