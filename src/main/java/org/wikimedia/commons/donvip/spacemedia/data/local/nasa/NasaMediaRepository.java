package org.wikimedia.commons.donvip.spacemedia.data.local.nasa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface NasaMediaRepository<T extends NasaMedia> extends CrudRepository<T, String> {

    @Query("select distinct m from NasaMedia m") // TODO
    List<NasaMedia> findMissingInCommons();

    @Query("select distinct m from NasaMedia m") // TODO
    List<NasaMedia> findDuplicateInCommons();
}
