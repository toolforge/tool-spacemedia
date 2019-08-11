package org.wikimedia.commons.donvip.spacemedia.data.local.esa;

import java.net.URL;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.wikimedia.commons.donvip.spacemedia.data.local.MediaRepository;

public interface EsaFileRepository extends MediaRepository<EsaFile, String> {

    List<EsaFile> findByUrl(URL imageUrl);

    List<EsaFile> findByIgnoredTrue();

    @Override
    @Query("select f from EsaFile f where (f.ignored is null or f.ignored is false) and not exists elements (f.commonsFileNames)")
    List<EsaFile> findMissingInCommons();

    @Override
    @Query("select count(*) from EsaFile f where (f.ignored is null or f.ignored is false) and not exists elements (f.commonsFileNames)")
    long countMissingInCommons();

    @Override
    @Query("select f from EsaFile f where size (f.commonsFileNames) >= 2")
    List<EsaFile> findDuplicateInCommons();
}
