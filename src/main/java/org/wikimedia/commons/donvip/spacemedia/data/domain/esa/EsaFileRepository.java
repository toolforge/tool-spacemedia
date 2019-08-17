package org.wikimedia.commons.donvip.spacemedia.data.domain.esa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;

public interface EsaFileRepository extends MediaRepository<EsaFile, String> {

    @Override
    @Query("select f from #{#entityName} f where (f.ignored is null or f.ignored is false) and not exists elements (f.commonsFileNames)")
    List<EsaFile> findMissingInCommons();

    @Override
    @Query("select count(*) from #{#entityName} f where (f.ignored is null or f.ignored is false) and not exists elements (f.commonsFileNames)")
    long countMissingInCommons();

    @Override
    @Query("select f from #{#entityName} f where size (f.commonsFileNames) >= 2")
    List<EsaFile> findDuplicateInCommons();
}
