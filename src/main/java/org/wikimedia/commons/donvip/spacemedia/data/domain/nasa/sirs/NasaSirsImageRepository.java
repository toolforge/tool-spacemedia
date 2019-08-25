package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;

public interface NasaSirsImageRepository extends MediaRepository<NasaSirsImage, String> {

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames)")
    List<NasaSirsImage> findMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames)")
    Page<NasaSirsImage> findMissingInCommons(Pageable page);

    @Override
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and not exists elements (m.commonsFileNames)")
    long countMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames)")
    List<NasaSirsImage> findUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where exists elements (m.commonsFileNames)")
    Page<NasaSirsImage> findUploadedToCommons(Pageable page);

    @Override
    @Query("select count(*) from #{#entityName} m where exists elements (m.commonsFileNames)")
    long countUploadedToCommons();

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2")
    List<NasaSirsImage> findDuplicateInCommons();
}
