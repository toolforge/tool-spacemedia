package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface FullResMediaRepository<T extends FullResMedia, ID> extends MediaRepository<T, ID> {

    int countByFullResSha1(String sha1);

    List<T> findByFullResAssetUrl(URL imageUrl);

    Optional<T> findByFullResSha1(String sha1);

    @Override
    @Query("select m from #{#entityName} m where (m.ignored is null or m.ignored is false) and not (exists elements (m.commonsFileNames) and exists elements (m.fullResCommonsFileNames))")
    List<T> findMissingInCommons();

    @Override
    @Query("select count(*) from #{#entityName} m where (m.ignored is null or m.ignored is false) and not (exists elements (m.commonsFileNames) and exists elements (m.fullResCommonsFileNames))")
    long countMissingInCommons();

    @Override
    @Query("select m from #{#entityName} m where size (m.commonsFileNames) >= 2 or size (m.fullResCommonsFileNames) >= 2")
    List<T> findDuplicateInCommons();
}
