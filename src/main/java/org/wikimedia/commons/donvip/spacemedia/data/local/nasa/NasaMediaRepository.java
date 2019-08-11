package org.wikimedia.commons.donvip.spacemedia.data.local.nasa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.local.MediaRepository;

public interface NasaMediaRepository<T extends NasaMedia> extends MediaRepository<T, String> {

    @Override
    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames)")
    List<T> findMissingInCommons();

    @Override
    @Query("select count(*) from #{#entityName} m where not exists elements (m.commonsFileNames)")
    long countMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2")
    List<T> findDuplicateInCommons();

    @Query("select distinct(center) from #{#entityName}")
    List<String> listCenters();

    List<T> findByCenter(String center);

    long countByCenter(String center);

    @Query("select m from #{#entityName} m where not exists elements (m.commonsFileNames) and m.center = ?1")
    List<T> findMissingInCommonsByCenter(String center);

    @Query("select count(*) from #{#entityName} m where not exists elements (m.commonsFileNames) and m.center = ?1")
    long countMissingInCommonsByCenter(String center);

    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2 and m.center = ?1")
    List<T> findDuplicateInCommonsByCenter(String center);
}
